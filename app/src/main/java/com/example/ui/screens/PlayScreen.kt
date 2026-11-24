package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.ChessMove
import com.example.chess.GuessComparison
import com.example.chess.Piece
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.data.UserSettings
import com.example.ui.ActiveGameUiState
import com.example.ui.GuessPhase
import com.example.ui.board.ChessBoardView
import com.example.ui.board.ChessPieceIcon
import com.example.ui.board.EvaluationBar
import com.example.ui.board.PieceSetStyle
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.ArrowBluePlayer
import com.example.ui.theme.ArrowGoldBest
import com.example.ui.theme.ArrowGreenEngine
import com.example.ui.theme.JetBrainsMonoFamily
import java.util.Locale

@Composable
fun PlayScreen(
    state: ActiveGameUiState,
    settings: UserSettings,
    pausedGamesCount: Int,
    onMoveSelected: (ChessMove) -> Unit,
    onStartGuessMode: () -> Unit,
    onStopGuessMode: () -> Unit,
    onRetryGuess: () -> Unit,
    onCommitGuessChoice: (Boolean) -> Unit,
    onNewMatchClick: () -> Unit,
    onPauseAndSaveClick: () -> Unit,
    onUnpauseClick: () -> Unit,
    onOfferDrawClick: () -> Unit,
    onResignConfirmed: () -> Unit,
    onFlipBoardClick: () -> Unit,
    onAnalyzeCurrentGame: () -> Unit,
    onExportPdfClick: () -> Unit,
    onPrintPdfClick: () -> Unit,
    onSharePdfClick: () -> Unit,
    onOpenPausedHistory: () -> Unit,
    onDismissGameOver: () -> Unit,
    onDismissBanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResignDialog by remember { mutableStateOf(false) }
    var showDrawDialog by remember { mutableStateOf(false) }
    var showPdfMenuDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWideTablet = maxWidth >= 720.dp

        if (isWideTablet) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OpeningHeaderBanner(
                        state = state,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    BoardWithEvalBar(
                        state = state,
                        settings = settings,
                        onMoveSelected = onMoveSelected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(0.85f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatusBannerCard(state.statusBanner, onDismissBanner)
                    OpponentAndPlayerClocksColumn(state = state, settings = settings)
                    GuessTheMovePanel(
                        state = state,
                        playerName = settings.displayPlayerName,
                        onStartGuessMode = onStartGuessMode,
                        onStopGuessMode = onStopGuessMode,
                        onRetryGuess = onRetryGuess,
                        onCommitChoice = onCommitGuessChoice
                    )
                    ActionControlBar(
                        state = state,
                        onPauseOrResumeClick = {
                            if (state.isPaused) onUnpauseClick() else onPauseAndSaveClick()
                        },
                        onDrawClick = { showDrawDialog = true },
                        onResignClick = { showResignDialog = true },
                        onFlipClick = onFlipBoardClick,
                        onPdfClick = { showPdfMenuDialog = true }
                    )
                    if (state.gameResult.isGameOver) {
                        PostMatchQuickCard(
                            state = state,
                            onAnalyze = onAnalyzeCurrentGame,
                            onPdf = { showPdfMenuDialog = true }
                        )
                    }
                    MoveHistoryStripCard(movesSan = state.movesSan)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Brand Row + Player Name Badge + New Match button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Albie's Chess",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = settings.displayPlayerName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (pausedGamesCount > 0 && state.movesUci.isEmpty()) {
                            FilledTonalButton(
                                onClick = onOpenPausedHistory,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Saved ($pausedGamesCount)", fontSize = 12.sp)
                            }
                        }
                        FilledTonalButton(
                            onClick = onNewMatchClick,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("top_new_match_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("New", fontSize = 12.sp)
                        }
                    }
                }

                OpeningHeaderBanner(
                    state = state,
                    modifier = Modifier.fillMaxWidth()
                )

                StatusBannerCard(state.statusBanner, onDismissBanner)

                val topIsWhite = state.boardFlipped
                PlayerClockRow(
                    isWhiteSide = topIsWhite,
                    state = state,
                    pieceStyle = settings.pieceSet,
                    playerName = settings.displayPlayerName
                )

                BoardWithEvalBar(
                    state = state,
                    settings = settings,
                    onMoveSelected = onMoveSelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                )

                PlayerClockRow(
                    isWhiteSide = !topIsWhite,
                    state = state,
                    pieceStyle = settings.pieceSet,
                    playerName = settings.displayPlayerName
                )

                // Continuous Start / Stop Guess the Move Hero Panel
                GuessTheMovePanel(
                    state = state,
                    playerName = settings.displayPlayerName,
                    onStartGuessMode = onStartGuessMode,
                    onStopGuessMode = onStopGuessMode,
                    onRetryGuess = onRetryGuess,
                    onCommitChoice = onCommitGuessChoice
                )

                if (state.gameResult.isGameOver) {
                    PostMatchQuickCard(
                        state = state,
                        onAnalyze = onAnalyzeCurrentGame,
                        onPdf = { showPdfMenuDialog = true }
                    )
                }

                ActionControlBar(
                    state = state,
                    onPauseOrResumeClick = {
                        if (state.isPaused) onUnpauseClick() else onPauseAndSaveClick()
                    },
                    onDrawClick = { showDrawDialog = true },
                    onResignClick = { showResignDialog = true },
                    onFlipClick = onFlipBoardClick,
                    onPdfClick = { showPdfMenuDialog = true }
                )

                MoveHistoryStripCard(movesSan = state.movesSan)
                Spacer(Modifier.height(6.dp))
            }
        }
    }

    if (showResignDialog) {
        AlertDialog(
            onDismissRequest = { showResignDialog = false },
            title = { Text("Resign Match?") },
            text = { Text("Resigning ends the match and saves your game to offline history for post-game analysis or PDF export.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResignDialog = false
                        onResignConfirmed()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertCrimson)
                ) {
                    Text("Resign")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResignDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDrawDialog) {
        AlertDialog(
            onDismissRequest = { showDrawDialog = false },
            title = { Text("Offer a Draw?") },
            text = { Text("The Stockfish engine will evaluate the position objectively before deciding whether to accept your draw offer.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDrawDialog = false
                        onOfferDrawClick()
                    }
                ) {
                    Text("Offer Draw")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDrawDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showPdfMenuDialog) {
        PdfActionDialog(
            onDismiss = { showPdfMenuDialog = false },
            onSavePdf = {
                showPdfMenuDialog = false
                onExportPdfClick()
            },
            onPrintPdf = {
                showPdfMenuDialog = false
                onPrintPdfClick()
            },
            onSharePdf = {
                showPdfMenuDialog = false
                onSharePdfClick()
            }
        )
    }

    if (state.showGameOverSummary) {
        GameOverSummaryDialog(
            state = state,
            playerName = settings.displayPlayerName,
            onDismiss = onDismissGameOver,
            onAnalyze = {
                onDismissGameOver()
                onAnalyzeCurrentGame()
            },
            onSharePdf = {
                onDismissGameOver()
                showPdfMenuDialog = true
            },
            onNewMatch = {
                onDismissGameOver()
                onNewMatchClick()
            }
        )
    }
}

@Composable
private fun OpeningHeaderBanner(
    state: ActiveGameUiState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag("opening_banner"),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = state.opening.eco,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.opening.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = state.opening.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = "MAX",
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun BoardWithEvalBar(
    state: ActiveGameUiState,
    settings: UserSettings,
    onMoveSelected: (ChessMove) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val heightCap = if (constraints.hasBoundedHeight) maxHeight else maxWidth
        val boardSize = minOf(maxWidth - 34.dp, heightCap).coerceAtLeast(180.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EvaluationBar(
                scoreCpWhite = state.currentEvalWhiteCp,
                mateIn = state.currentMateIn,
                flipped = state.boardFlipped,
                modifier = Modifier.height(boardSize)
            )
            ChessBoardView(
                board = state.board,
                boardTheme = settings.boardTheme,
                pieceStyle = settings.pieceSet,
                flipped = state.boardFlipped,
                interactive = state.isPlayersTurn && state.guessPhase !is GuessPhase.ShowingResult && state.guessPhase !is GuessPhase.Evaluating,
                showCoordinates = settings.showCoordinates,
                showLegalHints = settings.showLegalHints,
                lastMove = state.lastMove,
                arrows = state.arrows,
                arrowOpacity = settings.arrowOpacity,
                guessModeActive = state.isGuessModeContinuous && state.guessPhase is GuessPhase.WaitingForGuess,
                onMoveSelected = onMoveSelected,
                modifier = Modifier.size(boardSize)
            )
        }
    }
}

@Composable
private fun OpponentAndPlayerClocksColumn(
    state: ActiveGameUiState,
    settings: UserSettings
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PlayerClockRow(
            isWhiteSide = state.playerColor == PieceColor.BLACK,
            state = state,
            pieceStyle = settings.pieceSet,
            playerName = settings.displayPlayerName
        )
        PlayerClockRow(
            isWhiteSide = state.playerColor == PieceColor.WHITE,
            state = state,
            pieceStyle = settings.pieceSet,
            playerName = settings.displayPlayerName
        )
    }
}

@Composable
private fun PlayerClockRow(
    isWhiteSide: Boolean,
    state: ActiveGameUiState,
    pieceStyle: PieceSetStyle,
    playerName: String
) {
    val sideColor = if (isWhiteSide) PieceColor.WHITE else PieceColor.BLACK
    val isHuman = sideColor == state.playerColor
    val isActiveTurn = !state.gameResult.isGameOver && !state.isPaused && state.board.sideToMove == sideColor
    val timeMs = if (isWhiteSide) state.whiteTimeMs else state.blackTimeMs
    val isLowTime = state.timeControl.isTimed && timeMs in 1L..30_000L

    val captured = remember(state.board, sideColor) { state.board.getCapturedBy(sideColor) }
    val matAdv = remember(state.board, sideColor) { state.board.materialAdvantageFor(sideColor) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isActiveTurn) MaterialTheme.colorScheme.surfaceContainerHigh
                else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isWhiteSide) Color(0xFFF3EFE6) else Color(0xFF1B201E)),
                contentAlignment = Alignment.Center
            ) {
                ChessPieceIcon(
                    piece = Piece(sideColor, if (isHuman) PieceType.KNIGHT else PieceType.KING),
                    pieceStyle = pieceStyle,
                    size = 22.dp
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isHuman) "$playerName (${sideColor.label})" else "Stockfish · ${state.engineStyle.displayName}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!isHuman && state.isEngineThinking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isHuman) {
                            if (state.isGuessModeContinuous) "Guess Mode Active" else "Playing"
                        } else "GM Max Level",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isHuman && state.isGuessModeContinuous) ArrowGoldBest else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    if (captured.isNotEmpty()) {
                        val sym = captured.take(6).joinToString("") { capturedMiniSymbol(it) }
                        Text(
                            text = sym,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    if (matAdv > 0) {
                        Text(
                            text = "+$matAdv",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = when {
                isLowTime && isActiveTurn -> AlertCrimson
                isActiveTurn -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surface
            },
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Text(
                text = if (state.timeControl.isTimed) formatClock(timeMs) else "∞",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = when {
                    isLowTime && isActiveTurn -> Color.White
                    isActiveTurn -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun GuessTheMovePanel(
    state: ActiveGameUiState,
    playerName: String,
    onStartGuessMode: () -> Unit,
    onStopGuessMode: () -> Unit,
    onRetryGuess: () -> Unit,
    onCommitChoice: (Boolean) -> Unit
) {
    if (!state.isGuessModeContinuous) {
        // Primary Start Guess the Move button
        Button(
            onClick = onStartGuessMode,
            enabled = !state.gameResult.isGameOver && !state.isPaused,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("start_guess_the_move_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(Icons.Rounded.Psychology, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (state.guessAttempts > 0) {
                    "Start Guess the Move · ${state.guessExactMatches}/${state.guessAttempts} Best (${state.averageGuessAccuracy.toInt()}%)"
                } else {
                    "Start Guess the Move"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        return
    }

    // Continuous Guess the Move is ACTIVE — every succeeding move stays in Guess Mode until stopped!
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (val phase = state.guessPhase) {
            GuessPhase.Idle, GuessPhase.WaitingForGuess -> {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("guess_waiting_banner"),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.5.dp, ArrowGoldBest)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = ArrowGoldBest
                            )
                            Column {
                                Text(
                                    text = if (state.isEngineThinking) "Guess Mode On · Engine Replying…" else "Guess the Move Active",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = if (state.guessAttempts > 0) {
                                        "Score: ${state.guessExactMatches}/${state.guessAttempts} Best (${state.averageGuessAccuracy.toInt()}%) · Next moves auto-guess."
                                    } else {
                                        "Play your candidate move to compare with Stockfish."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            GuessPhase.Evaluating -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Comparing your move with Stockfish's best move…",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }

            is GuessPhase.ShowingResult -> {
                GuessComparisonCard(
                    comparison = phase.comparison,
                    onPlayMyMove = { onCommitChoice(false) },
                    onPlayBestMove = { onCommitChoice(true) },
                    onGuessAgain = onRetryGuess
                )
            }
        }

        // Persistent Stop Guess the Move button so the game continues seamlessly as the player
        OutlinedButton(
            onClick = onStopGuessMode,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("stop_guess_the_move_button"),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Icon(
                imageVector = Icons.Rounded.Stop,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Stop Guess the Move · Continue as $playerName",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun GuessComparisonCard(
    comparison: GuessComparison,
    onPlayMyMove: () -> Unit,
    onPlayBestMove: () -> Unit,
    onGuessAgain: () -> Unit
) {
    val borderColor = if (comparison.isExactBest) ArrowGoldBest else MaterialTheme.colorScheme.outline
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("guess_result_card"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (comparison.isExactBest) "★ Best Possible Move!" else comparison.classification.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (comparison.isExactBest) ArrowGoldBest else comparison.classification.color
                    )
                    Text(
                        text = if (comparison.isExactBest) {
                            "Exact Stockfish match on ${comparison.playerMoveSan} — Gold arrow!"
                        } else {
                            "Centipawn delta: -${String.format(Locale.US, "%.2f", comparison.cpLoss / 100.0)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = comparison.classification.color.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, comparison.classification.color)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.0f%%", comparison.accuracyPercent),
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = comparison.classification.color
                        )
                        Text(
                            text = "ACCURACY",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (comparison.isExactBest) {
                    ArrowLegendPill(
                        dotColor = ArrowGoldBest,
                        label = "Gold · Your Move = Stockfish (${comparison.playerMoveSan})",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    ArrowLegendPill(
                        dotColor = ArrowBluePlayer,
                        label = "Your Move: ${comparison.playerMoveSan}",
                        modifier = Modifier.weight(1f)
                    )
                    ArrowLegendPill(
                        dotColor = ArrowGreenEngine,
                        label = "Stockfish: ${comparison.engineBestSan}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (comparison.isExactBest) {
                Button(
                    onClick = onPlayBestMove,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("commit_best_move_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ArrowGoldBest,
                        contentColor = Color(0xFF1F1602)
                    )
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Play ${comparison.playerMoveSan}", fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPlayMyMove,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("commit_my_move_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ArrowBluePlayer,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Play ${comparison.playerMoveSan}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Button(
                        onClick = onPlayBestMove,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("commit_engine_move_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ArrowGreenEngine,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Play ${comparison.engineBestSan}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    IconButton(
                        onClick = onGuessAgain,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Try another guess"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArrowLegendPill(
    dotColor: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = dotColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, dotColor.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActionControlBar(
    state: ActiveGameUiState,
    onPauseOrResumeClick: () -> Unit,
    onDrawClick: () -> Unit,
    onResignClick: () -> Unit,
    onFlipClick: () -> Unit,
    onPdfClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ActionChipButton(
            icon = if (state.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
            label = if (state.isPaused) "Resume" else "Pause",
            enabled = !state.gameResult.isGameOver,
            onClick = onPauseOrResumeClick,
            modifier = Modifier
                .weight(1f)
                .testTag("pause_save_button")
        )
        ActionChipButton(
            icon = Icons.Rounded.Handshake,
            label = "Draw",
            enabled = !state.gameResult.isGameOver && !state.isEngineThinking,
            onClick = onDrawClick,
            modifier = Modifier
                .weight(1f)
                .testTag("offer_draw_button")
        )
        ActionChipButton(
            icon = Icons.Rounded.Flag,
            label = "Resign",
            enabled = !state.gameResult.isGameOver,
            onClick = onResignClick,
            modifier = Modifier
                .weight(1f)
                .testTag("resign_button")
        )
        ActionChipButton(
            icon = Icons.Rounded.SwapVert,
            label = "Flip",
            enabled = true,
            onClick = onFlipClick,
            modifier = Modifier
                .weight(1f)
                .testTag("flip_board_button")
        )
        ActionChipButton(
            icon = Icons.Rounded.PictureAsPdf,
            label = "PDF",
            enabled = state.movesUci.isNotEmpty(),
            onClick = onPdfClick,
            modifier = Modifier
                .weight(1f)
                .testTag("export_pdf_button")
        )
    }
}

@Composable
private fun ActionChipButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) MaterialTheme.colorScheme.surfaceContainer
        else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (enabled) 0.6f else 0.25f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 10.sp,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PostMatchQuickCard(
    state: ActiveGameUiState,
    onAnalyze: () -> Unit,
    onPdf: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.gameResult.displayLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Accuracy: ${String.format(Locale.US, "%.1f%%", state.averageMatchAccuracy)} · ${(state.movesSan.size + 1) / 2} moves",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalButton(
                    onClick = onAnalyze,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Rounded.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Analyze", fontSize = 12.sp)
                }
                IconButton(onClick = onPdf) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = "Export PDF")
                }
            }
        }
    }
}

@Composable
private fun MoveHistoryStripCard(movesSan: List<String>) {
    val scrollState = rememberScrollState()
    LaunchedEffect(movesSan.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.75f)
    ) {
        if (movesSan.isEmpty()) {
            Text(
                text = "Moves will appear here in algebraic notation…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            )
        } else {
            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var i = 0
                while (i < movesSan.size) {
                    val moveNum = (i / 2) + 1
                    val w = movesSan[i]
                    val b = movesSan.getOrNull(i + 1)
                    Text(
                        text = "$moveNum.",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = w,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (b != null) {
                        Text(
                            text = b,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    i += 2
                }
            }
        }
    }
}

@Composable
private fun StatusBannerCard(
    message: String?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(visible = message != null) {
        if (message != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDismiss),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun PdfActionDialog(
    onDismiss: () -> Unit,
    onSavePdf: () -> Unit,
    onPrintPdf: () -> Unit,
    onSharePdf: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Match Score Sheet") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Generate an offline printable PDF with final board diagram, algebraic moves, and accuracy summary.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onSharePdf,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Send to Email or Google Drive")
                }
                FilledTonalButton(
                    onClick = onPrintPdf,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Print Offline Score Sheet")
                }
                OutlinedButton(
                    onClick = onSavePdf,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save PDF to Device")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun GameOverSummaryDialog(
    state: ActiveGameUiState,
    playerName: String,
    onDismiss: () -> Unit,
    onAnalyze: () -> Unit,
    onSharePdf: () -> Unit,
    onNewMatch: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp)
            )
        },
        title = {
            Text(
                text = state.gameResult.displayLabel,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "$playerName (${state.playerColor.label}) · ${state.opening.eco} ${state.opening.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatHighlightBox(
                        title = "ACCURACY",
                        value = String.format(Locale.US, "%.1f%%", state.averageMatchAccuracy),
                        subtitle = "${(state.movesSan.size + 1) / 2} moves",
                        modifier = Modifier.weight(1f)
                    )
                    StatHighlightBox(
                        title = "GUESS SCORE",
                        value = if (state.guessAttempts > 0) "${state.guessExactMatches}/${state.guessAttempts}" else "—",
                        subtitle = if (state.guessAttempts > 0) "${state.averageGuessAccuracy.toInt()}% avg" else "No guesses",
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = onAnalyze,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_analyze_match_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Analyze Match Moves")
                }
                FilledTonalButton(
                    onClick = onSharePdf,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Export / Print / Share PDF")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onNewMatch) { Text("New Match") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Inspect Board") }
        }
    )
}

@Composable
private fun StatHighlightBox(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatClock(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return if (ms < 10_000L && ms > 0L) {
        val tenths = (ms % 1000L) / 100L
        String.format(Locale.US, "0:%02d.%d", secs, tenths)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}

private fun capturedMiniSymbol(type: PieceType): String = when (type) {
    PieceType.QUEEN -> "♛"
    PieceType.ROOK -> "♜"
    PieceType.BISHOP -> "♝"
    PieceType.KNIGHT -> "♞"
    PieceType.PAWN -> "♟"
    PieceType.KING -> ""
}
