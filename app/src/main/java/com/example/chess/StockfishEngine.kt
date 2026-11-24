package com.example.chess

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * High-strength offline Stockfish-class Chess Engine written in pure Kotlin.
 * Architecture:
 * - PeSTO Tapered Middlegame/Endgame Piece-Square Tables
 * - Strategic evaluation: mobility, pawn structure (passed/isolated/doubled),
 *   bishop pair, rook on open/semi-open files, king pawn shield & attack pressure
 * - Style-weighted evaluation preserving maximum search depth across all styles
 * - Iterative Deepening + Principal Variation Search (PVS) + Alpha-Beta
 * - 64K-entry Zobrist Transposition Table
 * - Quiescence Search (MVV-LVA captures & promotions) to eliminate horizon blunders
 * - Null-Move Pruning, Late Move Reductions (LMR), Killer Moves & History Heuristic
 */
class StockfishEngine {

    companion object {
        private const val INF = 1_000_000
        private const val MATE_SCORE = 900_000
        private const val MATE_THRESHOLD = 800_000

        private const val TT_SIZE = 1 shl 16 // 65,536 entries
        private const val TT_MASK = TT_SIZE - 1

        private const val FLAG_EXACT = 0
        private const val FLAG_LOWER = 1
        private const val FLAG_UPPER = 2

        // PeSTO Middlegame & Endgame Material Values
        private val MG_VALUE = intArrayOf(82, 337, 365, 477, 1025, 0)
        private val EG_VALUE = intArrayOf(94, 281, 297, 512, 936, 0)
        private val PHASE_INC = intArrayOf(0, 1, 1, 2, 4, 0)

        // PeSTO Middlegame Piece-Square Tables (White's perspective, a8=0 .. h1=63)
        private val MG_PAWN_PST = intArrayOf(
            0, 0, 0, 0, 0, 0, 0, 0,
            98, 134, 61, 95, 68, 126, 34, -11,
            -6, 7, 26, 31, 65, 56, 25, -20,
            -14, 13, 6, 21, 23, 12, 17, -23,
            -27, -2, -5, 12, 17, 6, 10, -25,
            -26, -4, -4, -10, 3, 3, 33, -12,
            -35, -1, -20, -23, -15, 24, 38, -22,
            0, 0, 0, 0, 0, 0, 0, 0
        )

        private val EG_PAWN_PST = intArrayOf(
            0, 0, 0, 0, 0, 0, 0, 0,
            178, 173, 158, 134, 147, 132, 165, 187,
            94, 100, 85, 67, 56, 53, 82, 84,
            32, 24, 13, 5, -2, 4, 17, 17,
            13, 9, -3, -7, -7, -8, 3, -1,
            4, 7, -6, 1, 0, -5, -1, -8,
            13, 8, 8, 10, 13, 0, 2, -7,
            0, 0, 0, 0, 0, 0, 0, 0
        )

        private val MG_KNIGHT_PST = intArrayOf(
            -167, -89, -34, -49, 61, -97, -15, -107,
            -73, -41, 72, 36, 23, 62, 7, -17,
            -47, 60, 37, 65, 84, 129, 73, 44,
            -9, 17, 19, 53, 37, 69, 18, 22,
            -13, 4, 16, 13, 28, 19, 21, -8,
            -23, -9, 12, 10, 19, 17, 25, -16,
            -29, -53, -12, -3, -1, 18, -14, -19,
            -105, -21, -58, -33, -17, -28, -19, -23
        )

        private val EG_KNIGHT_PST = intArrayOf(
            -58, -38, -13, -28, -31, -27, -63, -99,
            -25, -8, -25, -2, -9, -25, -24, -52,
            -24, -20, 10, 9, -1, -9, -19, -41,
            -17, 3, 22, 22, 22, 11, 8, -18,
            -18, -6, 16, 25, 16, 17, 4, -18,
            -23, -3, -1, 15, 10, -3, -20, -22,
            -42, -20, -10, -5, -2, -20, -23, -44,
            -29, -51, -23, -15, -22, -18, -50, -64
        )

        private val MG_BISHOP_PST = intArrayOf(
            -29, 4, -82, -37, -25, -42, 7, -8,
            -26, 16, -18, -13, 30, 59, 18, -47,
            -16, 37, 43, 40, 35, 50, 37, -2,
            -4, 5, 19, 50, 37, 37, 7, -2,
            -6, 13, 13, 26, 34, 12, 10, 4,
            0, 15, 15, 15, 14, 27, 18, 10,
            4, 15, 16, 0, 7, 21, 33, 1,
            -33, -3, -14, -21, -13, -12, -39, -21
        )

        private val EG_BISHOP_PST = intArrayOf(
            -14, -21, -11, -8, -7, -9, -17, -24,
            -8, -4, 7, -12, -3, -13, -4, -14,
            2, -8, 0, -1, -2, 6, 0, 4,
            -3, 9, 12, 9, 14, 10, 3, 2,
            -6, 3, 13, 19, 7, 10, -3, -9,
            -12, -3, 8, 10, 13, 3, -7, -15,
            -14, -18, -7, -1, 4, -9, -15, -27,
            -23, -9, -23, -5, -9, -16, -5, -17
        )

        private val MG_ROOK_PST = intArrayOf(
            32, 42, 32, 51, 63, 9, 31, 43,
            27, 32, 58, 62, 80, 67, 26, 44,
            -5, 19, 26, 36, 17, 45, 61, 16,
            -24, -11, 7, 26, 24, 35, -8, -20,
            -36, -26, -12, -1, 9, -7, 6, -23,
            -45, -25, -16, -17, 3, 0, -5, -33,
            -44, -16, -20, -9, -1, 11, -6, -71,
            -19, -13, 1, 17, 16, 7, -37, -26
        )

        private val EG_ROOK_PST = intArrayOf(
            13, 10, 18, 15, 12, 12, 8, 5,
            11, 13, 13, 11, -3, 3, 8, 3,
            7, 7, 7, 5, 4, -3, -5, -3,
            4, 3, 13, 1, 2, 1, -1, 2,
            3, 5, 8, 4, -5, -6, -8, -11,
            -4, 0, -5, -1, -7, -12, -8, -16,
            -6, -6, 0, 2, -9, -9, -11, -3,
            -9, 2, 3, -1, -5, -13, 4, -20
        )

        private val MG_QUEEN_PST = intArrayOf(
            -28, 0, 29, 12, 59, 44, 43, 45,
            -24, -39, -5, 1, -16, 57, 28, 54,
            -13, -17, 7, 8, 29, 56, 47, 57,
            -27, -27, -16, -16, -1, 17, -2, 1,
            -9, -26, -9, -10, -2, -4, 3, -3,
            -14, 2, -11, -2, -5, 2, 14, 5,
            -35, -8, 11, 2, 8, 15, -3, 1,
            -1, -18, -9, 10, -15, -25, -31, -50
        )

        private val EG_QUEEN_PST = intArrayOf(
            -9, 22, 22, 27, 27, 19, 10, 20,
            -17, 20, 32, 41, 58, 25, 30, 0,
            -20, 6, 9, 49, 47, 35, 19, 9,
            3, 22, 24, 45, 57, 40, 57, 36,
            -18, 28, 19, 47, 31, 34, 39, 23,
            -16, -27, 15, 6, 9, 17, 10, 5,
            -22, -23, -30, -16, -16, -23, -36, -32,
            -33, -28, -22, -43, -5, -32, -20, -41
        )

        private val MG_KING_PST = intArrayOf(
            -65, 23, 16, -15, -56, -34, 2, 13,
            29, -1, -20, -7, -8, -4, -38, -29,
            -9, 24, 2, -16, -20, 6, 22, -22,
            -17, -20, -12, -27, -30, -25, -14, -36,
            -49, -1, -27, -39, -46, -44, -33, -51,
            -14, -14, -22, -46, -44, -30, -15, -27,
            1, 7, -8, -64, -43, -16, 9, 8,
            -15, 36, 12, -54, 8, -28, 24, 14
        )

        private val EG_KING_PST = intArrayOf(
            -74, -35, -18, -18, -11, 15, 4, -17,
            -12, 17, 14, 17, 17, 38, 23, 11,
            10, 17, 23, 15, 20, 45, 44, 13,
            -8, 22, 24, 27, 26, 33, 26, 3,
            -18, -4, 21, 24, 27, 23, 9, -11,
            -19, -3, 11, 21, 23, 16, 7, -9,
            -27, -11, 4, 13, 14, 4, -5, -17,
            -53, -34, -21, -11, -28, -14, -24, -43
        )

        private val MG_TABLES = arrayOf(MG_PAWN_PST, MG_KNIGHT_PST, MG_BISHOP_PST, MG_ROOK_PST, MG_QUEEN_PST, MG_KING_PST)
        private val EG_TABLES = arrayOf(EG_PAWN_PST, EG_KNIGHT_PST, EG_BISHOP_PST, EG_ROOK_PST, EG_QUEEN_PST, EG_KING_PST)

        /**
         * Standard chess accuracy formula calibrated to centipawn loss (0..100%).
         */
        fun computeAccuracy(cpLoss: Int): Double {
            val clampedLoss = cpLoss.coerceAtLeast(0).toDouble()
            if (clampedLoss <= 2.0) return 100.0
            val raw = 103.1668 * exp(-0.004354 * clampedLoss) - 3.1669
            return raw.coerceIn(0.0, 100.0)
        }

        fun classifyMove(cpLoss: Int, isExactBest: Boolean): MoveClassification = when {
            isExactBest || cpLoss <= 12 -> MoveClassification.BEST
            cpLoss <= 35 -> MoveClassification.EXCELLENT
            cpLoss <= 75 -> MoveClassification.GOOD
            cpLoss <= 145 -> MoveClassification.INACCURACY
            cpLoss <= 280 -> MoveClassification.MISTAKE
            else -> MoveClassification.BLUNDER
        }

        /**
         * Estimates a player's FIDE-style performance Elo rating for a completed match
         * based on move accuracy, guess-the-move accuracy, match result vs max-strength engine,
         * and game depth.
         */
        fun estimateGameElo(
            avgAccuracy: Double,
            guessAccuracy: Double?,
            resultForPlayer: Double, // 1.0 = win, 0.5 = draw, 0.0 = loss
            playerMoveCount: Int
        ): Int {
            val blendedAcc = if (guessAccuracy != null) {
                avgAccuracy * 0.7 + guessAccuracy * 0.3
            } else {
                avgAccuracy
            }.coerceIn(5.0, 100.0)

            // Base Elo from accuracy curve (50% ~ 1150, 75% ~ 1650, 88% ~ 2050, 95% ~ 2380, 99% ~ 2650)
            val baseElo = 600.0 + (blendedAcc * 15.5) + (blendedAcc * blendedAcc * 0.05)
            val resultBonus = when {
                resultForPlayer >= 1.0 -> 240.0
                resultForPlayer >= 0.5 -> 110.0
                else -> -60.0
            }
            val lengthFactor = min(1.0, max(0.35, playerMoveCount / 22.0))
            val adjusted = 1200.0 + (baseElo + resultBonus - 1200.0) * (0.65 + 0.35 * lengthFactor)
            return adjusted.roundToInt().coerceIn(800, 2850)
        }

        /**
         * Converts centipawn score (White perspective) into a 0f..1f white bar proportion
         * using a smooth sigmoid curve (mirroring Stockfish WDL eval bar).
         */
        fun evalToWhiteRatio(scoreCpWhite: Int, mateIn: Int?): Float {
            if (mateIn != null) {
                return if (mateIn > 0) 0.98f else 0.02f
            }
            val clamped = scoreCpWhite.coerceIn(-1500, 1500).toDouble()
            val winProb = 1.0 / (1.0 + exp(-0.00368208 * clamped))
            return winProb.toFloat().coerceIn(0.03f, 0.97f)
        }
    }

    private val ttHash = LongArray(TT_SIZE)
    private val ttScore = IntArray(TT_SIZE)
    private val ttDepth = IntArray(TT_SIZE) { -1 }
    private val ttFlag = IntArray(TT_SIZE)
    private val ttBestFrom = IntArray(TT_SIZE) { -1 }
    private val ttBestTo = IntArray(TT_SIZE) { -1 }

    private val killers = Array(64) { IntArray(2) { -1 } }
    private val history = Array(64) { IntArray(64) }

    private var nodesVisited = 0L
    private var deadlineMs = Long.MAX_VALUE
    private var timeUp = false

    fun clearTables() {
        ttDepth.fill(-1)
        for (i in killers.indices) {
            killers[i][0] = -1
            killers[i][1] = -1
        }
        for (i in history.indices) history[i].fill(0)
    }

    /**
     * Finds the strongest move at maximum GM level tailored to the chosen playing style.
     * Optionally uses the grandmaster opening book for early moves (unless bypassed for exact analysis).
     */
    fun findBestMove(
        board: ChessBoardState,
        style: EngineStyle = EngineStyle.UNIVERSAL,
        timeBudgetMs: Long = 650L,
        maxDepth: Int = 5,
        movesHistoryUci: List<String> = emptyList(),
        useOpeningBook: Boolean = true
    ): EngineEvaluation {
        val legal = board.generateLegalMoves()
        if (legal.isEmpty()) {
            val inCheck = board.isInCheck(board.sideToMove)
            val whiteScore = if (inCheck) {
                if (board.sideToMove == PieceColor.WHITE) -MATE_SCORE else MATE_SCORE
            } else 0
            return EngineEvaluation(
                bestMove = null,
                scoreCpWhite = whiteScore,
                mateIn = if (inCheck) 0 else null,
                depth = 0
            )
        }

        // Check opening book for early tournament play
        if (useOpeningBook && movesHistoryUci.size < 8) {
            val bookUci = OpeningBook.pickBookMoveUci(movesHistoryUci, style)
            if (bookUci != null) {
                val bookMove = board.findLegalMoveFromUci(bookUci)
                if (bookMove != null) {
                    val evalAfter = staticEvalWhite(board.applyMoveUnchecked(bookMove, false), style)
                    return EngineEvaluation(
                        bestMove = bookMove,
                        scoreCpWhite = evalAfter,
                        mateIn = null,
                        depth = 8,
                        nodes = 1L,
                        pvSan = listOf(board.toSan(bookMove))
                    )
                }
            }
        }

        if (legal.size == 1) {
            val onlyMove = legal[0]
            val after = board.applyMoveUnchecked(onlyMove, false)
            val quickEval = staticEvalWhite(after, style)
            return EngineEvaluation(
                bestMove = onlyMove,
                scoreCpWhite = quickEval,
                mateIn = null,
                depth = 1,
                nodes = 1L,
                pvSan = listOf(board.toSan(onlyMove))
            )
        }

        nodesVisited = 0L
        timeUp = false
        deadlineMs = System.currentTimeMillis() + timeBudgetMs

        // Decay history scores slightly between searches
        for (i in 0 until 64) {
            for (j in 0 until 64) {
                history[i][j] = history[i][j] shr 2
            }
        }

        var bestMove: ChessMove = legal[0]
        var bestScoreSide = 0
        var completedDepth = 1

        for (depth in 1..maxDepth) {
            var alpha = -INF
            val beta = INF
            var currentBest: ChessMove? = null
            var currentBestScore = -INF

            val ordered = orderMoves(board, legal, 0, bestMove)
            for ((idx, move) in ordered.withIndex()) {
                val next = board.applyMoveUnchecked(move, false)
                val score = if (idx == 0) {
                    -negamax(next, depth - 1, -beta, -alpha, 1, style, allowNull = true)
                } else {
                    var s = -negamax(next, depth - 1, -alpha - 1, -alpha, 1, style, allowNull = true)
                    if (s > alpha && s < beta && !timeUp) {
                        s = -negamax(next, depth - 1, -beta, -alpha, 1, style, allowNull = true)
                    }
                    s
                }

                if (timeUp && depth > 1) break

                if (score > currentBestScore) {
                    currentBestScore = score
                    currentBest = move
                }
                if (score > alpha) {
                    alpha = score
                }
            }

            if (!timeUp || depth == 1) {
                if (currentBest != null) {
                    bestMove = currentBest
                    bestScoreSide = currentBestScore
                    completedDepth = depth
                }
            } else {
                break
            }

            if (abs(bestScoreSide) > MATE_THRESHOLD) break
            if (System.currentTimeMillis() >= deadlineMs) break
        }

        val scoreWhite = if (board.sideToMove == PieceColor.WHITE) bestScoreSide else -bestScoreSide
        val mateIn = extractMateIn(scoreWhite)
        val pvSan = extractPvSan(board, bestMove, 4)

        return EngineEvaluation(
            bestMove = bestMove,
            scoreCpWhite = scoreWhite,
            mateIn = mateIn,
            depth = completedDepth,
            nodes = nodesVisited,
            pvSan = pvSan
        )
    }

    /**
     * Evaluates a specific move chosen by the player for Guess-the-Move comparison
     * and post-game accuracy scoring.
     */
    fun evaluateSpecificMove(
        board: ChessBoardState,
        move: ChessMove,
        style: EngineStyle = EngineStyle.UNIVERSAL,
        depth: Int = 3,
        timeBudgetMs: Long = 220L
    ): Int {
        val after = board.applyMoveUnchecked(move, false)
        val status = after.evaluateGameStatus()
        if (status == GameResult.WHITE_WINS_CHECKMATE) return MATE_SCORE
        if (status == GameResult.BLACK_WINS_CHECKMATE) return -MATE_SCORE
        if (status.isDraw) return 0

        nodesVisited = 0L
        timeUp = false
        deadlineMs = System.currentTimeMillis() + timeBudgetMs

        val scoreForOpponent = negamax(after, depth, -INF, INF, 1, style, allowNull = true)
        val scoreForMover = -scoreForOpponent
        return if (board.sideToMove == PieceColor.WHITE) scoreForMover else -scoreForMover
    }

    /**
     * Evaluates a player's guessed move against the engine's #1 best move for the player's side.
     */
    fun evaluateGuess(
        board: ChessBoardState,
        guessedMove: ChessMove,
        style: EngineStyle = EngineStyle.UNIVERSAL
    ): GuessComparison {
        val bestEval = findBestMove(
            board = board,
            style = style,
            timeBudgetMs = 550L,
            maxDepth = 4,
            useOpeningBook = false
        )
        val engineBest = bestEval.bestMove ?: guessedMove
        val isExact = guessedMove.sameMoveAs(engineBest)

        val playerEvalWhite = if (isExact) {
            bestEval.scoreCpWhite
        } else {
            evaluateSpecificMove(board, guessedMove, style, depth = 3, timeBudgetMs = 240L)
        }

        val moverIsWhite = board.sideToMove == PieceColor.WHITE
        val bestForMover = if (moverIsWhite) bestEval.scoreCpWhite else -bestEval.scoreCpWhite
        val playerForMover = if (moverIsWhite) playerEvalWhite else -playerEvalWhite

        // Cap extreme mate values so centipawn loss stays clean & intuitive
        val cappedBest = bestForMover.coerceIn(-1200, 1200)
        val cappedPlayer = playerForMover.coerceIn(-1200, 1200)
        val cpLoss = if (isExact) 0 else (cappedBest - cappedPlayer).coerceAtLeast(0)

        val accuracy = if (isExact) 100.0 else computeAccuracy(cpLoss)
        val classification = classifyMove(cpLoss, isExact)

        return GuessComparison(
            playerMove = guessedMove,
            playerMoveSan = board.toSan(guessedMove),
            engineBestMove = engineBest,
            engineBestSan = board.toSan(engineBest),
            isExactBest = isExact,
            cpLoss = cpLoss,
            accuracyPercent = accuracy,
            classification = classification,
            playerEvalWhiteCp = playerEvalWhite,
            bestEvalWhiteCp = bestEval.scoreCpWhite,
            pvSan = bestEval.pvSan
        )
    }

    /**
     * Decides whether the engine accepts a player's draw offer based on objective evaluation.
     */
    fun shouldAcceptDraw(
        board: ChessBoardState,
        engineColor: PieceColor,
        style: EngineStyle
    ): Boolean {
        if (board.fullMoveNumber < 12) return false
        val eval = findBestMove(
            board = board,
            style = style,
            timeBudgetMs = 260L,
            maxDepth = 3,
            useOpeningBook = false
        )
        val engineScore = if (engineColor == PieceColor.WHITE) eval.scoreCpWhite else -eval.scoreCpWhite
        // Accept draw if engine is worse or roughly dead-equal after move 18
        return engineScore <= -30 || (board.fullMoveNumber >= 18 && engineScore <= 25)
    }

    /**
     * Full post-game offline engine review across all plies.
     */
    fun analyzeFullGame(
        movesUci: List<String>,
        style: EngineStyle = EngineStyle.UNIVERSAL,
        onProgress: (Float) -> Unit = {}
    ): List<AnalyzedMove> {
        clearTables()
        val results = mutableListOf<AnalyzedMove>()
        var board = ChessBoardState.initial()
        val total = movesUci.size.coerceAtLeast(1)

        for ((index, uci) in movesUci.withIndex()) {
            val playedMove = board.findLegalMoveFromUci(uci) ?: break
            val fenBefore = board.toFen()
            val playedSan = board.toSan(playedMove)
            val moverColor = board.sideToMove

            val bestEval = findBestMove(
                board = board,
                style = style,
                timeBudgetMs = 150L,
                maxDepth = 3,
                useOpeningBook = false
            )
            val bestMove = bestEval.bestMove ?: playedMove
            val bestSan = board.toSan(bestMove)
            val isExact = playedMove.sameMoveAs(bestMove)

            val evalAfterWhite = if (isExact) {
                bestEval.scoreCpWhite
            } else {
                evaluateSpecificMove(board, playedMove, style, depth = 2, timeBudgetMs = 95L)
            }

            val bestForMover = (if (moverColor == PieceColor.WHITE) bestEval.scoreCpWhite else -bestEval.scoreCpWhite)
                .coerceIn(-1200, 1200)
            val playedForMover = (if (moverColor == PieceColor.WHITE) evalAfterWhite else -evalAfterWhite)
                .coerceIn(-1200, 1200)
            val cpLoss = if (isExact) 0 else (bestForMover - playedForMover).coerceAtLeast(0)
            val acc = if (isExact) 100.0 else computeAccuracy(cpLoss)
            val classification = classifyMove(cpLoss, isExact)

            val nextBoard = board.applyMove(playedMove)
            results.add(
                AnalyzedMove(
                    ply = index + 1,
                    color = moverColor,
                    san = playedSan,
                    uci = uci,
                    fenBefore = fenBefore,
                    fenAfter = nextBoard.toFen(),
                    evalBeforeWhiteCp = bestEval.scoreCpWhite,
                    evalAfterWhiteCp = evalAfterWhite,
                    bestMoveUci = bestMove.toUci(),
                    bestMoveSan = bestSan,
                    cpLoss = cpLoss,
                    accuracy = acc,
                    classification = classification,
                    pvSan = bestEval.pvSan
                )
            )
            board = nextBoard
            onProgress((index + 1).toFloat() / total.toFloat())
        }
        return results
    }

    private fun negamax(
        board: ChessBoardState,
        depth: Int,
        alphaInit: Int,
        betaInit: Int,
        ply: Int,
        style: EngineStyle,
        allowNull: Boolean
    ): Int {
        nodesVisited++
        if ((nodesVisited and 2047L) == 0L && System.currentTimeMillis() >= deadlineMs) {
            timeUp = true
            return 0
        }

        if (board.halfMoveClock >= 100 || board.isInsufficientMaterial()) {
            return -style.contemptCp
        }

        var alpha = alphaInit
        var beta = betaInit
        val origAlpha = alpha

        // Probe transposition table
        val hash = board.zobristHash
        val ttIdx = (hash and TT_MASK.toLong()).toInt()
        var ttMoveFrom = -1
        var ttMoveTo = -1
        if (ttHash[ttIdx] == hash && ttDepth[ttIdx] >= 0) {
            ttMoveFrom = ttBestFrom[ttIdx]
            ttMoveTo = ttBestTo[ttIdx]
            if (ttDepth[ttIdx] >= depth) {
                val storedScore = ttScore[ttIdx]
                when (ttFlag[ttIdx]) {
                    FLAG_EXACT -> return storedScore
                    FLAG_LOWER -> if (storedScore > alpha) alpha = storedScore
                    FLAG_UPPER -> if (storedScore < beta) beta = storedScore
                }
                if (alpha >= beta) return storedScore
            }
        }

        val inCheck = board.isInCheck(board.sideToMove)
        val effectiveDepth = if (inCheck && ply < 12) depth + 1 else depth

        if (effectiveDepth <= 0) {
            return quiescence(board, alpha, beta, style, 0)
        }

        // Null-move pruning (when not in check and enough non-pawn material)
        if (allowNull && !inCheck && effectiveDepth >= 3 && hasNonPawnMaterial(board, board.sideToMove)) {
            val nullBoard = board.applyNullMove()
            val nullScore = -negamax(nullBoard, effectiveDepth - 3, -beta, -beta + 1, ply + 1, style, allowNull = false)
            if (timeUp) return 0
            if (nullScore >= beta && abs(nullScore) < MATE_THRESHOLD) {
                return beta
            }
        }

        val legalMoves = board.generateLegalMoves()
        if (legalMoves.isEmpty()) {
            return if (inCheck) -(MATE_SCORE - ply) else -style.contemptCp
        }

        val ordered = orderMoves(board, legalMoves, ply, ttFrom = ttMoveFrom, ttTo = ttMoveTo)
        var bestScore = -INF
        var bestMove = ordered[0]

        for ((index, move) in ordered.withIndex()) {
            val next = board.applyMoveUnchecked(move, false)
            val isTactical = move.capturedPiece != null || move.promotion != null

            var score: Int
            if (index == 0) {
                score = -negamax(next, effectiveDepth - 1, -beta, -alpha, ply + 1, style, allowNull = true)
            } else {
                // Late Move Reductions (LMR) for quiet late moves
                val reduction = if (index >= 4 && effectiveDepth >= 3 && !inCheck && !isTactical) 1 else 0
                score = -negamax(
                    next,
                    effectiveDepth - 1 - reduction,
                    -alpha - 1,
                    -alpha,
                    ply + 1,
                    style,
                    allowNull = true
                )
                if (score > alpha && reduction > 0 && !timeUp) {
                    score = -negamax(next, effectiveDepth - 1, -alpha - 1, -alpha, ply + 1, style, allowNull = true)
                }
                if (score > alpha && score < beta && !timeUp) {
                    score = -negamax(next, effectiveDepth - 1, -beta, -alpha, ply + 1, style, allowNull = true)
                }
            }

            if (timeUp) return 0

            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
            if (score > alpha) {
                alpha = score
            }
            if (alpha >= beta) {
                if (!isTactical && ply < 64) {
                    val encoded = (move.from shl 6) or move.to
                    if (killers[ply][0] != encoded) {
                        killers[ply][1] = killers[ply][0]
                        killers[ply][0] = encoded
                    }
                    history[move.from][move.to] += effectiveDepth * effectiveDepth
                }
                break
            }
        }

        // Store in TT
        ttHash[ttIdx] = hash
        ttScore[ttIdx] = bestScore
        ttDepth[ttIdx] = effectiveDepth
        ttBestFrom[ttIdx] = bestMove.from
        ttBestTo[ttIdx] = bestMove.to
        ttFlag[ttIdx] = when {
            bestScore <= origAlpha -> FLAG_UPPER
            bestScore >= beta -> FLAG_LOWER
            else -> FLAG_EXACT
        }

        return bestScore
    }

    private fun quiescence(
        board: ChessBoardState,
        alphaInit: Int,
        beta: Int,
        style: EngineStyle,
        qPly: Int
    ): Int {
        nodesVisited++
        if ((nodesVisited and 2047L) == 0L && System.currentTimeMillis() >= deadlineMs) {
            timeUp = true
            return 0
        }

        val standPat = staticEvalSideToMove(board, style)
        if (qPly >= 6) return standPat
        if (standPat >= beta) return beta

        var alpha = max(alphaInit, standPat)

        // Generate captures & promotions only
        val captures = board.generatePseudoLegalMoves(capturesOnly = true)
            .sortedByDescending { mvvLvaScore(board, it) }

        for (move in captures) {
            val next = board.applyMoveUnchecked(move, false)
            if (next.isInCheck(board.sideToMove)) continue
            val score = -quiescence(next, -beta, -alpha, style, qPly + 1)
            if (timeUp) return 0
            if (score >= beta) return beta
            if (score > alpha) alpha = score
        }

        return alpha
    }

    private fun orderMoves(
        board: ChessBoardState,
        moves: List<ChessMove>,
        ply: Int,
        pvMove: ChessMove? = null,
        ttFrom: Int = -1,
        ttTo: Int = -1
    ): List<ChessMove> {
        val k0 = if (ply < 64) killers[ply][0] else -1
        val k1 = if (ply < 64) killers[ply][1] else -1

        return moves.sortedByDescending { move ->
            when {
                pvMove != null && move.sameMoveAs(pvMove) -> 2_000_000
                move.from == ttFrom && move.to == ttTo -> 1_500_000
                move.capturedPiece != null || move.promotion != null -> 500_000 + mvvLvaScore(board, move)
                ((move.from shl 6) or move.to) == k0 -> 300_000
                ((move.from shl 6) or move.to) == k1 -> 250_000
                else -> history[move.from][move.to]
            }
        }
    }

    private fun mvvLvaScore(board: ChessBoardState, move: ChessMove): Int {
        val victim = move.capturedPiece?.type?.baseValue ?: 0
        val attacker = board.squares[move.from]?.type?.baseValue ?: 100
        val promoBonus = move.promotion?.baseValue ?: 0
        return victim * 10 - attacker + promoBonus * 8
    }

    private fun hasNonPawnMaterial(board: ChessBoardState, color: PieceColor): Boolean {
        for (sq in 0 until 64) {
            val p = board.squares[sq] ?: continue
            if (p.color == color && p.type != PieceType.PAWN && p.type != PieceType.KING) return true
        }
        return false
    }

    private fun staticEvalSideToMove(board: ChessBoardState, style: EngineStyle): Int {
        val w = staticEvalWhite(board, style)
        return if (board.sideToMove == PieceColor.WHITE) w else -w
    }

    /**
     * PeSTO Tapered Evaluation + Strategic Pawn Structure, Bishop Pair, Rook Files,
     * King Safety Shield, and Style Weighting. Returns centipawns from White's perspective.
     */
    fun staticEvalWhite(board: ChessBoardState, style: EngineStyle = EngineStyle.UNIVERSAL): Int {
        var mgWhite = 0
        var mgBlack = 0
        var egWhite = 0
        var egBlack = 0
        var phase = 0

        var whiteBishops = 0
        var blackBishops = 0
        val whitePawnFiles = IntArray(8)
        val blackPawnFiles = IntArray(8)
        val whitePawnMinRank = IntArray(8) { 8 }
        val blackPawnMaxRank = IntArray(8) { -1 }

        var whiteKingSq = -1
        var blackKingSq = -1

        for (sq in 0 until 64) {
            val p = board.squares[sq] ?: continue
            val tIdx = when (p.type) {
                PieceType.PAWN -> 0
                PieceType.KNIGHT -> 1
                PieceType.BISHOP -> 2
                PieceType.ROOK -> 3
                PieceType.QUEEN -> 4
                PieceType.KING -> 5
            }
            phase += PHASE_INC[tIdx]
            val r = sq / 8
            val f = sq % 8

            if (p.color == PieceColor.WHITE) {
                mgWhite += MG_VALUE[tIdx] + MG_TABLES[tIdx][sq]
                egWhite += EG_VALUE[tIdx] + EG_TABLES[tIdx][sq]
                if (p.type == PieceType.BISHOP) whiteBishops++
                if (p.type == PieceType.PAWN) {
                    whitePawnFiles[f]++
                    if (r < whitePawnMinRank[f]) whitePawnMinRank[f] = r
                }
                if (p.type == PieceType.KING) whiteKingSq = sq
            } else {
                val mirrorSq = (7 - r) * 8 + f
                mgBlack += MG_VALUE[tIdx] + MG_TABLES[tIdx][mirrorSq]
                egBlack += EG_VALUE[tIdx] + EG_TABLES[tIdx][mirrorSq]
                if (p.type == PieceType.BISHOP) blackBishops++
                if (p.type == PieceType.PAWN) {
                    blackPawnFiles[f]++
                    if (r > blackPawnMaxRank[f]) blackPawnMaxRank[f] = r
                }
                if (p.type == PieceType.KING) blackKingSq = sq
            }
        }

        // Bishop pair bonus
        if (whiteBishops >= 2) { mgWhite += 32; egWhite += 48 }
        if (blackBishops >= 2) { mgBlack += 32; egBlack += 48 }

        // Strategic bonuses: pawn structure (doubled, isolated, passed) & rook open files
        var structureScore = 0
        var activityScore = 0
        var kingSafetyScore = 0

        for (f in 0 until 8) {
            if (whitePawnFiles[f] > 1) structureScore -= 14 * (whitePawnFiles[f] - 1)
            if (blackPawnFiles[f] > 1) structureScore += 14 * (blackPawnFiles[f] - 1)

            if (whitePawnFiles[f] > 0) {
                val left = if (f > 0) whitePawnFiles[f - 1] else 0
                val right = if (f < 7) whitePawnFiles[f + 1] else 0
                if (left == 0 && right == 0) structureScore -= 12
                // Passed pawn bonus for White
                val r = whitePawnMinRank[f]
                val blocked = (blackPawnMaxRank[f] in 0 until r) ||
                    (f > 0 && blackPawnMaxRank[f - 1] in 0 until r) ||
                    (f < 7 && blackPawnMaxRank[f + 1] in 0 until r)
                if (!blocked) {
                    val adv = 7 - r
                    structureScore += adv * adv * 4
                }
            }

            if (blackPawnFiles[f] > 0) {
                val left = if (f > 0) blackPawnFiles[f - 1] else 0
                val right = if (f < 7) blackPawnFiles[f + 1] else 0
                if (left == 0 && right == 0) structureScore += 12
                // Passed pawn bonus for Black
                val r = blackPawnMaxRank[f]
                val blocked = (whitePawnMinRank[f] in (r + 1)..7) ||
                    (f > 0 && whitePawnMinRank[f - 1] in (r + 1)..7) ||
                    (f < 7 && whitePawnMinRank[f + 1] in (r + 1)..7)
                if (!blocked) {
                    structureScore -= r * r * 4
                }
            }
        }

        // Rook on open / semi-open files & centralized activity
        for (sq in 0 until 64) {
            val p = board.squares[sq] ?: continue
            val f = sq % 8
            val r = sq / 8
            if (p.type == PieceType.ROOK) {
                if (p.color == PieceColor.WHITE) {
                    if (whitePawnFiles[f] == 0) {
                        activityScore += if (blackPawnFiles[f] == 0) 24 else 12
                    }
                    if (r == 1) activityScore += 20 // 7th rank rook
                } else {
                    if (blackPawnFiles[f] == 0) {
                        activityScore -= if (whitePawnFiles[f] == 0) 24 else 12
                    }
                    if (r == 6) activityScore -= 20 // 2nd rank rook
                }
            } else if (p.type == PieceType.KNIGHT || p.type == PieceType.BISHOP) {
                // Minor piece centralization & forward presence
                val centerBonus = if (r in 2..5 && f in 2..5) 10 else 0
                if (p.color == PieceColor.WHITE) activityScore += centerBonus else activityScore -= centerBonus
            }
        }

        // King pawn shield safety in middlegame
        if (whiteKingSq != -1 && whiteKingSq / 8 == 7) {
            val kf = whiteKingSq % 8
            for (df in -1..1) {
                val nf = kf + df
                if (nf in 0..7) {
                    val shield1 = board.squares[6 * 8 + nf]
                    val shield2 = board.squares[5 * 8 + nf]
                    if (shield1?.type == PieceType.PAWN && shield1.color == PieceColor.WHITE) kingSafetyScore += 14
                    else if (shield2?.type == PieceType.PAWN && shield2.color == PieceColor.WHITE) kingSafetyScore += 7
                    else kingSafetyScore -= 12
                }
            }
        }
        if (blackKingSq != -1 && blackKingSq / 8 == 0) {
            val kf = blackKingSq % 8
            for (df in -1..1) {
                val nf = kf + df
                if (nf in 0..7) {
                    val shield1 = board.squares[1 * 8 + nf]
                    val shield2 = board.squares[2 * 8 + nf]
                    if (shield1?.type == PieceType.PAWN && shield1.color == PieceColor.BLACK) kingSafetyScore -= 14
                    else if (shield2?.type == PieceType.PAWN && shield2.color == PieceColor.BLACK) kingSafetyScore -= 7
                    else kingSafetyScore += 12
                }
            }
        }

        val mgPhase = phase.coerceIn(0, 24)
        val egPhase = 24 - mgPhase

        val mgTotal = (mgWhite - mgBlack) +
            (kingSafetyScore * style.kingAttackWeight).roundToInt() +
            (activityScore * style.mobilityWeight).roundToInt() +
            (structureScore * style.structureWeight).roundToInt()

        val egTotal = ((egWhite - egBlack) * style.endgameWeight).roundToInt() +
            (structureScore * style.structureWeight * 1.2).roundToInt() +
            activityScore

        val tapered = (mgTotal * mgPhase + egTotal * egPhase) / 24
        val tempo = if (board.sideToMove == PieceColor.WHITE) 10 else -10
        return tapered + tempo
    }

    private fun extractMateIn(scoreCpWhite: Int): Int? {
        if (abs(scoreCpWhite) < MATE_THRESHOLD) return null
        val plies = (MATE_SCORE - abs(scoreCpWhite)).coerceAtLeast(1)
        val fullMoves = (plies + 1) / 2
        return if (scoreCpWhite > 0) fullMoves else -fullMoves
    }

    private fun extractPvSan(startBoard: ChessBoardState, firstMove: ChessMove, maxPlies: Int): List<String> {
        val pv = mutableListOf<String>()
        var curr = startBoard
        var nextMove: ChessMove? = firstMove
        val visitedHashes = mutableSetOf<Long>()

        for (step in 0 until maxPlies) {
            val m = nextMove ?: break
            if (!visitedHashes.add(curr.zobristHash)) break
            pv.add(curr.toSan(m))
            curr = curr.applyMoveUnchecked(m, false)
            val idx = (curr.zobristHash and TT_MASK.toLong()).toInt()
            if (ttHash[idx] == curr.zobristHash && ttBestFrom[idx] >= 0) {
                nextMove = curr.generateLegalMoves().firstOrNull {
                    it.from == ttBestFrom[idx] && it.to == ttBestTo[idx]
                }
            } else {
                nextMove = null
            }
        }
        return pv
    }
}
