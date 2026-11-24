package com.example.ui.board

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.StockfishEngine
import com.example.ui.theme.EvalBlackFill
import com.example.ui.theme.EvalWhiteFill
import com.example.ui.theme.JetBrainsMonoFamily
import java.util.Locale
import kotlin.math.abs

/**
 * Vertical Stockfish-style Evaluation Bar placed alongside the chessboard.
 * Smoothly rises and falls with spring physics to show real-time White/Black advantage.
 */
@Composable
fun EvaluationBar(
    scoreCpWhite: Int,
    mateIn: Int?,
    flipped: Boolean,
    modifier: Modifier = Modifier
) {
    val targetWhiteRatio = StockfishEngine.evalToWhiteRatio(scoreCpWhite, mateIn)
    val animatedWhiteRatio by animateFloatAsState(
        targetValue = targetWhiteRatio,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "eval_bar_ratio"
    )

    val scoreLabel = when {
        mateIn != null -> "M${abs(mateIn)}"
        abs(scoreCpWhite) >= 995 -> String.format(Locale.US, "%.0f", abs(scoreCpWhite) / 100.0)
        else -> String.format(Locale.US, "%.1f", abs(scoreCpWhite) / 100.0)
    }

    val whiteWinning = if (mateIn != null) mateIn > 0 else scoreCpWhite >= 0
    val whiteAtBottom = !flipped

    Box(
        modifier = modifier
            .width(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(EvalBlackFill)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .testTag("evaluation_bar")
            .semantics {
                contentDescription = "Position evaluation: ${if (whiteWinning) "White" else "Black"} $scoreLabel"
            }
    ) {
        // White portion bar (bottom when playing White, top when board flipped)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(animatedWhiteRatio)
                .align(if (whiteAtBottom) Alignment.BottomCenter else Alignment.TopCenter)
                .background(EvalWhiteFill)
        )

        // Subtle center equilibrium tick line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.Center)
                .background(Color(0x55D9B45B))
        )

        // Score readout pinned inside the winning side's end of the bar
        val labelAtBottom = (whiteWinning && whiteAtBottom) || (!whiteWinning && !whiteAtBottom)
        val labelColor = if (whiteWinning) Color(0xFF19221C) else Color(0xFFF3EFE6)

        Text(
            text = scoreLabel,
            color = labelColor,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .align(if (labelAtBottom) Alignment.BottomCenter else Alignment.TopCenter)
                .padding(vertical = 4.dp, horizontal = 1.dp)
        )
    }
}
