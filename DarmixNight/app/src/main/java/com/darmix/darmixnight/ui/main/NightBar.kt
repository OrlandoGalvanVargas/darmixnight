package com.darmix.darmixnight.ui.main

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.darmix.darmixnight.ui.components.DnGlyph
import com.darmix.darmixnight.ui.components.Glyph
import com.darmix.darmixnight.ui.theme.DnTheme
import com.darmix.darmixnight.ui.theme.DnType
import kotlin.math.abs

enum class MainTab(val label: String, val glyph: DnGlyph) {
    NIGHT("Noche", DnGlyph.MOON),
    EMERGENCY("Emergencia", DnGlyph.SIREN),
    SETTINGS("Ajustes", DnGlyph.SLIDERS)
}

private val BarHeight = 64.dp
private val IndicatorWidth = 28.dp
private val IndicatorHeight = 2.dp

@Composable
fun NightBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DnTheme.colors
    val tabs = MainTab.entries
    val position by animateFloatAsState(
        targetValue = selected.ordinal.toFloat(),
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "barPosition"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.ink)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .drawWithCache {
                    val cell = size.width / tabs.size
                    val barW = IndicatorWidth.toPx()
                    val barH = IndicatorHeight.toPx()
                    val line = 1.dp.toPx()
                    val glow = Brush.radialGradient(
                        0f to colors.ember.copy(alpha = 0.28f),
                        1f to colors.ember.copy(alpha = 0f),
                        center = Offset.Zero,
                        radius = 1f
                    )
                    onDrawBehind {
                        drawRect(colors.hairline.copy(alpha = 0.7f), size = Size(size.width, line))
                        val cx = cell * (position + 0.5f)
                        translate(cx, 0f) {
                            scale(barW * 1.6f, barW * 0.9f, Offset.Zero) {
                                drawCircle(glow, 1f, Offset.Zero)
                            }
                        }
                        drawRoundRect(
                            color = colors.ember,
                            topLeft = Offset(cx - barW / 2f, 0f),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(barH, barH)
                        )
                    }
                }
        ) {
            tabs.forEachIndexed { index, tab ->
                val closeness = (1f - abs(position - index)).coerceIn(0f, 1f)
                val tint = lerp(colors.textMute, colors.text, closeness)
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
                            if (tab != selected) onSelect(tab)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Glyph(tab.glyph, tint, size = 22.dp, strokeWidth = 1.8f)
                    Spacer(Modifier.height(6.dp))
                    Text(tab.label, style = DnType.navLabel, color = tint)
                }
            }
        }
    }
}
