package com.example.chess

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.ClassBestGold
import com.example.ui.theme.ClassBlunderRed
import com.example.ui.theme.ClassExcellentGreen
import com.example.ui.theme.ClassGoodTeal
import com.example.ui.theme.ClassInaccuracyAmber
import com.example.ui.theme.ClassMistakeOrange

enum class PieceColor {
    WHITE, BLACK;

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
    val label: String get() = if (this == WHITE) "White" else "Black"
}

enum class PieceType(val symbol: Char, val baseValue: Int) {
    PAWN('P', 100),
    KNIGHT('N', 320),
    BISHOP('B', 330),
    ROOK('R', 500),
    QUEEN('Q', 900),
    KING('K', 20000);

    companion object {
        fun fromChar(c: Char): PieceType? = when (c.uppercaseChar()) {
            'P' -> PAWN
            'N' -> KNIGHT
            'B' -> BISHOP
            'R' -> ROOK
            'Q' -> QUEEN
            'K' -> KING
            else -> null
        }
    }
}

data class Piece(
    val color: PieceColor,
    val type: PieceType
) {
    val fenChar: Char
        get() = if (color == PieceColor.WHITE) type.symbol else type.symbol.lowercaseChar()

    companion object {
        fun fromFenChar(c: Char): Piece? {
            val type = PieceType.fromChar(c) ?: return null
            val color = if (c.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
            return Piece(color, type)
        }
    }
}

/**
 * Square index: 0 = a8, 7 = h8, 56 = a1, 63 = h1.
 * rank = sq / 8 (0 is rank 8, 7 is rank 1)
 * file = sq % 8 (0 is file 'a', 7 is file 'h')
 */
data class ChessMove(
    val from: Int,
    val to: Int,
    val promotion: PieceType? = null,
    val isEnPassant: Boolean = false,
    val isCastleKingside: Boolean = false,
    val isCastleQueenside: Boolean = false,
    val capturedPiece: Piece? = null
) {
    fun toUci(): String {
        val promo = promotion?.symbol?.lowercaseChar()?.toString().orEmpty()
        return "${squareToAlgebraic(from)}${squareToAlgebraic(to)}$promo"
    }

    fun sameMoveAs(other: ChessMove?): Boolean {
        if (other == null) return false
        return from == other.from && to == other.to && promotion == other.promotion
    }

    companion object {
        fun squareToAlgebraic(sq: Int): String {
            val file = 'a' + (sq % 8)
            val rank = '8' - (sq / 8)
            return "$file$rank"
        }

        fun algebraicToSquare(alg: String): Int {
            if (alg.length < 2) return -1
            val file = alg[0] - 'a'
            val rank = '8' - alg[1]
            if (file !in 0..7 || rank !in 0..7) return -1
            return rank * 8 + file
        }
    }
}

enum class GameResult(val displayLabel: String, val pgnResult: String) {
    IN_PROGRESS("In Progress", "*"),
    WHITE_WINS_CHECKMATE("White Wins · Checkmate", "1-0"),
    BLACK_WINS_CHECKMATE("Black Wins · Checkmate", "0-1"),
    WHITE_WINS_TIME("White Wins · On Time", "1-0"),
    BLACK_WINS_TIME("Black Wins · On Time", "0-1"),
    WHITE_WINS_RESIGN("White Wins · Resignation", "1-0"),
    BLACK_WINS_RESIGN("Black Wins · Resignation", "0-1"),
    DRAW_STALEMATE("Draw · Stalemate", "1/2-1/2"),
    DRAW_AGREED("Draw · Agreed", "1/2-1/2"),
    DRAW_REPETITION("Draw · Threefold Repetition", "1/2-1/2"),
    DRAW_FIFTY_MOVE("Draw · 50-Move Rule", "1/2-1/2"),
    DRAW_INSUFFICIENT_MATERIAL("Draw · Insufficient Material", "1/2-1/2");

    val isGameOver: Boolean get() = this != IN_PROGRESS
    val isDraw: Boolean
        get() = this == DRAW_STALEMATE || this == DRAW_AGREED ||
            this == DRAW_REPETITION || this == DRAW_FIFTY_MOVE ||
            this == DRAW_INSUFFICIENT_MATERIAL

    fun scoreFor(color: PieceColor): Double = when {
        this == IN_PROGRESS -> 0.5
        isDraw -> 0.5
        color == PieceColor.WHITE && (this == WHITE_WINS_CHECKMATE || this == WHITE_WINS_TIME || this == WHITE_WINS_RESIGN) -> 1.0
        color == PieceColor.BLACK && (this == BLACK_WINS_CHECKMATE || this == BLACK_WINS_TIME || this == BLACK_WINS_RESIGN) -> 1.0
        else -> 0.0
    }
}

/**
 * Engine playing styles. All styles play at Maximum Grandmaster Search Strength;
 * style adjusts strategic evaluation priorities, contempt, and opening repertoire.
 */
enum class EngineStyle(
    val displayName: String,
    val subtitle: String,
    val kingAttackWeight: Double,
    val mobilityWeight: Double,
    val structureWeight: Double,
    val endgameWeight: Double,
    val contemptCp: Int
) {
    UNIVERSAL(
        displayName = "Universal GM",
        subtitle = "Max Strength · Balanced Stockfish-style precision",
        kingAttackWeight = 1.0,
        mobilityWeight = 1.0,
        structureWeight = 1.0,
        endgameWeight = 1.0,
        contemptCp = 12
    ),
    AGGRESSIVE(
        displayName = "Tactical Attacker",
        subtitle = "Max Strength · Relentless king hunts & initiative",
        kingAttackWeight = 1.45,
        mobilityWeight = 1.25,
        structureWeight = 0.85,
        endgameWeight = 0.95,
        contemptCp = 28
    ),
    POSITIONAL(
        displayName = "Positional Master",
        subtitle = "Max Strength · Iron pawn structure & prophylaxis",
        kingAttackWeight = 0.9,
        mobilityWeight = 1.05,
        structureWeight = 1.4,
        endgameWeight = 1.15,
        contemptCp = 8
    ),
    SHARP(
        displayName = "Sharp Gambiteer",
        subtitle = "Max Strength · Concrete lines & dynamic play",
        kingAttackWeight = 1.35,
        mobilityWeight = 1.35,
        structureWeight = 0.8,
        endgameWeight = 0.95,
        contemptCp = 32
    ),
    ENDGAME(
        displayName = "Endgame Specialist",
        subtitle = "Max Strength · Simplification & technical conversion",
        kingAttackWeight = 0.85,
        mobilityWeight = 1.0,
        structureWeight = 1.3,
        endgameWeight = 1.4,
        contemptCp = 10
    )
}

enum class TimeControl(
    val displayName: String,
    val category: String,
    val initialMinutes: Int,
    val incrementSeconds: Int
) {
    CLASSICAL_90_30("90+30", "FIDE Classical", 90, 30),
    CLASSICAL_30_0("30+0", "Classical", 30, 0),
    RAPID_15_10("15+10", "FIDE Rapid", 15, 10),
    RAPID_10_5("10+5", "Rapid", 10, 5),
    BLITZ_5_3("5+3", "Competition Blitz", 5, 3),
    BLITZ_3_2("3+2", "FIDE Blitz", 3, 2),
    BULLET_2_1("2+1", "Bullet", 2, 1),
    UNLIMITED("Unlimited", "No Clock", 0, 0);

    val initialTimeMs: Long get() = initialMinutes * 60_000L
    val incrementMs: Long get() = incrementSeconds * 1_000L
    val isTimed: Boolean get() = this != UNLIMITED
    val fullLabel: String get() = if (isTimed) "$displayName · $category" else "Unlimited · No Clock"
}

enum class MoveClassification(
    val label: String,
    val glyph: String,
    val color: Color
) {
    BEST("Best Move", "★", ClassBestGold),
    EXCELLENT("Excellent", "!", ClassExcellentGreen),
    GOOD("Good", "✓", ClassGoodTeal),
    INACCURACY("Inaccuracy", "?!", ClassInaccuracyAmber),
    MISTAKE("Mistake", "?", ClassMistakeOrange),
    BLUNDER("Blunder", "??", ClassBlunderRed)
}

enum class ArrowKind {
    PLAYER_GUESS_BLUE,
    ENGINE_BEST_GREEN,
    PERFECT_MATCH_GOLD
}

data class BoardArrow(
    val from: Int,
    val to: Int,
    val kind: ArrowKind
)

data class OpeningInfo(
    val eco: String,
    val name: String,
    val description: String
)

data class EngineEvaluation(
    val bestMove: ChessMove?,
    val scoreCpWhite: Int, // Positive = White advantage, Negative = Black advantage
    val mateIn: Int? = null, // Positive = White mates in N, Negative = Black mates in N
    val depth: Int = 0,
    val nodes: Long = 0L,
    val pvSan: List<String> = emptyList()
) {
    fun formatScore(): String {
        if (mateIn != null) {
            return if (mateIn > 0) "M$mateIn" else "-M${-mateIn}"
        }
        val pawns = scoreCpWhite / 100.0
        return when {
            scoreCpWhite >= 0 -> String.format(java.util.Locale.US, "+%.1f", pawns)
            else -> String.format(java.util.Locale.US, "%.1f", pawns)
        }
    }
}

data class GuessComparison(
    val playerMove: ChessMove,
    val playerMoveSan: String,
    val engineBestMove: ChessMove,
    val engineBestSan: String,
    val isExactBest: Boolean,
    val cpLoss: Int,
    val accuracyPercent: Double,
    val classification: MoveClassification,
    val playerEvalWhiteCp: Int,
    val bestEvalWhiteCp: Int,
    val pvSan: List<String> = emptyList()
)

data class AnalyzedMove(
    val ply: Int, // 1-based ply index
    val color: PieceColor,
    val san: String,
    val uci: String,
    val fenBefore: String,
    val fenAfter: String,
    val evalBeforeWhiteCp: Int,
    val evalAfterWhiteCp: Int,
    val bestMoveUci: String,
    val bestMoveSan: String,
    val cpLoss: Int,
    val accuracy: Double,
    val classification: MoveClassification,
    val pvSan: List<String> = emptyList()
)
