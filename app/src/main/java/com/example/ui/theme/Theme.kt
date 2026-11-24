package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ColorOutlineDim = SubtleEmeraldBorder.copy(alpha = 0.5f)

private val GrandmasterDarkColorScheme = darkColorScheme(
    primary = ChampagneGold,
    onPrimary = OnGold,
    primaryContainer = GoldContainer,
    onPrimaryContainer = BrightGold,
    secondary = TournamentEmerald,
    onSecondary = OnEmerald,
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = IvoryText,
    tertiary = ArrowBluePlayer,
    onTertiary = IvoryText,
    background = ObsidianForest,
    onBackground = IvoryText,
    surface = DeepClubSurface,
    onSurface = IvoryText,
    surfaceVariant = ElevatedSalonCard,
    onSurfaceVariant = WarmSageText,
    surfaceContainerLowest = ObsidianForest,
    surfaceContainerLow = DeepClubSurface,
    surfaceContainer = ElevatedSalonCard,
    surfaceContainerHigh = SalonCardHigh,
    outline = SubtleEmeraldBorder,
    outlineVariant = ColorOutlineDim,
    error = AlertCrimson,
    onError = IvoryText,
    errorContainer = AlertCrimsonContainer,
    onErrorContainer = IvoryText
)

val ChessShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AlbiesChessTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GrandmasterDarkColorScheme,
        typography = ChessTypography,
        shapes = ChessShapes,
        content = content
    )
}
