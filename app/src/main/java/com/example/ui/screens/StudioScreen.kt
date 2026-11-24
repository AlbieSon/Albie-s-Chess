package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.ArrowKind
import com.example.chess.BoardArrow
import com.example.chess.ChessBoardState
import com.example.chess.ChessMove
import com.example.chess.Piece
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.data.UserSettings
import com.example.ui.board.BoardThemeStyle
import com.example.ui.board.ChessBoardView
import com.example.ui.board.ChessPieceIcon
import com.example.ui.board.PieceSetStyle
import com.example.ui.theme.JetBrainsMonoFamily
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun StudioScreen(
    settings: UserSettings,
    onPlayerNameChanged: (String) -> Unit,
    onArrowOpacityChanged: (Float) -> Unit,
    onSelectBoardTheme: (BoardThemeStyle) -> Unit,
    onSelectPieceSet: (PieceSetStyle) -> Unit,
    onToggleCoordinates: (Boolean) -> Unit,
    onToggleLegalHints: (Boolean) -> Unit,
    onDefaultEmailChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Classic Italian Game preview position with subtle Blue, Green & Gold demonstration arrows
    val previewBoard = remember {
        ChessBoardState.replayUciMoves(
            listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "f8c5")
        ).first
    }
    val previewArrows = remember {
        listOf(
            BoardArrow(ChessMove.algebraicToSquare("c2"), ChessMove.algebraicToSquare("c3"), ArrowKind.PERFECT_MATCH_GOLD),
            BoardArrow(ChessMove.algebraicToSquare("d2"), ChessMove.algebraicToSquare("d3"), ArrowKind.ENGINE_BEST_GREEN),
            BoardArrow(ChessMove.algebraicToSquare("b1"), ChessMove.algebraicToSquare("c3"), ArrowKind.PLAYER_GUESS_BLUE)
        )
    }

    var nameDraft by remember(settings.playerName) { mutableStateOf(settings.playerName) }
    var emailDraft by remember(settings.defaultExportEmail) { mutableStateOf(settings.defaultExportEmail) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .testTag("studio_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Board & Piece Studio",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Customize your player name, arrow brightness, board surface, and piece set.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Live Interactive Preview showing Board, Pieces & Arrow Lightness/Darkness
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ChessBoardView(
                        board = previewBoard,
                        boardTheme = settings.boardTheme,
                        pieceStyle = settings.pieceSet,
                        flipped = false,
                        interactive = false,
                        showCoordinates = settings.showCoordinates,
                        showLegalHints = settings.showLegalHints,
                        arrows = previewArrows,
                        arrowOpacity = settings.arrowOpacity,
                        modifier = Modifier
                            .fillMaxWidth(0.76f)
                            .widthIn(max = 320.dp)
                    )
                    Text(
                        text = "Live Preview · Gold (Best Match) · Green (Engine Best) · Blue (Your Move)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Arrow Appearance: Lighter <-> Darker
            item {
                val opacityPercent = (settings.arrowOpacity * 100f).roundToInt()
                val presets = remember {
                    listOf(
                        "Light" to 0.30f,
                        "Subtle" to 0.52f,
                        "Medium" to 0.70f,
                        "Dark" to 0.88f
                    )
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ARROW APPEARANCE (LIGHTER / DARKER)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Keep arrows subtle so pieces stay clear and unobstructed.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "$opacityPercent%",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Lighter",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = settings.arrowOpacity,
                                onValueChange = { onArrowOpacityChanged(it) },
                                valueRange = 0.22f..0.90f,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("arrow_opacity_slider")
                            )
                            Text(
                                text = "Darker",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presets.forEach { (label, presetVal) ->
                                val selected = abs(settings.arrowOpacity - presetVal) < 0.05f
                                FilterChip(
                                    selected = selected,
                                    onClick = { onArrowOpacityChanged(presetVal) },
                                    label = { Text(label, maxLines = 1) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("arrow_preset_${label.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            // Player Profile Card (Type in Your Name)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "PLAYER PROFILE",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedTextField(
                            value = nameDraft,
                            onValueChange = {
                                val trimmed = it.take(28)
                                nameDraft = trimmed
                                onPlayerNameChanged(trimmed)
                            },
                            label = { Text("Your Name") },
                            placeholder = { Text("Enter your player name") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Person, contentDescription = null)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("studio_player_name_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Text(
                            text = "Shown beside your board clock, in Match Archive, and on exported PDF score sheets.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Board Styles Grid (2 columns)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "BOARD STYLE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    BoardThemeStyle.entries.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { theme ->
                                val selected = theme == settings.boardTheme
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onSelectBoardTheme(theme) }
                                        .testTag("board_theme_${theme.name.lowercase()}"),
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainer,
                                    border = BorderStroke(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // 2x2 mini checkerboard swatch
                                        Column(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .border(1.dp, theme.frameColor, RoundedCornerShape(6.dp))
                                        ) {
                                            Row(Modifier.weight(1f)) {
                                                Box(Modifier.weight(1f).fillMaxSize().background(theme.lightSquare))
                                                Box(Modifier.weight(1f).fillMaxSize().background(theme.darkSquare))
                                            }
                                            Row(Modifier.weight(1f)) {
                                                Box(Modifier.weight(1f).fillMaxSize().background(theme.darkSquare))
                                                Box(Modifier.weight(1f).fillMaxSize().background(theme.lightSquare))
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = theme.displayName,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = theme.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Piece Styles Selection
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "PIECES STYLE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    PieceSetStyle.entries.forEach { pieceSet ->
                        val selected = pieceSet == settings.pieceSet
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectPieceSet(pieceSet) }
                                .testTag("piece_set_${pieceSet.name.lowercase()}"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                            )
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
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    ChessPieceIcon(Piece(PieceColor.WHITE, PieceType.KING), pieceSet, size = 34.dp)
                                    ChessPieceIcon(Piece(PieceColor.WHITE, PieceType.KNIGHT), pieceSet, size = 32.dp)
                                    ChessPieceIcon(Piece(PieceColor.BLACK, PieceType.QUEEN), pieceSet, size = 34.dp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pieceSet.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = pieceSet.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected piece set",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Board Display Toggles & PDF Email Recipient Prefill
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "DISPLAY & PDF EXPORT PREFERENCES",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Board Coordinates (a–h, 1–8)", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Display rank and file labels along board edges",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.showCoordinates,
                                onCheckedChange = onToggleCoordinates,
                                modifier = Modifier.testTag("toggle_coordinates")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Legal Move Indicators", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Show target dots & capture rings when selecting pieces",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.showLegalHints,
                                onCheckedChange = onToggleLegalHints,
                                modifier = Modifier.testTag("toggle_legal_hints")
                            )
                        }

                        OutlinedTextField(
                            value = emailDraft,
                            onValueChange = {
                                emailDraft = it
                                onDefaultEmailChanged(it)
                            },
                            label = { Text("Default Email for PDF Score Sheets (Optional)") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Email, contentDescription = null)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("default_export_email_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}
