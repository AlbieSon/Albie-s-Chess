package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chess.AnalyzedMove
import com.example.data.GameRecordEntity
import com.example.export.ChessPdfExporter
import com.example.ui.AppTab
import com.example.ui.ChessViewModel
import com.example.ui.screens.AnalysisScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.NewGameDialog
import com.example.ui.screens.PlayScreen
import com.example.ui.screens.PuzzlesScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.AlbiesChessTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlbiesChessTheme {
                AlbiesChessApp()
            }
        }
    }
}

@Composable
fun AlbiesChessApp(
    viewModel: ChessViewModel = viewModel()
) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()
    val puzzleSession by viewModel.puzzleSession.collectAsStateWithLifecycle()
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val pausedGames by viewModel.pausedGames.collectAsStateWithLifecycle()
    val completedGames by viewModel.completedGames.collectAsStateWithLifecycle()

    var showNewGameDialog by remember { mutableStateOf(false) }

    // Storage Access Framework (zero-permission) PDF Save-to-Device launcher
    var pendingSavePdfPayload by remember { mutableStateOf<Pair<GameRecordEntity, List<AnalyzedMove>>?>(null) }
    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val payload = pendingSavePdfPayload
        pendingSavePdfPayload = null
        if (uri != null && payload != null) {
            runCatching {
                ChessPdfExporter.writePdfToUri(
                    context = context,
                    uri = uri,
                    game = payload.first,
                    analyzedMoves = payload.second,
                    playerName = settings.displayPlayerName
                )
            }.onSuccess {
                Toast.makeText(context, "PDF score sheet saved offline.", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Could not save PDF: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchSavePdf(game: GameRecordEntity, analyzed: List<AnalyzedMove> = emptyList()) {
        pendingSavePdfPayload = game to analyzed
        val safeEco = game.openingEco.replace(Regex("[^A-Za-z0-9]"), "")
        createPdfLauncher.launch("AlbiesChess_${safeEco}_ScoreSheet.pdf")
    }

    fun launchPrintPdf(game: GameRecordEntity, analyzed: List<AnalyzedMove> = emptyList()) {
        runCatching {
            ChessPdfExporter.printPdf(
                context = context,
                game = game,
                analyzedMoves = analyzed,
                playerName = settings.displayPlayerName
            )
        }.onFailure {
            Toast.makeText(context, "Printing unavailable: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchSharePdf(game: GameRecordEntity, analyzed: List<AnalyzedMove> = emptyList()) {
        runCatching {
            ChessPdfExporter.sharePdf(
                context = context,
                game = game,
                analyzedMoves = analyzed,
                recipientEmail = settings.defaultExportEmail,
                playerName = settings.displayPlayerName
            )
        }.onFailure {
            Toast.makeText(context, "Share failed: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // If Post-Game Analysis Screen is open, render full-screen adaptive Analysis Experience
    val activeAnalysis = analysisState
    if (activeAnalysis != null) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            AnalysisScreen(
                state = activeAnalysis,
                settings = settings,
                onBack = viewModel::closeAnalysis,
                onSelectPly = viewModel::setAnalysisPly,
                onToggleAutoPlay = viewModel::toggleAnalysisAutoPlay,
                onFlipBoard = viewModel::toggleAnalysisFlip,
                onSavePdf = {
                    activeAnalysis.game?.let { launchSavePdf(it, activeAnalysis.analyzedMoves) }
                },
                onPrintPdf = {
                    activeAnalysis.game?.let { launchPrintPdf(it, activeAnalysis.analyzedMoves) }
                },
                onSharePdf = {
                    activeAnalysis.game?.let { launchSharePdf(it, activeAnalysis.analyzedMoves) }
                },
                modifier = Modifier.padding(innerPadding)
            )
        }
        return
    }

    // Main Adaptive Shell: NavigationRail on Tablets/Landscape, NavigationBar on Phones
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 720.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == AppTab.PLAY,
                            onClick = { viewModel.selectTab(AppTab.PLAY) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == AppTab.PLAY) Icons.Rounded.SportsEsports else Icons.Outlined.SportsEsports,
                                    contentDescription = "Play Chess"
                                )
                            },
                            label = { Text("Play") },
                            modifier = Modifier.testTag("nav_play")
                        )
                        NavigationBarItem(
                            selected = selectedTab == AppTab.PUZZLES,
                            onClick = { viewModel.selectTab(AppTab.PUZZLES) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == AppTab.PUZZLES) Icons.Rounded.Extension else Icons.Outlined.Extension,
                                    contentDescription = "Tactics and Strategy Trainer"
                                )
                            },
                            label = { Text("Train") },
                            modifier = Modifier.testTag("nav_puzzles")
                        )
                        NavigationBarItem(
                            selected = selectedTab == AppTab.HISTORY,
                            onClick = { viewModel.selectTab(AppTab.HISTORY) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == AppTab.HISTORY) Icons.Rounded.History else Icons.Outlined.History,
                                    contentDescription = "Match History"
                                )
                            },
                            label = { Text("History") },
                            modifier = Modifier.testTag("nav_history")
                        )
                        NavigationBarItem(
                            selected = selectedTab == AppTab.STUDIO,
                            onClick = { viewModel.selectTab(AppTab.STUDIO) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == AppTab.STUDIO) Icons.Rounded.Palette else Icons.Outlined.Palette,
                                    contentDescription = "Board & Piece Studio"
                                )
                            },
                            label = { Text("Studio") },
                            modifier = Modifier.testTag("nav_studio")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        NavigationRailItem(
                            selected = selectedTab == AppTab.PLAY,
                            onClick = { viewModel.selectTab(AppTab.PLAY) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == AppTab.PLAY) Icons.Rounded.SportsEsports else Icons.Outlined.SportsEsports,
                                    contentDescription = "Play Chess"
                                )
                            },
                            label = { Text("Play") },
                            modifier = Modifier.testTag("rail_play")
                        )
                        NavigationRailItem(
                            selected = selectedTab == AppTab.PUZZLES,
                            onClick = { viewModel.selectTab(AppTab.PUZZLES) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == AppTab.PUZZLES) Icons.Rounded.Extension else Icons.Outlined.Extension,
                                    contentDescription = "Tactics and Strategy Trainer"
                                )
                            },
                            label = { Text("Train") },
                            modifier = Modifier.testTag("rail_puzzles")
                        )
                        NavigationRailItem(
                            selected = selectedTab == AppTab.HISTORY,
                            onClick = { viewModel.selectTab(AppTab.HISTORY) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == AppTab.HISTORY) Icons.Rounded.History else Icons.Outlined.History,
                                    contentDescription = "Match History"
                                )
                            },
                            label = { Text("History") },
                            modifier = Modifier.testTag("rail_history")
                        )
                        NavigationRailItem(
                            selected = selectedTab == AppTab.STUDIO,
                            onClick = { viewModel.selectTab(AppTab.STUDIO) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == AppTab.STUDIO) Icons.Rounded.Palette else Icons.Outlined.Palette,
                                    contentDescription = "Board & Piece Studio"
                                )
                            },
                            label = { Text("Studio") },
                            modifier = Modifier.testTag("rail_studio")
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (selectedTab) {
                        AppTab.PLAY -> {
                            PlayScreen(
                                state = gameState,
                                settings = settings,
                                pausedGamesCount = pausedGames.size,
                                onMoveSelected = viewModel::onPlayerBoardMove,
                                onStartGuessMode = viewModel::startGuessTheMoveMode,
                                onStopGuessMode = viewModel::stopGuessTheMoveMode,
                                onRetryGuess = viewModel::retryCurrentGuess,
                                onCommitGuessChoice = viewModel::commitGuessChoice,
                                onNewMatchClick = { showNewGameDialog = true },
                                onPauseAndSaveClick = viewModel::pauseAndSaveCurrentGame,
                                onUnpauseClick = viewModel::unpauseCurrentGame,
                                onOfferDrawClick = viewModel::offerDraw,
                                onResignConfirmed = viewModel::resignGame,
                                onFlipBoardClick = viewModel::toggleBoardFlip,
                                onAnalyzeCurrentGame = {
                                    viewModel.openPostGameAnalysis(viewModel.buildCurrentGameSnapshotEntity())
                                },
                                onExportPdfClick = { launchSavePdf(viewModel.buildCurrentGameSnapshotEntity()) },
                                onPrintPdfClick = { launchPrintPdf(viewModel.buildCurrentGameSnapshotEntity()) },
                                onSharePdfClick = { launchSharePdf(viewModel.buildCurrentGameSnapshotEntity()) },
                                onOpenPausedHistory = { viewModel.selectTab(AppTab.HISTORY) },
                                onDismissGameOver = viewModel::dismissGameOverSummary,
                                onDismissBanner = viewModel::clearStatusBanner
                            )
                        }

                        AppTab.PUZZLES -> {
                            PuzzlesScreen(
                                activeSession = puzzleSession,
                                settings = settings,
                                onSelectPuzzle = viewModel::openPuzzle,
                                onClosePuzzle = viewModel::closePuzzle,
                                onPuzzleMove = viewModel::onPuzzleMoveAttempted,
                                onHintClick = viewModel::showPuzzleHint,
                                onResetPuzzle = viewModel::resetCurrentPuzzle,
                                onNextPuzzle = viewModel::openNextPuzzle
                            )
                        }

                        AppTab.HISTORY -> {
                            HistoryScreen(
                                pausedGames = pausedGames,
                                completedGames = completedGames,
                                settings = settings,
                                onResumeGame = viewModel::resumeSavedGame,
                                onAnalyzeGame = viewModel::openPostGameAnalysis,
                                onSavePdf = { launchSavePdf(it) },
                                onPrintPdf = { launchPrintPdf(it) },
                                onSharePdf = { launchSharePdf(it) },
                                onDeleteGame = viewModel::deleteSavedGame,
                                onStartNewMatch = { showNewGameDialog = true }
                            )
                        }

                        AppTab.STUDIO -> {
                            StudioScreen(
                                settings = settings,
                                onPlayerNameChanged = viewModel::setPlayerName,
                                onArrowOpacityChanged = viewModel::setArrowOpacity,
                                onSelectBoardTheme = viewModel::setBoardTheme,
                                onSelectPieceSet = viewModel::setPieceSet,
                                onToggleCoordinates = viewModel::setShowCoordinates,
                                onToggleLegalHints = viewModel::setShowLegalHints,
                                onDefaultEmailChanged = viewModel::setDefaultExportEmail
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewGameDialog) {
        NewGameDialog(
            initialPlayerName = settings.playerName,
            initialStyle = settings.preferredEngineStyle,
            initialTimeControl = settings.preferredTimeControl,
            pieceStyle = settings.pieceSet,
            onDismiss = { showNewGameDialog = false },
            onStartGame = { color, style, timeControl, playerName ->
                showNewGameDialog = false
                viewModel.startNewGame(color, style, timeControl, playerName)
            }
        )
    }
}
