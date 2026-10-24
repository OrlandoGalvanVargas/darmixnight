package com.darmix.darmixnight.ui.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.darmix.darmixnight.ui.components.DnGlyph
import com.darmix.darmixnight.ui.components.Glyph
import com.darmix.darmixnight.ui.theme.DnTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

enum class MainTab(val label: String, val glyph: DnGlyph) {
    NIGHT("Noche", DnGlyph.MOON),
    EMERGENCY("Emergencia", DnGlyph.SIREN),
    SETTINGS("Ajustes", DnGlyph.SLIDERS)
}

private val DockHeight = 76.dp
private val DiscRadius = 21.dp
private val IconCenterY = 30.dp

@Composable
fun MoonDock(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DnTheme.colors
    val haptics = LocalHapticFeedback.current
    val tabs = MainTab.entries
    val position by animateFloatAsState(
        targetValue = selected.ordinal.toFloat(),
        animationSpec = spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessMediumLow),
        label = "dockPosition"
    )
    val twinkle = rememberInfiniteTransition(label = "dockStars").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
        label = "dockTwinkle"
    )
    val moonColor = colors.accent
    val haloColor = if (colors.isDark) colors.glow else colors.accentAlt.copy(alpha = 0.6f)

    Surface(
        shape = RoundedCornerShape(38.dp),
        color = colors.card.copy(alpha = if (colors.isDark) 0.94f else 0.97f),
        border = BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else colors.text.copy(alpha = 0.08f)),
        shadowElevation = 14.dp,
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 460.dp)
            .height(DockHeight)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val cell = size.width / tabs.size
                    val r = DiscRadius.toPx()
                    val cy = IconCenterY.toPx()
                    val haloR = r * 2.1f
                    onDrawBehind {
                        val cx = cell * (position + 0.5f)
                        val nearest = position.roundToInt().toFloat()
                        val travel = (abs(position - nearest) * 2f).coerceIn(0f, 1f)
                        val direction = sign(selected.ordinal - position).let { if (it == 0f) 1f else it }
                        val center = Offset(cx, cy)

                        drawCircle(
                            Brush.radialGradient(
                                listOf(haloColor.copy(alpha = 0.45f * (1f - travel * 0.5f)), Color.Transparent),
                                center = center,
                                radius = haloR
                            ),
                            haloR,
                            center
                        )

                        if (travel < 0.02f) {
                            drawCircle(moonColor, r, center)
                        } else {
                            val offset = r * (2.2f - 1.5f * travel)
                            val disc = Path().apply { addOval(Rect(center, r)) }
                            val cut = Path().apply {
                                addOval(Rect(Offset(cx + offset * direction, cy - offset * 0.3f), r * 0.96f))
                            }
                            val crescent = Path().apply { op(disc, cut, PathOperation.Difference) }
                            drawPath(crescent, moonColor)
                        }

                        val settled = 1f - travel
                        if (settled > 0.05f) {
                            listOf(
                                Triple(-1.45f, -0.7f, 1.6f),
                                Triple(1.35f, -0.85f, 1.2f),
                                Triple(1.6f, 0.4f, 1.0f)
                            ).forEachIndexed { i, (dx, dy, s) ->
                                val t = 0.5f + 0.5f * sin((twinkle.value + i * 0.33f) * 2f * PI.toFloat())
                                drawCircle(
                                    haloColor,
                                    s.dp.toPx(),
                                    Offset(cx + dx * r, cy + dy * r),
                                    alpha = settled * (0.35f + 0.6f * t)
                                )
                            }
                        }
                    }
                }
        ) {
            Row(Modifier.fillMaxSize()) {
                tabs.forEachIndexed { index, tab ->
                    val closeness = (1f - abs(position - index)).coerceIn(0f, 1f)
                    val iconTint = lerp(colors.textDim, colors.onAccent, closeness)
                    val labelTint = lerp(colors.textDim, colors.accent, closeness)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .semantics {
                                role = Role.Tab
                                this.selected = tab == selected
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (tab != selected) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelect(tab)
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(IconCenterY - 11.dp))
                        Glyph(tab.glyph, iconTint, size = 22.dp, strokeWidth = 1.8f)
                        Spacer(Modifier.height(13.dp))
                        Text(
                            tab.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (closeness > 0.5f) FontWeight.Bold else FontWeight.SemiBold
                            ),
                            color = labelTint
                        )
                    }
                }
            }
        }
    }
}
