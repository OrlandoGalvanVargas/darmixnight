package com.darmix.darmixnight.ui.components

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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import com.darmix.darmixnight.ui.theme.DnTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val TWO_PI = 6.2831855f

private class Ribbon(
    val base: Float,
    val amplitude: Float,
    val frequency: Float,
    val speed: Float,
    val width: Float,
    val from: Color,
    val to: Color,
    val alpha: Float
)

private class Mote(val x: Float, val phase: Float, val speed: Float, val size: Float)

private class Cloud(val y: Float, val speed: Float, val scale: Float, val offset: Float, val alpha: Float)

private val NightRibbons = listOf(
    Ribbon(0.16f, 0.035f, 1.1f, 1f, 0.075f, Color(0xFF2E7FD0), Color(0xFF5BA8DE), 0.30f),
    Ribbon(0.24f, 0.040f, 0.8f, -1f, 0.060f, Color(0xFF5BA8DE), Color(0xFFE8C796), 0.20f),
    Ribbon(0.10f, 0.030f, 1.4f, 2f, 0.050f, Color(0xFF1D4C83), Color(0xFF78C8A6), 0.16f)
)

private val DayRibbons = listOf(
    Ribbon(0.20f, 0.035f, 1.0f, 1f, 0.070f, Color(0xFF9EC3EA), Color(0xFFF2CF9C), 0.42f),
    Ribbon(0.30f, 0.030f, 0.7f, -1f, 0.060f, Color(0xFFF2CF9C), Color(0xFF9EC3EA), 0.30f)
)

private val NightMote = Color(0xFFF4E3BC)
private val DayMote = Color(0xFFC99A55)
private val NightMist = Color(0xFF5BA8DE)
private val DayMist = Color.White
private val DayTop = Color(0xFFC9DBF0)
private val DaySun = Color(0xFFFFE7B8)

private val CloudShape: Path by lazy {
    PathParser().parsePathString(
        "M-46 10 A16 16 0 0 1 -26 -6 A22 22 0 0 1 14 -10 A17 17 0 0 1 44 6 A10 10 0 0 1 44 10 Z"
    ).toPath()
}

@Composable
fun SkyBackdrop(
    modifier: Modifier = Modifier,
    starCount: Int = 90,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = DnTheme.colors
    val clock = rememberSceneClock()
    val stars = remember(starCount) { buildStars(count = starCount, seed = 21, maxY = 0.6f) }
    val motes = remember {
        val random = Random(23)
        List(18) { Mote(random.nextFloat(), random.nextFloat(), 0.5f + random.nextFloat(), 1.1f + random.nextFloat() * 1.2f) }
    }
    val clouds = remember {
        listOf(
            Cloud(0.09f, 5f, 0.9f, 0.1f, 0.42f),
            Cloud(0.21f, 8f, 1.15f, 0.55f, 0.32f),
            Cloud(0.33f, 11f, 1.35f, 0.8f, 0.24f)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val dark = colors.isDark
                val top = if (dark) colors.bgTop else DayTop
                val sky = Brush.verticalGradient(0f to top, 0.62f to colors.bg, 1f to colors.bg)
                val ribbons = if (dark) NightRibbons else DayRibbons
                val ribbonBrushes = ribbons.map { Brush.horizontalGradient(listOf(it.from, it.to)) }
                val ribbonPath = Path()
                val mistPath = Path()
                val haloCenter = Offset(size.width * 0.88f, size.height * 0.05f)
                val haloRadius = size.width * if (dark) 0.75f else 0.95f
                val haloColor = if (dark) colors.glow else DaySun
                val halo = Brush.radialGradient(
                    0f to haloColor.copy(alpha = if (dark) 0.16f else 0.62f),
                    0.45f to haloColor.copy(alpha = if (dark) 0.05f else 0.2f),
                    1f to Color.Transparent,
                    center = haloCenter,
                    radius = haloRadius
                )
                onDrawBehind {
                    val t = clock.value
                    drawRect(sky)
                    if (!dark) sunRays(haloCenter, t)
                    ribbons.forEachIndexed { i, ribbon -> drawRibbon(ribbon, ribbonBrushes[i], ribbonPath, i, t) }
                    drawRect(halo)
                    if (dark) {
                        drawStars(stars, t)
                        shootingStar(t)
                    } else {
                        drawClouds(clouds, t)
                    }
                    drawMist(mistPath, if (dark) NightMist else DayMist, if (dark) 0.07f else 0.28f, t)
                    drawMotes(motes, if (dark) NightMote else DayMote, if (dark) 0.75f else 0.5f, t)
                }
            },
        content = content
    )
}

private fun DrawScope.drawRibbon(ribbon: Ribbon, brush: Brush, path: Path, index: Int, t: Float) {
    val w = size.width
    val h = size.height
    val omega = TWO_PI / 46f * ribbon.speed
    val band = ribbon.width * h
    path.rewind()
    val steps = 32
    for (s in 0..steps) {
        val fx = s / steps.toFloat()
        val y = h * (ribbon.base +
                ribbon.amplitude * sin(ribbon.frequency * TWO_PI * fx + t * omega + index * 1.7f) +
                ribbon.amplitude * 0.45f * sin(ribbon.frequency * 4.3f * PI.toFloat() * fx - t * omega * 1.3f + index))
        if (s == 0) path.moveTo(-8f, y) else path.lineTo(w * fx + if (s == steps) 8f else 0f, y)
    }
    val layers = floatArrayOf(1.7f, 1.05f, 0.6f, 0.28f)
    val lifts = floatArrayOf(0.55f, 0.32f, 0.16f, 0f)
    val weights = floatArrayOf(0.30f, 0.40f, 0.55f, 0.75f)
    for (k in layers.indices) {
        translate(0f, -lifts[k] * band) {
            drawPath(
                path,
                brush,
                alpha = ribbon.alpha * weights[k],
                style = Stroke(width = band * layers[k], cap = StrokeCap.Round)
            )
        }
    }
    val streaks = 16
    for (j in 0 until streaks) {
        val fx = (j + 0.5f) / streaks
        val shimmer = 0.5f + 0.5f * sin(t * 0.9f * (1f + (j % 3) * 0.3f) + j * 2.1f + index)
        val y = h * (ribbon.base +
                ribbon.amplitude * sin(ribbon.frequency * TWO_PI * fx + t * omega + index * 1.7f) +
                ribbon.amplitude * 0.45f * sin(ribbon.frequency * 4.3f * PI.toFloat() * fx - t * omega * 1.3f + index))
        drawLine(
            lerp(ribbon.from, ribbon.to, fx),
            Offset(w * fx, y + band * 0.2f),
            Offset(w * fx, y - band * 1.3f),
            strokeWidth = band * 0.09f,
            cap = StrokeCap.Round,
            alpha = ribbon.alpha * 0.35f * shimmer
        )
    }
}

private fun DrawScope.drawStars(stars: List<Star>, t: Float) {
    val w = size.width
    val h = size.height
    val drifts = floatArrayOf(0.6f, 1.3f, 2.4f)
    stars.forEachIndexed { i, s ->
        val layer = i % 3
        val x = ((s.x * w - drifts[layer] * density * t) % w + w) % w
        val y = s.y * h
        val twinkle = 0.5f + 0.5f * sin(t * (0.9f + layer * 0.45f) + s.phase)
        val fade = (1f - s.y * 1.2f).coerceIn(0.2f, 1f)
        val color = if (s.warm) NightMote else Color.White
        val radius = (s.radiusDp + layer * 0.35f) * density
        drawCircle(color, radius, Offset(x, y), alpha = (0.2f + 0.7f * twinkle) * fade)
        if (layer == 2 && twinkle > 0.82f) {
            val flare = 5f * density * (twinkle - 0.82f) / 0.18f
            drawLine(color, Offset(x - flare, y), Offset(x + flare, y), strokeWidth = 0.7f * density, alpha = 0.6f * fade)
            drawLine(color, Offset(x, y - flare), Offset(x, y + flare), strokeWidth = 0.7f * density, alpha = 0.6f * fade)
        }
    }
}

private fun DrawScope.shootingStar(t: Float) {
    val period = 13f
    val p = t % period
    val duration = 1.2f
    if (p > duration) return
    val k = p / duration
    val cycle = (t / period).toInt()
    val startX = size.width * (0.7f + 0.2f * ((cycle * 37) % 10) / 10f)
    val head = Offset(startX - size.width * 0.5f * k, size.height * (0.06f + 0.14f * k))
    val tail = Offset(head.x + 70f * density, head.y - 32f * density)
    val strength = sin(PI.toFloat() * k)
    drawLine(
        Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.9f * strength)), start = tail, end = head),
        tail,
        head,
        strokeWidth = 1.6f * density,
        cap = StrokeCap.Round
    )
    drawCircle(Color.White, 1.6f * density, head, alpha = strength)
}

private fun DrawScope.sunRays(center: Offset, t: Float) {
    val spin = t * TWO_PI / 140f
    for (i in 0 until 12) {
        val a = spin + i * PI.toFloat() / 6f
        val inner = 70f * density
        val outer = (260f + 40f * sin(t * 0.5f + i)) * density
        val spread = 0.05f
        val ray = Path().apply {
            moveTo(center.x + inner * cos(a), center.y + inner * sin(a))
            lineTo(center.x + outer * cos(a - spread), center.y + outer * sin(a - spread))
            lineTo(center.x + outer * cos(a + spread), center.y + outer * sin(a + spread))
            close()
        }
        drawPath(ray, Color.White, alpha = 0.16f)
    }
}

private fun DrawScope.drawClouds(clouds: List<Cloud>, t: Float) {
    val span = size.width + 180f * density
    clouds.forEach { c ->
        val x = ((c.offset * span + c.speed * density * t) % span) - 90f * density
        translate(x, size.height * c.y) {
            scale(c.scale * density, c.scale * density, Offset.Zero) {
                drawPath(CloudShape, Color.White, alpha = c.alpha)
            }
        }
    }
}

private fun DrawScope.drawMist(path: Path, color: Color, strength: Float, t: Float) {
    val w = size.width
    val h = size.height
    val bands = floatArrayOf(0.80f, 0.88f, 0.95f)
    val directions = floatArrayOf(1f, -1f, 1f)
    bands.forEachIndexed { i, base ->
        path.rewind()
        val steps = 20
        for (s in 0..steps) {
            val fx = s / steps.toFloat()
            val y = h * (base + 0.012f * sin(6f * PI.toFloat() * fx + t * 0.12f * directions[i] + i * 2f))
            if (s == 0) path.moveTo(-10f, y) else path.lineTo(w * fx + if (s == steps) 10f else 0f, y)
        }
        val breathe = 0.7f + 0.3f * sin(t * 0.2f + i * 2f)
        drawPath(path, color, alpha = strength * 0.5f * breathe, style = Stroke(width = h * 0.09f, cap = StrokeCap.Round))
        drawPath(path, color, alpha = strength * breathe, style = Stroke(width = h * 0.035f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawMotes(motes: List<Mote>, color: Color, strength: Float, t: Float) {
    val w = size.width
    val h = size.height
    motes.forEach { m ->
        val k = (m.phase + t * m.speed / 16f) % 1f
        val y = h * (1.02f - k * 0.7f)
        val x = m.x * w + 10f * density * sin(t * m.speed * 0.8f + m.phase * 6f)
        val a = sin(PI.toFloat() * k) * strength
        val r = m.size * density
        drawCircle(color, r * 3.2f, Offset(x, y), alpha = a * 0.16f)
        drawCircle(color, r, Offset(x, y), alpha = a)
    }
}
