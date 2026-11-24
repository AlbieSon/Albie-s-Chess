package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PauseCircleOutline
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.GameRecordEntity
import com.example.data.UserSettings
import com.example.ui.theme.JetBrainsMonoFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    pausedGames: List<GameRecordEntity>,
    completedGames: List<GameRecordEntity>,
    settings: UserSettings,
    onResumeGame: (GameRecordEntity) -> Unit,
    onAnalyzeGame: (GameRecordEntity) -> Unit,
    onSavePdf: (GameRecordEntity) -> Unit,
    onPrintPdf: (GameRecordEntity) -> Unit,
    onSharePdf: (GameRecordEntity) -> Unit,
    onDeleteGame: (Long) -> Unit,
    onStartNewMatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableIntStateOf(if (pausedGames.isNotEmpty()) 0 else 1) }
    var pdfTargetGame by remember { mutableStateOf<GameRecordEntity?>(null) }

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Luxury Editorial Hero Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(152.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_chess_hero_1795546039958),
                        contentDescription = "Albie's Chess tournament archive",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xE80E1612),
                                        Color(0xB30E1612),
                                        Color(0x550E1612)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Match Archive",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color(0xFFF4EFE6)
                            )
                            Text(
                                text = "${settings.displayPlayerName} · 100% Offline Room Storage",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD9B45B)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ArchivePill("COMPLETED", "${completedGames.size}")
                            ArchivePill("PAUSED", "${pausedGames.size}")
                            ArchivePill("PUZZLES", "${settings.solvedPuzzleIds.size}")
                        }
                    }
                }
            }

            // Segmented Filter: Paused vs Completed History
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedSection == 0,
                        onClick = { selectedSection = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("tab_paused_games")
                    ) {
                        Text("Paused (${pausedGames.size})")
                    }
                    SegmentedButton(
                        selected = selectedSection == 1,
                        onClick = { selectedSection = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("tab_completed_games")
                    ) {
                        Text("Completed (${completedGames.size})")
                    }
                }
            }

            val displayedList = if (selectedSection == 0) pausedGames else completedGames

            if (displayedList.isEmpty()) {
                item {
                    EmptyArchiveCard(
                        isPausedTab = selectedSection == 0,
                        onStartNewMatch = onStartNewMatch
                    )
                }
            } else {
                items(displayedList, key = { it.id }) { record ->
                    GameHistoryCard(
                        record = record,
                        onResume = { onResumeGame(record) },
                        onAnalyze = { onAnalyzeGame(record) },
                        onPdf = { pdfTargetGame = record },
                        onDelete = { onDeleteGame(record.id) }
                    )
                }
            }
        }
    }

    pdfTargetGame?.let { target ->
        PdfActionDialog(
            onDismiss = { pdfTargetGame = null },
            onSavePdf = {
                pdfTargetGame = null
                onSavePdf(target)
            },
            onPrintPdf = {
                pdfTargetGame = null
                onPrintPdf(target)
            },
            onSharePdf = {
                pdfTargetGame = null
                onSharePdf(target)
            }
        )
    }
}

@Composable
private fun ArchivePill(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xCC15201B),
        border = BorderStroke(1.dp, Color(0x55D9B45B))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFA9B9AF)
            )
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFFF4EFE6)
            )
        }
    }
}

@Composable
private fun GameHistoryCard(
    record: GameRecordEntity,
    onResume: () -> Unit,
    onAnalyze: () -> Unit,
    onPdf: () -> Unit,
    onDelete: () -> Unit
) {
    val dateLabel = remember(record.updatedAtMillis) {
        SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.US).format(Date(record.updatedAtMillis))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = record.openingEco,
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = record.openingName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "$dateLabel · ${record.playerColor.label} vs ${record.engineStyle.displayName} · ${record.timeControl.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete saved game",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Metrics chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(
                    text = if (record.isPaused) "Paused · ${record.moveCount} plies" else record.gameResult.displayLabel,
                    highlighted = !record.isPaused
                )
                if (record.playerAccuracy > 0.0) {
                    MetricChip(
                        text = String.format(Locale.US, "Acc %.0f%%", record.playerAccuracy),
                        highlighted = false
                    )
                }
                if (record.guessAttempts > 0) {
                    MetricChip(
                        text = "Guess ${record.guessExactMatches}/${record.guessAttempts}",
                        highlighted = false
                    )
                }
            }

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (record.isPaused) {
                    Button(
                        onClick = onResume,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("resume_saved_game_${record.id}"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Resume Match")
                    }
                } else {
                    Button(
                        onClick = onAnalyze,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("analyze_saved_game_${record.id}"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Analyze")
                    }
                }
                FilledTonalButton(
                    onClick = onPdf,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("pdf_saved_game_${record.id}")
                ) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("PDF / Share")
                }
            }
        }
    }
}

@Composable
private fun MetricChip(text: String, highlighted: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (highlighted) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (highlighted) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyArchiveCard(
    isPausedTab: Boolean,
    onStartNewMatch: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (isPausedTab) Icons.Rounded.PauseCircleOutline else Icons.Rounded.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(42.dp)
            )
            Text(
                text = if (isPausedTab) "No Paused Matches" else "No Completed Matches Yet",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = if (isPausedTab) {
                    "Tap Pause while playing to save your board, clocks, and Guess-the-Move progress offline."
                } else {
                    "Finish a match against Stockfish to review full engine analysis and printable PDF score sheets."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Button(onClick = onStartNewMatch, shape = RoundedCornerShape(12.dp)) {
                Text("Start New Match")
            }
        }
    }
}
