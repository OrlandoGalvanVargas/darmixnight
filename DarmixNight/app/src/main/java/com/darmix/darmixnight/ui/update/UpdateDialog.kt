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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darmix.darmixnight.PermissionUtils
import com.darmix.darmixnight.R
import com.darmix.darmixnight.ui.components.EmberButton
import com.darmix.darmixnight.ui.components.QuietLink
import com.darmix.darmixnight.ui.components.rememberSceneClock
import com.darmix.darmixnight.ui.theme.DnTheme
import com.darmix.darmixnight.ui.theme.DnType
import com.darmix.darmixnight.ui.theme.InstrumentSerif
import com.darmix.darmixnight.update.ReleaseInfo
import com.darmix.darmixnight.update.UpdateInstaller
import com.darmix.darmixnight.update.UpdateManager
import com.darmix.darmixnight.update.UpdateState
import java.util.Locale
import kotlin.math.roundToInt

private const val HEADER_X = 300
private const val HEADER_Y = 230
private const val HEADER_W = 840f
private const val HEADER_H = 470f

private val CardColor = Color(0xFF150B07)

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
    val header = ImageBitmap.imageResource(R.drawable.settings_window_lit)
    val shape = RoundedCornerShape(30.dp)

    Dialog(
        onDismissRequest = { if (!busy) onLater() },
        properties = DialogProperties(
            dismissOnBackPress = !busy,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth(0.88f)
                .widthIn(max = 400.dp)
                .clip(shape)
                .background(CardColor)
                .border(1.dp, colors.ember.copy(alpha = 0.3f), shape)
        ) {
            val headerHeight = maxWidth * (HEADER_H / HEADER_W)
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .drawBehind {
                        drawImage(
                            header,
                            srcOffset = IntOffset(HEADER_X, HEADER_Y),
                            srcSize = IntSize(HEADER_W.toInt(), HEADER_H.toInt()),
                            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
                        )
                        drawRect(Brush.verticalGradient(0.4f to CardColor.copy(alpha = 0f), 1f to CardColor))
                    }
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = headerHeight - 30.dp, start = 24.dp, end = 24.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Hay una nueva versión",
                    style = TextStyle(fontFamily = InstrumentSerif, fontSize = 30.sp, lineHeight = 34.sp),
                    color = colors.text,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                val jump = buildAnnotatedString {
                    append(installed)
                    append("  →  ")
                    withStyle(SpanStyle(color = colors.ember)) { append(release.version) }
                    if (release.apkSize > 0) append("  ·  ${megabytes(release.apkSize)}")
                }
                Text(
                    jump,
                    style = DnType.note.copy(fontSize = 19.sp, lineHeight = 24.sp),
                    color = colors.textSoft,
                    textAlign = TextAlign.Center
                )
                if (release.notes.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
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
private fun ReleaseNotes(notes: String) {
    val colors = DnTheme.colors
    val lines = remember(notes) { notes.lines().map { it.trim() }.filter { it.isNotEmpty() } }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 150.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        lines.forEach { line ->
            val bullet = line.startsWith("•") || line.startsWith("-") || line.startsWith("*")
            if (bullet) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        "·",
                        style = DnType.note.copy(fontSize = 18.sp, lineHeight = 22.sp),
                        color = colors.ember,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        line.drop(1).trim(),
                        style = TextStyle(fontFamily = InstrumentSerif, fontSize = 17.sp, lineHeight = 22.sp),
                        color = colors.textSoft
                    )
                }
            } else {
                Text(
                    line.trimStart('#').trim(),
                    style = DnType.note.copy(fontSize = 18.sp, lineHeight = 22.sp),
                    color = colors.clay
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
    val noteStyle = DnType.noteSmall.copy(fontSize = 16.sp, lineHeight = 21.sp)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        when (state) {
            is UpdateState.Available -> {
                EmberButton("Actualizar", onClick = onUpdate)
                Spacer(Modifier.height(4.dp))
                QuietLink("Más tarde", onClick = onLater)
            }
            is UpdateState.NeedsPermission -> {
                Text(
                    "Android pide permitir que DarmixNight instale apps. Solo se usa para actualizarse; al volver, la descarga empieza sola.",
                    style = noteStyle,
                    color = colors.clay,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))
                EmberButton("Dar permiso", onClick = onGrantPermission)
                Spacer(Modifier.height(4.dp))
                QuietLink("Más tarde", onClick = onLater)
            }
            is UpdateState.Downloading -> {
                val fraction = if (state.total > 0) (state.downloaded.toFloat() / state.total).coerceIn(0f, 1f) else 0f
                Text(
                    if (state.total > 0) "${(fraction * 100).toInt()} %  ·  ${megabytes(state.downloaded)} de ${megabytes(state.total)}" else megabytes(state.downloaded),
                    style = noteStyle,
                    color = colors.textSoft
                )
                Spacer(Modifier.height(10.dp))
                EmberLine(fraction = fraction)
                Spacer(Modifier.height(6.dp))
                QuietLink("Cancelar", onClick = onCancelDownload)
            }
            is UpdateState.Installing -> {
                if (state.waitingUser) {
                    Text(
                        "Descarga lista. Confirma la instalación en la ventana de Android y la app se abrirá con la nueva versión.",
                        style = noteStyle,
                        color = colors.clay,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    QuietLink("Cerrar", onClick = onLater)
                } else {
                    Text("Preparando la instalación", style = noteStyle, color = colors.textSoft)
                    Spacer(Modifier.height(10.dp))
                    EmberLine(fraction = null)
                    Spacer(Modifier.height(12.dp))
                }
            }
            is UpdateState.Failed -> {
                Text(state.message, style = noteStyle, color = colors.danger, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                EmberButton("Reintentar", onClick = onUpdate)
                Spacer(Modifier.height(4.dp))
                QuietLink("Más tarde", onClick = onLater)
            }
            UpdateState.Hidden -> Unit
        }
    }
}

@Composable
private fun EmberLine(fraction: Float?) {
    val colors = DnTheme.colors
    val clock = rememberSceneClock()
    val animated by animateFloatAsState(fraction ?: 0f, tween(250, easing = LinearEasing), label = "updateProgress")
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .drawWithCache {
                val y = size.height / 2f
                val stroke = 2.dp.toPx()
                onDrawBehind {
                    drawLine(colors.hairline, Offset(0f, y), Offset(size.width, y), stroke, StrokeCap.Round)
                    if (fraction == null) {
                        val head = ((clock.value / 1.2f) % 1f) * size.width * 1.3f - size.width * 0.15f
                        drawLine(
                            Brush.horizontalGradient(
                                listOf(colors.ember.copy(alpha = 0f), colors.ember, colors.ember.copy(alpha = 0f)),
                                startX = head - size.width * 0.15f,
                                endX = head + size.width * 0.15f
                            ),
                            Offset((head - size.width * 0.15f).coerceAtLeast(0f), y),
                            Offset((head + size.width * 0.15f).coerceAtMost(size.width), y),
                            stroke,
                            StrokeCap.Round
                        )
                    } else if (animated > 0f) {
                        val end = size.width * animated
                        drawLine(colors.ember, Offset(0f, y), Offset(end, y), stroke, StrokeCap.Round)
                        drawCircle(colors.ember, 6.dp.toPx(), Offset(end, y), alpha = 0.25f)
                        drawCircle(colors.flame, 3.dp.toPx(), Offset(end, y))
                    }
                }
            }
    )
}
