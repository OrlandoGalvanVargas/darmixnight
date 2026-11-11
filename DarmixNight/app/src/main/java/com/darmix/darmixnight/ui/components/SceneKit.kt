package com.darmix.darmixnight.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import kotlin.math.sin

private const val FADE_FROM = 0.58f
private const val FADE_TO = 0.94f

internal val SceneLampLight = Color(0xFFFFB067)
internal val SceneLampCore = Color(0xFFFFD596)

internal fun unitRadial(vararg stops: Pair<Float, Color>): Brush =
    Brush.radialGradient(*stops, center = Offset.Zero, radius = 1f)

internal fun lampFlicker(t: Float): Float =
    0.5f + 0.5f * (0.6f * sin(t * 2.09f) + 0.25f * sin(t * 5.2f + 0.7f) + 0.15f * sin(t * 9.4f + 2.1f))

internal fun DrawScope.drawGlow(brush: Brush, center: Offset, rx: Float, ry: Float, alpha: Float) {
    if (alpha <= 0.001f) return
    translate(center.x, center.y) {
        scale(rx, ry, Offset.Zero) {
            drawCircle(brush, 1f, Offset.Zero, alpha = alpha.coerceAtMost(1f), blendMode = BlendMode.Screen)
        }
    }
}

internal fun DrawScope.drawRoom(base: ImageBitmap, aspect: Float, vararg layers: Pair<ImageBitmap?, Float>) {
    val height = size.width * aspect
    val target = IntSize(size.width.roundToInt(), height.roundToInt())
    drawImage(base, dstSize = target)
    layers.forEach { (image, alpha) ->
        if (image != null && alpha > 0f) drawImage(image, dstSize = target, alpha = alpha)
    }
    drawRect(
        Brush.verticalGradient(
            0f to Color.Black,
            1f to Color.Transparent,
            startY = height * FADE_FROM,
            endY = height * FADE_TO
        ),
        blendMode = BlendMode.DstIn
    )
}
