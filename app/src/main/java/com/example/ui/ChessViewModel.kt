package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.AnalyzedMove
import com.example.chess.ArrowKind
import com.example.chess.BoardArrow
import com.example.chess.ChessBoardState
import com.example.chess.ChessMove
import com.example.chess.EngineStyle
import com.example.chess.GameResult
import com.example.chess.GuessComparison
import com.example.chess.HistoricalPuzzle
import com.example.chess.HistoricalPuzzleLibrary
import com.example.chess.OpeningBook
import com.example.chess.OpeningInfo
import com.example.chess.PieceColor
import com.example.chess.StockfishEngine
import com.example.chess.TimeControl
import com.example.data.ChessDatabase
import com.example.data.GameRecordEntity
import com.example.data.GameRepository
import com.example.data.UserPreferencesRepository
import com.example.data.UserSettings
import com.example.ui.board.BoardThemeStyle
import com.example.ui.board.PieceSetStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

enum class AppTab {
    PLAY, PUZZLES, HISTORY, STUDIO
}

sealed interface GuessPhase {
    data object Idle : GuessPhase
    data object WaitingForGuess : GuessPhase
    data object Evaluating : GuessPhase
    data class ShowingResult(val comparison: GuessComparison) : GuessPhase
}

data class ActiveGameUiState(
    val savedGameId: Long = 0L,
    val board: ChessBoardState = ChessBoardState.initial(),
    val playerColor: PieceColor = PieceColor.WHITE,
    val boardFlipped: Boolean = false,
    val engineStyle: EngineStyle = EngineStyle.UNIVERSAL,
    val timeControl: TimeControl = TimeControl.RAPID_10_5,
    val movesUci: List<String> = emptyList(),
    val movesSan: List<String> = emptyList(),
    val evalsWhiteCp: List<Int> = emptyList(),
    val playerMoveAccuracies: List<Double> = emptyList(),
    val lastMove: ChessMove? = null,
    val currentEvalWhiteCp: Int = 15,
    val currentMateIn: Int? = null,
    val opening: OpeningInfo = OpeningBook.identifyOpening(emptyList()),
    val gameResult: GameResult = GameResult.IN_PROGRESS,
    val isPaused: Boolean = false,
    val isEngineThinking: Boolean = false,
    val isGuessModeContinuous: Boolean = false,
    val guessPhase: GuessPhase = GuessPhase.Idle,
    val guessAttempts: Int = 0,
    val guessExactMatches: Int = 0,
    val guessAccuracySum: Double = 0.0,
    val whiteTimeMs: Long = TimeControl.RAPID_10_5.initialTimeMs,
    val blackTimeMs: Long = TimeControl.RAPID_10_5.initialTimeMs,
    val showGameOverSummary: Boolean = false,
    val statusBanner: String? = null
) {
    val isPlayersTurn: Boolean
        get() = !gameResult.isGameOver && !isPaused && !isEngineThinking && board.sideToMove == playerColor

    val arrows: List<BoardArrow>
        get() = when (val gp = guessPhase) {
            is GuessPhase.ShowingResult -> {
                val c = gp.comparison
                if (c.isExactBest) {
                    listOf(BoardArrow(c.playerMove.from, c.playerMove.to, ArrowKind.PERFECT_MATCH_GOLD))
                } else {
                    listOf(
                        BoardArrow(c.engineBestMove.from, c.engineBestMove.to, ArrowKind.ENGINE_BEST_GREEN),
                        BoardArrow(c.playerMove.from, c.playerMove.to, ArrowKind.PLAYER_GUESS_BLUE)
                    )
                }
            }
            else -> emptyList()
        }

    val averageMatchAccuracy: Double
        get() = if (playerMoveAccuracies.isEmpty()) 0.0 else playerMoveAccuracies.average()

    val averageGuessAccuracy: Double
        get() = if (guessAttempts == 0) 0.0 else guessAccuracySum / guessAttempts
}

data class AnalysisUiState(
    val game: GameRecordEntity? = null,
    val isAnalyzing: Boolean = false,
    val progress: Float = 0f,
    val analyzedMoves: List<AnalyzedMove> = emptyList(),
    val currentPly: Int = 0,
    val boardFlipped: Boolean = false,
    val isAutoPlaying: Boolean = false
) {
    val currentBoard: ChessBoardState
        get() {
            if (currentPly <= 0 || analyzedMoves.isEmpty()) {
                val g = game ?: return ChessBoardState.initial()
                val uci = g.movesUciList().take(currentPly)
                return ChessBoardState.replayUciMoves(uci).first
            }
            val move = analyzedMoves.getOrNull(currentPly - 1)
            return if (move != null) ChessBoardState.fromFen(move.fenAfter) else ChessBoardState.initial()
        }

    val currentAnalyzedMove: AnalyzedMove?
        get() = if (currentPly > 0) analyzedMoves.getOrNull(currentPly - 1) else null

    val currentEvalWhiteCp: Int
        get() = currentAnalyzedMove?.evalAfterWhiteCp ?: 15

    val currentArrows: List<BoardArrow>
        get() {
            val m = currentAnalyzedMove ?: return emptyList()
            val playedFrom = ChessMove.algebraicToSquare(m.uci.take(2))
            val playedTo = ChessMove.algebraicToSquare(m.uci.drop(2).take(2))
            val bestFrom = ChessMove.algebraicToSquare(m.bestMoveUci.take(2))
            val bestTo = ChessMove.algebraicToSquare(m.bestMoveUci.drop(2).take(2))
            if (playedFrom < 0 || playedTo < 0 || bestFrom < 0 || bestTo < 0) return emptyList()
            return if (m.uci.take(4) == m.bestMoveUci.take(4)) {
                listOf(BoardArrow(playedFrom, playedTo, ArrowKind.PERFECT_MATCH_GOLD))
            } else {
                listOf(
                    BoardArrow(bestFrom, bestTo, ArrowKind.ENGINE_BEST_GREEN),
                    BoardArrow(playedFrom, playedTo, ArrowKind.PLAYER_GUESS_BLUE)
                )
            }
        }

    val whiteAccuracy: Double
        get() {
            val wm = analyzedMoves.filter { it.color == PieceColor.WHITE }
            return if (wm.isEmpty()) 0.0 else wm.map { it.accuracy }.average()
        }

    val blackAccuracy: Double
        get() {
            val bm = analyzedMoves.filter { it.color == PieceColor.BLACK }
            return if (bm.isEmpty()) 0.0 else bm.map { it.accuracy }.average()
        }

    val bestMovesCount: Int
        get() = analyzedMoves.count { it.uci.take(4) == it.bestMoveUci.take(4) }
}

sealed interface PuzzleFeedback {
    data object Neutral : PuzzleFeedback
    data class HintShown(val hint: String) : PuzzleFeedback
    data class IncorrectMove(val triedSan: String, val message: String) : PuzzleFeedback
    data class StepCorrect(val san: String) : PuzzleFeedback
    data class Solved(val explanation: String) : PuzzleFeedback
}

data class PuzzleSessionUiState(
    val puzzle: HistoricalPuzzle,
    val board: ChessBoardState,
    val stepIndex: Int = 0, // Current index into solutionLineUci
    val flipped: Boolean = false,
    val lastMove: ChessMove? = null,
    val arrows: List<BoardArrow> = emptyList(),
    val evalWhiteCp: Int = 0,
    val feedback: PuzzleFeedback = PuzzleFeedback.Neutral,
    val isOpponentReplying: Boolean = false,
    val attempts: Int = 0
) {
    val isSolved: Boolean get() = feedback is PuzzleFeedback.Solved
}

class ChessViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ChessDatabase.getInstance(application)
    private val gameRepository = GameRepository(database.gameDao())
    private val prefsRepository = UserPreferencesRepository(application)
    private val engine = StockfishEngine()

    val userSettings: StateFlow<UserSettings> = prefsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val pausedGames: StateFlow<List<GameRecordEntity>> = gameRepository.pausedGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedGames: StateFlow<List<GameRecordEntity>> = gameRepository.completedGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTab = MutableStateFlow(AppTab.PLAY)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    private val _gameState = MutableStateFlow(ActiveGameUiState())
    val gameState: StateFlow<ActiveGameUiState> = _gameState.asStateFlow()

    private val _analysisState = MutableStateFlow<AnalysisUiState?>(null)
    val analysisState: StateFlow<AnalysisUiState?> = _analysisState.asStateFlow()

    private val _puzzleSession = MutableStateFlow<PuzzleSessionUiState?>(null)
    val puzzleSession: StateFlow<PuzzleSessionUiState?> = _puzzleSession.asStateFlow()

    private var clockJob: Job? = null
    private var engineJob: Job? = null
    private var analysisJob: Job? = null
    private var autoPlayJob: Job? = null
    private var puzzleReplyJob: Job? = null

    init {
        startClockTicker()
    }

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun clearStatusBanner() {
        _gameState.update { it.copy(statusBanner = null) }
    }

    fun dismissGameOverSummary() {
        _gameState.update { it.copy(showGameOverSummary = false) }
    }

    fun toggleBoardFlip() {
        _gameState.update { it.copy(boardFlipped = !it.boardFlipped) }
    }

    /**
     * Starts a fresh match against Stockfish-class offline engine at maximum strength.
     */
    fun startNewGame(
        chosenColor: PieceColor?,
        style: EngineStyle,
        timeControl: TimeControl,
        playerName: String? = null
    ) {
        engineJob?.cancel()
        engine.clearTables()

        val playerColor = chosenColor ?: if (Random.nextBoolean()) PieceColor.WHITE else PieceColor.BLACK
        viewModelScope.launch {
            prefsRepository.setEngineStyle(style)
            prefsRepository.setTimeControl(timeControl)
            if (!playerName.isNullOrBlank()) {
                prefsRepository.setPlayerName(playerName)
            }
        }

        _gameState.value = ActiveGameUiState(
            savedGameId = 0L,
            board = ChessBoardState.initial(),
            playerColor = playerColor,
            boardFlipped = (playerColor == PieceColor.BLACK),
            engineStyle = style,
            timeControl = timeControl,
            whiteTimeMs = timeControl.initialTimeMs,
            blackTimeMs = timeControl.initialTimeMs,
            isGuessModeContinuous = false,
            guessPhase = GuessPhase.Idle
        )
        _analysisState.value = null
        _puzzleSession.value = null
        _selectedTab.value = AppTab.PLAY

        if (playerColor == PieceColor.BLACK) {
            triggerEngineMove()
        }
    }

    /**
     * Resumes a previously paused & saved match from Room DB.
     */
    fun resumeSavedGame(record: GameRecordEntity) {
        engineJob?.cancel()
        engine.clearTables()

        val uciList = record.movesUciList()
        val (replayedBoard, sanList) = ChessBoardState.replayUciMoves(uciList)
        val lastUci = uciList.lastOrNull()
        val lastMove = if (lastUci != null && uciList.isNotEmpty()) {
            val prevBoard = ChessBoardState.replayUciMoves(uciList.dropLast(1)).first
            prevBoard.findLegalMoveFromUci(lastUci)
        } else null

        val evals = record.evalsCpList()
        val opening = OpeningBook.identifyOpening(uciList)

        _gameState.value = ActiveGameUiState(
            savedGameId = record.id,
            board = replayedBoard,
            playerColor = record.playerColor,
            boardFlipped = (record.playerColor == PieceColor.BLACK),
            engineStyle = record.engineStyle,
            timeControl = record.timeControl,
            movesUci = uciList,
            movesSan = sanList,
            evalsWhiteCp = evals,
            lastMove = lastMove,
            currentEvalWhiteCp = evals.lastOrNull() ?: 15,
            opening = opening,
            gameResult = record.gameResult,
            isPaused = false,
            isGuessModeContinuous = false,
            guessPhase = GuessPhase.Idle,
            guessAttempts = record.guessAttempts,
            guessExactMatches = record.guessExactMatches,
            guessAccuracySum = record.guessAvgAccuracy * record.guessAttempts,
            whiteTimeMs = record.whiteTimeMs,
            blackTimeMs = record.blackTimeMs,
            statusBanner = "Resumed saved match (${opening.eco} ${opening.name})"
        )
        _analysisState.value = null
        _puzzleSession.value = null
        _selectedTab.value = AppTab.PLAY

        if (!record.gameResult.isGameOver && replayedBoard.sideToMove != record.playerColor) {
            triggerEngineMove()
        }
    }

    /**
     * Starts continuous Guess the Move mode: the current move and EVERY succeeding player move
     * will be Guess the Move until the player taps "Stop Guess the Move".
     */
    fun startGuessTheMoveMode() {
        val state = _gameState.value
        if (state.gameResult.isGameOver || state.isPaused) return
        _gameState.update { current ->
            current.copy(
                isGuessModeContinuous = true,
                guessPhase = if (current.isPlayersTurn) GuessPhase.WaitingForGuess else GuessPhase.Idle,
                statusBanner = null
            )
        }
    }

    /**
     * Stops continuous Guess the Move mode so the game continues seamlessly as normal play.
     */
    fun stopGuessTheMoveMode() {
        val state = _gameState.value
        if (state.guessPhase is GuessPhase.Evaluating) {
            engineJob?.cancel()
        }
        _gameState.update {
            it.copy(
                isGuessModeContinuous = false,
                guessPhase = GuessPhase.Idle,
                statusBanner = "Guess the Move stopped — continuing match as ${userSettings.value.displayPlayerName}."
            )
        }
    }

    fun retryCurrentGuess() {
        val state = _gameState.value
        if (!state.isPlayersTurn) return
        _gameState.update {
            it.copy(
                isGuessModeContinuous = true,
                guessPhase = GuessPhase.WaitingForGuess
            )
        }
    }

    /**
     * Called when the player taps or drags a legal move on the board.
     * Routes to Guess-the-Move evaluation when continuous Guess Mode is active, or direct play otherwise.
     */
    fun onPlayerBoardMove(move: ChessMove) {
        val state = _gameState.value
        if (!state.isPlayersTurn) return

        if (state.isGuessModeContinuous || state.guessPhase is GuessPhase.WaitingForGuess) {
            evaluatePlayerGuess(move)
        } else if (state.guessPhase is GuessPhase.Idle) {
            commitPlayerMove(move, precomputedAccuracy = null)
        }
    }

    private fun evaluatePlayerGuess(guessedMove: ChessMove) {
        val state = _gameState.value
        _gameState.update { it.copy(guessPhase = GuessPhase.Evaluating) }

        engineJob?.cancel()
        engineJob = viewModelScope.launch {
            val comparison = withContext(Dispatchers.Default) {
                engine.evaluateGuess(
                    board = state.board,
                    guessedMove = guessedMove,
                    style = state.engineStyle
                )
            }
            _gameState.update { current ->
                current.copy(
                    guessPhase = GuessPhase.ShowingResult(comparison),
                    currentEvalWhiteCp = comparison.bestEvalWhiteCp,
                    guessAttempts = current.guessAttempts + 1,
                    guessExactMatches = current.guessExactMatches + if (comparison.isExactBest) 1 else 0,
                    guessAccuracySum = current.guessAccuracySum + comparison.accuracyPercent
                )
            }
        }
    }

    fun commitGuessChoice(useEngineBest: Boolean) {
        val state = _gameState.value
        val resultPhase = state.guessPhase as? GuessPhase.ShowingResult ?: return
        val comparison = resultPhase.comparison
        val moveToPlay = if (useEngineBest) comparison.engineBestMove else comparison.playerMove
        commitPlayerMove(moveToPlay, precomputedAccuracy = comparison.accuracyPercent)
    }

    private fun commitPlayerMove(move: ChessMove, precomputedAccuracy: Double?) {
        val state = _gameState.value
        val boardBefore = state.board
        val san = boardBefore.toSan(move)
        val uci = move.toUci()
        val nextBoard = boardBefore.applyMove(move)
        val nextUci = state.movesUci + uci
        val nextSan = state.movesSan + san
        val opening = OpeningBook.identifyOpening(nextUci)
        val result = nextBoard.evaluateGameStatus()

        val inc = if (state.timeControl.isTimed) state.timeControl.incrementMs else 0L
        val nextWhiteMs = if (state.playerColor == PieceColor.WHITE) state.whiteTimeMs + inc else state.whiteTimeMs
        val nextBlackMs = if (state.playerColor == PieceColor.BLACK) state.blackTimeMs + inc else state.blackTimeMs

        val quickEval = engine.staticEvalWhite(nextBoard, state.engineStyle)
        val updatedEvals = state.evalsWhiteCp + quickEval

        val updatedAccuracies = if (precomputedAccuracy != null) {
            state.playerMoveAccuracies + precomputedAccuracy
        } else {
            val prevForPlayer = if (state.playerColor == PieceColor.WHITE) state.currentEvalWhiteCp else -state.currentEvalWhiteCp
            val nowForPlayer = if (state.playerColor == PieceColor.WHITE) quickEval else -quickEval
            val swingLoss = (prevForPlayer.coerceIn(-900, 900) - nowForPlayer.coerceIn(-900, 900)).coerceAtLeast(0)
            state.playerMoveAccuracies + StockfishEngine.computeAccuracy(swingLoss)
        }

        _gameState.update {
            it.copy(
                board = nextBoard,
                movesUci = nextUci,
                movesSan = nextSan,
                evalsWhiteCp = updatedEvals,
                playerMoveAccuracies = updatedAccuracies,
                lastMove = move,
                currentEvalWhiteCp = quickEval,
                currentMateIn = null,
                opening = opening,
                gameResult = result,
                guessPhase = GuessPhase.Idle,
                whiteTimeMs = nextWhiteMs,
                blackTimeMs = nextBlackMs
            )
        }

        if (result.isGameOver) {
            finalizeCompletedGame(result)
        } else {
            triggerEngineMove()
        }
    }

    private fun triggerEngineMove() {
        engineJob?.cancel()
        _gameState.update { it.copy(isEngineThinking = true) }

        engineJob = viewModelScope.launch {
            val snapshot = _gameState.value
            val thinkStart = System.currentTimeMillis()
            val evaluation = withContext(Dispatchers.Default) {
                engine.findBestMove(
                    board = snapshot.board,
                    style = snapshot.engineStyle,
                    timeBudgetMs = 680L,
                    maxDepth = 5,
                    movesHistoryUci = snapshot.movesUci,
                    useOpeningBook = true
                )
            }
            val elapsed = System.currentTimeMillis() - thinkStart
            if (elapsed < 220L) delay(220L - elapsed)

            val move = evaluation.bestMove
            if (move == null) {
                val status = snapshot.board.evaluateGameStatus()
                _gameState.update { it.copy(isEngineThinking = false, gameResult = status) }
                if (status.isGameOver) finalizeCompletedGame(status)
                return@launch
            }

            val current = _gameState.value
            if (current.isPaused || current.gameResult.isGameOver) {
                _gameState.update { it.copy(isEngineThinking = false) }
                return@launch
            }

            val san = current.board.toSan(move)
            val uci = move.toUci()
            val nextBoard = current.board.applyMove(move)
            val nextUci = current.movesUci + uci
            val nextSan = current.movesSan + san
            val opening = OpeningBook.identifyOpening(nextUci)
            val result = nextBoard.evaluateGameStatus()

            val inc = if (current.timeControl.isTimed) current.timeControl.incrementMs else 0L
            val engineColor = current.playerColor.opposite()
            val nextWhiteMs = if (engineColor == PieceColor.WHITE) current.whiteTimeMs + inc else current.whiteTimeMs
            val nextBlackMs = if (engineColor == PieceColor.BLACK) current.blackTimeMs + inc else current.blackTimeMs

            // Auto-continue Guess the Move on the player's next turn if continuous mode is enabled!
            val nextGuessPhase = if (!result.isGameOver && current.isGuessModeContinuous) {
                GuessPhase.WaitingForGuess
            } else {
                GuessPhase.Idle
            }

            _gameState.update {
                it.copy(
                    board = nextBoard,
                    movesUci = nextUci,
                    movesSan = nextSan,
                    evalsWhiteCp = it.evalsWhiteCp + evaluation.scoreCpWhite,
                    lastMove = move,
                    currentEvalWhiteCp = evaluation.scoreCpWhite,
                    currentMateIn = evaluation.mateIn,
                    opening = opening,
                    gameResult = result,
                    isEngineThinking = false,
                    guessPhase = nextGuessPhase,
                    whiteTimeMs = nextWhiteMs,
                    blackTimeMs = nextBlackMs
                )
            }

            if (result.isGameOver) {
                finalizeCompletedGame(result)
            }
        }
    }

    fun pauseAndSaveCurrentGame() {
        val state = _gameState.value
        if (state.gameResult.isGameOver) return

        engineJob?.cancel()
        val now = System.currentTimeMillis()
        val entity = buildEntityFromState(state.copy(isPaused = true, isEngineThinking = false), isPaused = true, now = now)

        viewModelScope.launch {
            val savedId = gameRepository.saveGame(entity)
            _gameState.update {
                it.copy(
                    savedGameId = savedId,
                    isPaused = true,
                    isEngineThinking = false,
                    guessPhase = GuessPhase.Idle,
                    statusBanner = "Match paused & saved offline. Resume anytime from Play or History."
                )
            }
        }
    }

    fun unpauseCurrentGame() {
        val state = _gameState.value
        if (!state.isPaused || state.gameResult.isGameOver) return
        _gameState.update {
            it.copy(
                isPaused = false,
                statusBanner = null,
                guessPhase = if (it.isGuessModeContinuous && it.board.sideToMove == it.playerColor) {
                    GuessPhase.WaitingForGuess
                } else GuessPhase.Idle
            )
        }
        if (state.board.sideToMove != state.playerColor) {
            triggerEngineMove()
        }
    }

    fun offerDraw() {
        val state = _gameState.value
        if (state.gameResult.isGameOver || state.isEngineThinking) return

        viewModelScope.launch {
            val accepted = withContext(Dispatchers.Default) {
                engine.shouldAcceptDraw(state.board, state.playerColor.opposite(), state.engineStyle)
            }
            if (accepted) {
                finalizeCompletedGame(GameResult.DRAW_AGREED)
            } else {
                _gameState.update {
                    it.copy(statusBanner = "Engine declined your draw offer — it sees winning chances.")
                }
            }
        }
    }

    fun resignGame() {
        val state = _gameState.value
        if (state.gameResult.isGameOver) return
        engineJob?.cancel()
        val result = if (state.playerColor == PieceColor.WHITE) {
            GameResult.BLACK_WINS_RESIGN
        } else {
            GameResult.WHITE_WINS_RESIGN
        }
        finalizeCompletedGame(result)
    }

    private fun finalizeCompletedGame(result: GameResult) {
        engineJob?.cancel()
        val state = _gameState.value
        viewModelScope.launch {
            prefsRepository.incrementCompletedGames()
            val now = System.currentTimeMillis()
            val updatedState = state.copy(
                gameResult = result,
                isPaused = false,
                isEngineThinking = false,
                isGuessModeContinuous = false,
                guessPhase = GuessPhase.Idle,
                showGameOverSummary = true
            )
            val savedId = gameRepository.saveGame(buildEntityFromState(updatedState, isPaused = false, now = now))
            _gameState.value = updatedState.copy(savedGameId = savedId)
        }
    }

    fun buildCurrentGameSnapshotEntity(): GameRecordEntity {
        val s = _gameState.value
        return buildEntityFromState(s, isPaused = s.isPaused, now = System.currentTimeMillis())
    }

    private fun buildEntityFromState(
        s: ActiveGameUiState,
        isPaused: Boolean,
        now: Long
    ): GameRecordEntity = GameRecordEntity(
        id = s.savedGameId,
        createdAtMillis = now,
        updatedAtMillis = now,
        playerColorName = s.playerColor.name,
        engineStyleName = s.engineStyle.name,
        timeControlName = s.timeControl.name,
        openingEco = s.opening.eco,
        openingName = s.opening.name,
        resultName = s.gameResult.name,
        isPaused = isPaused,
        currentFen = s.board.toFen(),
        movesUciCsv = s.movesUci.joinToString(","),
        movesSanPipe = s.movesSan.joinToString("|"),
        evalsCpCsv = s.evalsWhiteCp.joinToString(","),
        whiteTimeMs = s.whiteTimeMs,
        blackTimeMs = s.blackTimeMs,
        playerAccuracy = s.averageMatchAccuracy,
        guessAttempts = s.guessAttempts,
        guessExactMatches = s.guessExactMatches,
        guessAvgAccuracy = s.averageGuessAccuracy,
        estimatedElo = 0,
        moveCount = s.movesUci.size
    )

    fun deleteSavedGame(id: Long) {
        viewModelScope.launch {
            gameRepository.deleteById(id)
        }
    }

    // ---------------- POST-GAME ANALYSIS ENGINE ----------------

    fun openPostGameAnalysis(game: GameRecordEntity) {
        analysisJob?.cancel()
        autoPlayJob?.cancel()
        val uciList = game.movesUciList()
        _analysisState.value = AnalysisUiState(
            game = game,
            isAnalyzing = uciList.isNotEmpty(),
            progress = 0f,
            currentPly = uciList.size,
            boardFlipped = (game.playerColor == PieceColor.BLACK)
        )

        if (uciList.isEmpty()) return

        analysisJob = viewModelScope.launch {
            val analyzed = withContext(Dispatchers.Default) {
                engine.analyzeFullGame(
                    movesUci = uciList,
                    style = game.engineStyle,
                    onProgress = { p ->
                        _analysisState.update { it?.copy(progress = p) }
                    }
                )
            }
            val playerAnalyzed = analyzed.filter { it.color == game.playerColor }
            val preciseAcc = if (playerAnalyzed.isNotEmpty()) playerAnalyzed.map { it.accuracy }.average() else game.playerAccuracy

            val updatedGame = game.copy(
                playerAccuracy = preciseAcc,
                estimatedElo = 0,
                evalsCpCsv = analyzed.joinToString(",") { it.evalAfterWhiteCp.toString() }
            )
            if (game.id > 0L) {
                gameRepository.saveGame(updatedGame)
            }

            _analysisState.update {
                it?.copy(
                    game = updatedGame,
                    isAnalyzing = false,
                    progress = 1f,
                    analyzedMoves = analyzed,
                    currentPly = analyzed.size
                )
            }
        }
    }

    fun closeAnalysis() {
        analysisJob?.cancel()
        autoPlayJob?.cancel()
        _analysisState.value = null
    }

    fun setAnalysisPly(ply: Int) {
        autoPlayJob?.cancel()
        _analysisState.update { st ->
            if (st == null) null else {
                val maxPly = st.analyzedMoves.size.coerceAtLeast(st.game?.movesUciList()?.size ?: 0)
                st.copy(currentPly = ply.coerceIn(0, maxPly), isAutoPlaying = false)
            }
        }
    }

    fun toggleAnalysisFlip() {
        _analysisState.update { it?.copy(boardFlipped = !it.boardFlipped) }
    }

    fun toggleAnalysisAutoPlay() {
        val current = _analysisState.value ?: return
        if (current.isAutoPlaying) {
            autoPlayJob?.cancel()
            _analysisState.update { it?.copy(isAutoPlaying = false) }
            return
        }
        val maxPly = current.analyzedMoves.size
        if (maxPly == 0) return

        val startPly = if (current.currentPly >= maxPly) 0 else current.currentPly
        _analysisState.update { it?.copy(currentPly = startPly, isAutoPlaying = true) }

        autoPlayJob?.cancel()
        autoPlayJob = viewModelScope.launch {
            while (isActive) {
                delay(950L)
                val st = _analysisState.value ?: break
                if (!st.isAutoPlaying || st.currentPly >= st.analyzedMoves.size) {
                    _analysisState.update { it?.copy(isAutoPlaying = false) }
                    break
                }
                _analysisState.update { it?.copy(currentPly = it.currentPly + 1) }
            }
        }
    }

    // ---------------- HISTORICAL TACTICS & STRATEGY SOLVER ----------------

    fun openPuzzle(puzzle: HistoricalPuzzle) {
        puzzleReplyJob?.cancel()
        val board = ChessBoardState.fromFen(puzzle.fen)
        val initialEval = engine.staticEvalWhite(board)
        _puzzleSession.value = PuzzleSessionUiState(
            puzzle = puzzle,
            board = board,
            stepIndex = 0,
            flipped = (board.sideToMove == PieceColor.BLACK),
            evalWhiteCp = initialEval
        )
    }

    fun closePuzzle() {
        puzzleReplyJob?.cancel()
        _puzzleSession.value = null
    }

    fun resetCurrentPuzzle() {
        val current = _puzzleSession.value ?: return
        openPuzzle(current.puzzle)
    }

    fun openNextPuzzle() {
        val current = _puzzleSession.value ?: return
        val sameCategory = HistoricalPuzzleLibrary.allPuzzles.filter {
            it.group.category == current.puzzle.group.category
        }
        val idx = sameCategory.indexOfFirst { it.id == current.puzzle.id }
        val next = sameCategory.getOrNull((idx + 1) % sameCategory.size) ?: return
        openPuzzle(next)
    }

    fun showPuzzleHint() {
        val session = _puzzleSession.value ?: return
        if (session.isSolved || session.isOpponentReplying) return
        val expectedUci = session.puzzle.solutionLineUci.getOrNull(session.stepIndex) ?: return
        val from = ChessMove.algebraicToSquare(expectedUci.take(2))
        val to = ChessMove.algebraicToSquare(expectedUci.drop(2).take(2))
        val hintArrows = if (from in 0..63 && to in 0..63) {
            listOf(BoardArrow(from, to, ArrowKind.ENGINE_BEST_GREEN))
        } else emptyList()

        _puzzleSession.update {
            it?.copy(
                arrows = hintArrows,
                feedback = PuzzleFeedback.HintShown(session.puzzle.hintText)
            )
        }
    }

    fun onPuzzleMoveAttempted(move: ChessMove) {
        val session = _puzzleSession.value ?: return
        if (session.isSolved || session.isOpponentReplying) return

        val expectedUci = session.puzzle.solutionLineUci.getOrNull(session.stepIndex) ?: return
        val expectedMove = session.board.findLegalMoveFromUci(expectedUci)
        val playedUci = move.toUci()
        val playedSan = session.board.toSan(move)

        val isMatch = playedUci == expectedUci ||
            (expectedMove != null && move.sameMoveAs(expectedMove))

        if (!isMatch) {
            // Subtle Blue arrow for player's attempt; let them retry or tap Hint for Green arrow
            val attemptArrows = listOf(BoardArrow(move.from, move.to, ArrowKind.PLAYER_GUESS_BLUE))
            _puzzleSession.update {
                it?.copy(
                    attempts = it.attempts + 1,
                    arrows = attemptArrows,
                    feedback = PuzzleFeedback.IncorrectMove(
                        triedSan = playedSan,
                        message = "$playedSan isn't the historical master move. Try again or tap Hint."
                    )
                )
            }
            return
        }

        // Exact historical master move found — Gold Arrow!
        val nextBoard = session.board.applyMove(move)
        val goldArrow = listOf(BoardArrow(move.from, move.to, ArrowKind.PERFECT_MATCH_GOLD))
        val nextStep = session.stepIndex + 1
        val newEval = engine.staticEvalWhite(nextBoard)

        if (nextStep >= session.puzzle.solutionLineUci.size) {
            viewModelScope.launch {
                prefsRepository.markPuzzleSolved(session.puzzle.id)
            }
            _puzzleSession.update {
                it?.copy(
                    board = nextBoard,
                    stepIndex = nextStep,
                    lastMove = move,
                    arrows = goldArrow,
                    evalWhiteCp = newEval,
                    feedback = PuzzleFeedback.Solved(session.puzzle.explanation)
                )
            }
        } else {
            // Multi-move combination: play forced opponent reply smoothly
            _puzzleSession.update {
                it?.copy(
                    board = nextBoard,
                    stepIndex = nextStep,
                    lastMove = move,
                    arrows = goldArrow,
                    evalWhiteCp = newEval,
                    isOpponentReplying = true,
                    feedback = PuzzleFeedback.StepCorrect(playedSan)
                )
            }

            puzzleReplyJob?.cancel()
            puzzleReplyJob = viewModelScope.launch {
                delay(480L)
                val curr = _puzzleSession.value ?: return@launch
                val replyUci = curr.puzzle.solutionLineUci.getOrNull(curr.stepIndex) ?: return@launch
                val replyMove = curr.board.findLegalMoveFromUci(replyUci) ?: return@launch
                val afterReply = curr.board.applyMove(replyMove)
                val afterStep = curr.stepIndex + 1
                val replyEval = engine.staticEvalWhite(afterReply)

                if (afterStep >= curr.puzzle.solutionLineUci.size) {
                    prefsRepository.markPuzzleSolved(curr.puzzle.id)
                    _puzzleSession.update {
                        it?.copy(
                            board = afterReply,
                            stepIndex = afterStep,
                            lastMove = replyMove,
                            arrows = emptyList(),
                            evalWhiteCp = replyEval,
                            isOpponentReplying = false,
                            feedback = PuzzleFeedback.Solved(curr.puzzle.explanation)
                        )
                    }
                } else {
                    _puzzleSession.update {
                        it?.copy(
                            board = afterReply,
                            stepIndex = afterStep,
                            lastMove = replyMove,
                            arrows = emptyList(),
                            evalWhiteCp = replyEval,
                            isOpponentReplying = false
                        )
                    }
                }
            }
        }
    }

    // ---------------- PREFERENCES & STUDIO ----------------

    fun setPlayerName(name: String) = viewModelScope.launch {
        prefsRepository.setPlayerName(name)
    }

    fun setBoardTheme(theme: BoardThemeStyle) = viewModelScope.launch {
        prefsRepository.setBoardTheme(theme)
    }

    fun setPieceSet(pieceSet: PieceSetStyle) = viewModelScope.launch {
        prefsRepository.setPieceSet(pieceSet)
    }

    fun setShowCoordinates(show: Boolean) = viewModelScope.launch {
        prefsRepository.setShowCoordinates(show)
    }

    fun setShowLegalHints(show: Boolean) = viewModelScope.launch {
        prefsRepository.setShowLegalHints(show)
    }

    fun setArrowOpacity(opacity: Float) = viewModelScope.launch {
        prefsRepository.setArrowOpacity(opacity)
    }

    fun setDefaultExportEmail(email: String) = viewModelScope.launch {
        prefsRepository.setDefaultExportEmail(email)
    }

    // ---------------- COMPETITION CLOCK TICKER ----------------

    private fun startClockTicker() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            var lastTick = System.currentTimeMillis()
            while (isActive) {
                delay(120L)
                val now = System.currentTimeMillis()
                val delta = (now - lastTick).coerceIn(0L, 500L)
                lastTick = now

                val s = _gameState.value
                val shouldTick = s.timeControl.isTimed &&
                    !s.gameResult.isGameOver &&
                    !s.isPaused &&
                    s.movesUci.isNotEmpty() &&
                    s.guessPhase !is GuessPhase.ShowingResult &&
                    s.guessPhase !is GuessPhase.Evaluating

                if (!shouldTick) continue

                if (s.board.sideToMove == PieceColor.WHITE) {
                    val remaining = (s.whiteTimeMs - delta).coerceAtLeast(0L)
                    _gameState.update { it.copy(whiteTimeMs = remaining) }
                    if (remaining == 0L) {
                        finalizeCompletedGame(GameResult.BLACK_WINS_TIME)
                    }
                } else {
                    val remaining = (s.blackTimeMs - delta).coerceAtLeast(0L)
                    _gameState.update { it.copy(blackTimeMs = remaining) }
                    if (remaining == 0L) {
                        finalizeCompletedGame(GameResult.WHITE_WINS_TIME)
                    }
                }
            }
        }
    }
}
