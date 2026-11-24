package com.example.chess

import kotlin.math.abs

/**
 * Immutable-style chess board state supporting full FIDE rules:
 * castling, en passant, promotion, pins/checks, SAN/UCI/FEN, Zobrist hashing,
 * threefold repetition, 50-move rule, and insufficient material detection.
 */
class ChessBoardState(
    val squares: Array<Piece?> = Array(64) { null },
    val sideToMove: PieceColor = PieceColor.WHITE,
    val whiteCastleKingside: Boolean = true,
    val whiteCastleQueenside: Boolean = true,
    val blackCastleKingside: Boolean = true,
    val blackCastleQueenside: Boolean = true,
    val enPassantSquare: Int = -1,
    val halfMoveClock: Int = 0,
    val fullMoveNumber: Int = 1,
    val positionCounts: Map<Long, Int> = emptyMap()
) {
    val zobristHash: Long = computeZobristHash()

    fun copyWithPositionCount(): ChessBoardState {
        val updated = positionCounts.toMutableMap()
        updated[zobristHash] = (updated[zobristHash] ?: 0) + 1
        return ChessBoardState(
            squares = squares.copyOf(),
            sideToMove = sideToMove,
            whiteCastleKingside = whiteCastleKingside,
            whiteCastleQueenside = whiteCastleQueenside,
            blackCastleKingside = blackCastleKingside,
            blackCastleQueenside = blackCastleQueenside,
            enPassantSquare = enPassantSquare,
            halfMoveClock = halfMoveClock,
            fullMoveNumber = fullMoveNumber,
            positionCounts = updated
        )
    }

    fun findKing(color: PieceColor): Int {
        for (sq in 0 until 64) {
            val p = squares[sq]
            if (p != null && p.color == color && p.type == PieceType.KING) return sq
        }
        return -1
    }

    fun isInCheck(color: PieceColor = sideToMove): Boolean {
        val kingSq = findKing(color)
        if (kingSq == -1) return false
        return isSquareAttacked(kingSq, color.opposite())
    }

    fun isSquareAttacked(sq: Int, byColor: PieceColor): Boolean {
        val r = sq / 8
        val f = sq % 8

        // Pawn attacks
        val pawnRow = if (byColor == PieceColor.WHITE) r + 1 else r - 1
        if (pawnRow in 0..7) {
            if (f > 0) {
                val p = squares[pawnRow * 8 + (f - 1)]
                if (p != null && p.color == byColor && p.type == PieceType.PAWN) return true
            }
            if (f < 7) {
                val p = squares[pawnRow * 8 + (f + 1)]
                if (p != null && p.color == byColor && p.type == PieceType.PAWN) return true
            }
        }

        // Knight attacks
        for (i in KNIGHT_DR.indices) {
            val nr = r + KNIGHT_DR[i]
            val nf = f + KNIGHT_DF[i]
            if (nr in 0..7 && nf in 0..7) {
                val p = squares[nr * 8 + nf]
                if (p != null && p.color == byColor && p.type == PieceType.KNIGHT) return true
            }
        }

        // King attacks
        for (dr in -1..1) {
            for (df in -1..1) {
                if (dr == 0 && df == 0) continue
                val nr = r + dr
                val nf = f + df
                if (nr in 0..7 && nf in 0..7) {
                    val p = squares[nr * 8 + nf]
                    if (p != null && p.color == byColor && p.type == PieceType.KING) return true
                }
            }
        }

        // Rook / Queen orthogonal rays
        for (i in ROOK_DR.indices) {
            var nr = r + ROOK_DR[i]
            var nf = f + ROOK_DF[i]
            while (nr in 0..7 && nf in 0..7) {
                val p = squares[nr * 8 + nf]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) return true
                    break
                }
                nr += ROOK_DR[i]
                nf += ROOK_DF[i]
            }
        }

        // Bishop / Queen diagonal rays
        for (i in BISHOP_DR.indices) {
            var nr = r + BISHOP_DR[i]
            var nf = f + BISHOP_DF[i]
            while (nr in 0..7 && nf in 0..7) {
                val p = squares[nr * 8 + nf]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) return true
                    break
                }
                nr += BISHOP_DR[i]
                nf += BISHOP_DF[i]
            }
        }

        return false
    }

    fun generateLegalMoves(): List<ChessMove> {
        val pseudo = generatePseudoLegalMoves()
        val legal = ArrayList<ChessMove>(pseudo.size)
        for (move in pseudo) {
            val next = applyMoveUnchecked(move, trackHistory = false)
            if (!next.isInCheck(sideToMove)) {
                legal.add(move)
            }
        }
        return legal
    }

    fun generateLegalMovesFrom(fromSq: Int): List<ChessMove> {
        val p = squares[fromSq] ?: return emptyList()
        if (p.color != sideToMove) return emptyList()
        return generateLegalMoves().filter { it.from == fromSq }
    }

    fun generatePseudoLegalMoves(capturesOnly: Boolean = false): List<ChessMove> {
        val moves = ArrayList<ChessMove>(48)
        val us = sideToMove
        val them = us.opposite()

        for (sq in 0 until 64) {
            val piece = squares[sq] ?: continue
            if (piece.color != us) continue
            val r = sq / 8
            val f = sq % 8

            when (piece.type) {
                PieceType.PAWN -> {
                    val dir = if (us == PieceColor.WHITE) -1 else 1
                    val startRow = if (us == PieceColor.WHITE) 6 else 1
                    val promoRow = if (us == PieceColor.WHITE) 0 else 7

                    // Forward pushes
                    val oneStepRow = r + dir
                    if (oneStepRow in 0..7) {
                        val oneSq = oneStepRow * 8 + f
                        if (squares[oneSq] == null) {
                            if (oneStepRow == promoRow) {
                                addPromotions(moves, sq, oneSq, null)
                            } else if (!capturesOnly) {
                                moves.add(ChessMove(sq, oneSq))
                                if (r == startRow) {
                                    val twoSq = (r + 2 * dir) * 8 + f
                                    if (squares[twoSq] == null) {
                                        moves.add(ChessMove(sq, twoSq))
                                    }
                                }
                            }
                        }

                        // Diagonal captures & en passant
                        for (df in intArrayOf(-1, 1)) {
                            val nf = f + df
                            if (nf in 0..7) {
                                val targetSq = oneStepRow * 8 + nf
                                val targetPiece = squares[targetSq]
                                if (targetPiece != null && targetPiece.color == them) {
                                    if (oneStepRow == promoRow) {
                                        addPromotions(moves, sq, targetSq, targetPiece)
                                    } else {
                                        moves.add(ChessMove(sq, targetSq, capturedPiece = targetPiece))
                                    }
                                } else if (targetSq == enPassantSquare) {
                                    val capturedSq = r * 8 + nf
                                    moves.add(
                                        ChessMove(
                                            from = sq,
                                            to = targetSq,
                                            isEnPassant = true,
                                            capturedPiece = squares[capturedSq]
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                PieceType.KNIGHT -> {
                    for (i in KNIGHT_DR.indices) {
                        val nr = r + KNIGHT_DR[i]
                        val nf = f + KNIGHT_DF[i]
                        if (nr in 0..7 && nf in 0..7) {
                            val toSq = nr * 8 + nf
                            val target = squares[toSq]
                            if (target == null) {
                                if (!capturesOnly) moves.add(ChessMove(sq, toSq))
                            } else if (target.color == them) {
                                moves.add(ChessMove(sq, toSq, capturedPiece = target))
                            }
                        }
                    }
                }

                PieceType.BISHOP -> addSlidingMoves(moves, sq, r, f, BISHOP_DR, BISHOP_DF, them, capturesOnly)
                PieceType.ROOK -> addSlidingMoves(moves, sq, r, f, ROOK_DR, ROOK_DF, them, capturesOnly)
                PieceType.QUEEN -> {
                    addSlidingMoves(moves, sq, r, f, BISHOP_DR, BISHOP_DF, them, capturesOnly)
                    addSlidingMoves(moves, sq, r, f, ROOK_DR, ROOK_DF, them, capturesOnly)
                }

                PieceType.KING -> {
                    for (dr in -1..1) {
                        for (df in -1..1) {
                            if (dr == 0 && df == 0) continue
                            val nr = r + dr
                            val nf = f + df
                            if (nr in 0..7 && nf in 0..7) {
                                val toSq = nr * 8 + nf
                                val target = squares[toSq]
                                if (target == null) {
                                    if (!capturesOnly) moves.add(ChessMove(sq, toSq))
                                } else if (target.color == them) {
                                    moves.add(ChessMove(sq, toSq, capturedPiece = target))
                                }
                            }
                        }
                    }

                    if (!capturesOnly && !isSquareAttacked(sq, them)) {
                        if (us == PieceColor.WHITE && sq == 60) {
                            if (whiteCastleKingside &&
                                squares[61] == null && squares[62] == null &&
                                squares[63] == Piece(PieceColor.WHITE, PieceType.ROOK) &&
                                !isSquareAttacked(61, them) && !isSquareAttacked(62, them)
                            ) {
                                moves.add(ChessMove(60, 62, isCastleKingside = true))
                            }
                            if (whiteCastleQueenside &&
                                squares[59] == null && squares[58] == null && squares[57] == null &&
                                squares[56] == Piece(PieceColor.WHITE, PieceType.ROOK) &&
                                !isSquareAttacked(59, them) && !isSquareAttacked(58, them)
                            ) {
                                moves.add(ChessMove(60, 58, isCastleQueenside = true))
                            }
                        } else if (us == PieceColor.BLACK && sq == 4) {
                            if (blackCastleKingside &&
                                squares[5] == null && squares[6] == null &&
                                squares[7] == Piece(PieceColor.BLACK, PieceType.ROOK) &&
                                !isSquareAttacked(5, them) && !isSquareAttacked(6, them)
                            ) {
                                moves.add(ChessMove(4, 6, isCastleKingside = true))
                            }
                            if (blackCastleQueenside &&
                                squares[3] == null && squares[2] == null && squares[1] == null &&
                                squares[0] == Piece(PieceColor.BLACK, PieceType.ROOK) &&
                                !isSquareAttacked(3, them) && !isSquareAttacked(2, them)
                            ) {
                                moves.add(ChessMove(4, 2, isCastleQueenside = true))
                            }
                        }
                    }
                }
            }
        }
        return moves
    }

    private fun addPromotions(
        moves: MutableList<ChessMove>,
        from: Int,
        to: Int,
        captured: Piece?
    ) {
        moves.add(ChessMove(from, to, promotion = PieceType.QUEEN, capturedPiece = captured))
        moves.add(ChessMove(from, to, promotion = PieceType.ROOK, capturedPiece = captured))
        moves.add(ChessMove(from, to, promotion = PieceType.BISHOP, capturedPiece = captured))
        moves.add(ChessMove(from, to, promotion = PieceType.KNIGHT, capturedPiece = captured))
    }

    private fun addSlidingMoves(
        moves: MutableList<ChessMove>,
        sq: Int,
        r: Int,
        f: Int,
        drs: IntArray,
        dfs: IntArray,
        them: PieceColor,
        capturesOnly: Boolean
    ) {
        for (i in drs.indices) {
            var nr = r + drs[i]
            var nf = f + dfs[i]
            while (nr in 0..7 && nf in 0..7) {
                val toSq = nr * 8 + nf
                val target = squares[toSq]
                if (target == null) {
                    if (!capturesOnly) moves.add(ChessMove(sq, toSq))
                } else {
                    if (target.color == them) {
                        moves.add(ChessMove(sq, toSq, capturedPiece = target))
                    }
                    break
                }
                nr += drs[i]
                nf += dfs[i]
            }
        }
    }

    fun applyMove(move: ChessMove): ChessBoardState = applyMoveUnchecked(move, trackHistory = true)

    fun applyMoveUnchecked(move: ChessMove, trackHistory: Boolean): ChessBoardState {
        val nextSquares = squares.copyOf()
        val movingPiece = nextSquares[move.from]
            ?: return this

        val isCapture = nextSquares[move.to] != null || move.isEnPassant
        nextSquares[move.from] = null

        if (move.isEnPassant) {
            val capturedRow = move.from / 8
            val capturedFile = move.to % 8
            nextSquares[capturedRow * 8 + capturedFile] = null
        }

        if (move.promotion != null) {
            nextSquares[move.to] = Piece(movingPiece.color, move.promotion)
        } else {
            nextSquares[move.to] = movingPiece
        }

        if (move.isCastleKingside) {
            if (movingPiece.color == PieceColor.WHITE) {
                nextSquares[63] = null
                nextSquares[61] = Piece(PieceColor.WHITE, PieceType.ROOK)
            } else {
                nextSquares[7] = null
                nextSquares[5] = Piece(PieceColor.BLACK, PieceType.ROOK)
            }
        } else if (move.isCastleQueenside) {
            if (movingPiece.color == PieceColor.WHITE) {
                nextSquares[56] = null
                nextSquares[59] = Piece(PieceColor.WHITE, PieceType.ROOK)
            } else {
                nextSquares[0] = null
                nextSquares[3] = Piece(PieceColor.BLACK, PieceType.ROOK)
            }
        }

        var wK = whiteCastleKingside
        var wQ = whiteCastleQueenside
        var bK = blackCastleKingside
        var bQ = blackCastleQueenside

        if (move.from == 60 || move.to == 60) { wK = false; wQ = false }
        if (move.from == 4 || move.to == 4) { bK = false; bQ = false }
        if (move.from == 63 || move.to == 63) wK = false
        if (move.from == 56 || move.to == 56) wQ = false
        if (move.from == 7 || move.to == 7) bK = false
        if (move.from == 0 || move.to == 0) bQ = false

        val nextEp = if (movingPiece.type == PieceType.PAWN && abs((move.from / 8) - (move.to / 8)) == 2) {
            (move.from + move.to) / 2
        } else {
            -1
        }

        val nextHalfMove = if (movingPiece.type == PieceType.PAWN || isCapture) 0 else halfMoveClock + 1
        val nextFullMove = if (sideToMove == PieceColor.BLACK) fullMoveNumber + 1 else fullMoveNumber

        val nextBoard = ChessBoardState(
            squares = nextSquares,
            sideToMove = sideToMove.opposite(),
            whiteCastleKingside = wK,
            whiteCastleQueenside = wQ,
            blackCastleKingside = bK,
            blackCastleQueenside = bQ,
            enPassantSquare = nextEp,
            halfMoveClock = nextHalfMove,
            fullMoveNumber = nextFullMove,
            positionCounts = if (trackHistory) positionCounts else emptyMap()
        )
        return if (trackHistory) nextBoard.copyWithPositionCount() else nextBoard
    }

    fun applyNullMove(): ChessBoardState {
        return ChessBoardState(
            squares = squares.copyOf(),
            sideToMove = sideToMove.opposite(),
            whiteCastleKingside = whiteCastleKingside,
            whiteCastleQueenside = whiteCastleQueenside,
            blackCastleKingside = blackCastleKingside,
            blackCastleQueenside = blackCastleQueenside,
            enPassantSquare = -1,
            halfMoveClock = halfMoveClock + 1,
            fullMoveNumber = if (sideToMove == PieceColor.BLACK) fullMoveNumber + 1 else fullMoveNumber,
            positionCounts = emptyMap()
        )
    }

    fun findLegalMoveFromUci(uci: String): ChessMove? {
        if (uci.length < 4) return null
        val from = ChessMove.algebraicToSquare(uci.substring(0, 2))
        val to = ChessMove.algebraicToSquare(uci.substring(2, 4))
        val promo = if (uci.length >= 5) PieceType.fromChar(uci[4]) else null
        return generateLegalMoves().firstOrNull {
            it.from == from && it.to == to && it.promotion == promo
        }
    }

    fun toSan(move: ChessMove): String {
        val moving = squares[move.from] ?: return move.toUci()
        val sb = StringBuilder()

        if (move.isCastleKingside) {
            sb.append("O-O")
        } else if (move.isCastleQueenside) {
            sb.append("O-O-O")
        } else {
            val isCapture = squares[move.to] != null || move.isEnPassant
            if (moving.type != PieceType.PAWN) {
                sb.append(moving.type.symbol)
                val ambiguous = generateLegalMoves().filter {
                    it.from != move.from &&
                        it.to == move.to &&
                        squares[it.from]?.type == moving.type
                }
                if (ambiguous.isNotEmpty()) {
                    val sameFile = ambiguous.any { it.from % 8 == move.from % 8 }
                    val sameRank = ambiguous.any { it.from / 8 == move.from / 8 }
                    val fromFile = 'a' + (move.from % 8)
                    val fromRank = '8' - (move.from / 8)
                    when {
                        !sameFile -> sb.append(fromFile)
                        !sameRank -> sb.append(fromRank)
                        else -> sb.append(fromFile).append(fromRank)
                    }
                }
            } else if (isCapture) {
                sb.append('a' + (move.from % 8))
            }

            if (isCapture) sb.append('x')
            sb.append(ChessMove.squareToAlgebraic(move.to))
            if (move.promotion != null) {
                sb.append('=').append(move.promotion.symbol)
            }
        }

        val after = applyMoveUnchecked(move, trackHistory = false)
        if (after.isInCheck(after.sideToMove)) {
            val oppMoves = after.generateLegalMoves()
            sb.append(if (oppMoves.isEmpty()) '#' else '+')
        }
        return sb.toString()
    }

    fun evaluateGameStatus(): GameResult {
        val legal = generateLegalMoves()
        if (legal.isEmpty()) {
            return if (isInCheck(sideToMove)) {
                if (sideToMove == PieceColor.WHITE) GameResult.BLACK_WINS_CHECKMATE
                else GameResult.WHITE_WINS_CHECKMATE
            } else {
                GameResult.DRAW_STALEMATE
            }
        }
        if ((positionCounts[zobristHash] ?: 0) >= 3) {
            return GameResult.DRAW_REPETITION
        }
        if (halfMoveClock >= 100) {
            return GameResult.DRAW_FIFTY_MOVE
        }
        if (isInsufficientMaterial()) {
            return GameResult.DRAW_INSUFFICIENT_MATERIAL
        }
        return GameResult.IN_PROGRESS
    }

    fun isInsufficientMaterial(): Boolean {
        val whitePieces = mutableListOf<Pair<PieceType, Int>>()
        val blackPieces = mutableListOf<Pair<PieceType, Int>>()
        for (sq in 0 until 64) {
            val p = squares[sq] ?: continue
            if (p.type == PieceType.KING) continue
            if (p.color == PieceColor.WHITE) whitePieces.add(p.type to sq)
            else blackPieces.add(p.type to sq)
        }
        if (whitePieces.isEmpty() && blackPieces.isEmpty()) return true
        if (whitePieces.size == 1 && blackPieces.isEmpty() &&
            (whitePieces[0].first == PieceType.KNIGHT || whitePieces[0].first == PieceType.BISHOP)
        ) return true
        if (blackPieces.size == 1 && whitePieces.isEmpty() &&
            (blackPieces[0].first == PieceType.KNIGHT || blackPieces[0].first == PieceType.BISHOP)
        ) return true
        if (whitePieces.size == 1 && blackPieces.size == 1 &&
            whitePieces[0].first == PieceType.BISHOP && blackPieces[0].first == PieceType.BISHOP
        ) {
            val wSqColor = ((whitePieces[0].second / 8) + (whitePieces[0].second % 8)) % 2
            val bSqColor = ((blackPieces[0].second / 8) + (blackPieces[0].second % 8)) % 2
            if (wSqColor == bSqColor) return true
        }
        return false
    }

    fun getCapturedBy(capturingColor: PieceColor): List<PieceType> {
        val victimColor = capturingColor.opposite()
        val initialCounts = mutableMapOf(
            PieceType.QUEEN to 1,
            PieceType.ROOK to 2,
            PieceType.BISHOP to 2,
            PieceType.KNIGHT to 2,
            PieceType.PAWN to 8
        )
        for (sq in 0 until 64) {
            val p = squares[sq]
            if (p != null && p.color == victimColor && p.type != PieceType.KING) {
                initialCounts[p.type] = ((initialCounts[p.type] ?: 0) - 1).coerceAtLeast(0)
            }
        }
        val captured = mutableListOf<PieceType>()
        for (type in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT, PieceType.PAWN)) {
            repeat(initialCounts[type] ?: 0) { captured.add(type) }
        }
        return captured
    }

    fun materialAdvantageFor(color: PieceColor): Int {
        var whiteMat = 0
        var blackMat = 0
        for (sq in 0 until 64) {
            val p = squares[sq] ?: continue
            val v = when (p.type) {
                PieceType.PAWN -> 1
                PieceType.KNIGHT, PieceType.BISHOP -> 3
                PieceType.ROOK -> 5
                PieceType.QUEEN -> 9
                PieceType.KING -> 0
            }
            if (p.color == PieceColor.WHITE) whiteMat += v else blackMat += v
        }
        return if (color == PieceColor.WHITE) whiteMat - blackMat else blackMat - whiteMat
    }

    fun toFen(): String {
        val sb = StringBuilder()
        for (r in 0 until 8) {
            var empty = 0
            for (f in 0 until 8) {
                val p = squares[r * 8 + f]
                if (p == null) {
                    empty++
                } else {
                    if (empty > 0) {
                        sb.append(empty)
                        empty = 0
                    }
                    sb.append(p.fenChar)
                }
            }
            if (empty > 0) sb.append(empty)
            if (r < 7) sb.append('/')
        }
        sb.append(' ').append(if (sideToMove == PieceColor.WHITE) 'w' else 'b').append(' ')
        val castle = buildString {
            if (whiteCastleKingside) append('K')
            if (whiteCastleQueenside) append('Q')
            if (blackCastleKingside) append('k')
            if (blackCastleQueenside) append('q')
        }
        sb.append(castle.ifEmpty { "-" }).append(' ')
        sb.append(if (enPassantSquare in 0..63) ChessMove.squareToAlgebraic(enPassantSquare) else "-")
        sb.append(' ').append(halfMoveClock).append(' ').append(fullMoveNumber)
        return sb.toString()
    }

    private fun computeZobristHash(): Long {
        var h = 0L
        for (sq in 0 until 64) {
            val p = squares[sq] ?: continue
            val idx = pieceIndex(p)
            h = h xor ZOBRIST_PIECES[idx][sq]
        }
        if (sideToMove == PieceColor.BLACK) h = h xor ZOBRIST_SIDE
        if (whiteCastleKingside) h = h xor ZOBRIST_CASTLE[0]
        if (whiteCastleQueenside) h = h xor ZOBRIST_CASTLE[1]
        if (blackCastleKingside) h = h xor ZOBRIST_CASTLE[2]
        if (blackCastleQueenside) h = h xor ZOBRIST_CASTLE[3]
        if (enPassantSquare in 0..63) h = h xor ZOBRIST_EP[enPassantSquare % 8]
        return h
    }

    companion object {
        const val STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        private val KNIGHT_DR = intArrayOf(-2, -2, -1, -1, 1, 1, 2, 2)
        private val KNIGHT_DF = intArrayOf(-1, 1, -2, 2, -2, 2, -1, 1)
        private val ROOK_DR = intArrayOf(-1, 1, 0, 0)
        private val ROOK_DF = intArrayOf(0, 0, -1, 1)
        private val BISHOP_DR = intArrayOf(-1, -1, 1, 1)
        private val BISHOP_DF = intArrayOf(-1, 1, -1, 1)

        // Deterministic PRNG for Zobrist keys
        private val ZOBRIST_PIECES: Array<LongArray>
        private val ZOBRIST_CASTLE: LongArray
        private val ZOBRIST_EP: LongArray
        private val ZOBRIST_SIDE: Long

        init {
            var seed = 0x9E3779B97F4A7C15UL
            fun nextLong(): Long {
                seed = seed xor (seed shl 13)
                seed = seed xor (seed shr 7)
                seed = seed xor (seed shl 17)
                return seed.toLong()
            }
            ZOBRIST_PIECES = Array(12) { LongArray(64) { nextLong() } }
            ZOBRIST_CASTLE = LongArray(4) { nextLong() }
            ZOBRIST_EP = LongArray(8) { nextLong() }
            ZOBRIST_SIDE = nextLong()
        }

        private fun pieceIndex(p: Piece): Int {
            val base = when (p.type) {
                PieceType.PAWN -> 0
                PieceType.KNIGHT -> 1
                PieceType.BISHOP -> 2
                PieceType.ROOK -> 3
                PieceType.QUEEN -> 4
                PieceType.KING -> 5
            }
            return if (p.color == PieceColor.WHITE) base else base + 6
        }

        fun initial(): ChessBoardState = fromFen(STARTING_FEN).copyWithPositionCount()

        fun fromFen(fen: String): ChessBoardState {
            val parts = fen.trim().split(" ")
            val boardPart = parts.getOrNull(0) ?: return initial()
            val squares = Array<Piece?>(64) { null }
            val ranks = boardPart.split("/")
            for (r in 0 until minOf(8, ranks.size)) {
                var f = 0
                for (ch in ranks[r]) {
                    if (ch.isDigit()) {
                        f += ch - '0'
                    } else if (f < 8) {
                        squares[r * 8 + f] = Piece.fromFenChar(ch)
                        f++
                    }
                }
            }
            val side = if (parts.getOrNull(1) == "b") PieceColor.BLACK else PieceColor.WHITE
            val castling = parts.getOrNull(2) ?: "-"
            val ep = parts.getOrNull(3)?.let {
                if (it == "-") -1 else ChessMove.algebraicToSquare(it)
            } ?: -1
            val half = parts.getOrNull(4)?.toIntOrNull() ?: 0
            val full = parts.getOrNull(5)?.toIntOrNull() ?: 1

            return ChessBoardState(
                squares = squares,
                sideToMove = side,
                whiteCastleKingside = 'K' in castling,
                whiteCastleQueenside = 'Q' in castling,
                blackCastleKingside = 'k' in castling,
                blackCastleQueenside = 'q' in castling,
                enPassantSquare = ep,
                halfMoveClock = half,
                fullMoveNumber = full
            )
        }

        fun replayUciMoves(movesUci: List<String>): Pair<ChessBoardState, List<String>> {
            var board = initial()
            val sans = mutableListOf<String>()
            for (uci in movesUci) {
                val move = board.findLegalMoveFromUci(uci) ?: break
                sans.add(board.toSan(move))
                board = board.applyMove(move)
            }
            return board to sans
        }
    }
}
