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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.darmix.darmixnight.ui.theme.DnTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

private const val TWO_PI = 6.2831855f

private class Orb(
    val color: Color,
    val radiusDp: Float,
    val x: Float,
    val y: Float,
    val alpha: Float,
    val driftX: Float,
    val driftY: Float,
    val phase: Float
)

private class SkyStyle(
    val stops: Array<Pair<Float, Color>>,
    val orbs: List<Orb>,
    val horizon: Color,
    val horizonAlpha: Float,
    val mote: Color,
    val moteStrength: Float,
    val grainAlpha: Int
)

private val NightStyle = SkyStyle(
    stops = arrayOf(0f to Color(0xFF123068), 0.38f to Color(0xFF0C1A3C), 1f to Color(0xFF060A18)),
    orbs = listOf(
        Orb(Color(0xFF2F6FD0), 320f, 0.12f, 0.10f, 0.75f, 0.07f, 0.05f, 0.0f),
        Orb(Color(0xFF1C9AAE), 260f, 0.96f, 0.38f, 0.50f, 0.06f, 0.07f, 1.3f),
        Orb(Color(0xFF2A58B0), 330f, 0.14f, 0.80f, 0.50f, 0.08f, 0.05f, 2.4f),
        Orb(Color(0xFFE8C796), 200f, 0.90f, 0.03f, 0.26f, 0.04f, 0.03f, 3.1f),
        Orb(Color(0xFF3B7FD8), 230f, 0.80f, 0.70f, 0.32f, 0.07f, 0.06f, 4.0f)
    ),
    horizon = Color(0xFF2A6DB8),
    horizonAlpha = 0.30f,
    mote = Color(0xFFF4E3BC),
    moteStrength = 0.7f,
    grainAlpha = 18
)

private val DayStyle = SkyStyle(
    stops = arrayOf(0f to Color(0xFFB9D4F1), 0.45f to Color(0xFFE6EFF8), 1f to Color(0xFFF8F1E3)),
    orbs = listOf(
        Orb(Color(0xFFFFDC98), 330f, 0.94f, -0.02f, 0.92f, 0.03f, 0.02f, 0.0f),
        Orb(Color(0xFFF7BE97), 250f, 0.04f, 0.55f, 0.52f, 0.07f, 0.05f, 1.1f),
        Orb(Color(0xFF9CC4EE), 270f, 0.92f, 0.46f, 0.55f, 0.06f, 0.06f, 2.2f),
        Orb(Color(0xFFC3E3D6), 240f, 0.30f, 0.98f, 0.48f, 0.06f, 0.04f, 3.3f),
        Orb(Color(0xFFBFD7F2), 200f, 0.30f, 0.20f, 0.30f, 0.05f, 0.05f, 4.4f)
    ),
    horizon = Color.White,
    horizonAlpha = 0.40f,
    mote = Color(0xFFD9A85E),
    moteStrength = 0.55f,
    grainAlpha = 14
)

private val Constellations = listOf(
    listOf(0.05f to 0.135f, 0.12f to 0.155f, 0.19f to 0.14f, 0.24f to 0.17f, 0.20f to 0.20f, 0.13f to 0.19f, 0.12f to 0.155f),
    listOf(0.80f to 0.36f, 0.86f to 0.33f, 0.92f to 0.37f, 0.95f to 0.43f, 0.89f to 0.46f)
)

private class Puff(val dx: Float, val dy: Float, val r: Float)

private val CloudPuffs = listOf(
    Puff(-40f, 8f, 26f),
    Puff(-14f, -8f, 34f),
    Puff(20f, -2f, 30f),
    Puff(46f, 9f, 22f),
    Puff(0f, 13f, 30f)
)

private class Cloud(val y: Float, val speedDp: Float, val scale: Float, val offset: Float, val alpha: Float)

private val Clouds = listOf(
    Cloud(0.09f, 9f, 0.95f, 0.15f, 1f),
    Cloud(0.24f, 13f, 1.25f, 0.62f, 0.95f),
    Cloud(0.40f, 7f, 0.8f, 0.38f, 0.85f),
    Cloud(0.58f, 11f, 1.05f, 0.88f, 0.75f)
)

private val StarlightHalo = Color(0xFFBFD8FF)
private val ConstellationLine = Color(0xFFCFE1FF)
private val CloudShade = Color(0xFF9DB8D8)

private class Mote(val x: Float, val phase: Float, val speed: Float, val size: Float)

private class LayeredStar(val star: Star, val layer: Int)

private fun noise(seed: Int, lane: Int, salt: Int): Float {
    val v = sin(seed * 12.9898f + lane * 78.233f + salt * 37.719f) * 43758.547f
    return v - floor(v)
}

private fun grainBitmap(alphaMax: Int): ImageBitmap {
    val size = 160
    val random = Random(11)
    val pixels = IntArray(size * size) {
        val a = random.nextInt(alphaMax + 1)
        val v = if (random.nextBoolean()) 255 else 0
        (a shl 24) or (v shl 16) or (v shl 8) or v
    }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888).asImageBitmap()
}

@Composable
fun SkyBackdrop(
    modifier: Modifier = Modifier,
    starCount: Int = 90,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = DnTheme.colors
    val dark = colors.isDark
    val style = if (dark) NightStyle else DayStyle
    val clock = rememberSceneClock()
    val stars = remember(starCount) {
        buildStars(count = starCount, seed = 5, maxY = 0.75f).mapIndexed { i, s -> LayeredStar(s, i % 3) }
    }
    val motes = remember {
        val random = Random(23)
        List(16) { Mote(random.nextFloat(), random.nextFloat(), 0.5f + random.nextFloat(), 1f + random.nextFloat()) }
    }
    val grain = remember(style.grainAlpha) { grainBitmap(style.grainAlpha) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val sky = Brush.verticalGradient(*style.stops)
                val orbBrushes = style.orbs.map {
                    Brush.radialGradient(
                        0f to it.color.copy(alpha = it.alpha),
                        0.5f to it.color.copy(alpha = it.alpha * 0.25f),
                        1f to Color.Transparent,
                        center = Offset.Zero,
                        radius = 1f
                    )
                }
                val horizonBrush = Brush.radialGradient(
                    0f to style.horizon.copy(alpha = style.horizonAlpha),
                    1f to Color.Transparent,
                    center = Offset.Zero,
                    radius = 1f
                )
                val puffBrush = Brush.radialGradient(
                    0f to Color.White.copy(alpha = 0.95f),
                    0.55f to Color.White.copy(alpha = 0.75f),
                    0.8f to Color.White.copy(alpha = 0.3f),
                    1f to Color.Transparent,
                    center = Offset.Zero,
                    radius = 1f
                )
                val shadeBrush = Brush.radialGradient(
                    0f to CloudShade.copy(alpha = 0.32f),
                    1f to Color.Transparent,
                    center = Offset.Zero,
                    radius = 1f
                )
                val grainBrush = ShaderBrush(ImageShader(grain, TileMode.Repeated, TileMode.Repeated))
                val constellationPath = Path()
                onDrawBehind {
                    val t = clock.value
                    drawRect(sky)
                    drawOrbs(style.orbs, orbBrushes, t)
                    drawHorizon(horizonBrush)
                    if (dark) {
                        drawStars(stars, t)
                        drawConstellations(constellationPath, t)
                        drawShootingStars(t)
                    } else {
                        drawClouds(puffBrush, shadeBrush, t)
                    }
                    drawMotes(motes, style.mote, style.moteStrength, t)
                    drawRect(grainBrush)
                }
            },
        content = content
    )
}

private fun DrawScope.drawUnitBrush(brush: Brush, center: Offset, radius: Float, scaleY: Float = 1f) {
    translate(center.x, center.y) {
        scale(radius, radius * scaleY, Offset.Zero) {
            drawCircle(brush, 1f, Offset.Zero)
        }
    }
}

private fun DrawScope.drawOrbs(orbs: List<Orb>, brushes: List<Brush>, t: Float) {
    val w = size.width
    val h = size.height
    val a = t * TWO_PI / 42f
    orbs.forEachIndexed { i, orb ->
        val cx = (orb.x + orb.driftX * sin(a + orb.phase)) * w
        val cy = (orb.y + orb.driftY * cos(a * 0.8f + orb.phase)) * h
        val r = orb.radiusDp * density * (1f + 0.06f * sin(a * 1.3f + orb.phase))
        drawUnitBrush(brushes[i], Offset(cx, cy), r)
    }
}

private fun DrawScope.drawHorizon(brush: Brush) {
    drawUnitBrush(brush, Offset(size.width / 2f, size.height * 1.04f), size.width * 0.9f, (size.height * 0.22f) / (size.width * 0.9f))
}

private fun DrawScope.drawStars(stars: List<LayeredStar>, t: Float) {
    val w = size.width
    val h = size.height
    stars.forEach { ls ->
        val s = ls.star
        val layer = ls.layer
        val twinkle = 0.5f + 0.5f * sin(t * (0.9f + layer * 0.6f) + s.phase)
        val c = Offset(s.x * w, s.y * h)
        val r = (0.35f + layer * 0.4f + s.radiusDp * 0.3f) * density
        drawCircle(Color.White, r, c, alpha = 0.15f + 0.7f * twinkle)
        if (layer == 2) {
            drawCircle(StarlightHalo, r * 4f, c, alpha = 0.08f * twinkle)
        }
    }
}

private fun DrawScope.drawConstellations(path: Path, t: Float) {
    val w = size.width
    val h = size.height
    Constellations.forEachIndexed { ci, points ->
        val visible = 0.5f + 0.5f * sin(t * 0.35f + ci * 2.1f)
        path.rewind()
        points.forEachIndexed { i, (px, py) ->
            if (i == 0) path.moveTo(px * w, py * h) else path.lineTo(px * w, py * h)
        }
        drawPath(
            path,
            ConstellationLine,
            alpha = 0.10f + 0.14f * visible,
            style = Stroke(0.8f * density, join = StrokeJoin.Round)
        )
        points.forEach { (px, py) ->
            val c = Offset(px * w, py * h)
            drawCircle(ConstellationLine, 5f * density, c, alpha = 0.06f + 0.08f * visible)
            drawCircle(Color.White, 1.5f * density, c, alpha = 0.35f + 0.45f * visible)
        }
    }
}

private val ShootingPeriods = floatArrayOf(4.3f, 5.7f, 6.4f, 7.9f, 9.1f)
private val ShootingOffsets = floatArrayOf(0f, 1.6f, 3.1f, 0.8f, 4.4f)

private fun DrawScope.drawShootingStars(t: Float) {
    val w = size.width
    val h = size.height
    for (lane in ShootingPeriods.indices) {
        val period = ShootingPeriods[lane]
        val local = t + ShootingOffsets[lane]
        val cycle = floor(local / period).toInt()
        val p = local - cycle * period
        val duration = 0.9f + 0.4f * noise(cycle, lane, 1)
        if (p > duration) continue
        val k = p / duration
        val eased = 1f - (1f - k) * (1f - k)
        val angle = (PI.toFloat() * (0.80f + 0.12f * noise(cycle, lane, 2)))
        val dir = Offset(cos(angle), sin(angle))
        val start = Offset(w * (0.35f + 0.7f * noise(cycle, lane, 3)), h * (0.02f + 0.45f * noise(cycle, lane, 4)))
        val travel = w * (0.35f + 0.3f * noise(cycle, lane, 5))
        val head = Offset(start.x + dir.x * travel * eased, start.y + dir.y * travel * eased)
        val tailLength = (60f + 50f * noise(cycle, lane, 6)) * density * (0.4f + 0.6f * sin(PI.toFloat() * k))
        val tail = Offset(head.x - dir.x * tailLength, head.y - dir.y * tailLength)
        val strength = sin(PI.toFloat() * k)
        drawLine(
            Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.9f * strength)), start = tail, end = head),
            tail,
            head,
            strokeWidth = 1.5f * density,
            cap = StrokeCap.Round
        )
        drawCircle(StarlightHalo, 5f * density, head, alpha = 0.25f * strength)
        drawCircle(Color.White, 1.6f * density, head, alpha = strength)
    }
}

private fun DrawScope.drawClouds(puff: Brush, shade: Brush, t: Float) {
    val span = size.width + 260f * density
    Clouds.forEach { cloud ->
        val x = ((cloud.offset * span + cloud.speedDp * density * t) % span) - 130f * density
        val y = size.height * cloud.y
        val s = cloud.scale * density
        CloudPuffs.forEach { p ->
            drawUnitBrush(shade, Offset(x + p.dx * s, y + p.dy * s + 7f * s), p.r * s * 1.5f, 0.75f)
        }
        CloudPuffs.forEach { p ->
            val c = Offset(x + p.dx * s, y + p.dy * s)
            translate(c.x, c.y) {
                scale(p.r * s * 1.5f, p.r * s * 1.5f, Offset.Zero) {
                    drawCircle(puff, 1f, Offset.Zero, alpha = cloud.alpha)
                }
            }
        }
    }
}

private fun DrawScope.drawMotes(motes: List<Mote>, color: Color, strength: Float, t: Float) {
    val w = size.width
    val h = size.height
    motes.forEach { m ->
        val k = (m.phase + t * m.speed / 18f) % 1f
        val y = h * (1.02f - k * 0.7f)
        val x = m.x * w + 8f * density * sin(t * m.speed * 0.8f + m.phase * 6f)
        val a = sin(PI.toFloat() * k) * strength
        val r = m.size * density
        drawCircle(color, r * 3.5f, Offset(x, y), alpha = a * 0.15f)
        drawCircle(color, r, Offset(x, y), alpha = a)
    }
}
