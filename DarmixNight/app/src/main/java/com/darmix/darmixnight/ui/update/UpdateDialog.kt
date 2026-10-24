package com.darmix.darmixnight.ui.update

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darmix.darmixnight.PermissionUtils
import com.darmix.darmixnight.R
import com.darmix.darmixnight.ui.components.DnGlyph
import com.darmix.darmixnight.ui.components.DnPrimaryButton
import com.darmix.darmixnight.ui.components.DnSecondaryButton
import com.darmix.darmixnight.ui.components.Glyph
import com.darmix.darmixnight.ui.components.buildStars
import com.darmix.darmixnight.ui.theme.DnTheme
import com.darmix.darmixnight.ui.theme.DnType
import com.darmix.darmixnight.ui.theme.NightBg
import com.darmix.darmixnight.ui.theme.NightBgTop
import com.darmix.darmixnight.update.ReleaseInfo
import com.darmix.darmixnight.update.UpdateInstaller
import com.darmix.darmixnight.update.UpdateManager
import com.darmix.darmixnight.update.UpdateState
import java.util.Locale
import kotlin.math.sin

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

    Dialog(
        onDismissRequest = { if (!busy) onLater() },
        properties = DialogProperties(
            dismissOnBackPress = !busy,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.outline),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 420.dp)
        ) {
            Column {
                DialogHeader()
                Column(
                    modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("NUEVA VERSIÓN DISPONIBLE", style = DnType.overline, color = colors.accent)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "DarmixNight ${release.version}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.text,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    VersionChips(installed = installed, next = release.version, size = release.apkSize)

                    if (release.notes.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 170.dp)
                                .background(colors.raised, RoundedCornerShape(18.dp))
                                .verticalScroll(rememberScrollState())
                                .padding(14.dp)
                        ) {
                            Text("Novedades", style = MaterialTheme.typography.titleSmall, color = colors.text)
                            Spacer(Modifier.height(6.dp))
                            Text(release.notes, style = MaterialTheme.typography.bodySmall, color = colors.textDim)
                        }
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
}

@Composable
private fun DialogHeader() {
    val colors = DnTheme.colors
    val stars = remember { buildStars(count = 26, seed = 5, maxY = 0.9f) }
    val twinkle = rememberInfiniteTransition(label = "updateStars").animateFloat(
        initialValue = 0f,
        targetValue = 6.2832f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "updateTwinkle"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(104.dp)
                .align(Alignment.TopCenter)
                .drawWithCache {
                    val sky = Brush.verticalGradient(listOf(NightBgTop, NightBg.copy(alpha = 0.9f), colors.card))
                    onDrawBehind {
                        drawRect(sky)
                        stars.forEach { s ->
                            drawCircle(
                                Color.White,
                                s.radiusDp * density,
                                Offset(s.x * size.width, s.y * size.height * 0.85f),
                                alpha = 0.35f + 0.45f * (0.5f + 0.5f * sin(twinkle.value + s.phase))
                            )
                        }
                    }
                }
        )
        Image(
            painter = painterResource(R.drawable.dn_panda_avatar),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(bottom = 10.dp)
                .size(78.dp)
                .clip(CircleShape)
                .border(2.dp, colors.accent.copy(alpha = 0.6f), CircleShape)
        )
    }
}

@Composable
private fun VersionChips(installed: String, next: String, size: Long) {
    val colors = DnTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Chip("Tienes $installed", colors.raised, colors.textDim)
            Glyph(
                DnGlyph.CHEVRON,
                colors.textDim,
                size = 16.dp,
                strokeWidth = 2f,
                modifier = Modifier.graphicsLayer { rotationZ = -90f }
            )
            Chip("Nueva $next", colors.accent.copy(alpha = 0.16f), colors.accent)
        }
        if (size > 0) {
            Spacer(Modifier.height(6.dp))
            Text("Descarga de ${megabytes(size)}", style = MaterialTheme.typography.bodySmall, color = colors.textDim)
        }
    }
}

@Composable
private fun Chip(text: String, background: Color, content: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        modifier = Modifier
            .background(background, CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Descargando…", style = MaterialTheme.typography.titleSmall, color = colors.text, modifier = Modifier.weight(1f))
                    Text("${(fraction * 100).toInt()}%", style = DnType.numeric.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize), color = colors.accent)
                }
                ProgressBar(fraction = fraction)
                Text(
                    if (state.total > 0) "${megabytes(state.downloaded)} de ${megabytes(state.total)}" else megabytes(state.downloaded),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textDim
                )
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
    val animated by animateFloatAsState(fraction ?: 0f, tween(250), label = "updateProgress")
    val sweep = rememberInfiniteTransition(label = "indeterminate").animateFloat(
        initialValue = -0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "indeterminateSweep"
    )
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
                        val start = sweep.value * size.width
                        drawRoundRect(brush, Offset(start, 0f), Size(size.width * 0.35f, size.height), radius)
                    } else if (animated > 0f) {
                        drawRoundRect(brush, Offset.Zero, Size(size.width * animated, size.height), radius)
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
