package com.darmix.darmixnight.ui.main.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darmix.darmixnight.ui.components.AppConstellation
import com.darmix.darmixnight.ui.components.ConstellationApp
import com.darmix.darmixnight.ui.components.DnGlyph
import com.darmix.darmixnight.ui.components.Glyph
import com.darmix.darmixnight.ui.components.rememberSceneClock
import com.darmix.darmixnight.ui.main.SectionHeader
import com.darmix.darmixnight.ui.main.TimeText
import com.darmix.darmixnight.ui.theme.DnTheme
import com.darmix.darmixnight.ui.theme.DnType
import com.darmix.darmixnight.update.ManualCheck
import com.darmix.darmixnight.update.UpdateManager
import com.darmix.darmixnight.update.UpdateRepository
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.darmix.darmixnight.ui.components.DnPrimaryButton
import com.darmix.darmixnight.ui.components.DnSecondaryButton
import com.darmix.darmixnight.ui.panda.PANDA_VIEWPORT
import com.darmix.darmixnight.ui.panda.PandaMood
import com.darmix.darmixnight.ui.panda.PandaMotion
import com.darmix.darmixnight.ui.panda.drawPanda
import androidx.compose.foundation.layout.widthIn
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val MoonLight = Color(0xFFFFF8E6)
private val MoonGoldNight = Color(0xFFF4E3BC)
private val MoonGoldDay = Color(0xFFF2CF9C)
private val MoonEdge = Color(0xFFC99A55)
private val SilverLight = Color(0xFFF2F4F8)
private val Silver = Color(0xFFC3C9D5)
private val SilverEdge = Color(0xFF8A93A6)
private val EmblemTop = Color(0xFF123068)
private val EmblemBottom = Color(0xFF0C1A3C)
private val PandaRimNight = Color(0xFF4A5680)
private val PauseStars = listOf(
    0.08f to 0.18f, 0.18f to 0.52f, 0.26f to 0.12f, 0.42f to 0.3f, 0.52f to 0.1f,
    0.86f to 0.16f, 0.92f to 0.48f, 0.12f to 0.78f, 0.88f to 0.74f, 0.58f to 0.62f
)

private class Crater(val dx: Float, val dy: Float, val r: Float)

private val Craters = listOf(
    Crater(-0.31f, -0.22f, 0.16f),
    Crater(0.28f, 0.16f, 0.2f),
    Crater(-0.12f, 0.41f, 0.11f),
    Crater(0.41f, -0.37f, 0.09f),
    Crater(-0.47f, 0.22f, 0.08f)
)

private val AllowedApps = listOf(
    ConstellationApp(DnGlyph.CALL, "Llamadas"),
    ConstellationApp(DnGlyph.MESSAGE, "Mensajes"),
    ConstellationApp(DnGlyph.CONTACT, "Contactos"),
    ConstellationApp(DnGlyph.CAMERA, "Cámara"),
    ConstellationApp(DnGlyph.SLIDERS, "Ajustes")
)

@Composable
internal fun SettingsTab(
    paused: Boolean,
    canToggle: Boolean,
    resumesAt: Int,
    onToggle: () -> Unit
) {
    val colors = DnTheme.colors
    val locked = !paused && !canToggle
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("LUNA DE GUARDIA", style = DnType.overline.copy(letterSpacing = 3.sp), color = colors.accent)
        GuardianMoon(
            active = !paused,
            locked = locked,
            enabled = paused || canToggle,
            onToggle = onToggle
        )
        AnimatedContent(
            targetState = Triple(paused, locked, resumesAt),
            transitionSpec = { fadeIn(tween(320, delayMillis = 120)) togetherWith fadeOut(tween(160)) },
            label = "guardianText"
        ) { (isPaused, isLocked, resume) ->
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (isPaused) "En pausa" else "Protección activa",
                    style = DnType.clockMedium.copy(fontSize = 22.sp, lineHeight = 28.sp),
                    color = colors.text
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .background(if (isPaused) colors.warn else colors.success, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            isLocked -> "No se puede pausar hasta las ${TimeText.full(resume)}"
                            isPaused -> "Toca la luna para reactivar"
                            else -> "Toca la luna para pausar"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textDim,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(34.dp))
        SectionHeader(title = "DURANTE EL DESCANSO PUEDES USAR", hint = "")
        Spacer(Modifier.height(14.dp))
        AppConstellation(AllowedApps)

        Spacer(Modifier.height(30.dp))
        SectionHeader(title = "ACERCA DE", hint = "")
        Spacer(Modifier.height(12.dp))
        AboutCard()
    }
}

@Composable
private fun GuardianMoon(active: Boolean, locked: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    val colors = DnTheme.colors
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val clock = rememberSceneClock()
    val press = remember { Animatable(1f) }
    val on by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(900),
        label = "guardianOn"
    )
    val lock by animateFloatAsState(
        targetValue = if (locked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "guardianLock"
    )
    val toggle by rememberUpdatedState(onToggle)
    val canTap by rememberUpdatedState(enabled)
    val gold = if (colors.isDark) MoonGoldNight else MoonGoldDay
    val badge = colors.card
    val accent = colors.accent

    Spacer(
        Modifier
            .fillMaxWidth()
            .height(240.dp)
            .semantics {
                role = Role.Switch
                stateDescription = if (active) "Protección activa" else "En pausa"
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        scope.launch { press.animateTo(0.93f, tween(120)) }
                        tryAwaitRelease()
                        scope.launch { press.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium)) }
                    },
                    onTap = {
                        if (canTap) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            toggle()
                        } else {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                )
            }
            .drawBehind {
                drawGuardian(clock.value, on, lock, press.value, gold, badge, accent)
            }
    )
}

private fun DrawScope.drawGuardian(
    t: Float,
    on: Float,
    lock: Float,
    press: Float,
    gold: Color,
    badge: Color,
    accent: Color
) {
    val u = density
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = 64f * u
    val breathe = 1f + 0.015f * sin(t * 1.6f) * on
    val light = lerp(SilverLight, MoonLight, on)
    val mid = lerp(Silver, gold, on)
    val edge = lerp(SilverEdge, MoonEdge, on)

    if (on < 0.99f) {
        val cold = r * 1.5f
        drawCircle(
            Brush.radialGradient(
                0f to SilverLight.copy(alpha = 0.22f * (1f - on)),
                (r / cold) to SilverLight.copy(alpha = 0.16f * (1f - on)),
                1f to Color.Transparent,
                center = c,
                radius = cold
            ),
            radius = cold,
            center = c
        )
    }

    if (on > 0.01f) {
        val halo = r * 2.4f
        drawCircle(
            Brush.radialGradient(
                0f to gold.copy(alpha = 0.5f * on),
                0.45f to gold.copy(alpha = 0.14f * on),
                1f to Color.Transparent,
                center = c,
                radius = halo
            ),
            radius = halo,
            center = c
        )
        val reach = r * 2.05f
        val beamBrush = Brush.radialGradient(
            0f to gold.copy(alpha = 0.7f),
            0.48f to gold.copy(alpha = 0.32f),
            1f to Color.Transparent,
            center = c,
            radius = reach
        )
        drawBeams(c, r, reach, 12, t * 0.10f, 0.075f, t, 0f, beamBrush, on)
        drawBeams(c, r, reach * 0.86f, 9, -t * 0.07f + 0.2f, 0.055f, t, 2.1f, beamBrush, on * 0.8f)
        val shimmer = 0.88f + 0.12f * sin(t * 1.1f)
        val bloomRadii = floatArrayOf(1.9f, 1.45f, 1.18f)
        val bloomAlphas = floatArrayOf(0.30f, 0.42f, 0.55f)
        for (k in bloomRadii.indices) {
            val outer = r * bloomRadii[k] * (1f + 0.02f * sin(t * 0.9f + k))
            val edge = (r / outer).coerceIn(0f, 0.99f)
            drawCircle(
                Brush.radialGradient(
                    0f to gold.copy(alpha = bloomAlphas[k] * on * shimmer),
                    edge to gold.copy(alpha = bloomAlphas[k] * on * shimmer),
                    (edge + (1f - edge) * 0.35f) to gold.copy(alpha = bloomAlphas[k] * 0.35f * on * shimmer),
                    1f to Color.Transparent,
                    center = c,
                    radius = outer
                ),
                radius = outer,
                center = c
            )
        }
        drawCircle(
            Brush.radialGradient(
                0f to Color.White.copy(alpha = 0.0f),
                0.92f to Color.White.copy(alpha = 0.0f),
                0.97f to Color.White.copy(alpha = 0.35f * on * shimmer),
                1f to Color.Transparent,
                center = c,
                radius = r * 1.06f
            ),
            radius = r * 1.06f,
            center = c
        )
        val orbits = floatArrayOf(1.34f, 1.58f, 1.82f)
        val speeds = floatArrayOf(0.5f, -0.36f, 0.24f)
        for (i in orbits.indices) {
            val a = t * speeds[i] + i * 2.1f
            val pos = Offset(c.x + r * orbits[i] * cos(a), c.y + r * orbits[i] * sin(a))
            val twinkle = 0.55f + 0.45f * sin(t * 3f + i * 1.9f)
            drawSparkle(pos, (3.5f + 2.5f * twinkle) * u, gold, on * twinkle)
        }
    }

    drawCircle(
        lerp(Silver, gold, on),
        r + 8f * u,
        c,
        alpha = 0.25f + 0.15f * on,
        style = Stroke(1f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f * u, 6f * u), t * 4f * u))
    )

    scale(press * breathe, press * breathe, c) {
        drawCircle(
            Brush.radialGradient(
                0f to light,
                0.6f to mid,
                1f to edge,
                center = Offset(c.x - r * 0.24f, c.y - r * 0.3f),
                radius = r * 1.5f
            ),
            r,
            c
        )
        Craters.forEach { cr ->
            drawCircle(edge, cr.r * r, Offset(c.x + cr.dx * r, c.y + cr.dy * r), alpha = 0.32f)
        }
        drawCircle(Color.White, r, c, alpha = 0.08f + 0.06f * (1f - on), style = Stroke(1.2f * u))
    }

    if (lock > 0.01f) {
        val lc = Offset(c.x + r * 0.72f, c.y + r * 0.72f)
        scale(lock, lock, lc) {
            drawCircle(badge, 17f * u, lc)
            drawCircle(accent, 17f * u, lc, style = Stroke(1.5f * u))
            val g = 0.75f * u
            translate(lc.x - 12f * g, lc.y - 12f * g) {
                scale(g, g, Offset.Zero) {
                    drawPath(
                        DnGlyph.LOCK.path,
                        accent,
                        style = Stroke(2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawBeams(
    c: Offset,
    r: Float,
    reach: Float,
    count: Int,
    spin: Float,
    width: Float,
    t: Float,
    seed: Float,
    brush: Brush,
    strength: Float
) {
    val step = (2 * PI / count).toFloat()
    for (i in 0 until count) {
        val a = spin + i * step
        val length = reach * (0.72f + 0.28f * (0.5f + 0.5f * sin(t * 0.8f + i * 1.7f + seed)))
        val inner = r * 1.02f
        val beam = Path().apply {
            moveTo(c.x + inner * cos(a - width), c.y + inner * sin(a - width))
            lineTo(c.x + length * cos(a), c.y + length * sin(a))
            lineTo(c.x + inner * cos(a + width), c.y + inner * sin(a + width))
            close()
        }
        val flicker = 0.6f + 0.4f * sin(t * 1.3f + i * 2.3f + seed)
        drawPath(beam, brush, alpha = strength * flicker)
    }
}

private fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color, alpha: Float) {
    val thin = radius * 0.22f
    val sparkle = Path().apply {
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
    drawCircle(color, radius * 1.4f, center, alpha = alpha * 0.18f)
    drawPath(sparkle, color, alpha = alpha)
}

@Composable
private fun AboutCard() {
    val context = LocalContext.current
    val colors = DnTheme.colors
    val version = remember { UpdateRepository.installedVersion(context) }
    val manual by UpdateManager.manualCheck.collectAsStateWithLifecycle()
    val pending = remember(manual) { UpdateManager.hasPendingRelease(context) }
    val (chip, chipTone) = when {
        manual == ManualCheck.CHECKING -> "Buscando…" to colors.accent
        manual == ManualCheck.OFFLINE -> "Sin conexión" to colors.warn
        manual == ManualCheck.FOUND || pending -> "Nueva versión" to colors.accent
        else -> "Al día" to colors.success
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.card.copy(alpha = if (colors.isDark) 0.72f else 0.85f), RoundedCornerShape(22.dp))
            .border(1.dp, colors.text.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MoonEmblem()
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row {
                    Text("Darmix", style = DnType.clockMedium.copy(fontSize = 17.sp, lineHeight = 22.sp), color = colors.text)
                    Text(
                        "Night",
                        style = DnType.clockMedium.copy(
                            fontSize = 17.sp,
                            lineHeight = 22.sp,
                            brush = Brush.horizontalGradient(listOf(colors.accent, if (colors.isDark) colors.glow else colors.accent))
                        )
                    )
                }
                Text("Versión $version", style = MaterialTheme.typography.bodySmall, color = colors.textDim)
            }
            AnimatedContent(
                targetState = chip to chipTone,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                label = "updateChip"
            ) { (text, tone) ->
                Text(
                    text,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = tone,
                    modifier = Modifier
                        .background(tone.copy(alpha = 0.14f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton(
                glyph = DnGlyph.DOWNLOAD,
                label = if (manual == ManualCheck.CHECKING) "Buscando…" else "Buscar actualización",
                enabled = manual != ManualCheck.CHECKING,
                modifier = Modifier.weight(1f)
            ) { UpdateManager.checkNow(context) }
            PillButton(
                glyph = DnGlyph.OPEN,
                label = "Ver en GitHub",
                enabled = true,
                modifier = Modifier.weight(1f)
            ) {
                try {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(UpdateRepository.RELEASES_PAGE))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: Exception) {
                }
            }
        }
    }
}

@Composable
private fun PillButton(
    glyph: DnGlyph,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = DnTheme.colors
    Row(
        modifier = modifier
            .height(42.dp)
            .clip(CircleShape)
            .background(colors.raised.copy(alpha = 0.85f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Glyph(glyph, colors.accent, size = 16.dp, strokeWidth = 2.2f)
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.text.copy(alpha = if (enabled) 1f else 0.6f),
            maxLines = 1
        )
    }
}

@Composable
private fun MoonEmblem() {
    val colors = DnTheme.colors
    val clock = rememberSceneClock()
    Spacer(
        Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .drawBehind {
                val u = density
                drawRect(Brush.linearGradient(listOf(EmblemTop, EmblemBottom), start = Offset.Zero, end = Offset(size.width, size.height)))
                val c = Offset(size.width * 0.46f, size.height * 0.54f)
                val r = size.minDimension * 0.27f
                drawCircle(
                    Brush.radialGradient(
                        0f to MoonGoldNight.copy(alpha = 0.45f),
                        1f to Color.Transparent,
                        center = c,
                        radius = r * 2f
                    ),
                    radius = r * 2f,
                    center = c
                )
                val outer = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
                val bite = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(c.x + r * 0.42f, c.y - r * 0.36f), r * 0.86f)) }
                val crescent = Path().apply { op(outer, bite, androidx.compose.ui.graphics.PathOperation.Difference) }
                drawPath(crescent, Brush.linearGradient(listOf(MoonLight, MoonGoldNight, MoonEdge), start = Offset(c.x - r, c.y - r), end = Offset(c.x + r, c.y + r)))
                val twinkle = 0.75f + 0.25f * sin(clock.value * 2.2f)
                drawSparkle(Offset(size.width * 0.72f, size.height * 0.3f), 5f * u * twinkle, MoonGoldNight, 1f)
                drawCircle(Color.White, 1.2f * u, Offset(size.width * 0.24f, size.height * 0.24f), alpha = 0.7f)
                drawCircle(Color.White, 0.9f * u, Offset(size.width * 0.8f, size.height * 0.74f), alpha = 0.5f)
                drawRoundRect(
                    colors.accent.copy(alpha = 0.35f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                    style = Stroke(1f * u)
                )
            }
    )
}

@Composable
internal fun PauseDialog(startMinutes: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = DnTheme.colors
    val clock = rememberSceneClock()
    val dim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            dim.animateTo(1f, tween(1400, delayMillis = 500))
            dim.animateTo(0f, tween(1100, delayMillis = 1300))
        }
    }
    val shape = RoundedCornerShape(30.dp)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.88f)
                .widthIn(max = 400.dp)
                .clip(shape)
                .background(colors.card, shape)
                .border(1.dp, colors.warn.copy(alpha = 0.3f), shape)
        ) {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .drawBehind { drawPauseScene(clock.value, dim.value, colors.card) }
            )
            Column(
                Modifier.padding(start = 22.dp, end = 22.dp, bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("NOCHE LIBRE", style = DnType.overline.copy(letterSpacing = 2.6.sp), color = colors.warn)
                Spacer(Modifier.height(6.dp))
                Text(
                    "¿Pausar DarmixNight?",
                    style = DnType.clockMedium.copy(fontSize = 22.sp, lineHeight = 28.sp),
                    color = colors.text,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "La luna se tomará un descanso y dejará de vigilar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textDim,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(colors.raised.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PauseFact(DnGlyph.MOON, colors.warn, "Tu teléfono no se bloqueará, ni siquiera a las ${TimeText.full(startMinutes)}.")
                    PauseFact(DnGlyph.POWER, colors.success, "Reactívalo cuando quieras tocando la luna.")
                }
                Spacer(Modifier.height(18.dp))
                DnPrimaryButton(label = "Mantener activo", onClick = onDismiss, glyph = DnGlyph.CHECK)
                Spacer(Modifier.height(10.dp))
                DnSecondaryButton(label = "Sí, pausar", onClick = onConfirm, glyph = DnGlyph.POWER, tint = colors.warn)
            }
        }
    }
}

@Composable
private fun PauseFact(glyph: DnGlyph, tone: Color, text: String) {
    val colors = DnTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(32.dp)
                .background(tone.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Glyph(glyph, tone, size = 17.dp, strokeWidth = 2f)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = colors.text.copy(alpha = 0.9f))
    }
}

private fun DrawScope.drawPauseScene(t: Float, dim: Float, cardColor: Color) {
    val u = density
    drawRect(Brush.verticalGradient(0f to EmblemTop, 0.65f to EmblemBottom, 1f to cardColor))
    PauseStars.forEachIndexed { i, (fx, fy) ->
        val twinkle = 0.5f + 0.5f * sin(t * (1f + (i % 3) * 0.4f) + i)
        drawCircle(Color.White, (0.8f + (i % 3) * 0.4f) * u, Offset(size.width * fx, size.height * fy), alpha = 0.2f + 0.6f * twinkle)
    }
    val mc = Offset(size.width * 0.68f, size.height * 0.36f)
    val mr = 34f * u
    val gold = lerp(MoonGoldNight, Silver, dim)
    drawCircle(
        Brush.radialGradient(
            0f to gold.copy(alpha = 0.45f * (1f - 0.6f * dim)),
            1f to Color.Transparent,
            center = mc,
            radius = mr * 2.4f
        ),
        radius = mr * 2.4f,
        center = mc
    )
    drawCircle(
        Brush.radialGradient(
            0f to lerp(MoonLight, SilverLight, dim),
            0.6f to gold,
            1f to lerp(MoonEdge, SilverEdge, dim),
            center = Offset(mc.x - mr * 0.25f, mc.y - mr * 0.3f),
            radius = mr * 1.5f
        ),
        mr,
        mc
    )
    Craters.forEach { cr ->
        drawCircle(lerp(MoonEdge, SilverEdge, dim), cr.r * mr, Offset(mc.x + cr.dx * mr, mc.y + cr.dy * mr), alpha = 0.3f)
    }
    val pandaSize = 118f * u
    val s = pandaSize / PANDA_VIEWPORT
    translate(size.width * 0.38f - pandaSize / 2f, size.height - pandaSize - 6f * u) {
        scale(s, s, Offset.Zero) {
            drawPanda(PandaMood.PAUSED, PandaMotion(t, 0f, 0f, 0f, PandaRimNight))
        }
    }
}
