package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.chess.EngineStyle
import com.example.chess.TimeControl
import com.example.ui.board.BoardThemeStyle
import com.example.ui.board.PieceSetStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.chessPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "albies_chess_preferences"
)

data class UserSettings(
    val playerName: String = "Albie",
    val boardTheme: BoardThemeStyle = BoardThemeStyle.CHAMPIONSHIP_EMERALD,
    val pieceSet: PieceSetStyle = PieceSetStyle.CLASSIC_STAUNTON,
    val preferredEngineStyle: EngineStyle = EngineStyle.UNIVERSAL,
    val preferredTimeControl: TimeControl = TimeControl.RAPID_10_5,
    val showCoordinates: Boolean = true,
    val showLegalHints: Boolean = true,
    val arrowOpacity: Float = 0.52f, // Subtle default; user can slide lighter (0.22f) to darker (0.90f)
    val completedGamesCount: Int = 0,
    val defaultExportEmail: String = "",
    val solvedPuzzleIds: Set<String> = emptySet()
) {
    val displayPlayerName: String
        get() = playerName.trim().ifEmpty { "Player" }
}

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val PLAYER_NAME = stringPreferencesKey("player_name")
        val BOARD_THEME = stringPreferencesKey("board_theme")
        val PIECE_SET = stringPreferencesKey("piece_set")
        val ENGINE_STYLE = stringPreferencesKey("engine_style")
        val TIME_CONTROL = stringPreferencesKey("time_control")
        val SHOW_COORDS = booleanPreferencesKey("show_coords")
        val SHOW_HINTS = booleanPreferencesKey("show_hints")
        val ARROW_OPACITY = floatPreferencesKey("arrow_opacity")
        val COMPLETED_GAMES = intPreferencesKey("completed_games")
        val DEFAULT_EMAIL = stringPreferencesKey("default_email")
        val SOLVED_PUZZLES = stringSetPreferencesKey("solved_puzzles")
    }

    val settingsFlow: Flow<UserSettings> = context.chessPrefsDataStore.data.map { prefs ->
        UserSettings(
            playerName = prefs[Keys.PLAYER_NAME] ?: "Albie",
            boardTheme = prefs[Keys.BOARD_THEME]?.let {
                runCatching { BoardThemeStyle.valueOf(it) }.getOrNull()
            } ?: BoardThemeStyle.CHAMPIONSHIP_EMERALD,
            pieceSet = prefs[Keys.PIECE_SET]?.let {
                runCatching { PieceSetStyle.valueOf(it) }.getOrNull()
            } ?: PieceSetStyle.CLASSIC_STAUNTON,
            preferredEngineStyle = prefs[Keys.ENGINE_STYLE]?.let {
                runCatching { EngineStyle.valueOf(it) }.getOrNull()
            } ?: EngineStyle.UNIVERSAL,
            preferredTimeControl = prefs[Keys.TIME_CONTROL]?.let {
                runCatching { TimeControl.valueOf(it) }.getOrNull()
            } ?: TimeControl.RAPID_10_5,
            showCoordinates = prefs[Keys.SHOW_COORDS] ?: true,
            showLegalHints = prefs[Keys.SHOW_HINTS] ?: true,
            arrowOpacity = (prefs[Keys.ARROW_OPACITY] ?: 0.52f).coerceIn(0.22f, 0.90f),
            completedGamesCount = prefs[Keys.COMPLETED_GAMES] ?: 0,
            defaultExportEmail = prefs[Keys.DEFAULT_EMAIL] ?: "",
            solvedPuzzleIds = prefs[Keys.SOLVED_PUZZLES] ?: emptySet()
        )
    }

    suspend fun setPlayerName(name: String) {
        context.chessPrefsDataStore.edit { it[Keys.PLAYER_NAME] = name.take(32) }
    }

    suspend fun setBoardTheme(theme: BoardThemeStyle) {
        context.chessPrefsDataStore.edit { it[Keys.BOARD_THEME] = theme.name }
    }

    suspend fun setPieceSet(pieceSet: PieceSetStyle) {
        context.chessPrefsDataStore.edit { it[Keys.PIECE_SET] = pieceSet.name }
    }

    suspend fun setEngineStyle(style: EngineStyle) {
        context.chessPrefsDataStore.edit { it[Keys.ENGINE_STYLE] = style.name }
    }

    suspend fun setTimeControl(tc: TimeControl) {
        context.chessPrefsDataStore.edit { it[Keys.TIME_CONTROL] = tc.name }
    }

    suspend fun setShowCoordinates(show: Boolean) {
        context.chessPrefsDataStore.edit { it[Keys.SHOW_COORDS] = show }
    }

    suspend fun setShowLegalHints(show: Boolean) {
        context.chessPrefsDataStore.edit { it[Keys.SHOW_HINTS] = show }
    }

    suspend fun setArrowOpacity(opacity: Float) {
        context.chessPrefsDataStore.edit { it[Keys.ARROW_OPACITY] = opacity.coerceIn(0.22f, 0.90f) }
    }

    suspend fun setDefaultExportEmail(email: String) {
        context.chessPrefsDataStore.edit { it[Keys.DEFAULT_EMAIL] = email.trim() }
    }

    suspend fun incrementCompletedGames() {
        context.chessPrefsDataStore.edit { prefs ->
            prefs[Keys.COMPLETED_GAMES] = (prefs[Keys.COMPLETED_GAMES] ?: 0) + 1
        }
    }

    suspend fun markPuzzleSolved(puzzleId: String) {
        context.chessPrefsDataStore.edit { prefs ->
            val current = prefs[Keys.SOLVED_PUZZLES] ?: emptySet()
            prefs[Keys.SOLVED_PUZZLES] = current + puzzleId
        }
    }
}
