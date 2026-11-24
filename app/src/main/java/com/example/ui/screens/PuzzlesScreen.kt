package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.ChessMove
import com.example.chess.HistoricalPuzzle
import com.example.chess.HistoricalPuzzleLibrary
import com.example.chess.PuzzleCategory
import com.example.data.UserSettings
import com.example.ui.PuzzleFeedback
import com.example.ui.PuzzleSessionUiState
import com.example.ui.board.ChessBoardView
import com.example.ui.board.EvaluationBar
import com.example.ui.theme.ArrowBluePlayer
import com.example.ui.theme.ArrowGoldBest
import com.example.ui.theme.ArrowGreenEngine
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun PuzzlesScreen(
    activeSession: PuzzleSessionUiState?,
    settings: UserSettings,
    onSelectPuzzle: (HistoricalPuzzle) -> Unit,
    onClosePuzzle: () -> Unit,
    onPuzzleMove: (ChessMove) -> Unit,
    onHintClick: () -> Unit,
    onResetPuzzle: () -> Unit,
    onNextPuzzle: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (activeSession != null) {
        PuzzleSolverView(
            session = activeSession,
            settings = settings,
            onBack = onClosePuzzle,
            onMove = onPuzzleMove,
            onHint = onHintClick,
            onReset = onResetPuzzle,
            onNext = onNextPuzzle,
            modifier = modifier
        )
    } else {
        PuzzleBrowserView(
            solvedIds = settings.solvedPuzzleIds,
            onSelectPuzzle = onSelectPuzzle,
            modifier = modifier
        )
    }
}

@Composable
private fun PuzzleBrowserView(
    solvedIds: Set<String>,
    onSelectPuzzle: (HistoricalPuzzle) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(PuzzleCategory.TACTICS) }
    val grouped = remember(selectedCategory) {
        HistoricalPuzzleLibrary.groupedByCategory(selectedCategory)
    }
    val totalInCat = grouped.sumOf { it.second.size }
    val solvedInCat = grouped.sumOf { pair -> pair.second.count { it.id in solvedIds } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Master Classics Trainer",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Solve iconic Tactics & Strategy positions from chess history, grouped by theme.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tactics vs Strategy Segmented Switcher
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedCategory == PuzzleCategory.TACTICS,
                        onClick = { selectedCategory = PuzzleCategory.TACTICS },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {
                            Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("tab_tactics")
                    ) {
                        Text("Tactics")
                    }
                    SegmentedButton(
                        selected = selectedCategory == PuzzleCategory.STRATEGY,
                        onClick = { selectedCategory = PuzzleCategory.STRATEGY },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {
                            Icon(Icons.Rounded.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("tab_strategy")
                    ) {
                        Text("Strategy")
                    }
                }
            }

            // Category Progress Banner
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedCategory.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = selectedCategory.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "$solvedInCat / $totalInCat Solved",
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Thematic Groups & Historical Position Cards
            grouped.forEach { (group, puzzles) ->
                item(key = "header_${group.name}") {
                    Column(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = group.displayName.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = group.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(puzzles, key = { it.id }) { puzzle ->
                    val isSolved = puzzle.id in solvedIds
                    HistoricalPuzzleCard(
                        puzzle = puzzle,
                        isSolved = isSolved,
                        onClick = { onSelectPuzzle(puzzle) }
                    )
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun HistoricalPuzzleCard(
    puzzle: HistoricalPuzzle,
    isSolved: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("puzzle_card_${puzzle.id}"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSolved) ArrowGreenEngine.copy(alpha = 0.65f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = puzzle.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = puzzle.difficulty,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = puzzle.matchInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = puzzle.objectivePrompt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(10.dp))

            if (isSolved) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Solved",
                    tint = ArrowGreenEngine,
                    modifier = Modifier.size(26.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Solve puzzle",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PuzzleSolverView(
    session: PuzzleSessionUiState,
    settings: UserSettings,
    onBack: () -> Unit,
    onMove: (ChessMove) -> Unit,
    onHint: () -> Unit,
    onReset: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("puzzle_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to puzzles")
                }
                Column {
                    Text(
                        text = session.puzzle.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${session.puzzle.group.displayName} · ${session.puzzle.difficulty}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
            IconButton(onClick = onReset) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Reset puzzle")
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            val isWide = maxWidth >= 720.dp

            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        PuzzleBoardWithEval(session, settings, onMove, Modifier.widthIn(max = 520.dp))
                    }
                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PuzzleInfoAndFeedbackColumn(session, onHint, onReset, onNext)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PuzzleBoardWithEval(session, settings, onMove, Modifier.widthIn(max = 480.dp))
                    PuzzleInfoAndFeedbackColumn(session, onHint, onReset, onNext)
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun PuzzleBoardWithEval(
    session: PuzzleSessionUiState,
    settings: UserSettings,
    onMove: (ChessMove) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val heightCap = if (constraints.hasBoundedHeight) maxHeight else maxWidth
        val boardSize = minOf(maxWidth - 34.dp, heightCap).coerceAtLeast(180.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EvaluationBar(
                scoreCpWhite = session.evalWhiteCp,
                mateIn = null,
                flipped = session.flipped,
                modifier = Modifier.height(boardSize)
            )
            ChessBoardView(
                board = session.board,
                boardTheme = settings.boardTheme,
                pieceStyle = settings.pieceSet,
                flipped = session.flipped,
                interactive = !session.isSolved && !session.isOpponentReplying,
                showCoordinates = settings.showCoordinates,
                showLegalHints = settings.showLegalHints,
                lastMove = session.lastMove,
                arrows = session.arrows,
                arrowOpacity = settings.arrowOpacity,
                onMoveSelected = onMove,
                modifier = Modifier.size(boardSize)
            )
        }
    }
}

@Composable
private fun PuzzleInfoAndFeedbackColumn(
    session: PuzzleSessionUiState,
    onHint: () -> Unit,
    onReset: () -> Unit,
    onNext: () -> Unit
) {
    // Historical Context Card
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = session.puzzle.matchInfo,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = session.puzzle.objectivePrompt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    // Dynamic Feedback Card
    when (val fb = session.feedback) {
        PuzzleFeedback.Neutral -> Unit
        is PuzzleFeedback.HintShown -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = ArrowGreenEngine.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, ArrowGreenEngine)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = ArrowGreenEngine)
                    Text(
                        text = "Hint: ${fb.hint}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        is PuzzleFeedback.IncorrectMove -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = ArrowBluePlayer.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, ArrowBluePlayer)
            ) {
                Text(
                    text = fb.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        is PuzzleFeedback.StepCorrect -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = ArrowGoldBest.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, ArrowGoldBest)
            ) {
                Text(
                    text = "★ Brilliant! ${fb.san} is the master move — keep calculating the continuation…",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = ArrowGoldBest,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        is PuzzleFeedback.Solved -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("puzzle_solved_card"),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.5.dp, ArrowGoldBest)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = ArrowGoldBest)
                        Text(
                            text = "Position Mastered!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ArrowGoldBest
                        )
                    }
                    Text(
                        text = fb.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Button(
                        onClick = onNext,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("next_puzzle_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ArrowGoldBest,
                            contentColor = Color(0xFF1F1602)
                        )
                    ) {
                        Text("Next Historical Position", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    // Hint & Retry Controls when not yet solved
    if (!session.isSolved) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = onHint,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("puzzle_hint_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Show Hint Arrow")
            }
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Reset Position")
            }
        }
    }
}
