package com.example.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.chess.Piece
import com.example.chess.PieceColor
import com.example.chess.PieceType

/**
 * Custom-crafted Compose vector silhouettes for every chess piece across 4 distinct PieceSetStyles.
 * Renders crisply at any phone or tablet DPI with zero raster blur.
 */
@Composable
fun ChessPieceIcon(
    piece: Piece,
    pieceStyle: PieceSetStyle,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    Canvas(modifier = modifier.size(size)) {
        drawChessPiece(
            piece = piece,
            style = pieceStyle,
            topLeft = Offset.Zero,
            squareSize = this.size.minDimension
        )
    }
}

fun DrawScope.drawChessPiece(
    piece: Piece,
    style: PieceSetStyle,
    topLeft: Offset,
    squareSize: Float,
    alpha: Float = 1f
) {
    val ox = topLeft.x
    val oy = topLeft.y
    val s = squareSize

    val isWhite = piece.color == PieceColor.WHITE
    val mainColor = if (isWhite) style.whiteFill else style.blackFill
    val accentColor = if (isWhite) style.whiteAccent else style.blackAccent
    val strokeColor = if (isWhite) style.whiteStroke else style.blackStroke
    val detailStrokeColor = if (isWhite) style.whiteStroke.copy(alpha = 0.72f) else style.blackStroke.copy(alpha = 0.72f)

    val fillBrush = Brush.linearGradient(
        colors = listOf(mainColor, accentColor),
        start = Offset(ox + s * 0.22f, oy + s * 0.12f),
        end = Offset(ox + s * 0.82f, oy + s * 0.90f)
    )
    val outlineStroke = Stroke(
        width = (s * style.strokeRatio).coerceAtLeast(1.4f),
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )
    val fineStroke = Stroke(
        width = (s * style.strokeRatio * 0.65f).coerceAtLeast(1f),
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )

    // Soft grounding shadow beneath piece base
    drawOval(
        color = Color.Black.copy(alpha = 0.24f * alpha),
        topLeft = Offset(ox + s * 0.16f, oy + s * 0.83f),
        size = Size(s * 0.68f, s * 0.11f)
    )

    fun px(nx: Float) = ox + nx * s
    fun py(ny: Float) = oy + ny * s

    when (piece.type) {
        PieceType.PAWN -> {
            // Base plinth
            val body = Path().apply {
                moveTo(px(0.24f), py(0.86f))
                lineTo(px(0.76f), py(0.86f))
                lineTo(px(0.72f), py(0.76f))
                cubicTo(px(0.66f), py(0.68f), px(0.58f), py(0.56f), px(0.57f), py(0.45f))
                lineTo(px(0.43f), py(0.45f))
                cubicTo(px(0.42f), py(0.56f), px(0.34f), py(0.68f), px(0.28f), py(0.76f))
                close()
            }
            drawPath(body, brush = fillBrush, alpha = alpha, style = Fill)
            drawPath(body, color = strokeColor.copy(alpha = alpha), style = outlineStroke)

            // Collar
            drawRoundRect(
                brush = fillBrush,
                topLeft = Offset(px(0.34f), py(0.42f)),
                size = Size(s * 0.32f, s * 0.08f),
                cornerRadius = CornerRadius(s * 0.04f),
                alpha = alpha
            )
            drawRoundRect(
                color = strokeColor.copy(alpha = alpha),
                topLeft = Offset(px(0.34f), py(0.42f)),
                size = Size(s * 0.32f, s * 0.08f),
                cornerRadius = CornerRadius(s * 0.04f),
                style = fineStroke
            )

            // Head orb
            drawCircle(
                brush = fillBrush,
                radius = s * 0.145f,
                center = Offset(px(0.50f), py(0.29f)),
                alpha = alpha
            )
            drawCircle(
                color = strokeColor.copy(alpha = alpha),
                radius = s * 0.145f,
                center = Offset(px(0.50f), py(0.29f)),
                style = outlineStroke
            )

            if (!style.minimalStyle) {
                drawLine(
                    color = detailStrokeColor.copy(alpha = alpha),
                    start = Offset(px(0.28f), py(0.79f)),
                    end = Offset(px(0.72f), py(0.79f)),
                    strokeWidth = fineStroke.width
                )
            }
        }

        PieceType.ROOK -> {
            val rook = Path().apply {
                // Base
                moveTo(px(0.20f), py(0.86f))
                lineTo(px(0.80f), py(0.86f))
                lineTo(px(0.76f), py(0.76f))
                lineTo(px(0.68f), py(0.71f))
                // Tower body
                lineTo(px(0.65f), py(0.38f))
                lineTo(px(0.74f), py(0.32f))
                // Battlements (3 crenellations)
                lineTo(px(0.74f), py(0.18f))
                lineTo(px(0.62f), py(0.18f))
                lineTo(px(0.62f), py(0.25f))
                lineTo(px(0.55f), py(0.25f))
                lineTo(px(0.55f), py(0.18f))
                lineTo(px(0.45f), py(0.18f))
                lineTo(px(0.45f), py(0.25f))
                lineTo(px(0.38f), py(0.25f))
                lineTo(px(0.38f), py(0.18f))
                lineTo(px(0.26f), py(0.18f))
                lineTo(px(0.26f), py(0.32f))
                lineTo(px(0.35f), py(0.38f))
                lineTo(px(0.32f), py(0.71f))
                lineTo(px(0.24f), py(0.76f))
                close()
            }
            drawPath(rook, brush = fillBrush, alpha = alpha)
            drawPath(rook, color = strokeColor.copy(alpha = alpha), style = outlineStroke)

            if (!style.minimalStyle) {
                drawLine(detailStrokeColor.copy(alpha = alpha), Offset(px(0.34f), py(0.39f)), Offset(px(0.66f), py(0.39f)), fineStroke.width)
                drawLine(detailStrokeColor.copy(alpha = alpha), Offset(px(0.32f), py(0.71f)), Offset(px(0.68f), py(0.71f)), fineStroke.width)
                drawLine(detailStrokeColor.copy(alpha = alpha), Offset(px(0.25f), py(0.79f)), Offset(px(0.75f), py(0.79f)), fineStroke.width)
            }
        }

        PieceType.KNIGHT -> {
            val knight = Path().apply {
                moveTo(px(0.21f), py(0.86f))
                lineTo(px(0.80f), py(0.86f))
                lineTo(px(0.77f), py(0.76f))
                // Sweeping arched mane back
                cubicTo(px(0.78f), py(0.50f), px(0.74f), py(0.26f), px(0.55f), py(0.18f))
                // Ear tip
                lineTo(px(0.49f), py(0.12f))
                lineTo(px(0.43f), py(0.20f))
                // Forehead & snout
                cubicTo(px(0.31f), py(0.24f), px(0.20f), py(0.37f), px(0.18f), py(0.48f))
                cubicTo(px(0.17f), py(0.54f), px(0.24f), py(0.58f), px(0.30f), py(0.54f))
                // Jaw tuck
                lineTo(px(0.42f), py(0.48f))
                cubicTo(px(0.42f), py(0.57f), px(0.30f), py(0.66f), px(0.24f), py(0.76f))
                close()
            }
            drawPath(knight, brush = fillBrush, alpha = alpha)
            drawPath(knight, color = strokeColor.copy(alpha = alpha), style = outlineStroke)

            // Eye dot
            drawCircle(
                color = strokeColor.copy(alpha = alpha),
                radius = s * 0.032f,
                center = Offset(px(0.38f), py(0.32f))
            )
            // Nostril & mane detail
            if (!style.minimalStyle) {
                val mane = Path().apply {
                    moveTo(px(0.58f), py(0.27f))
                    cubicTo(px(0.68f), py(0.42f), px(0.68f), py(0.58f), px(0.64f), py(0.73f))
                }
                drawPath(mane, color = detailStrokeColor.copy(alpha = alpha), style = fineStroke)
                drawLine(detailStrokeColor.copy(alpha = alpha), Offset(px(0.25f), py(0.79f)), Offset(px(0.75f), py(0.79f)), fineStroke.width)
            }
        }

        PieceType.BISHOP -> {
            // Finial top ball
            drawCircle(
                brush = fillBrush,
                radius = s * 0.06f,
                center = Offset(px(0.50f), py(0.15f)),
                alpha = alpha
            )
            drawCircle(
                color = strokeColor.copy(alpha = alpha),
                radius = s * 0.06f,
                center = Offset(px(0.50f), py(0.15f)),
                style = fineStroke
            )

            // Mitre & body
            val bishop = Path().apply {
                moveTo(px(0.20f), py(0.86f))
                lineTo(px(0.80f), py(0.86f))
                lineTo(px(0.75f), py(0.77f))
                cubicTo(px(0.65f), py(0.71f), px(0.60f), py(0.62f), px(0.61f), py(0.54f))
                cubicTo(px(0.69f), py(0.43f), px(0.64f), py(0.25f), px(0.50f), py(0.21f))
                cubicTo(px(0.36f), py(0.25f), px(0.31f), py(0.43f), px(0.39f), py(0.54f))
                cubicTo(px(0.40f), py(0.62f), px(0.35f), py(0.71f), px(0.25f), py(0.77f))
                close()
            }
            drawPath(bishop, brush = fillBrush, alpha = alpha)
            drawPath(bishop, color = strokeColor.copy(alpha = alpha), style = outlineStroke)

            // Diagonal mitre slit & cross mark
            drawLine(
                color = strokeColor.copy(alpha = alpha),
                start = Offset(px(0.50f), py(0.30f)),
                end = Offset(px(0.59f), py(0.39f)),
                strokeWidth = outlineStroke.width * 0.9f,
                cap = StrokeCap.Round
            )
            if (!style.minimalStyle) {
                drawLine(detailStrokeColor.copy(alpha = alpha), Offset(px(0.39f), py(0.55f)), Offset(px(0.61f), py(0.55f)), fineStroke.width)
                drawLine(detailStrokeColor.copy(alpha = alpha), Offset(px(0.28f), py(0.78f)), Offset(px(0.72f), py(0.78f)), fineStroke.width)
            }
        }

        PieceType.QUEEN -> {
            // Five-point royal coronet
            val queen = Path().apply {
                moveTo(px(0.21f), py(0.86f))
                lineTo(px(0.79f), py(0.86f))
                lineTo(px(0.75f), py(0.74f))
                lineTo(px(0.84f), py(0.32f))
                lineTo(px(0.68f), py(0.52f))
                lineTo(px(0.67f), py(0.24f))
                lineTo(px(0.56f), py(0.48f))
                lineTo(px(0.50f), py(0.20f))
                lineTo(px(0.44f), py(0.48f))
                lineTo(px(0.33f), py(0.24f))
                lineTo(px(0.32f), py(0.52f))
                lineTo(px(0.16f), py(0.32f))
                lineTo(px(0.25f), py(0.74f))
                close()
            }
            drawPath(queen, brush = fillBrush, alpha = alpha)
            drawPath(queen, color = strokeColor.copy(alpha = alpha), style = outlineStroke)

            // Pearls on crown points
            val tips = listOf(
                Offset(px(0.16f), py(0.29f)),
                Offset(px(0.33f), py(0.21f)),
                Offset(px(0.50f), py(0.17f)),
                Offset(px(0.67f), py(0.21f)),
                Offset(px(0.84f), py(0.29f))
            )
            for (tip in tips) {
                drawCircle(brush = fillBrush, radius = s * 0.044f, center = tip, alpha = alpha)
                drawCircle(color = strokeColor.copy(alpha = alpha), radius = s * 0.044f, center = tip, style = fineStroke)
            }

            if (!style.minimalStyle) {
                drawLine(detailStrokeColor.copy(alpha = alpha), Offset(px(0.27f), py(0.72f)), Offset(px(0.73f), py(0.72f)), fineStroke.width)
                drawLine(detailStrokeColor.copy(alpha = alpha), Offset(px(0.25f), py(0.79f)), Offset(px(0.75f), py(0.79f)), fineStroke.width)
            }
        }

        PieceType.KING -> {
            // Sovereign Cross atop crown
            drawLine(
                color = strokeColor.copy(alpha = alpha),
                start = Offset(px(0.50f), py(0.09f)),
                end = Offset(px(0.50f), py(0.27f)),
                strokeWidth = outlineStroke.width * 1.4f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = strokeColor.copy(alpha = alpha),
                start = Offset(px(0.41f), py(0.16f)),
                end = Offset(px(0.59f), py(0.16f)),
                strokeWidth = outlineStroke.width * 1.4f,
                cap = StrokeCap.Round
            )

            // Imperial Crown lobes & pedestal
            val king = Path().apply {
                moveTo(px(0.21f), py(0.86f))
                lineTo(px(0.79f), py(0.86f))
                lineTo(px(0.75f), py(0.74f))
                cubicTo(px(0.86f), py(0.56f), px(0.86f), py(0.32f), px(0.66f), py(0.32f))
                cubicTo(px(0.58f), py(0.32f), px(0.54f), py(0.37f), px(0.50f), py(0.41f))
                cubicTo(px(0.46f), py(0.37f), px(0.42f), py(0.32f), px(0.34f), py(0.32f))
                cubicTo(px(0.14f), py(0.32f), px(0.14f), py(0.56f), px(0.25f), py(0.74f))
                close()
            }
            drawPath(king, brush = fillBrush, alpha = alpha)
            drawPath(king, color = strokeColor.copy(alpha = alpha), style = outlineStroke)

            // Center finial arch
            val centerArch = Path().apply {
                moveTo(px(0.50f), py(0.68f))
                lineTo(px(0.50f), py(0.28f))
            }
            drawPath(centerArch, color = detailStrokeColor.copy(alpha = alpha), style = fineStroke)

            if (!style.minimalStyle) {
                drawArc(
                    color = detailStrokeColor.copy(alpha = alpha),
                    startAngle = 195f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(px(0.27f), py(0.68f)),
                    size = Size(s * 0.46f, s * 0.14f),
                    style = fineStroke
                )
                drawLine(detailStrokeColor.copy(alpha = alpha), Offset(px(0.25f), py(0.79f)), Offset(px(0.75f), py(0.79f)), fineStroke.width)
            }
        }
    }
}
