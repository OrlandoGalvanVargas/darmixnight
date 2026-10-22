package com.darmix.darmixnight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.darmix.darmixnight.ui.theme.DnTheme

@Composable
fun MoonMark(modifier: Modifier = Modifier, size: Dp = 30.dp) {
    val colors = DnTheme.colors
    val moon = remember { Crescent.unitPath() }
    Box(
        modifier
            .size(size)
            .background(colors.raised, CircleShape)
            .drawWithCache {
                val r = this.size.minDimension * 0.30f
                val c = Offset(this.size.width / 2f, this.size.height / 2f)
                onDrawBehind {
                    translate(c.x, c.y) {
                        scale(r, r, pivot = Offset.Zero) {
                            drawPath(moon, if (colors.isDark) colors.glow else colors.accent)
                        }
                    }
                }
            }
    )
}
