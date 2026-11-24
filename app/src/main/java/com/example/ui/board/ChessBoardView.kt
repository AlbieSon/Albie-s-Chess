package com.example.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.chess.ArrowKind
import com.example.chess.BoardArrow
import com.example.chess.ChessBoardState
import com.example.chess.ChessMove
import com.example.chess.Piece
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.ui.theme.ArrowBluePlayer
import com.example.ui.theme.ArrowGoldBest
import com.example.ui.theme.ArrowGreenEngine
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Interactive tournament-grade 8x8 Chessboard supporting:
 * - Tap-to-select & tap-to-move AND smooth drag-and-drop
 * - Blue (player guess), Green (Stockfish best), and Gold (exact match) vector arrows
 * - Last move highlights, check radial glow, legal target dots/capture rings, coordinates
 * - Pawn promotion chooser dialog
 */
@Composable
fun ChessBoardView(
    board: ChessBoardState,
    boardTheme: BoardThemeStyle,
    pieceStyle: PieceSetStyle,
    flipped: Boolean,
    interactive: Boolean,
    showCoordinates: Boolean,
    showLegalHints: Boolean,
    lastMove: ChessMove? = null,
    arrows: List<BoardArrow> = emptyList(),
    arrowOpacity: Float = 0.52f,
    guessModeActive: Boolean = false,
    onMoveSelected: (ChessMove) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedSquare by remember(board, interactive) { mutableStateOf(-1) }
    var legalMovesForSelected by remember(board, interactive) { mutableStateOf<List<ChessMove>>(emptyList()) }
    var draggingSquare by remember(board) { mutableStateOf(-1) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var pendingPromotions by remember(board) { mutableStateOf<List<ChessMove>>(emptyList()) }

    val kingInCheckSq = remember(board) {
        if (board.isInCheck(board.sideToMove)) board.findKing(board.sideToMove) else -1
    }

    fun visualToSquare(col: Int, row: Int): Int {
        val boardFile = if (flipped) 7 - col else col
        val boardRank = if (flipped) 7 - row else row
        return boardRank * 8 + boardFile
    }

    fun squareToVisual(sq: Int): Pair<Int, Int> {
        val boardFile = sq % 8
        val boardRank = sq / 8
        val col = if (flipped) 7 - boardFile else boardFile
        val row = if (flipped) 7 - boardRank else boardRank
        return col to row
    }

    fun commitSquarePair(from: Int, to: Int) {
        val matching = board.generateLegalMovesFrom(from).filter { it.to == to }
        if (matching.isEmpty()) return
        if (matching.size > 1 && matching.any { it.promotion != null }) {
            pendingPromotions = matching
        } else {
            selectedSquare = -1
            legalMovesForSelected = emptyList()
            onMoveSelected(matching.first())
        }
    }

    val frameBorderColor = if (guessModeActive) ArrowGoldBest else boardTheme.frameColor

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(boardTheme.frameColor)
            .border(
                width = if (guessModeActive) 2.5.dp else 1.5.dp,
                color = frameBorderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(2.dp)
            .testTag("chess_board")
            .semantics { contentDescription = "Interactive chess board" }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(board, flipped, interactive) {
                        if (!interactive) return@pointerInput
                        detectTapGestures { offset ->
                            if (pendingPromotions.isNotEmpty()) {
                                pendingPromotions = emptyList()
                                return@detectTapGestures
                            }
                            val squarePx = (size.width.toFloat().coerceAtLeast(1f)) / 8f
                            val col = (offset.x / squarePx).toInt().coerceIn(0, 7)
                            val row = (offset.y / squarePx).toInt().coerceIn(0, 7)
                            val tappedSq = visualToSquare(col, row)

                            if (selectedSquare != -1 && legalMovesForSelected.any { it.to == tappedSq }) {
                                commitSquarePair(selectedSquare, tappedSq)
                                return@detectTapGestures
                            }

                            val tappedPiece = board.squares[tappedSq]
                            if (tappedPiece != null && tappedPiece.color == board.sideToMove) {
                                if (selectedSquare == tappedSq) {
                                    selectedSquare = -1
                                    legalMovesForSelected = emptyList()
                                } else {
                                    selectedSquare = tappedSq
                                    legalMovesForSelected = board.generateLegalMovesFrom(tappedSq)
                                }
                            } else {
                                selectedSquare = -1
                                legalMovesForSelected = emptyList()
                            }
                        }
                    }
                    .pointerInput(board, flipped, interactive) {
                        if (!interactive) return@pointerInput
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                val squarePx = (size.width.toFloat().coerceAtLeast(1f)) / 8f
                                val col = (startOffset.x / squarePx).toInt().coerceIn(0, 7)
                                val row = (startOffset.y / squarePx).toInt().coerceIn(0, 7)
                                val sq = visualToSquare(col, row)
                                val piece = board.squares[sq]
                                if (piece != null && piece.color == board.sideToMove) {
                                    draggingSquare = sq
                                    dragOffset = startOffset
                                    selectedSquare = sq
                                    legalMovesForSelected = board.generateLegalMovesFrom(sq)
                                }
                            },
                            onDrag = { change, _ ->
                                if (draggingSquare != -1) {
                                    change.consume()
                                    dragOffset = change.position
                                }
                            },
                            onDragCancel = {
                                draggingSquare = -1
                            },
                            onDragEnd = {
                                if (draggingSquare != -1) {
                                    val squarePx = (size.width.toFloat().coerceAtLeast(1f)) / 8f
                                    val col = (dragOffset.x / squarePx).toInt().coerceIn(0, 7)
                                    val row = (dragOffset.y / squarePx).toInt().coerceIn(0, 7)
                                    val targetSq = visualToSquare(col, row)
                                    val fromSq = draggingSquare
                                    draggingSquare = -1
                                    if (targetSq != fromSq && legalMovesForSelected.any { it.to == targetSq }) {
                                        commitSquarePair(fromSq, targetSq)
                                    }
                                }
                            }
                        )
                    }
            ) {
                val sqSize = size.width / 8f
                val legalTargets = legalMovesForSelected.map { it.to }.toSet()

                // 1. Draw 64 squares & highlights
                for (row in 0 until 8) {
                    for (col in 0 until 8) {
                        val sq = visualToSquare(col, row)
                        val isLight = ((sq / 8) + (sq % 8)) % 2 == 0
                        val isLastMove = lastMove != null && (sq == lastMove.from || sq == lastMove.to)
                        val isSelected = sq == selectedSquare

                        val baseColor = when {
                            isSelected -> boardTheme.selectedSquare
                            isLastMove -> if (isLight) boardTheme.lastMoveLight else boardTheme.lastMoveDark
                            else -> if (isLight) boardTheme.lightSquare else boardTheme.darkSquare
                        }
                        val topLeft = Offset(col * sqSize, row * sqSize)
                        drawRect(color = baseColor, topLeft = topLeft, size = Size(sqSize, sqSize))

                        // King in check crimson radial glow
                        if (sq == kingInCheckSq) {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xD9E52222), Color(0x00E52222)),
                                    center = Offset(topLeft.x + sqSize / 2f, topLeft.y + sqSize / 2f),
                                    radius = sqSize * 0.62f
                                ),
                                topLeft = topLeft,
                                size = Size(sqSize, sqSize)
                            )
                        }

                        // Legal move hints
                        if (showLegalHints && sq in legalTargets) {
                            val center = Offset(topLeft.x + sqSize / 2f, topLeft.y + sqSize / 2f)
                            val hasPiece = board.squares[sq] != null || (sq == board.enPassantSquare)
                            if (hasPiece) {
                                drawCircle(
                                    color = Color.Black.copy(alpha = 0.28f),
                                    radius = sqSize * 0.43f,
                                    center = center,
                                    style = Stroke(width = sqSize * 0.085f)
                                )
                            } else {
                                drawCircle(
                                    color = Color.Black.copy(alpha = 0.24f),
                                    radius = sqSize * 0.155f,
                                    center = center
                                )
                            }
                        }
                    }
                }

                // 2. Draw Rank & File coordinates
                if (showCoordinates) {
                    val coordPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = sqSize * 0.22f
                        typeface = android.graphics.Typeface.create(
                            android.graphics.Typeface.MONOSPACE,
                            android.graphics.Typeface.BOLD
                        )
                    }
                    for (i in 0 until 8) {
                        // Left rank numbers
                        val rankSq = visualToSquare(0, i)
                        val isLightRank = ((rankSq / 8) + (rankSq % 8)) % 2 == 0
                        val rankLabel = ('8' - (rankSq / 8)).toString()
                        coordPaint.color = (if (isLightRank) boardTheme.coordOnLight else boardTheme.coordOnDark).toArgbCompat()
                        drawContext.canvas.nativeCanvas.drawText(
                            rankLabel,
                            sqSize * 0.07f,
                            i * sqSize + sqSize * 0.27f,
                            coordPaint
                        )

                        // Bottom file letters
                        val fileSq = visualToSquare(i, 7)
                        val isLightFile = ((fileSq / 8) + (fileSq % 8)) % 2 == 0
                        val fileLabel = ('a' + (fileSq % 8)).toString()
                        coordPaint.color = (if (isLightFile) boardTheme.coordOnLight else boardTheme.coordOnDark).toArgbCompat()
                        val textW = coordPaint.measureText(fileLabel)
                        drawContext.canvas.nativeCanvas.drawText(
                            fileLabel,
                            (i + 1) * sqSize - textW - sqSize * 0.07f,
                            8 * sqSize - sqSize * 0.08f,
                            coordPaint
                        )
                    }
                }

                // 3. Draw Pieces (except actively dragged piece)
                for (row in 0 until 8) {
                    for (col in 0 until 8) {
                        val sq = visualToSquare(col, row)
                        if (sq == draggingSquare) continue
                        val piece = board.squares[sq] ?: continue
                        drawChessPiece(
                            piece = piece,
                            style = pieceStyle,
                            topLeft = Offset(col * sqSize, row * sqSize),
                            squareSize = sqSize
                        )
                    }
                }

                // 4. Draw Evaluation / Guess Arrows (Green Engine Best -> Blue Player Guess -> Gold Perfect Match)
                // Draw Green first, then Blue, then Gold on top so both are visible cleanly
                val sortedArrows = arrows.sortedBy {
                    when (it.kind) {
                        ArrowKind.ENGINE_BEST_GREEN -> 0
                        ArrowKind.PLAYER_GUESS_BLUE -> 1
                        ArrowKind.PERFECT_MATCH_GOLD -> 2
                    }
                }
                for (arrow in sortedArrows) {
                    val (fromCol, fromRow) = squareToVisual(arrow.from)
                    val (toCol, toRow) = squareToVisual(arrow.to)
                    val start = Offset((fromCol + 0.5f) * sqSize, (fromRow + 0.5f) * sqSize)
                    val end = Offset((toCol + 0.5f) * sqSize, (toRow + 0.5f) * sqSize)
                    drawChessArrow(start, end, sqSize, arrow.kind, arrowOpacity)
                }

                // 5. Draw Actively Dragged Piece slightly enlarged under finger
                if (draggingSquare != -1) {
                    val draggedPiece = board.squares[draggingSquare]
                    if (draggedPiece != null) {
                        val liftSize = sqSize * 1.18f
                        drawChessPiece(
                            piece = draggedPiece,
                            style = pieceStyle,
                            topLeft = Offset(dragOffset.x - liftSize / 2f, dragOffset.y - liftSize * 0.65f),
                            squareSize = liftSize,
                            alpha = 0.96f
                        )
                    }
                }
            }

            // Pawn Promotion Overlay Picker
            if (pendingPromotions.isNotEmpty()) {
                val promoColor = board.sideToMove
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.52f))
                        .clickable { pendingPromotions = emptyList() },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val order = listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)
                            for (type in order) {
                                val promoMove = pendingPromotions.firstOrNull { it.promotion == type }
                                if (promoMove != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(boardTheme.lightSquare)
                                            .clickable {
                                                pendingPromotions = emptyList()
                                                selectedSquare = -1
                                                legalMovesForSelected = emptyList()
                                                onMoveSelected(promoMove)
                                            }
                                            .testTag("promote_${type.name.lowercase()}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ChessPieceIcon(
                                            piece = Piece(promoColor, type),
                                            pieceStyle = pieceStyle,
                                            size = 48.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawChessArrow(
    start: Offset,
    end: Offset,
    sqSize: Float,
    kind: ArrowKind,
    arrowOpacity: Float
) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val dist = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
    if (dist < 4f) return

    val angle = atan2(dy, dx)
    val ux = cos(angle)
    val uy = sin(angle)

    val opacity = arrowOpacity.coerceIn(0.22f, 0.90f)
    val goldBoost = (opacity + 0.08f).coerceAtMost(0.92f)

    val mainColor = when (kind) {
        ArrowKind.PLAYER_GUESS_BLUE -> ArrowBluePlayer.copy(alpha = opacity)
        ArrowKind.ENGINE_BEST_GREEN -> ArrowGreenEngine.copy(alpha = opacity)
        ArrowKind.PERFECT_MATCH_GOLD -> ArrowGoldBest.copy(alpha = goldBoost)
    }
    val rimColor = when (kind) {
        ArrowKind.PERFECT_MATCH_GOLD -> Color(0xFF4A3200).copy(alpha = opacity * 0.55f)
        ArrowKind.PLAYER_GUESS_BLUE -> Color(0xFF0E2A6E).copy(alpha = opacity * 0.45f)
        ArrowKind.ENGINE_BEST_GREEN -> Color(0xFF083D1C).copy(alpha = opacity * 0.45f)
    }

    // Subtle, refined proportions so pieces and squares underneath remain completely clear
    val startInset = sqSize * 0.24f
    val headLength = sqSize * 0.29f
    val headHalfWidth = sqSize * 0.165f
    val shaftWidth = if (kind == ArrowKind.PERFECT_MATCH_GOLD) sqSize * 0.12f else sqSize * 0.105f

    val lineStart = Offset(start.x + ux * startInset, start.y + uy * startInset)
    val tip = Offset(end.x - ux * (sqSize * 0.10f), end.y - uy * (sqSize * 0.10f))
    val baseCenter = Offset(tip.x - ux * headLength, tip.y - uy * headLength)
    val shaftEnd = Offset(baseCenter.x + ux * (sqSize * 0.03f), baseCenter.y + uy * (sqSize * 0.03f))

    val perpX = -uy
    val perpY = ux

    val leftWing = Offset(baseCenter.x + perpX * headHalfWidth, baseCenter.y + perpY * headHalfWidth)
    val rightWing = Offset(baseCenter.x - perpX * headHalfWidth, baseCenter.y - perpY * headHalfWidth)

    // Delicate hairline rim + translucent slim shaft
    drawLine(
        color = rimColor,
        start = lineStart,
        end = shaftEnd,
        strokeWidth = shaftWidth + sqSize * 0.025f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = mainColor,
        start = lineStart,
        end = shaftEnd,
        strokeWidth = shaftWidth,
        cap = StrokeCap.Round
    )

    val headPath = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(leftWing.x, leftWing.y)
        lineTo(rightWing.x, rightWing.y)
        close()
    }
    drawPath(headPath, color = mainColor)
    drawPath(headPath, color = rimColor, style = Stroke(width = sqSize * 0.018f))
}

private fun Color.toArgbCompat(): Int {
    val a = (alpha * 255f).toInt().coerceIn(0, 255)
    val r = (red * 255f).toInt().coerceIn(0, 255)
    val g = (green * 255f).toInt().coerceIn(0, 255)
    val b = (blue * 255f).toInt().coerceIn(0, 255)
    return android.graphics.Color.argb(a, r, g, b)
}
