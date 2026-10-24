package com.darmix.darmixnight.ui.panda

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class PandaMood { SNACK, COCOA, YAWN, MOON, SLEEP, MORNING, PAUSED }

internal const val PANDA_VIEWPORT = 200f

private val Ink = Color(0xFF1C2133)
private val Fur = Color(0xFFF7F4EC)
private val Blush = Color(0xFFF2A7A7)
private val Pad = Color(0xFF9C7A6A)
private val Cobalt = Color(0xFF2E5FA0)
private val Honey = Color(0xFFE8C796)
private val Crater = Color(0xFFD4AE74)
private val MouthIn = Color(0xFF7A2E3B)
private val Tongue = Color(0xFFF08A96)
private val Leaf = Color(0xFF8BD3A8)
private val Stalk = Color(0xFF6FB58E)
private val Pillow = Color(0xFFCFE0F5)
private val Knit = Color(0xFF5B8BC9)
private val InnerEar = Color(0xFF3A4160)
private val Tear = Color(0xFF9EC3EA)
private val Heart = Color(0xFFEE8A8A)

private const val OUTLINE = 3f
private const val TAU = 6.2831855f

private fun svg(data: String): Path = PathParser().parsePathString(data).toPath()

private fun line(width: Float) = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)

private object Art {
    val tuft by lazy { svg("M90 46 q3 -11 11 -5 q5 -9 11 1") }
    val shoulders by lazy { svg("M58 136 Q100 118 142 136 L140 146 Q100 130 60 146 Z") }
    val nose by lazy { svg("M94 106 q6 -3.5 12 0 q-1 6.5 -6 7.5 q-5 -1 -6 -7.5 Z") }
    val eyesHappy by lazy { svg("M72 96 q7 -8 14 0 M114 96 q7 -8 14 0") }
    val eyesClosed by lazy { svg("M72 92 q7 7 14 0 M114 92 q7 7 14 0") }
    val eyesSqueeze by lazy { svg("M73 89 l12 4.5 l-12 4.5 M127 89 l-12 4.5 l12 4.5") }
    val halfWhite by lazy { svg("M72.8 93 a6.2 6.2 0 0 0 12.4 0 Z M114.8 93 a6.2 6.2 0 0 0 12.4 0 Z") }
    val halfPupil by lazy { svg("M75.4 93.5 a3.6 3.6 0 0 0 7.2 0 Z M117.4 93.5 a3.6 3.6 0 0 0 7.2 0 Z") }
    val halfLid by lazy { svg("M71.5 93 H86.5 M113.5 93 H128.5") }
    val mouthSmile by lazy { svg("M100 113 v2.5 M93 115 q3.5 4.5 7 0 q3.5 4.5 7 0") }
    val mouthWave by lazy { svg("M93 115 q3.5 4.5 7 0 q3.5 4.5 7 0") }
    val mouthTiny by lazy { svg("M97 117 q3 2 6 0") }
    val grin by lazy { svg("M92 114.5 q8 11 16 0 Z") }
    val grinTongue by lazy { svg("M96 119.5 q4 3 8 0 Z") }
    val chewLeaf by lazy { svg("M106 118 l10 -2 l-3 6 Z") }
    val cap by lazy { svg("M50 66 C56 24 128 10 160 50 C171 64 174 84 169 99 C161 80 151 70 140 66 C110 56 80 58 50 66 Z") }
    val capSleep by lazy { svg("M50 66 C56 24 128 10 160 50 C168 62 166 80 150 96 C150 82 148 72 140 66 C110 56 80 58 50 66 Z") }
    val capBand by lazy { svg("M48 67 C80 54 120 54 148 67") }
    val capStar by lazy { star(108f, 38f, 7f) }
    val mugStar by lazy { star(143f, 156f, 4.2f) }
    val armSnackL by lazy { svg("M64 140 Q70 166 92 166") }
    val armSnackR by lazy { svg("M136 140 Q150 132 148 118") }
    val armCocoaL by lazy { svg("M62 140 Q66 162 88 162") }
    val armCocoaR by lazy { svg("M138 138 Q140 160 130 158") }
    val armUpL by lazy { svg("M64 136 Q46 108 42 72") }
    val armUpR by lazy { svg("M136 136 Q154 108 158 72") }
    val armMoonL by lazy { svg("M62 140 Q64 156 80 158") }
    val armMoonR by lazy { svg("M138 142 Q140 164 122 172") }
    val armSignL by lazy { svg("M60 136 Q54 150 64 148") }
    val armSignR by lazy { svg("M140 136 Q146 150 136 148") }
    val stalk by lazy { svg("M146 186 L152 54") }
    val stalkNodes by lazy { svg("M144 150 h10 M146 112 h10 M149 78 h9") }
    val leaves by lazy {
        listOf(
            svg("M152 62 q22 -14 36 -6 q-18 12 -36 6 Z"),
            svg("M150 70 q-20 -16 -34 -12 q14 16 34 12 Z"),
            svg("M154 100 q22 -6 30 4 q-18 6 -30 -4 Z")
        )
    }
    val mug by lazy { svg("M128 140 h30 v20 a9 9 0 0 1 -9 9 h-12 a9 9 0 0 1 -9 -9 Z") }
    val mugHandle by lazy { svg("M158 145 a6.5 6.5 0 0 1 0 13") }
    val mugShine by lazy { svg("M134 146 h18") }
    val wisp by lazy { svg("M0 0 q-3 -4 0 -8 q3 -4 0 -8") }
    val tear by lazy { svg("M58 100 q-4 6 0 9 q4 -3 0 -9 Z") }
    val moon by lazy { svg("M50.563,30.247 A24,24 0 1,0 77.226,60.047 A20,20 0 0,1 50.563,30.247 Z") }
    val sparkle by lazy { svg("M150 70 l3 -7 l3 7 l7 3 l-7 3 l-3 7 l-3 -7 l-7 -3 Z") }
    val signPost by lazy { svg("M100 150 V196") }
    val pillow by lazy { svg("M14 170 C14 150 40 144 74 146 C108 148 122 156 122 172 C122 186 100 192 68 192 C34 192 14 188 14 170 Z") }
    val pillowShine by lazy { svg("M30 160 q14 -6 30 -4") }
    val blanket by lazy { svg("M84 150 C96 122 156 114 186 136 C202 148 204 176 194 188 C190 193 184 194 176 194 H96 C84 192 78 168 84 150 Z") }
    val blanketHem by lazy { svg("M86 150 C100 136 120 132 140 134") }
    val knit by lazy { svg("M0 0 q5 9 0 18 q-5 9 0 18 q5 9 0 18") }
    val sleepArm by lazy { svg("M104 150 Q112 140 124 140") }
    val heart by lazy { svg("M6 3 C6 0 0 -1 0 4 C0 8 6 11 6 13 C6 11 12 8 12 4 C12 -1 6 0 6 3 Z") }
}

private fun star(cx: Float, cy: Float, r: Float): Path = Path().apply {
    for (i in 0 until 10) {
        val a = -PI / 2 + i * PI / 5
        val rr = if (i % 2 == 0) r else r * 0.45f
        val x = cx + rr * cos(a).toFloat()
        val y = cy + rr * sin(a).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private enum class Eyes { OPEN, HAPPY, CLOSED, HALF, SQUEEZE }
private enum class Mouth { SMILE, GRIN, YAWN, TINY, CHEW }

private class Look(
    val eyes: Eyes,
    val mouth: Mouth,
    val glance: Float = 0f,
    val blush: Float = 0.55f,
    val yawn: Float = 0f
)

class PandaMotion(
    val time: Float,
    val hop: Float,
    val joy: Float,
    val hearts: Float,
    val rim: Color
)

private fun DrawScope.oval(cx: Float, cy: Float, rx: Float, ry: Float, color: Color, alpha: Float = 1f) {
    drawOval(color, Offset(cx - rx, cy - ry), Size(rx * 2, ry * 2), alpha = alpha)
}

private fun DrawScope.ovalLine(cx: Float, cy: Float, rx: Float, ry: Float, color: Color, width: Float) {
    drawOval(color, Offset(cx - rx, cy - ry), Size(rx * 2, ry * 2), style = Stroke(width))
}

private fun DrawScope.solid(path: Path, fill: Color, width: Float = OUTLINE) {
    drawPath(path, fill)
    drawPath(path, Ink, style = line(width))
}

private fun DrawScope.limb(path: Path, rim: Color) {
    drawPath(path, rim, style = line(23f))
    drawPath(path, Ink, style = line(20f))
}

private fun blinkAt(t: Float): Float {
    val cycle = 4.2f
    val index = (t / cycle).toInt()
    val p = t - index * cycle
    if (p < 0.18f) return sin(PI.toFloat() * p / 0.18f)
    if (index % 3 == 1 && p in 0.3f..0.48f) return sin(PI.toFloat() * (p - 0.3f) / 0.18f)
    return 0f
}

private fun earTwitchAt(t: Float): Float {
    val p = t % 7.3f
    return if (p < 0.4f) 9f * sin(TAU * p / 0.4f) else 0f
}

private fun DrawScope.ears(rim: Color, twitch: Float) {
    drawCircle(Ink, 17f, Offset(60f, 50f))
    drawCircle(rim, 17f, Offset(60f, 50f), style = Stroke(1.6f))
    drawCircle(InnerEar, 8f, Offset(61f, 52f))
    rotate(twitch, Offset(134f, 62f)) {
        drawCircle(Ink, 17f, Offset(140f, 50f))
        drawCircle(rim, 17f, Offset(140f, 50f), style = Stroke(1.6f))
        drawCircle(InnerEar, 8f, Offset(139f, 52f))
    }
}

private fun DrawScope.head() {
    oval(100f, 90f, 54f, 47f, Fur)
    ovalLine(100f, 90f, 54f, 47f, Ink, OUTLINE)
    drawPath(Art.tuft, Fur)
    drawPath(Art.tuft, Ink, style = line(2.4f))
}

private fun DrawScope.patches() {
    rotate(-28f, Offset(77f, 94f)) { oval(77f, 94f, 15.5f, 12.5f, Ink) }
    rotate(28f, Offset(123f, 94f)) { oval(123f, 94f, 15.5f, 12.5f, Ink) }
}

private fun DrawScope.openEyes(glance: Float, blink: Float) {
    if (blink > 0.82f) {
        drawPath(Art.eyesClosed, Color.White, style = line(2.6f))
        return
    }
    for (ex in floatArrayOf(79f, 121f)) {
        scale(1f, 1f - blink, Offset(ex, 93f)) {
            drawCircle(Color.White, 6.2f, Offset(ex, 93f))
            drawCircle(Ink, 4.1f, Offset(ex + glance, 94f))
            drawCircle(Color.White, 1.6f, Offset(ex + glance - 1.6f, 92f))
        }
    }
}

private fun DrawScope.face(look: Look, blink: Float, chew: Float) {
    patches()
    when (look.eyes) {
        Eyes.OPEN -> openEyes(look.glance, blink)
        Eyes.HAPPY -> drawPath(Art.eyesHappy, Color.White, style = line(3f))
        Eyes.CLOSED -> drawPath(Art.eyesClosed, Color.White, style = line(2.6f))
        Eyes.SQUEEZE -> drawPath(Art.eyesSqueeze, Color.White, style = line(2.6f))
        Eyes.HALF -> {
            if (blink > 0.6f) {
                drawPath(Art.eyesClosed, Color.White, style = line(2.6f))
            } else {
                drawPath(Art.halfWhite, Color.White)
                drawPath(Art.halfPupil, Ink)
                drawPath(Art.halfLid, Color.White, style = line(2.2f))
            }
        }
    }
    drawPath(Art.nose, Ink)
    when (look.mouth) {
        Mouth.SMILE -> drawPath(Art.mouthSmile, Ink, style = line(2.3f))
        Mouth.TINY -> drawPath(Art.mouthTiny, Ink, style = line(2.2f))
        Mouth.GRIN -> {
            solid(Art.grin, MouthIn, 2.2f)
            drawPath(Art.grinTongue, Tongue)
        }
        Mouth.CHEW -> {
            drawPath(Art.mouthWave, Ink, style = line(2.3f))
            translate(0f, chew) { solid(Art.chewLeaf, Leaf, 1.4f) }
        }
        Mouth.YAWN -> {
            scale(1f, 0.3f + 0.7f * look.yawn, Offset(100f, 114f)) {
                oval(100f, 123f, 7.5f, 10f, MouthIn)
                ovalLine(100f, 123f, 7.5f, 10f, Ink, 2.2f)
                oval(100f, 128.5f, 4.5f, 3f, Tongue)
            }
        }
    }
    oval(65f, 110f, 7.5f, 4.5f, Blush, look.blush)
    oval(135f, 110f, 7.5f, 4.5f, Blush, look.blush)
}

private fun DrawScope.nightcap(path: Path, tilt: Float, pompom: Offset, sway: Float) {
    rotate(tilt + sway, Offset(100f, 60f)) {
        solid(path, Cobalt)
        drawPath(Art.capStar, Honey)
        drawPath(Art.capStar, Ink, style = line(1.4f))
        drawCircle(Honey, 2.2f, Offset(138f, 46f))
        drawCircle(Honey, 1.8f, Offset(82f, 44f))
        drawPath(Art.capBand, Ink, style = line(13f))
        drawPath(Art.capBand, Fur, style = line(8f))
        drawCircle(Fur, 9f, pompom)
        drawCircle(Ink, 9f, pompom, style = Stroke(2.4f))
    }
}

private fun DrawScope.body(breath: Float) {
    scale(1f + 0.012f * breath, 1f + 0.02f * breath, Offset(100f, 190f)) {
        oval(100f, 152f, 45f, 39f, Fur)
        ovalLine(100f, 152f, 45f, 39f, Ink, OUTLINE)
        drawPath(Art.shoulders, Ink)
    }
}

private fun DrawScope.feet(rim: Color) {
    for (fx in floatArrayOf(72f, 128f)) {
        oval(fx, 184f, 17f, 12f, Ink)
        drawOval(rim, Offset(fx - 17f, 172f), Size(34f, 24f), style = Stroke(1.6f))
        oval(fx, 186f, 7f, 5.5f, Pad)
        drawCircle(Pad, 2.3f, Offset(fx - 8f, 179f))
        drawCircle(Pad, 2.3f, Offset(fx, 176.5f))
        drawCircle(Pad, 2.3f, Offset(fx + 8f, 179f))
    }
}

private fun DrawScope.headGroup(
    rim: Color,
    t: Float,
    look: Look,
    nod: Float,
    blink: Float,
    chew: Float,
    cap: (DrawScope.() -> Unit)?
) {
    rotate(nod, Offset(100f, 136f)) {
        ears(rim, earTwitchAt(t))
        head()
        face(look, blink, chew)
        cap?.invoke(this)
    }
}

private fun DrawScope.bamboo(t: Float) {
    rotate(3f * sin(t * 1.6f), Offset(146f, 186f)) {
        drawPath(Art.stalk, Ink, style = line(13f))
        drawPath(Art.stalk, Stalk, style = line(8f))
        drawPath(Art.stalkNodes, Ink, style = line(2f))
        Art.leaves.forEachIndexed { i, leaf ->
            rotate(2.5f * sin(t * 2.2f + i), Offset(152f, 70f)) { solid(leaf, Leaf, 2f) }
        }
    }
}

private fun DrawScope.cocoa(t: Float) {
    for (i in 0 until 3) {
        val q = ((t * 0.55f) + i / 3f) % 1f
        translate(138f + i * 8f, 134f - q * 14f) {
            drawPath(Art.wisp, Fur, alpha = sin(PI.toFloat() * q) * 0.8f, style = line(2.2f))
        }
    }
    solid(Art.mug, Honey)
    drawPath(Art.mugHandle, Ink, style = line(OUTLINE))
    drawPath(Art.mugShine, Color.White, alpha = 0.6f, style = line(2f))
    drawPath(Art.mugStar, Fur)
    drawPath(Art.mugStar, Ink, style = line(1.4f))
}

private fun DrawScope.moonPlush(t: Float) {
    withTransform({
        translate(104f, 150f)
        rotate(-24f + 3f * sin(t * 1.2f), Offset.Zero)
        scale(1.75f, 1.75f, Offset.Zero)
        translate(-50f, -56f)
    }) {
        drawPath(Art.moon, Honey)
        drawPath(Art.moon, Ink, style = line(1.8f))
        drawCircle(Crater, 2.6f, Offset(38f, 62f))
        drawCircle(Crater, 1.8f, Offset(46f, 76f))
        drawCircle(Crater, 2f, Offset(34f, 48f))
    }
}

private fun DrawScope.sunRays(t: Float) {
    val pulse = 0.85f + 0.15f * sin(t * 2f)
    for (i in 0 until 7) {
        val a = PI.toFloat() * (1.05f + i * 0.15f) + 0.03f * sin(t * 0.9f)
        val r1 = 60f
        val r2 = 60f + 26f * pulse
        drawLine(
            Honey,
            Offset(100f + r1 * cos(a), 84f + r1 * sin(a)),
            Offset(100f + r2 * cos(a), 84f + r2 * sin(a)),
            strokeWidth = 5f,
            cap = StrokeCap.Round,
            alpha = 0.8f
        )
    }
}

private fun DrawScope.pauseSign(t: Float) {
    rotate(-6f + 3f * sin(t * 1.3f), Offset(100f, 196f)) {
        drawPath(Art.signPost, Ink, style = line(5f))
        drawRoundRect(Fur, Offset(62f, 124f), Size(76f, 38f), CornerRadius(10f))
        drawRoundRect(Ink, Offset(62f, 124f), Size(76f, 38f), CornerRadius(10f), style = Stroke(OUTLINE))
        drawRoundRect(Cobalt, Offset(89f, 134f), Size(7f, 18f), CornerRadius(2f))
        drawRoundRect(Cobalt, Offset(104f, 134f), Size(7f, 18f), CornerRadius(2f))
    }
}

private fun DrawScope.zees(t: Float, originX: Float, originY: Float, color: Color) {
    for (k in 0 until 3) {
        val q = ((t * 0.45f) + k / 3f) % 1f
        val x = originX + q * 34f
        val y = originY - q * 40f
        val s = 6f + q * 6f
        val z = Path().apply {
            moveTo(x, y)
            lineTo(x + s, y)
            lineTo(x, y + s)
            lineTo(x + s, y + s)
        }
        drawPath(z, color, alpha = sin(PI.toFloat() * q), style = line(2.4f))
    }
}

private fun DrawScope.sleeper(motion: PandaMotion) {
    val t = motion.time
    val breath = sin(TAU * t / 3.6f)
    solid(Art.pillow, Pillow)
    drawPath(Art.pillowShine, Color.White, alpha = 0.8f, style = line(3f))
    translate(0f, -1.6f * breath) {
        solid(Art.blanket, Cobalt)
        for (i in 0 until 5) {
            val lift = if (i in 1..3) 2f else 0f
            translate(104f + i * 18f, 136f - lift) { drawPath(Art.knit, Knit, style = line(2.2f)) }
        }
        drawPath(Art.blanketHem, Ink, style = line(12f))
        drawPath(Art.blanketHem, Fur, style = line(7f))
        drawPath(Art.sleepArm, motion.rim, style = line(19f))
        drawPath(Art.sleepArm, Ink, style = line(16f))
    }
    oval(196f, 184f, 11f, 8.5f, Ink)
    drawOval(motion.rim, Offset(185f, 175.5f), Size(22f, 17f), style = Stroke(1.6f))
    oval(199f, 185f, 4.5f, 3.5f, Pad)
    val smile = motion.joy > 0.05f
    withTransform({
        translate(68f, 128f)
        rotate(-12f + 1.2f * breath, Offset.Zero)
        scale(0.78f, 0.78f, Offset.Zero)
        translate(-100f, -92f)
    }) {
        ears(motion.rim, 0f)
        head()
        face(Look(Eyes.CLOSED, if (smile) Mouth.SMILE else Mouth.TINY, blush = 0.85f), 0f, 0f)
        nightcap(Art.capSleep, -14f, Offset(148f, 100f), 0f)
    }
    zees(t, 112f, 86f, Fur)
}

private fun DrawScope.hearts(progress: Float) {
    if (progress <= 0f || progress >= 1f) return
    val spots = floatArrayOf(-34f, -14f, 6f, 24f, 40f)
    spots.forEachIndexed { i, dx ->
        val local = ((progress - i * 0.08f) / 0.7f).coerceIn(0f, 1f)
        if (local <= 0f) return@forEachIndexed
        val y = 40f - local * 46f
        val x = 100f + dx + 6f * sin(local * TAU + i)
        val s = 0.9f + 0.4f * sin(PI.toFloat() * local)
        withTransform({
            translate(x - 6f * s, y)
            scale(s, s, Offset.Zero)
        }) {
            drawPath(Art.heart, Heart, alpha = sin(PI.toFloat() * local))
        }
    }
}

fun DrawScope.drawPanda(mood: PandaMood, motion: PandaMotion) {
    val t = motion.time
    val breath = sin(TAU * t / 3.2f)
    val blink = blinkAt(t)
    val joy = motion.joy
    val hop = sin(PI.toFloat() * motion.hop.coerceIn(0f, 1f))
    val awakeJoy = joy > 0.05f && mood != PandaMood.SLEEP

    if (mood == PandaMood.SLEEP) {
        sleeper(motion)
        hearts(motion.hearts)
        return
    }

    translate(0f, -14f * hop) {
        scale(1f + 0.04f * hop, 1f - 0.03f * hop, Offset(100f, 196f)) {
            when (mood) {
                PandaMood.SNACK -> {
                    body(breath)
                    feet(motion.rim)
                    bamboo(t)
                    headGroup(
                        motion.rim, t,
                        if (awakeJoy) Look(Eyes.HAPPY, Mouth.GRIN, blush = 0.8f) else Look(Eyes.HAPPY, Mouth.CHEW),
                        2f * sin(t * 1.1f), blink, 0.8f * sin(t * 9f), null
                    )
                    limb(Art.armSnackL, motion.rim)
                    limb(Art.armSnackR, motion.rim)
                }
                PandaMood.COCOA -> {
                    body(breath)
                    feet(motion.rim)
                    headGroup(
                        motion.rim, t,
                        if (awakeJoy) Look(Eyes.HAPPY, Mouth.GRIN, blush = 0.8f) else Look(Eyes.OPEN, Mouth.SMILE, glance = 1.3f * sin(t * 0.5f)),
                        1.5f * sin(t * 0.9f), blink, 0f
                    ) { nightcap(Art.cap, -4f, Offset(168f, 103f), 3f * sin(t * 1.4f)) }
                    limb(Art.armCocoaL, motion.rim)
                    limb(Art.armCocoaR, motion.rim)
                    cocoa(t)
                }
                PandaMood.YAWN -> {
                    val p = t % 5.5f
                    val yawn = if (p < 2.6f) sin(PI.toFloat() * p / 2.6f) else 0f
                    val lift = -9f * yawn
                    rotate(-lift, Offset(64f, 136f)) { limb(Art.armUpL, motion.rim) }
                    rotate(lift, Offset(136f, 136f)) { limb(Art.armUpR, motion.rim) }
                    body(breath)
                    feet(motion.rim)
                    val look = when {
                        awakeJoy -> Look(Eyes.HAPPY, Mouth.GRIN, blush = 0.8f)
                        yawn > 0.25f -> Look(Eyes.SQUEEZE, Mouth.YAWN, blush = 0.7f, yawn = yawn)
                        else -> Look(Eyes.HALF, Mouth.TINY, blush = 0.7f)
                    }
                    headGroup(motion.rim, t, look, -3f * yawn, blink, 0f) {
                        nightcap(Art.cap, 6f, Offset(168f, 103f), 4f * yawn)
                        if (yawn > 0.45f && !awakeJoy) {
                            solid(Art.tear, Tear, 1.2f)
                        }
                    }
                }
                PandaMood.MOON -> {
                    body(breath)
                    feet(motion.rim)
                    headGroup(
                        motion.rim, t,
                        if (awakeJoy) Look(Eyes.HAPPY, Mouth.SMILE, blush = 0.85f) else Look(Eyes.HALF, Mouth.TINY, blush = 0.75f),
                        3f * sin(t * 0.8f), blink, 0f
                    ) { nightcap(Art.cap, -10f, Offset(168f, 103f), 2f * sin(t * 0.8f)) }
                    moonPlush(t)
                    limb(Art.armMoonL, motion.rim)
                    limb(Art.armMoonR, motion.rim)
                }
                PandaMood.MORNING -> {
                    sunRays(t)
                    val wave = 6f * sin(t * 3f)
                    rotate(-wave, Offset(64f, 136f)) { limb(Art.armUpL, motion.rim) }
                    rotate(wave, Offset(136f, 136f)) { limb(Art.armUpR, motion.rim) }
                    body(breath)
                    feet(motion.rim)
                    headGroup(
                        motion.rim, t,
                        if (awakeJoy) Look(Eyes.HAPPY, Mouth.GRIN, blush = 0.8f) else Look(Eyes.OPEN, Mouth.GRIN, blush = 0.7f),
                        2f * sin(t * 1.5f), blink, 0f
                    ) { nightcap(Art.cap, 22f, Offset(168f, 103f), 3f * sin(t * 2f)) }
                    val s = 1f + 0.2f * sin(t * 4f)
                    scale(s, s, Offset(156f, 73f)) {
                        drawPath(Art.sparkle, Honey)
                        drawPath(Art.sparkle, Ink, style = line(1.2f))
                    }
                }
                PandaMood.PAUSED -> {
                    body(breath)
                    feet(motion.rim)
                    headGroup(
                        motion.rim, t,
                        if (awakeJoy) Look(Eyes.HAPPY, Mouth.GRIN, blush = 0.8f) else Look(Eyes.OPEN, Mouth.SMILE, glance = -1f),
                        2f * sin(t * 0.7f), blink, 0f, null
                    )
                    pauseSign(t)
                    limb(Art.armSignL, motion.rim)
                    limb(Art.armSignR, motion.rim)
                }
                PandaMood.SLEEP -> Unit
            }
        }
    }
    hearts(motion.hearts)
}
