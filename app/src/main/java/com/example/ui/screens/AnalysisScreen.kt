package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FirstPage
import androidx.compose.material.icons.rounded.LastPage
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.AnalyzedMove
import com.example.chess.ChessMove
import com.example.chess.MoveClassification
import com.example.chess.StockfishEngine
import com.example.data.UserSettings
import com.example.ui.AnalysisUiState
import com.example.ui.board.ChessBoardView
import com.example.ui.board.EvaluationBar
import com.example.ui.theme.ArrowBluePlayer
import com.example.ui.theme.ArrowGoldBest
import com.example.ui.theme.ArrowGreenEngine
import com.example.ui.theme.EvalBlackFill
import com.example.ui.theme.EvalWhiteFill
import com.example.ui.theme.JetBrainsMonoFamily
import java.util.Locale

@Composable
fun AnalysisScreen(
    state: AnalysisUiState,
    settings: UserSettings,
    onBack: () -> Unit,
    onSelectPly: (Int) -> Unit,
    onToggleAutoPlay: () -> Unit,
    onFlipBoard: () -> Unit,
    onSavePdf: () -> Unit,
    onPrintPdf: () -> Unit,
    onSharePdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var showPdfDialog by remember { mutableStateOf(false) }
    val game = state.game ?: return

    val lastMoveObj = remember(state.currentAnalyzedMove) {
        val uci = state.currentAnalyzedMove?.uci ?: return@remember null
        val from = ChessMove.algebraicToSquare(uci.take(2))
        val to = ChessMove.algebraicToSquare(uci.drop(2).take(2))
        if (from in 0..63 && to in 0..63) ChessMove(from, to) else null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Analysis Bar
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
                    modifier = Modifier.testTag("analysis_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text(
                        text = "Post-Game Analysis",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1
                    )
                    Text(
                        text = "${game.openingEco} · ${game.openingName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row {
                IconButton(onClick = onFlipBoard) {
                    Icon(Icons.Rounded.SwapVert, contentDescription = "Flip analysis board")
                }
                IconButton(
                    onClick = { showPdfDialog = true },
                    modifier = Modifier.testTag("analysis_pdf_button")
                ) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = "Export PDF Report")
                }
            }
        }

        if (state.isAnalyzing) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Offline Stockfish Engine reviewing moves… ${(state.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
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
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnalysisBoardSection(state, settings, lastMoveObj, Modifier.widthIn(max = 520.dp))
                    }
                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AnalysisControlsRow(state, onSelectPly, onToggleAutoPlay)
                        CurrentPlyInspectionCard(state.currentAnalyzedMove)
                        EvaluationAdvantageGraph(
                            moves = state.analyzedMoves,
                            currentPly = state.currentPly,
                            onSelectPly = onSelectPly
                        )
                        AccuracySummaryRow(state)
                        ClassificationCountsRow(state.analyzedMoves)
                        AnalysisMoveListStrip(state.analyzedMoves, state.currentPly, onSelectPly)
                        Spacer(Modifier.height(12.dp))
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
                    AnalysisBoardSection(state, settings, lastMoveObj, Modifier.widthIn(max = 480.dp))
                    AnalysisControlsRow(state, onSelectPly, onToggleAutoPlay)
                    CurrentPlyInspectionCard(state.currentAnalyzedMove)
                    EvaluationAdvantageGraph(
                        moves = state.analyzedMoves,
                        currentPly = state.currentPly,
                        onSelectPly = onSelectPly
                    )
                    AccuracySummaryRow(state)
                    ClassificationCountsRow(state.analyzedMoves)
                    AnalysisMoveListStrip(state.analyzedMoves, state.currentPly, onSelectPly)
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }

    if (showPdfDialog) {
        PdfActionDialog(
            onDismiss = { showPdfDialog = false },
            onSavePdf = {
                showPdfDialog = false
                onSavePdf()
            },
            onPrintPdf = {
                showPdfDialog = false
                onPrintPdf()
            },
            onSharePdf = {
                showPdfDialog = false
                onSharePdf()
            }
        )
    }
}

@Composable
private fun AnalysisBoardSection(
    state: AnalysisUiState,
    settings: UserSettings,
    lastMoveObj: ChessMove?,
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
                scoreCpWhite = state.currentEvalWhiteCp,
                mateIn = null,
                flipped = state.boardFlipped,
                modifier = Modifier.height(boardSize)
            )
            ChessBoardView(
                board = state.currentBoard,
                boardTheme = settings.boardTheme,
                pieceStyle = settings.pieceSet,
                flipped = state.boardFlipped,
                interactive = false,
                showCoordinates = settings.showCoordinates,
                showLegalHints = false,
                lastMove = lastMoveObj,
                arrows = state.currentArrows,
                arrowOpacity = settings.arrowOpacity,
                modifier = Modifier.size(boardSize)
            )
        }
    }
}

@Composable
private fun AnalysisControlsRow(
    state: AnalysisUiState,
    onSelectPly: (Int) -> Unit,
    onToggleAutoPlay: () -> Unit
) {
    val maxPly = state.analyzedMoves.size.coerceAtLeast(state.game?.movesUciList()?.size ?: 0)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onSelectPly(0) }, enabled = state.currentPly > 0) {
                Icon(Icons.Rounded.FirstPage, contentDescription = "First move")
            }
            IconButton(onClick = { onSelectPly(state.currentPly - 1) }, enabled = state.currentPly > 0) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous move")
            }
            IconButton(
                onClick = onToggleAutoPlay,
                enabled = maxPly > 0,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = if (state.isAutoPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (state.isAutoPlaying) "Pause replay" else "Play replay",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            IconButton(onClick = { onSelectPly(state.currentPly + 1) }, enabled = state.currentPly < maxPly) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Next move")
            }
            IconButton(onClick = { onSelectPly(maxPly) }, enabled = state.currentPly < maxPly) {
                Icon(Icons.Rounded.LastPage, contentDescription = "Last move")
            }
        }
    }
}

@Composable
private fun CurrentPlyInspectionCard(move: AnalyzedMove?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            1.dp,
            move?.classification?.color?.copy(alpha = 0.6f) ?: MaterialTheme.colorScheme.outline
        )
    ) {
        if (move == null) {
            Text(
                text = "Starting Position · Step forward or tap any move on the advantage graph to inspect Stockfish's best line.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(14.dp)
            )
        } else {
            val isExact = move.uci.take(4) == move.bestMoveUci.take(4)
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Move ${(move.ply + 1) / 2} (${move.color.label}): ${move.san}",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = move.classification.color.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${move.classification.glyph} ${move.classification.label}",
                                style = MaterialTheme.typography.labelMedium,
                                color = move.classification.color,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = String.format(Locale.US, "%.0f%%", move.accuracy),
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        color = move.classification.color
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isExact) {
                        Text(
                            text = "★ Gold Arrow: Exact Stockfish top choice",
                            style = MaterialTheme.typography.bodySmall,
                            color = ArrowGoldBest
                        )
                    } else {
                        Text(
                            text = "● Blue: ${move.san}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ArrowBluePlayer
                        )
                        Text(
                            text = "● Green Best: ${move.bestMoveSan}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ArrowGreenEngine
                        )
                    }
                }

                if (move.pvSan.isNotEmpty()) {
                    Text(
                        text = "Best line: ${move.pvSan.joinToString(" ")}",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EvaluationAdvantageGraph(
    moves: List<AnalyzedMove>,
    currentPly: Int,
    onSelectPly: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "ADVANTAGE GRAPH (TAP TO JUMP TO MOVE)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            if (moves.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(74.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Evaluating advantage curve…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(EvalBlackFill)
                        .pointerInput(moves) {
                            detectTapGestures { offset ->
                                val ratio = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                val ply = (ratio * moves.size).toInt().coerceIn(0, moves.size)
                                onSelectPly(ply)
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val count = moves.size
                    val midY = h / 2f

                    // White fill area under advantage curve
                    val fillPath = Path().apply {
                        moveTo(0f, h)
                        lineTo(0f, midY)
                        moves.forEachIndexed { idx, m ->
                            val x = ((idx + 1).toFloat() / count.toFloat()) * w
                            val whiteProb = StockfishEngine.evalToWhiteRatio(m.evalAfterWhiteCp, null)
                            val y = h * (1f - whiteProb)
                            lineTo(x, y)
                        }
                        lineTo(w, h)
                        close()
                    }
                    drawPath(fillPath, color = EvalWhiteFill.copy(alpha = 0.88f))

                    // Equilibrium dashed/solid midline
                    drawLine(
                        color = Color(0x88D9B45B),
                        start = Offset(0f, midY),
                        end = Offset(w, midY),
                        strokeWidth = 1.5f
                    )

                    // Curve outline
                    val strokePath = Path().apply {
                        moveTo(0f, midY)
                        moves.forEachIndexed { idx, m ->
                            val x = ((idx + 1).toFloat() / count.toFloat()) * w
                            val whiteProb = StockfishEngine.evalToWhiteRatio(m.evalAfterWhiteCp, null)
                            val y = h * (1f - whiteProb)
                            lineTo(x, y)
                        }
                    }
                    drawPath(
                        strokePath,
                        color = Color(0xFFD9B45B),
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                    )

                    // Current ply vertical cursor
                    val cursorX = (currentPly.toFloat() / count.toFloat()) * w
                    drawLine(
                        color = ArrowBluePlayer,
                        start = Offset(cursorX, 0f),
                        end = Offset(cursorX, h),
                        strokeWidth = 3f
                    )
                }
            }
        }
    }
}

@Composable
private fun AccuracySummaryRow(
    state: AnalysisUiState
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MiniMetricCard(
            label = "WHITE ACC",
            value = String.format(Locale.US, "%.1f%%", state.whiteAccuracy),
            modifier = Modifier.weight(1f)
        )
        MiniMetricCard(
            label = "BLACK ACC",
            value = String.format(Locale.US, "%.1f%%", state.blackAccuracy),
            modifier = Modifier.weight(1f)
        )
        MiniMetricCard(
            label = "BEST MOVES",
            value = "${state.bestMovesCount}",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MiniMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ClassificationCountsRow(moves: List<AnalyzedMove>) {
    if (moves.isEmpty()) return
    val counts = MoveClassification.entries.map { cls ->
        cls to moves.count { it.classification == cls }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        counts.forEach { (cls, count) ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = cls.color.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, cls.color.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = cls.glyph,
                        color = cls.color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "$count ${cls.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalysisMoveListStrip(
    moves: List<AnalyzedMove>,
    currentPly: Int,
    onSelectPly: (Int) -> Unit
) {
    if (moves.isEmpty()) return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            moves.forEach { m ->
                val isSelected = m.ply == currentPly
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectPly(m.ply) }
                ) {
                    Text(
                        text = "${if (m.ply % 2 == 1) "${(m.ply + 1) / 2}." else ""}${m.san}${m.classification.glyph}",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else m.classification.color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}
