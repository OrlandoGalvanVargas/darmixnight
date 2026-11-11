package com.darmix.darmixnight.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.darmix.darmixnight.ui.theme.DnTheme
import kotlin.random.Random

private const val GrainSize = 180
private const val GrainAlpha = 22

private fun brasaGrain(): ImageBitmap {
    val random = Random(11)
    val pixels = IntArray(GrainSize * GrainSize) {
        val a = random.nextInt(GrainAlpha + 1)
        val light = random.nextBoolean()
        val r = if (light) 255 else 20
        val g = if (light) 226 else 8
        val b = if (light) 200 else 4
        (a shl 24) or (r shl 16) or (g shl 8) or b
    }
    return Bitmap.createBitmap(pixels, GrainSize, GrainSize, Bitmap.Config.ARGB_8888).asImageBitmap()
}

@Composable
fun SkyBackdrop(
    modifier: Modifier = Modifier,
    glowCenter: Offset? = Offset(0.78f, 0.58f),
    content: @Composable BoxScope.() -> Unit
) {
    val colors = DnTheme.colors
    val grain = remember { brasaGrain() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val sky = Brush.verticalGradient(
                    0f to colors.bgTop,
                    0.6f to colors.bg,
                    1f to colors.ink
                )
                val glow = Brush.radialGradient(
                    0f to colors.umber,
                    0.7f to colors.umber.copy(alpha = 0f),
                    center = Offset.Zero,
                    radius = 1f
                )
                val grainBrush = ShaderBrush(ImageShader(grain, TileMode.Repeated, TileMode.Repeated))
                val rx = size.width * 0.9f
                val ry = size.height * 0.55f
                onDrawBehind {
                    drawRect(sky)
                    if (glowCenter != null) {
                        translate(size.width * glowCenter.x, size.height * glowCenter.y) {
                            scale(rx, ry, Offset.Zero) {
                                drawCircle(glow, 1f, Offset.Zero)
                            }
                        }
                    }
                    drawRect(grainBrush)
                }
            },
        content = content
    )
}
