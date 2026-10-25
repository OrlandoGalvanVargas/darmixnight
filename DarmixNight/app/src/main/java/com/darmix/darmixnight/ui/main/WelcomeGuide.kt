package com.darmix.darmixnight.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.darmix.darmixnight.OverlayManager
import com.darmix.darmixnight.ui.components.DnGlyph
import com.darmix.darmixnight.ui.components.DnPrimaryButton
import com.darmix.darmixnight.ui.components.buildStars
import com.darmix.darmixnight.ui.components.rememberSceneClock
import com.darmix.darmixnight.ui.panda.PANDA_VIEWPORT
import com.darmix.darmixnight.ui.panda.PandaMood
import com.darmix.darmixnight.ui.panda.PandaMotion
import com.darmix.darmixnight.ui.panda.drawPanda
import com.darmix.darmixnight.ui.theme.DnTheme
import com.darmix.darmixnight.ui.theme.DnType
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val GuideSkyTop = Color(0xFF123068)
private val GuideSkyMid = Color(0xFF0C1A3C)
private val GuideGold = Color(0xFFE8C796)
private val GuideGoldLight = Color(0xFFFFF4D6)
private val GuideRed = Color(0xFFE5484D)
private val GuideRim = Color(0xFF4A5680)

private enum class GuideArt { HELLO, SCHEDULE, NIGHT, EMERGENCY, READY }

private class GuidePage(
    val art: GuideArt,
    val overline: String,
    val title: String,
    val body: String
)

private fun guidePages(start: Int, end: Int): List<GuidePage> = listOf(
    GuidePage(
        GuideArt.HELLO,
        "BIENVENIDO",
        "Hola, soy tu panda de guardia",
        "DarmixNight bloquea tu teléfono a la hora de dormir para que descanses de verdad, sin scroll infinito a medianoche."
    ),
    GuidePage(
        GuideArt.SCHEDULE,
        "TU HORARIO",
        "Tú eliges la noche",
        "En tu pase de descanso decides a qué hora te duermes y a qué hora despiertas. Ahora es de ${TimeText.full(start)} a ${TimeText.full(end)}, y lo cambias cuando quieras fuera de ese horario."
    ),
    GuidePage(
        GuideArt.NIGHT,
        "LLEGA LA NOCHE",
        "La pantalla se va a dormir",
        "A tu hora, cualquier app que abras queda cubierta por la pantalla de descanso. Llamadas, mensajes, contactos, cámara y ajustes siguen funcionando."
    ),
    GuidePage(
        GuideArt.EMERGENCY,
        "IMPREVISTOS",
        "Una salida, solo de día",
        "Si estás bloqueado de día y surge algo, mantén presionado ${OverlayManager.HOLD_DURATION_MS / 1000} segundos y escribe la frase. De noche no hay salida: así nada interrumpe tu descanso."
    ),
    GuidePage(
        GuideArt.READY,
        "TODO LISTO",
        "Buenas noches, de verdad",
        "Sigue tu ritual en la pestaña Noche y pausa el servicio en Ajustes tocando la luna. Yo me encargo del resto."
    )
)

@Composable
internal fun WelcomeGuide(startMinutes: Int, endMinutes: Int, onDone: () -> Unit) {
    val colors = DnTheme.colors
    val pages = remember(startMinutes, endMinutes) { guidePages(startMinutes, endMinutes) }
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val clock = rememberSceneClock()
    val shape = RoundedCornerShape(32.dp)
    val last = pager.currentPage == pages.lastIndex

    Dialog(
        onDismissRequest = onDone,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 420.dp)
                .clip(shape)
                .background(colors.card, shape)
                .border(1.dp, GuideGold.copy(alpha = 0.25f), shape)
        ) {
            Box {
                HorizontalPager(state = pager) { index ->
                    val page = pages[index]
                    val offset = (pager.currentPage - index) + pager.currentPageOffsetFraction
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GuideIllustration(page.art, offset, clock, colors.card)
                        Column(
                            Modifier
                                .padding(horizontal = 24.dp)
                                .graphicsLayer {
                                    val away = abs(offset).coerceIn(0f, 1f)
                                    alpha = 1f - away
                                    translationX = offset * 40.dp.toPx()
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                page.overline,
                                style = DnType.overline.copy(letterSpacing = 2.6.sp),
                                color = colors.accent
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                page.title,
                                style = DnType.clockMedium.copy(fontSize = 22.sp, lineHeight = 28.sp),
                                color = colors.text,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                page.body,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                                color = colors.textDim,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.height(110.dp)
                            )
                        }
                    }
                }
                AnimatedContent(
                    targetState = last,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp),
                    label = "guideSkip"
                ) { isLast ->
                    if (!isLast) {
                        Text(
                            "Saltar",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable(onClick = onDone)
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    } else {
                        Spacer(Modifier.size(1.dp))
                    }
                }
            }

            Column(
                Modifier.padding(start = 22.dp, end = 22.dp, bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MoonPhaseDots(pager, pages.size)
                Spacer(Modifier.height(18.dp))
                AnimatedContent(
                    targetState = last,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
                    label = "guideAction"
                ) { isLast ->
                    if (isLast) {
                        DnPrimaryButton("Comenzar", onDone, glyph = DnGlyph.MOON)
                    } else {
                        DnPrimaryButton(
                            "Siguiente",
                            { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoonPhaseDots(pager: PagerState, count: Int) {
    val colors = DnTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            Spacer(
                Modifier
                    .size(14.dp)
                    .drawBehind {
                        val position = pager.currentPage + pager.currentPageOffsetFraction
                        val fill = (position - i + 1f).coerceIn(0f, 1f)
                        val near = (1f - abs(position - i)).coerceIn(0f, 1f)
                        val r = size.minDimension / 2f * (0.7f + 0.3f * near)
                        val c = Offset(size.width / 2f, size.height / 2f)
                        drawCircle(colors.accent.copy(alpha = 0.4f), r, c, style = Stroke(1.2.dp.toPx()))
                        if (fill > 0f) {
                            val disc = Path().apply { addOval(Rect(c, r)) }
                            val shadow = Path().apply { addOval(Rect(Offset(c.x - r * 2.1f * fill, c.y), r)) }
                            val moon = if (fill >= 1f) disc else Path().apply { op(disc, shadow, PathOperation.Difference) }
                            drawPath(moon, colors.accent)
                        }
                        if (near > 0.5f) {
                            drawCircle(colors.accent, r * 1.9f, c, alpha = 0.12f * near)
                        }
                    }
            )
        }
    }
}

@Composable
private fun GuideIllustration(art: GuideArt, offset: Float, clock: State<Float>, cardColor: Color) {
    val stars = remember(art) { buildStars(count = 30, seed = art.ordinal * 7 + 3, maxY = 0.8f) }
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(250.dp)
            .drawBehind {
                val t = clock.value
                drawRect(Brush.verticalGradient(0f to GuideSkyTop, 0.62f to GuideSkyMid, 1f to cardColor))
                val drift = offset * 30.dp.toPx()
                stars.forEachIndexed { i, s ->
                    val twinkle = 0.5f + 0.5f * sin(t * (1f + (i % 3) * 0.5f) + s.phase)
                    drawCircle(
                        Color.White,
                        (s.radiusDp + 0.2f) * density,
                        Offset(s.x * size.width + drift * 0.4f, s.y * size.height),
                        alpha = 0.2f + 0.6f * twinkle
                    )
                }
                val center = Offset(size.width / 2f + drift * 2.2f, size.height * 0.56f)
                val haloR = 110.dp.toPx()
                drawCircle(
                    Brush.radialGradient(
                        0f to GuideGold.copy(alpha = 0.42f),
                        0.5f to GuideGold.copy(alpha = 0.12f),
                        1f to Color.Transparent,
                        center = center,
                        radius = haloR
                    ),
                    radius = haloR,
                    center = center
                )
                when (art) {
                    GuideArt.HELLO -> pandaAt(center, PandaMood.MORNING, t, 168f)
                    GuideArt.SCHEDULE -> {
                        scheduleIcons(center, t)
                        pandaAt(center, PandaMood.COCOA, t, 160f)
                    }
                    GuideArt.NIGHT -> {
                        drawCradleMoon(center, t)
                        pandaAt(Offset(center.x, center.y - 10.dp.toPx()), PandaMood.SLEEP, t, 150f)
                    }
                    GuideArt.EMERGENCY -> drawSiren(center, t)
                    GuideArt.READY -> pandaAt(center, PandaMood.MOON, t, 168f)
                }
            }
    )
}

private fun DrawScope.pandaAt(center: Offset, mood: PandaMood, t: Float, sizeDp: Float) {
    val box = sizeDp * density
    val s = box / PANDA_VIEWPORT
    translate(center.x - box / 2f, center.y - box / 2f) {
        scale(s, s, Offset.Zero) {
            drawPanda(mood, PandaMotion(t, 0f, 0f, 0f, GuideRim))
        }
    }
}

private fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color, alpha: Float = 1f) {
    val thin = radius * 0.32f
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + thin, center.y - thin)
        lineTo(center.x + radius, center.y)
        lineTo(center.x + thin, center.y + thin)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - thin, center.y + thin)
        lineTo(center.x - radius, center.y)
        lineTo(center.x - thin, center.y - thin)
        close()
    }
    drawPath(path, color, alpha = alpha)
}

private fun DrawScope.scheduleIcons(center: Offset, t: Float) {
    val u = density
    val orbit = 96f * u
    val a = t * 0.35f
    val moonPos = Offset(center.x + orbit * cos(a + 3.6f), center.y - 20f * u + orbit * 0.55f * sin(a + 3.6f))
    val sunPos = Offset(center.x + orbit * cos(a + 0.5f), center.y - 20f * u + orbit * 0.55f * sin(a + 0.5f))
    val moonR = 14f * u
    val disc = Path().apply { addOval(Rect(moonPos, moonR)) }
    val bite = Path().apply { addOval(Rect(Offset(moonPos.x + moonR * 0.45f, moonPos.y - moonR * 0.35f), moonR * 0.85f)) }
    drawCircle(GuideGold, moonR * 2f, moonPos, alpha = 0.15f)
    drawPath(Path().apply { op(disc, bite, PathOperation.Difference) }, GuideGold)
    drawCircle(Color(0xFFFFD36B), 10f * u, sunPos)
    for (i in 0 until 8) {
        val r = 0.785f * i + t
        drawLine(
            Color(0xFFFFD36B),
            Offset(sunPos.x + 14f * u * cos(r), sunPos.y + 14f * u * sin(r)),
            Offset(sunPos.x + 18f * u * cos(r), sunPos.y + 18f * u * sin(r)),
            strokeWidth = 2f * u,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawCradleMoon(center: Offset, t: Float) {
    val u = density
    val r = 92f * u
    val c = Offset(center.x, center.y - 4f * u)
    rotate(3f * sin(t * 0.9f), Offset(c.x, 0f)) {
        val outer = Path().apply { addOval(Rect(c, r)) }
        val inner = Path().apply { addOval(Rect(Offset(c.x, c.y - 38f * u), r)) }
        val crescent = Path().apply { op(outer, inner, PathOperation.Difference) }
        drawPath(
            crescent,
            Brush.verticalGradient(
                0f to GuideGoldLight,
                0.6f to GuideGold,
                1f to Color(0xFFC99A55),
                startY = c.y - r,
                endY = c.y + r
            )
        )
    }
}

private fun DrawScope.drawSiren(center: Offset, t: Float) {
    val u = density
    val c = Offset(center.x, center.y + 30f * u)
    val spin = t * 3.2f
    for (k in 0 until 2) {
        val a = spin + k * Math.PI.toFloat()
        val reach = 150f * u
        val beam = Path().apply {
            moveTo(c.x, c.y - 30f * u)
            lineTo(c.x + reach * cos(a - 0.22f), c.y - 30f * u + reach * sin(a - 0.22f))
            lineTo(c.x + reach * cos(a + 0.22f), c.y - 30f * u + reach * sin(a + 0.22f))
            close()
        }
        drawPath(beam, GuideRed, alpha = 0.16f)
    }
    val progress = (t / 3f) % 1f
    val ringR = 78f * u
    drawCircle(Color.White.copy(alpha = 0.12f), ringR, Offset(c.x, c.y - 18f * u), style = Stroke(5f * u))
    drawArc(
        GuideRed,
        startAngle = -90f,
        sweepAngle = 360f * progress,
        useCenter = false,
        topLeft = Offset(c.x - ringR, c.y - 18f * u - ringR),
        size = Size(ringR * 2f, ringR * 2f),
        style = Stroke(5f * u, cap = StrokeCap.Round)
    )
    drawCircle(GuideRed, 60f * u, Offset(c.x, c.y - 26f * u), alpha = 0.18f)
    drawRoundRect(
        Color(0xFF3A4160),
        Offset(c.x - 46f * u, c.y),
        Size(92f * u, 16f * u),
        androidx.compose.ui.geometry.CornerRadius(6f * u)
    )
    val dome = Path().apply {
        arcTo(Rect(c.x - 36f * u, c.y - 36f * u, c.x + 36f * u, c.y + 36f * u), 180f, 180f, false)
        close()
    }
    drawPath(
        dome,
        Brush.radialGradient(
            listOf(Color(0xFFFF9A9D), GuideRed, Color(0xFF8E1F25)),
            center = Offset(c.x - 8f * u, c.y - 22f * u),
            radius = 48f * u
        )
    )
    drawLine(Color.White.copy(alpha = 0.6f), Offset(c.x - 20f * u, c.y - 16f * u), Offset(c.x - 8f * u, c.y - 28f * u), 3f * u, StrokeCap.Round)
    drawSparkle(Offset(c.x + 70f * u, c.y - 92f * u), 7f * u * (0.8f + 0.2f * sin(t * 3f)), GuideGold)
}
