package com.darmix.darmixnight.ui.update

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darmix.darmixnight.PermissionUtils
import com.darmix.darmixnight.ui.components.DnGlyph
import com.darmix.darmixnight.ui.components.DnPrimaryButton
import com.darmix.darmixnight.ui.components.DnSecondaryButton
import com.darmix.darmixnight.ui.components.Glyph
import com.darmix.darmixnight.ui.components.buildStars
import com.darmix.darmixnight.ui.components.rememberSceneClock
import com.darmix.darmixnight.ui.panda.PANDA_VIEWPORT
import com.darmix.darmixnight.ui.panda.PandaMood
import com.darmix.darmixnight.ui.panda.PandaMotion
import com.darmix.darmixnight.ui.panda.drawPanda
import com.darmix.darmixnight.ui.theme.DnTheme
import com.darmix.darmixnight.ui.theme.DnType
import com.darmix.darmixnight.update.ReleaseInfo
import com.darmix.darmixnight.update.UpdateInstaller
import com.darmix.darmixnight.update.UpdateManager
import com.darmix.darmixnight.update.UpdateState
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val SkyTop = Color(0xFF123068)
private val SkyMid = Color(0xFF0C1A3C)
private val Gold = Color(0xFFE8C796)
private val GoldLight = Color(0xFFFFF4D6)
private val PandaRim = Color(0xFF4A5680)

@Composable
fun UpdateHost() {
    val context = LocalContext.current
    val state by UpdateManager.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        UpdateManager.onResume(context)
        onPauseOrDispose { }
    }

    val current = state
    if (current !is UpdateState.Hidden) {
        UpdateDialog(
            state = current,
            onUpdate = { UpdateManager.startUpdate(context) },
            onLater = { UpdateManager.dismiss() },
            onCancelDownload = { UpdateManager.cancelDownload() },
            onGrantPermission = {
                PermissionUtils.open(context, UpdateInstaller.installPermissionIntent(context))
            }
        )
    }
}

private fun releaseOf(state: UpdateState): Pair<ReleaseInfo, String>? = when (state) {
    is UpdateState.Available -> state.release to state.installed
    is UpdateState.NeedsPermission -> state.release to state.installed
    is UpdateState.Downloading -> state.release to state.installed
    is UpdateState.Installing -> state.release to state.installed
    is UpdateState.Failed -> state.release to state.installed
    UpdateState.Hidden -> null
}

private fun megabytes(bytes: Long): String =
    String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)

private fun ringProgress(state: UpdateState): Float? = when (state) {
    is UpdateState.Downloading -> if (state.total > 0) (state.downloaded.toFloat() / state.total).coerceIn(0f, 1f) else 0f
    is UpdateState.Installing -> 1f
    else -> null
}

@Composable
private fun UpdateDialog(
    state: UpdateState,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    onCancelDownload: () -> Unit,
    onGrantPermission: () -> Unit
) {
    val (release, installed) = releaseOf(state) ?: return
    val colors = DnTheme.colors
    val busy = state is UpdateState.Downloading || (state is UpdateState.Installing && !state.waitingUser)
    val clock = rememberSceneClock()
    val shape = RoundedCornerShape(32.dp)

    Dialog(
        onDismissRequest = { if (!busy) onLater() },
        properties = DialogProperties(
            dismissOnBackPress = !busy,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 420.dp)
                .clip(shape)
                .background(colors.card, shape)
                .border(1.dp, Gold.copy(alpha = 0.25f), shape)
        ) {
            DialogHeader(clock = clock, progress = ringProgress(state), cardColor = colors.card)
            Column(
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("UNA NUEVA LUNA LLEGÓ", style = DnType.overline.copy(letterSpacing = 2.6.sp), color = colors.accent)
                Spacer(Modifier.height(6.dp))
                Text(
                    release.title.ifBlank { "DarmixNight ${release.version}" },
                    style = DnType.clockMedium.copy(fontSize = 22.sp, lineHeight = 28.sp),
                    color = colors.text,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                VersionJump(installed = installed, next = release.version, bytes = release.apkSize, clock = clock)

                if (release.notes.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    ReleaseNotes(release.notes)
                }

                Spacer(Modifier.height(18.dp))

                AnimatedContent(
                    targetState = state,
                    transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
                    contentKey = { it::class },
                    label = "updateFooter"
                ) { footerState ->
                    Footer(
                        state = footerState,
                        onUpdate = onUpdate,
                        onLater = onLater,
                        onCancelDownload = onCancelDownload,
                        onGrantPermission = onGrantPermission
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogHeader(clock: State<Float>, progress: Float?, cardColor: Color) {
    val stars = remember { buildStars(count = 34, seed = 5, maxY = 0.85f) }
    val ring by animateFloatAsState(progress ?: 0f, tween(300), label = "headerRing")
    val showRing = progress != null
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(196.dp)
            .drawWithCache {
                val sky = Brush.verticalGradient(0f to SkyTop, 0.6f to SkyMid, 1f to cardColor)
                val center = Offset(size.width / 2f, size.height * 0.52f)
                val halo = Brush.radialGradient(
                    0f to Gold.copy(alpha = 0.45f),
                    0.5f to Gold.copy(alpha = 0.12f),
                    1f to Color.Transparent,
                    center = center,
                    radius = size.height * 0.62f
                )
                onDrawBehind {
                    val t = clock.value
                    drawRect(sky)
                    stars.forEachIndexed { i, s ->
                        val twinkle = 0.5f + 0.5f * sin(t * (1f + (i % 3) * 0.5f) + s.phase)
                        drawCircle(
                            Color.White,
                            (s.radiusDp + 0.2f) * density,
                            Offset(s.x * size.width, s.y * size.height * 0.8f),
                            alpha = 0.2f + 0.6f * twinkle
                        )
                    }
                    shootingStar(t)
                    drawCircle(halo, size.height * 0.62f, center)
                    drawOrbit(center, 64.dp.toPx(), t, if (showRing) ring else null)
                    val pandaSize = 118.dp.toPx()
                    val s = pandaSize / PANDA_VIEWPORT
                    translate(center.x - pandaSize / 2f, center.y - pandaSize / 2f - 4.dp.toPx()) {
                        scale(s, s, Offset.Zero) {
                            drawPanda(PandaMood.MORNING, PandaMotion(t, 0f, 0f, 0f, PandaRim))
                        }
                    }
                }
            }
    )
}

private fun DrawScope.shootingStar(t: Float) {
    val period = 4.5f
    val p = t % period
    if (p > 0.9f) return
    val k = p / 0.9f
    val head = Offset(size.width * (0.92f - 0.5f * k), size.height * (0.08f + 0.22f * k))
    val tail = Offset(head.x + 50.dp.toPx(), head.y - 22.dp.toPx())
    val strength = sin(Math.PI.toFloat() * k)
    drawLine(
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.9f * strength), Color.Transparent), start = head, end = tail),
        head,
        tail,
        strokeWidth = 1.4.dp.toPx(),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawOrbit(center: Offset, radius: Float, t: Float, progress: Float?) {
    drawCircle(
        Gold,
        radius,
        center,
        alpha = 0.4f,
        style = Stroke(1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 5.dp.toPx()), t * 6f))
    )
    if (progress != null) {
        val stroke = 4.dp.toPx()
        drawCircle(Color.White.copy(alpha = 0.12f), radius, center, style = Stroke(stroke))
        drawArc(
            Brush.sweepGradient(listOf(Gold, GoldLight, Gold), center),
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(stroke, cap = StrokeCap.Round)
        )
        val a = Math.toRadians((-90.0 + 360.0 * progress)).toFloat()
        val head = Offset(center.x + radius * cos(a), center.y + radius * sin(a))
        drawCircle(Gold, 7.dp.toPx(), head, alpha = 0.3f)
        drawCircle(GoldLight, 3.5.dp.toPx(), head)
    } else {
        val a = t * 0.8f
        val p = Offset(center.x + radius * cos(a), center.y + radius * sin(a))
        drawCircle(Gold, 6.dp.toPx(), p, alpha = 0.25f)
        drawSparkle(p, 4.5.dp.toPx(), Gold)
    }
}

private fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color) {
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
    drawPath(path, color)
}

@Composable
private fun VersionJump(installed: String, next: String, bytes: Long, clock: State<Float>) {
    val colors = DnTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.raised.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(vertical = 14.dp, horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TIENES", style = DnType.overline.copy(fontSize = 10.sp), color = colors.textDim)
            Text(installed, style = DnType.numeric.copy(fontSize = 24.sp), color = colors.textDim)
        }
        Spacer(
            Modifier
                .width(64.dp)
                .height(20.dp)
                .drawBehind {
                    val y = size.height / 2f
                    for (i in 0 until 5) {
                        val x = size.width * (0.1f + 0.17f * i)
                        val wave = 0.5f + 0.5f * sin(clock.value * 4f - i * 0.9f)
                        drawCircle(colors.accent, 2.6.dp.toPx(), Offset(x, y), alpha = 0.25f + 0.75f * wave)
                    }
                    val tip = Offset(size.width * 0.96f, y)
                    drawLine(colors.accent, Offset(tip.x - 6.dp.toPx(), y - 6.dp.toPx()), tip, 2.dp.toPx(), StrokeCap.Round)
                    drawLine(colors.accent, Offset(tip.x - 6.dp.toPx(), y + 6.dp.toPx()), tip, 2.dp.toPx(), StrokeCap.Round)
                }
        )
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("NUEVA", style = DnType.overline.copy(fontSize = 10.sp), color = colors.accent)
            Text(next, style = DnType.numeric.copy(fontSize = 30.sp), color = colors.accent)
        }
    }
    if (bytes > 0) {
        Spacer(Modifier.height(8.dp))
        Text("Descarga de ${megabytes(bytes)}", style = MaterialTheme.typography.bodySmall, color = colors.textDim)
    }
}

@Composable
private fun ReleaseNotes(notes: String) {
    val colors = DnTheme.colors
    val lines = remember(notes) { notes.lines().map { it.trim() }.filter { it.isNotEmpty() } }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 180.dp)
            .background(colors.raised.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("NOVEDADES", style = DnType.overline, color = colors.textDim)
        lines.forEach { line ->
            if (line.startsWith("•")) {
                Row(verticalAlignment = Alignment.Top) {
                    Spacer(
                        Modifier
                            .padding(top = 4.dp)
                            .size(12.dp)
                            .drawBehind { drawSparkle(Offset(size.width / 2f, size.height / 2f), size.minDimension / 2f, Gold) }
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        line.removePrefix("•").trim(),
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = colors.text.copy(alpha = 0.9f)
                    )
                }
            } else {
                Text(
                    line,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.text
                )
            }
        }
    }
}

@Composable
private fun Footer(
    state: UpdateState,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    onCancelDownload: () -> Unit,
    onGrantPermission: () -> Unit
) {
    val colors = DnTheme.colors
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (state) {
            is UpdateState.Available -> {
                DnPrimaryButton("Actualizar ahora", onUpdate, glyph = DnGlyph.DOWNLOAD)
                DnSecondaryButton("Más tarde", onLater, tint = colors.textDim)
            }
            is UpdateState.NeedsPermission -> {
                Notice(
                    glyph = DnGlyph.SHIELD,
                    tone = colors.warn,
                    text = "Android necesita que permitas a DarmixNight instalar apps. Solo se usa para actualizarse a sí misma. Actívalo y regresa: la descarga empezará sola."
                )
                DnPrimaryButton("Dar permiso", onGrantPermission, glyph = DnGlyph.OPEN)
                DnSecondaryButton("Más tarde", onLater, tint = colors.textDim)
            }
            is UpdateState.Downloading -> {
                val fraction = if (state.total > 0) (state.downloaded.toFloat() / state.total).coerceIn(0f, 1f) else 0f
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("Descargando la nueva luna…", style = MaterialTheme.typography.titleSmall, color = colors.text)
                        Text(
                            if (state.total > 0) "${megabytes(state.downloaded)} de ${megabytes(state.total)}" else megabytes(state.downloaded),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textDim
                        )
                    }
                    Text(
                        "${(fraction * 100).toInt()}%",
                        style = DnType.numeric.copy(fontSize = 28.sp),
                        color = colors.accent
                    )
                }
                ProgressBar(fraction = fraction)
                DnSecondaryButton("Cancelar", onCancelDownload, tint = colors.textDim)
            }
            is UpdateState.Installing -> {
                if (state.waitingUser) {
                    Notice(
                        glyph = DnGlyph.CHECK,
                        tone = colors.success,
                        text = "Descarga lista. Confirma la instalación en la ventana de Android; la app se reiniciará con la nueva versión."
                    )
                    DnSecondaryButton("Cerrar", onLater, tint = colors.textDim)
                } else {
                    Text("Preparando la instalación…", style = MaterialTheme.typography.titleSmall, color = colors.text)
                    ProgressBar(fraction = null)
                }
            }
            is UpdateState.Failed -> {
                Notice(glyph = DnGlyph.INFO, tone = colors.danger, text = state.message)
                DnPrimaryButton("Reintentar", onUpdate, glyph = DnGlyph.DOWNLOAD)
                DnSecondaryButton("Más tarde", onLater, tint = colors.textDim)
            }
            UpdateState.Hidden -> Unit
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float?) {
    val colors = DnTheme.colors
    val clock = rememberSceneClock()
    val animated by animateFloatAsState(fraction ?: 0f, tween(250, easing = LinearEasing), label = "updateProgress")
    Box(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(CircleShape)
            .background(colors.raised)
            .drawWithCache {
                val radius = CornerRadius(size.height / 2f)
                val brush = Brush.horizontalGradient(listOf(colors.accentAlt, colors.accent))
                onDrawBehind {
                    if (fraction == null) {
                        val sweep = ((clock.value / 1.1f) % 1f) * 1.35f - 0.35f
                        drawRoundRect(brush, Offset(sweep * size.width, 0f), Size(size.width * 0.35f, size.height), radius)
                    } else if (animated > 0f) {
                        val w = size.width * animated
                        drawRoundRect(brush, Offset.Zero, Size(w, size.height), radius)
                        val shine = ((clock.value / 1.4f) % 1f) * w
                        drawRect(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
                                startX = shine - 30.dp.toPx(),
                                endX = shine + 30.dp.toPx()
                            ),
                            Offset.Zero,
                            Size(w, size.height)
                        )
                    }
                }
            }
    )
}

@Composable
private fun Notice(glyph: DnGlyph, tone: Color, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(tone.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Glyph(glyph, tone, size = 18.dp)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = tone)
    }
}
