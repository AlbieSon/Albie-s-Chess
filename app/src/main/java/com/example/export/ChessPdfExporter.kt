package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.chess.AnalyzedMove
import com.example.chess.ChessBoardState
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.data.GameRecordEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 100% offline PDF Score Sheet & Analysis Report generator using Android's native PdfDocument,
 * Android PrintManager for offline printing, SAF for saving locally, and FileProvider for
 * zero-permission sharing to Email (Gmail) or Google Drive.
 */
object ChessPdfExporter {

    private const val PAGE_WIDTH = 595 // A4 points (72 dpi)
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    fun buildPdfDocument(
        game: GameRecordEntity,
        analyzedMoves: List<AnalyzedMove> = emptyList(),
        playerName: String = "Player"
    ): PdfDocument {
        val doc = PdfDocument()
        val finalBoard = if (game.currentFen.isNotBlank()) {
            ChessBoardState.fromFen(game.currentFen)
        } else {
            ChessBoardState.replayUciMoves(game.movesUciList()).first
        }

        val movesSan = game.movesSanList()
        val dateStr = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.US).format(Date(game.updatedAtMillis))

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(22, 36, 29)
            textSize = 22f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(88, 102, 95)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(22, 36, 29)
            textSize = 12.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(92, 105, 98)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(24, 30, 27)
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(25, 32, 28)
            textSize = 10f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        val accentGoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(184, 138, 42)
            strokeWidth = 2f
        }
        val cardFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(246, 244, 238)
            style = Paint.Style.FILL
        }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(218, 214, 202)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        // Page 1: Header + Match Summary + Final Position Diagram + Beginning of Score Sheet
        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
        var canvas = page.canvas

        // Header banner
        canvas.drawText("Albie's Chess", MARGIN, 58f, titlePaint)
        canvas.drawText("Official Offline Tournament Score Sheet & Engine Report", MARGIN, 74f, subtitlePaint)
        canvas.drawText(dateStr, PAGE_WIDTH - MARGIN - subtitlePaint.measureText(dateStr), 58f, subtitlePaint)
        canvas.drawLine(MARGIN, 84f, PAGE_WIDTH - MARGIN, 84f, accentGoldPaint)

        // Left metadata card & Right mini chessboard
        val topY = 98f
        val diagramSize = 176f
        val diagramX = PAGE_WIDTH - MARGIN - diagramSize
        val metaWidth = diagramX - MARGIN - 16f

        val metaRect = RectF(MARGIN, topY, MARGIN + metaWidth, topY + diagramSize)
        canvas.drawRoundRect(metaRect, 8f, 8f, cardFillPaint)
        canvas.drawRoundRect(metaRect, 8f, 8f, cardBorderPaint)

        var y = topY + 20f
        val col1 = MARGIN + 12f
        val col2 = MARGIN + metaWidth * 0.52f

        fun drawMetaPair(x: Float, yy: Float, label: String, value: String) {
            canvas.drawText(label.uppercase(Locale.US), x, yy, labelPaint)
            canvas.drawText(value, x, yy + 14f, valuePaint)
        }

        val cleanName = playerName.trim().ifEmpty { "Player" }
        val whitePlayer = if (game.playerColor == PieceColor.WHITE) "$cleanName (White)" else "Stockfish (${game.engineStyle.displayName})"
        val blackPlayer = if (game.playerColor == PieceColor.BLACK) "$cleanName (Black)" else "Stockfish (${game.engineStyle.displayName})"

        drawMetaPair(col1, y, "White", whitePlayer)
        drawMetaPair(col2, y, "Black", blackPlayer)
        y += 34f
        drawMetaPair(col1, y, "Result", game.gameResult.displayLabel)
        drawMetaPair(col2, y, "Time Control", game.timeControl.fullLabel)
        y += 34f
        drawMetaPair(col1, y, "Opening", "${game.openingEco} · ${game.openingName.take(28)}")
        drawMetaPair(col2, y, "Total Moves", "${(movesSan.size + 1) / 2} moves (${movesSan.size} plies)")
        y += 34f
        val accText = if (game.playerAccuracy > 0) String.format(Locale.US, "%.1f%%", game.playerAccuracy) else "—"
        val guessText = if (game.guessAttempts > 0) {
            String.format(Locale.US, "%d/%d Best (%.0f%%)", game.guessExactMatches, game.guessAttempts, game.guessAvgAccuracy)
        } else "None recorded"
        drawMetaPair(col1, y, "Match Accuracy", accText)
        drawMetaPair(col2, y, "Guess the Move", guessText)

        // Draw crisp vector final board snapshot on right
        drawMiniBoard(canvas, finalBoard, diagramX, topY, diagramSize)

        // Move Score Sheet section
        y = topY + diagramSize + 28f
        canvas.drawText("Algebraic Score Sheet & Engine Evaluation", MARGIN, y, sectionPaint)
        y += 8f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, cardBorderPaint)
        y += 18f

        // Table headers (2 columns of move pairs on a page)
        val totalFullMoves = (movesSan.size + 1) / 2
        val evals = game.evalsCpList()

        fun drawTableHeader(c: Canvas, yy: Float) {
            c.drawText("#", MARGIN + 4f, yy, labelPaint)
            c.drawText("WHITE", MARGIN + 34f, yy, labelPaint)
            c.drawText("EVAL", MARGIN + 115f, yy, labelPaint)
            c.drawText("BLACK", MARGIN + 180f, yy, labelPaint)
            c.drawText("EVAL", MARGIN + 265f, yy, labelPaint)
            c.drawText("NOTES / BEST LINE", MARGIN + 330f, yy, labelPaint)
        }

        drawTableHeader(canvas, y)
        y += 14f

        val rowHeight = 16.5f
        for (moveIdx in 0 until totalFullMoves) {
            if (y > PAGE_HEIGHT - 55f) {
                drawPageFooter(canvas, pageNum)
                doc.finishPage(page)
                pageNum++
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
                canvas = page.canvas
                canvas.drawText("Albie's Chess — Score Sheet (Continued)", MARGIN, 46f, sectionPaint)
                canvas.drawLine(MARGIN, 54f, PAGE_WIDTH - MARGIN, 54f, accentGoldPaint)
                y = 74f
                drawTableHeader(canvas, y)
                y += 14f
            }

            if (moveIdx % 2 == 1) {
                canvas.drawRect(MARGIN, y - 11f, PAGE_WIDTH - MARGIN, y + 4f, cardFillPaint)
            }

            val whitePly = moveIdx * 2
            val blackPly = whitePly + 1
            val wSan = movesSan.getOrNull(whitePly).orEmpty()
            val bSan = movesSan.getOrNull(blackPly).orEmpty()

            val wAnalyzed = analyzedMoves.getOrNull(whitePly)
            val bAnalyzed = analyzedMoves.getOrNull(blackPly)

            val wEvalStr = formatCpShort(wAnalyzed?.evalAfterWhiteCp ?: evals.getOrNull(whitePly))
            val bEvalStr = formatCpShort(bAnalyzed?.evalAfterWhiteCp ?: evals.getOrNull(blackPly))

            val wGlyph = wAnalyzed?.classification?.glyph?.takeIf { it != "✓" }.orEmpty()
            val bGlyph = bAnalyzed?.classification?.glyph?.takeIf { it != "✓" }.orEmpty()

            canvas.drawText("${moveIdx + 1}.", MARGIN + 4f, y, monoPaint)
            canvas.drawText("$wSan$wGlyph", MARGIN + 34f, y, monoPaint)
            canvas.drawText(wEvalStr, MARGIN + 115f, y, monoPaint)
            if (bSan.isNotEmpty()) {
                canvas.drawText("$bSan$bGlyph", MARGIN + 180f, y, monoPaint)
                canvas.drawText(bEvalStr, MARGIN + 265f, y, monoPaint)
            }

            val note = buildString {
                if (wAnalyzed != null && wAnalyzed.cpLoss >= 80) {
                    append("W best: ${wAnalyzed.bestMoveSan}  ")
                }
                if (bAnalyzed != null && bAnalyzed.cpLoss >= 80) {
                    append("B best: ${bAnalyzed.bestMoveSan}")
                }
            }
            if (note.isNotEmpty()) {
                canvas.drawText(note.take(32), MARGIN + 330f, y, subtitlePaint)
            }

            y += rowHeight
        }

        // FEN footnote
        if (y < PAGE_HEIGHT - 70f) {
            y += 14f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, cardBorderPaint)
            y += 14f
            canvas.drawText("FINAL FEN: ${finalBoard.toFen()}", MARGIN, y, subtitlePaint)
        }

        drawPageFooter(canvas, pageNum)
        doc.finishPage(page)
        return doc
    }

    private fun formatCpShort(cp: Int?): String {
        if (cp == null) return "—"
        if (kotlin.math.abs(cp) > 800_000) return if (cp > 0) "+M" else "-M"
        val p = cp / 100.0
        return if (cp >= 0) String.format(Locale.US, "+%.1f", p) else String.format(Locale.US, "%.1f", p)
    }

    private fun drawPageFooter(canvas: Canvas, pageNum: Int) {
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(120, 130, 125)
            textSize = 9f
        }
        val left = "Generated offline by Albie's Chess"
        val right = "Page $pageNum"
        canvas.drawText(left, MARGIN, PAGE_HEIGHT - 22f, footerPaint)
        canvas.drawText(right, PAGE_WIDTH - MARGIN - footerPaint.measureText(right), PAGE_HEIGHT - 22f, footerPaint)
    }

    private fun drawMiniBoard(canvas: Canvas, board: ChessBoardState, x: Float, y: Float, size: Float) {
        val sqSize = size / 8f
        val lightPaint = Paint().apply { color = Color.rgb(238, 238, 210) }
        val darkPaint = Paint().apply { color = Color.rgb(104, 138, 86) }
        val borderPaint = Paint().apply {
            color = Color.rgb(31, 52, 32)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val piecePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sqSize * 0.72f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val pieceStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sqSize * 0.72f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            style = Paint.Style.STROKE
            strokeWidth = 1.4f
        }

        for (r in 0 until 8) {
            for (f in 0 until 8) {
                val left = x + f * sqSize
                val top = y + r * sqSize
                val isLight = (r + f) % 2 == 0
                canvas.drawRect(left, top, left + sqSize, top + sqSize, if (isLight) lightPaint else darkPaint)
                val piece = board.squares[r * 8 + f]
                if (piece != null) {
                    val glyph = unicodeGlyph(piece.type, piece.color)
                    val cx = left + sqSize / 2f
                    val cy = top + sqSize * 0.76f
                    if (piece.color == PieceColor.WHITE) {
                        pieceStrokePaint.color = Color.rgb(20, 20, 20)
                        piecePaint.color = Color.WHITE
                        canvas.drawText(glyph, cx, cy, pieceStrokePaint)
                        canvas.drawText(glyph, cx, cy, piecePaint)
                    } else {
                        pieceStrokePaint.color = Color.rgb(245, 240, 230)
                        piecePaint.color = Color.rgb(22, 24, 26)
                        canvas.drawText(glyph, cx, cy, pieceStrokePaint)
                        canvas.drawText(glyph, cx, cy, piecePaint)
                    }
                }
            }
        }
        canvas.drawRect(x, y, x + size, y + size, borderPaint)
    }

    private fun unicodeGlyph(type: PieceType, color: PieceColor): String = when (color) {
        PieceColor.WHITE -> when (type) {
            PieceType.KING -> "♔"
            PieceType.QUEEN -> "♕"
            PieceType.ROOK -> "♖"
            PieceType.BISHOP -> "♗"
            PieceType.KNIGHT -> "♘"
            PieceType.PAWN -> "♙"
        }
        PieceColor.BLACK -> when (type) {
            PieceType.KING -> "♚"
            PieceType.QUEEN -> "♛"
            PieceType.ROOK -> "♜"
            PieceType.BISHOP -> "♝"
            PieceType.KNIGHT -> "♞"
            PieceType.PAWN -> "♟"
        }
    }

    fun generatePdfCacheFile(
        context: Context,
        game: GameRecordEntity,
        analyzedMoves: List<AnalyzedMove> = emptyList(),
        playerName: String = "Player"
    ): File {
        val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(game.updatedAtMillis))
        val safeOpening = game.openingEco.replace(Regex("[^A-Za-z0-9]"), "")
        val outFile = File(dir, "AlbiesChess_${safeOpening}_$stamp.pdf")
        val doc = buildPdfDocument(game, analyzedMoves, playerName)
        try {
            FileOutputStream(outFile).use { fos ->
                doc.writeTo(fos)
            }
        } finally {
            doc.close()
        }
        return outFile
    }

    fun writePdfToUri(
        context: Context,
        uri: Uri,
        game: GameRecordEntity,
        analyzedMoves: List<AnalyzedMove> = emptyList(),
        playerName: String = "Player"
    ) {
        val doc = buildPdfDocument(game, analyzedMoves, playerName)
        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                doc.writeTo(out)
            }
        } finally {
            doc.close()
        }
    }

    /**
     * Launches Android's native Share Sheet with a secure FileProvider PDF URI.
     * Supports Gmail / Email apps and "Save to Google Drive" directly with zero storage permissions.
     */
    fun sharePdf(
        context: Context,
        game: GameRecordEntity,
        analyzedMoves: List<AnalyzedMove> = emptyList(),
        recipientEmail: String = "",
        playerName: String = "Player"
    ) {
        val file = generatePdfCacheFile(context, game, analyzedMoves, playerName)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val cleanName = playerName.trim().ifEmpty { "Player" }
        val subject = "Albie's Chess Match Report ($cleanName): ${game.openingEco} ${game.openingName} (${game.gameResult.pgnResult})"
        val body = buildString {
            appendLine("Albie's Chess — Offline Match Summary")
            appendLine("Player: $cleanName (${game.playerColor.label})")
            appendLine("Opening: ${game.openingEco} ${game.openingName}")
            appendLine("Result: ${game.gameResult.displayLabel}")
            if (game.playerAccuracy > 0) appendLine("Accuracy: ${String.format(Locale.US, "%.1f%%", game.playerAccuracy)}")
            if (game.guessAttempts > 0) appendLine("Guess the Move: ${game.guessExactMatches}/${game.guessAttempts} Best")
            appendLine("Attached: Printable Tournament Score Sheet PDF.")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            if (recipientEmail.isNotBlank()) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Send PDF to Email or Google Drive")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Opens Android's native Print spooler for offline printing or "Save as PDF".
     */
    fun printPdf(
        context: Context,
        game: GameRecordEntity,
        analyzedMoves: List<AnalyzedMove> = emptyList(),
        playerName: String = "Player"
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "AlbiesChess_${game.openingEco}_Report"
        val cachedFile = generatePdfCacheFile(context, game, analyzedMoves, playerName)

        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(cachedFile.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                try {
                    cachedFile.inputStream().use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }
        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
    }
}
