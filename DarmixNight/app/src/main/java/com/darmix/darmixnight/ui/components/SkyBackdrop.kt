package com.darmix.darmixnight.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.darmix.darmixnight.ui.theme.DnTheme
import kotlin.math.sin

@Composable
fun SkyBackdrop(
    modifier: Modifier = Modifier,
    starCount: Int = 46,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = DnTheme.colors
    val stars = remember(starCount) { buildStars(count = starCount, seed = 21, maxY = 0.42f) }
    val transition = rememberInfiniteTransition(label = "sky")
    val time = transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2832f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "skyTime"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val sky = Brush.verticalGradient(
                    0f to colors.bgTop,
                    0.5f to colors.bg,
                    1f to colors.bg
                )
                val lightCenter = Offset(size.width * 0.86f, size.height * 0.05f)
                val lightRadius = size.width * if (colors.isDark) 0.75f else 0.95f
                val light = Brush.radialGradient(
                    0f to colors.glow.copy(alpha = if (colors.isDark) 0.10f else 0.38f),
                    0.5f to colors.glow.copy(alpha = if (colors.isDark) 0.03f else 0.10f),
                    1f to Color.Transparent,
                    center = lightCenter,
                    radius = lightRadius
                )
                onDrawBehind {
                    drawRect(sky)
                    drawRect(light)
                    if (colors.isDark) {
                        val t = time.value
                        stars.forEach { s ->
                            val twinkle = 0.45f + 0.55f * (0.5f + 0.5f * sin(t + s.phase))
                            drawCircle(
                                color = if (s.warm) colors.glow else Color.White,
                                radius = s.radiusDp * density,
                                center = Offset(s.x * size.width, s.y * size.height),
                                alpha = 0.75f * twinkle * (1f - s.y * 1.6f).coerceIn(0.15f, 1f)
                            )
                        }
                    }
                }
            },
        content = content
    )
}
