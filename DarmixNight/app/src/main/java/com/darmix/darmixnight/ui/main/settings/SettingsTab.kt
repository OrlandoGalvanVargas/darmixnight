package com.darmix.darmixnight.ui.main.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.darmix.darmixnight.R
import com.darmix.darmixnight.ui.components.DnGlyph
import com.darmix.darmixnight.ui.components.Glyph
import com.darmix.darmixnight.ui.components.SoundBank
import com.darmix.darmixnight.ui.components.rememberSoundBank
import com.darmix.darmixnight.ui.main.TimeText
import com.darmix.darmixnight.ui.theme.DnTheme
import com.darmix.darmixnight.ui.theme.DnType
import com.darmix.darmixnight.ui.theme.InstrumentSerif
import com.darmix.darmixnight.update.UpdateRepository
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val OFFICIAL_PAGE = "https://darmixista.dev/darmixnight"
private const val AUTHOR_PAGE = "https://darmixista.dev"
private const val SWITCH_ASPECT = 590f / 826f

private const val PAUSE_HEADER_X = 300
private const val PAUSE_HEADER_Y = 230
private const val PAUSE_HEADER_W = 840f
private const val PAUSE_HEADER_H = 470f

private val PauseCard = Color(0xFF150B07)
private val PauseButtonLight = Color(0xFFFF9A5C)
private val PauseButtonDeep = Color(0xFFE5582C)

@Composable
internal fun SettingsTab(
    paused: Boolean,
    canToggle: Boolean,
    resumesAt: Int,
    soundOn: Boolean,
    vibrationOn: Boolean,
    onToggle: () -> Unit,
    onSound: (Boolean) -> Unit,
    onVibration: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val f = DnTheme.scale.factor
    val locked = !paused && !canToggle
    val sounds = rememberSoundBank(R.raw.switch_on, R.raw.switch_off, R.raw.switch_locked, R.raw.button_press)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        ServiceSwitch(
            active = !paused,
            locked = locked,
            resumesAt = resumesAt,
            f = f,
            sounds = sounds,
            onToggle = onToggle
        )
        Spacer(Modifier.height((22 * f).dp))
        SectionTitle("Al girar la perilla", f)
        Spacer(Modifier.height((16 * f).dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PushButton(label = "Sonido", on = soundOn, f = f, sounds = sounds) { onSound(!soundOn) }
            PushButton(label = "Vibración", on = vibrationOn, f = f, sounds = sounds) { onVibration(!vibrationOn) }
        }
        Spacer(Modifier.height((20 * f).dp))
        Footer(f)
        Spacer(Modifier.height((8 * f).dp))
    }
}

@Composable
private fun ServiceSwitch(
    active: Boolean,
    locked: Boolean,
    resumesAt: Int,
    f: Float,
    sounds: SoundBank,
    onToggle: () -> Unit
) {
    val colors = DnTheme.colors
    val scope = rememberCoroutineScope()
    var heard by remember { mutableStateOf(active) }
    LaunchedEffect(active) {
        if (active != heard) {
            sounds.play(if (active) R.raw.switch_on else R.raw.switch_off)
            heard = active
        }
    }
    val shake = remember { Animatable(0f) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.95f else 1f, tween(140), label = "switchPress")
    val on by animateFloatAsState(if (active) 1f else 0f, tween(260), label = "switchOn")
    val width = (112 * f).dp
    val height = width / SWITCH_ASPECT

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width, height)
                .graphicsLayer {
                    scaleX = press
                    scaleY = press
                    rotationZ = shake.value
                }
                .drawBehind {
                    val glow = Brush.radialGradient(
                        0f to colors.ember.copy(alpha = 0.34f * on),
                        1f to colors.ember.copy(alpha = 0f),
                        center = Offset(size.width / 2f, size.height * 0.62f),
                        radius = size.width * 0.75f
                    )
                    drawCircle(glow, size.width * 0.75f, Offset(size.width / 2f, size.height * 0.62f))
                }
                .clickable(interactionSource = interaction, indication = null) {
                    if (locked) {
                        sounds.play(R.raw.switch_locked, volume = 0.9f)
                        scope.launch {
                            shake.snapTo(0f)
                            shake.animateTo(0f, keyframes {
                                durationMillis = 420
                                3f at 60
                                -3f at 140
                                2f at 230
                                -1f at 320
                            })
                        }
                    } else {
                        onToggle()
                    }
                }
                .semantics {
                    contentDescription = "Servicio de bloqueo"
                    stateDescription = if (active) "Activo" else "En pausa"
                }
        ) {
            Image(
                painter = painterResource(R.drawable.settings_toggle_off),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 1f - on }
            )
            Image(
                painter = painterResource(R.drawable.settings_toggle_on),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = on }
            )
        }
        Spacer(Modifier.width((18 * f).dp))
        Column(Modifier.weight(1f)) {
            Text(
                "El bloqueo está",
                style = DnType.lead.copy(fontSize = (26 * f).sp, lineHeight = (30 * f).sp),
                color = colors.textSoft
            )
            AnimatedContent(
                targetState = active,
                transitionSpec = { fadeIn(tween(500, delayMillis = 120)) togetherWith fadeOut(tween(200)) },
                label = "serviceState"
            ) { isActive ->
                Text(
                    if (isActive) "activo" else "en pausa",
                    style = DnType.countdown.copy(
                        fontStyle = FontStyle.Italic,
                        fontSize = (50 * f).sp,
                        lineHeight = (56 * f).sp
                    ),
                    color = if (isActive) colors.ember else colors.clay,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height((6 * f).dp))
            Spacer(
                Modifier
                    .fillMaxWidth(0.9f)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(colors.ember.copy(alpha = 0f), colors.ember.copy(alpha = 0.7f), colors.ember.copy(alpha = 0f))
                        )
                    )
            )
            AnimatedContent(
                targetState = locked,
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(200)) },
                label = "serviceNote"
            ) { isLocked ->
                if (isLocked) {
                    Text(
                        "se puede pausar a partir de las ${TimeText.full(resumesAt).lowercase()}",
                        style = DnType.noteSmall.copy(fontSize = (16 * f).sp, lineHeight = (20 * f).sp),
                        color = colors.clayDim,
                        maxLines = 2,
                        modifier = Modifier.padding(top = (8 * f).dp)
                    )
                } else {
                    Spacer(Modifier.height(0.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, f: Float) {
    val colors = DnTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Hairline(toRight = false, modifier = Modifier.weight(1f))
        Text(
            text,
            style = DnType.note.copy(fontSize = (21 * f).sp, lineHeight = (26 * f).sp),
            color = colors.textSoft,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        Hairline(toRight = true, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PushButton(label: String, on: Boolean, f: Float, sounds: SoundBank, onClick: () -> Unit) {
    val colors = DnTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.93f else 1f, tween(120), label = "buttonPress")
    val lit by animateFloatAsState(if (on) 1f else 0f, tween(240), label = "buttonLit")
    val size = (76 * f).dp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = press
                    scaleY = press
                }
                .drawBehind {
                    val r = this.size.width * 0.85f
                    drawCircle(
                        Brush.radialGradient(
                            0f to colors.ember.copy(alpha = 0.3f * lit),
                            1f to colors.ember.copy(alpha = 0f),
                            center = center,
                            radius = r
                        ),
                        r
                    )
                }
                .clickable(interactionSource = interaction, indication = null) {
                    sounds.play(R.raw.button_press, volume = 0.85f, rate = if (on) 0.88f else 1.08f)
                    onClick()
                }
                .semantics {
                    contentDescription = label
                    stateDescription = if (on) "Encendido" else "Apagado"
                }
        ) {
            Image(
                painter = painterResource(R.drawable.settings_button_off),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 1f - lit }
            )
            Image(
                painter = painterResource(R.drawable.settings_button_on),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = lit }
            )
        }
        Spacer(Modifier.height((10 * f).dp))
        Text(
            label,
            style = DnType.note.copy(fontSize = (19 * f).sp, lineHeight = (22 * f).sp),
            color = lerp(colors.textMute, colors.textSoft, lit)
        )
    }
}

@Composable
private fun Footer(f: Float) {
    val context = LocalContext.current
    val colors = DnTheme.colors
    val version = remember { UpdateRepository.installedVersion(context) }
    val open: (String) -> Unit = { url ->
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(colors.ember.copy(alpha = 0f), colors.ember.copy(alpha = 0.4f), colors.ember.copy(alpha = 0f))
                    )
                )
        )
        Spacer(Modifier.height((10 * f).dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "hecha de noche por",
                style = DnType.note.copy(fontSize = (20 * f).sp, lineHeight = (24 * f).sp),
                color = colors.textSoft
            )
            Text(
                "Darmixista",
                style = DnType.note.copy(fontSize = (20 * f).sp, lineHeight = (24 * f).sp),
                color = colors.ember,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClickLabel = "Abrir darmixista.dev") { open(AUTHOR_PAGE) }
                    .padding(horizontal = 5.dp, vertical = 4.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "versión $version",
                style = DnType.noteSmall.copy(fontSize = (16 * f).sp, lineHeight = (20 * f).sp),
                color = colors.textMute
            )
            Text(
                "·",
                style = DnType.noteSmall.copy(fontSize = (16 * f).sp, lineHeight = (20 * f).sp),
                color = colors.textMute,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClickLabel = "Abrir la página oficial") { open(OFFICIAL_PAGE) }
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Conócela",
                    style = DnType.noteSmall.copy(fontSize = (16 * f).sp, lineHeight = (20 * f).sp),
                    color = colors.ember.copy(alpha = 0.85f)
                )
                Spacer(Modifier.width(4.dp))
                Glyph(DnGlyph.OPEN, colors.ember.copy(alpha = 0.85f), size = 12.dp, strokeWidth = 2f)
            }
        }
    }
}

@Composable
private fun Hairline(toRight: Boolean, modifier: Modifier = Modifier) {
    val colors = DnTheme.colors
    Spacer(
        modifier
            .height(1.dp)
            .drawWithCache {
                val brush = Brush.horizontalGradient(
                    if (toRight) 0f to colors.ember.copy(alpha = 0.45f) else 0f to colors.ember.copy(alpha = 0f),
                    if (toRight) 1f to colors.ember.copy(alpha = 0f) else 1f to colors.ember.copy(alpha = 0.45f)
                )
                onDrawBehind { drawRect(brush) }
            }
    )
}

@Composable
internal fun PauseDialog(startMinutes: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = DnTheme.colors
    val header = ImageBitmap.imageResource(R.drawable.settings_window_paused)
    val shape = RoundedCornerShape(30.dp)
    val body = buildAnnotatedString {
        append("Esta noche tu teléfono no se bloqueará a las ")
        withStyle(SpanStyle(color = colors.ember)) { append(TimeText.full(startMinutes).lowercase() + ".") }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth(0.88f)
                .widthIn(max = 400.dp)
                .clip(shape)
                .background(PauseCard)
                .border(1.dp, colors.ember.copy(alpha = 0.3f), shape)
        ) {
            val headerHeight = maxWidth * (PAUSE_HEADER_H / PAUSE_HEADER_W)
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .drawBehind {
                        drawImage(
                            header,
                            srcOffset = IntOffset(PAUSE_HEADER_X, PAUSE_HEADER_Y),
                            srcSize = IntSize(PAUSE_HEADER_W.toInt(), PAUSE_HEADER_H.toInt()),
                            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
                        )
                        drawRect(Brush.verticalGradient(0.4f to PauseCard.copy(alpha = 0f), 1f to PauseCard))
                    }
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = headerHeight - 30.dp, start = 24.dp, end = 24.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "¿Pausar el bloqueo?",
                    style = TextStyle(fontFamily = InstrumentSerif, fontSize = 31.sp, lineHeight = 36.sp),
                    color = colors.text,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    body,
                    style = DnType.note.copy(fontSize = 19.sp, lineHeight = 24.sp),
                    color = colors.textSoft,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Reactívalo con la palanca cuando quieras.",
                    style = DnType.noteSmall.copy(fontSize = 15.sp, lineHeight = 19.sp),
                    color = colors.textMute,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(22.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(PauseButtonLight, PauseButtonDeep)))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Mantener activo",
                        style = TextStyle(fontFamily = InstrumentSerif, fontSize = 22.sp, lineHeight = 26.sp),
                        color = colors.ink
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Sí, pausar",
                    style = DnType.note.copy(fontSize = 19.sp, lineHeight = 24.sp),
                    color = colors.clay,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onConfirm)
                        .drawBehind {
                            val y = size.height - 7.dp.toPx()
                            drawLine(
                                colors.clay.copy(alpha = 0.45f),
                                Offset(10.dp.toPx(), y),
                                Offset(size.width - 10.dp.toPx(), y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                )
            }
        }
    }
}
