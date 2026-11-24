package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.chess.EngineStyle
import com.example.chess.GameResult
import com.example.chess.PieceColor
import com.example.chess.TimeControl

@Entity(tableName = "games")
data class GameRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val playerColorName: String = PieceColor.WHITE.name,
    val engineStyleName: String = EngineStyle.UNIVERSAL.name,
    val timeControlName: String = TimeControl.RAPID_10_5.name,
    val openingEco: String = "START",
    val openingName: String = "Starting Position",
    val resultName: String = GameResult.IN_PROGRESS.name,
    val isPaused: Boolean = false,
    val currentFen: String = "",
    val movesUciCsv: String = "",
    val movesSanPipe: String = "",
    val evalsCpCsv: String = "",
    val whiteTimeMs: Long = 600_000L,
    val blackTimeMs: Long = 600_000L,
    val playerAccuracy: Double = 0.0,
    val guessAttempts: Int = 0,
    val guessExactMatches: Int = 0,
    val guessAvgAccuracy: Double = 0.0,
    val estimatedElo: Int = 0,
    val moveCount: Int = 0
) {
    val playerColor: PieceColor
        get() = runCatching { PieceColor.valueOf(playerColorName) }.getOrDefault(PieceColor.WHITE)

    val engineStyle: EngineStyle
        get() = runCatching { EngineStyle.valueOf(engineStyleName) }.getOrDefault(EngineStyle.UNIVERSAL)

    val timeControl: TimeControl
        get() = runCatching { TimeControl.valueOf(timeControlName) }.getOrDefault(TimeControl.RAPID_10_5)

    val gameResult: GameResult
        get() = runCatching { GameResult.valueOf(resultName) }.getOrDefault(GameResult.IN_PROGRESS)

    fun movesUciList(): List<String> =
        if (movesUciCsv.isBlank()) emptyList() else movesUciCsv.split(",").filter { it.isNotBlank() }

    fun movesSanList(): List<String> =
        if (movesSanPipe.isBlank()) emptyList() else movesSanPipe.split("|").filter { it.isNotBlank() }

    fun evalsCpList(): List<Int> =
        if (evalsCpCsv.isBlank()) emptyList() else evalsCpCsv.split(",").mapNotNull { it.toIntOrNull() }
}
