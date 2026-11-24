package com.example.ui.board

import androidx.compose.ui.graphics.Color

enum class BoardThemeStyle(
    val displayName: String,
    val subtitle: String,
    val lightSquare: Color,
    val darkSquare: Color,
    val lastMoveLight: Color,
    val lastMoveDark: Color,
    val selectedSquare: Color,
    val frameColor: Color,
    val coordOnLight: Color,
    val coordOnDark: Color
) {
    TOURNAMENT_WALNUT(
        displayName = "Tournament Walnut",
        subtitle = "Hand-rubbed walnut & maple",
        lightSquare = Color(0xFFEDD9B9),
        darkSquare = Color(0xFFB58252),
        lastMoveLight = Color(0xFFF6E77B),
        lastMoveDark = Color(0xFFD9BC47),
        selectedSquare = Color(0xFFE7C654),
        frameColor = Color(0xFF3E2918),
        coordOnLight = Color(0xFF8C5B32),
        coordOnDark = Color(0xFFEDD9B9)
    ),
    CHAMPIONSHIP_EMERALD(
        displayName = "Championship Emerald",
        subtitle = "Classic FIDE club tournament green",
        lightSquare = Color(0xFFEEEED2),
        darkSquare = Color(0xFF567D46),
        lastMoveLight = Color(0xFFF7F17E),
        lastMoveDark = Color(0xFFAEC744),
        selectedSquare = Color(0xFFE0D256),
        frameColor = Color(0xFF1F3420),
        coordOnLight = Color(0xFF4B6E3D),
        coordOnDark = Color(0xFFEEEED2)
    ),
    GRANDMASTER_SLATE(
        displayName = "Grandmaster Slate",
        subtitle = "Cool architectural slate & alabaster",
        lightSquare = Color(0xFFE3E9EF),
        darkSquare = Color(0xFF6B859E),
        lastMoveLight = Color(0xFFCDE5F8),
        lastMoveDark = Color(0xFF538CC2),
        selectedSquare = Color(0xFF9AC5EB),
        frameColor = Color(0xFF22303D),
        coordOnLight = Color(0xFF526A82),
        coordOnDark = Color(0xFFE3E9EF)
    ),
    MAHOGANY_PRESTIGE(
        displayName = "Mahogany Prestige",
        subtitle = "Deep rosewood mahogany & champagne",
        lightSquare = Color(0xFFF2DFC7),
        darkSquare = Color(0xFF8C3B32),
        lastMoveLight = Color(0xFFF5D87B),
        lastMoveDark = Color(0xFFB87042),
        selectedSquare = Color(0xFFE8BE55),
        frameColor = Color(0xFF3D1714),
        coordOnLight = Color(0xFF7D3129),
        coordOnDark = Color(0xFFF2DFC7)
    ),
    MIDNIGHT_OBSIDIAN(
        displayName = "Midnight Obsidian",
        subtitle = "Sleek low-glare studio monochrome",
        lightSquare = Color(0xFF9AA7A0),
        darkSquare = Color(0xFF3A4942),
        lastMoveLight = Color(0xFFC7BA68),
        lastMoveDark = Color(0xFF7D7B3B),
        selectedSquare = Color(0xFFD6BE5C),
        frameColor = Color(0xFF1A2420),
        coordOnLight = Color(0xFF2A3631),
        coordOnDark = Color(0xFFBAC8C0)
    ),
    NORDIC_BIRCH(
        displayName = "Nordic Birch",
        subtitle = "Scandinavian pale oak & linen",
        lightSquare = Color(0xFFF6F1E6),
        darkSquare = Color(0xFFC5AC8E),
        lastMoveLight = Color(0xFFF9E98E),
        lastMoveDark = Color(0xFFD7BC68),
        selectedSquare = Color(0xFFE9CD68),
        frameColor = Color(0xFF594836),
        coordOnLight = Color(0xFF8C7357),
        coordOnDark = Color(0xFFF6F1E6)
    )
}

enum class PieceSetStyle(
    val displayName: String,
    val subtitle: String,
    val whiteFill: Color,
    val whiteAccent: Color,
    val whiteStroke: Color,
    val blackFill: Color,
    val blackAccent: Color,
    val blackStroke: Color,
    val strokeRatio: Float,
    val minimalStyle: Boolean
) {
    CLASSIC_STAUNTON(
        displayName = "Classic Staunton",
        subtitle = "Official tournament silhouette & crisp relief",
        whiteFill = Color(0xFFFCF9F2),
        whiteAccent = Color(0xFFE4DCC8),
        whiteStroke = Color(0xFF1D1B18),
        blackFill = Color(0xFF242426),
        blackAccent = Color(0xFF47484D),
        blackStroke = Color(0xFFF0E9DC),
        strokeRatio = 0.042f,
        minimalStyle = false
    ),
    NEO_MODERN(
        displayName = "Neo Modern",
        subtitle = "Clean high-contrast grandmaster broadcast",
        whiteFill = Color(0xFFFFFFFF),
        whiteAccent = Color(0xFFDCE3E8),
        whiteStroke = Color(0xFF18222B),
        blackFill = Color(0xFF1C2733),
        blackAccent = Color(0xFF3E5266),
        blackStroke = Color(0xFFE8EEF2),
        strokeRatio = 0.036f,
        minimalStyle = true
    ),
    ROYAL_GOLD_ONYX(
        displayName = "Royal Gold & Onyx",
        subtitle = "Brushed champagne ivory & obsidian gold trim",
        whiteFill = Color(0xFFF7E7B7),
        whiteAccent = Color(0xFFD9B253),
        whiteStroke = Color(0xFF2B200B),
        blackFill = Color(0xFF16181A),
        blackAccent = Color(0xFF363C40),
        blackStroke = Color(0xFFE6C260),
        strokeRatio = 0.044f,
        minimalStyle = false
    ),
    WOODEN_CARVED(
        displayName = "Boxwood & Ebony",
        subtitle = "Hand-turned wooden chess club set",
        whiteFill = Color(0xFFF3DCAC),
        whiteAccent = Color(0xFFD3AF74),
        whiteStroke = Color(0xFF3B2614),
        blackFill = Color(0xFF3B2418),
        blackAccent = Color(0xFF63402D),
        blackStroke = Color(0xFFEBD7B2),
        strokeRatio = 0.045f,
        minimalStyle = false
    )
}
