package com.darmix.darmixnight.ui.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.darmix.darmixnight.PermissionStatus
import com.darmix.darmixnight.PermissionUtils
import com.darmix.darmixnight.R
import com.darmix.darmixnight.RestrictionState
import com.darmix.darmixnight.SetupPrefs
import com.darmix.darmixnight.ui.components.EmberButton
import com.darmix.darmixnight.ui.components.QuietLink
import com.darmix.darmixnight.ui.components.SkyBackdrop
import com.darmix.darmixnight.ui.components.drawGlow
import com.darmix.darmixnight.ui.components.lampFlicker
import com.darmix.darmixnight.ui.components.rememberSceneClock
import com.darmix.darmixnight.ui.components.unitRadial
import com.darmix.darmixnight.ui.main.MainHeader
import com.darmix.darmixnight.ui.theme.DnTheme
import com.darmix.darmixnight.ui.theme.DnType
import com.darmix.darmixnight.ui.theme.InstrumentSerif
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val ROOM_W = 1440f
private const val ROOM_H = 1800f
private const val DESK_Y = 940f
private const val DESK_SHARE = 0.46f

private val FlameLight = Color(0xFFFFC27A)
private val MoonLight = Color(0xFFFFE2B0)

private enum class Step { RESTRICTED, ACCESSIBILITY, OVERLAY, NOTIFICATIONS, BATTERY, EXACT_ALARM, AUTOSTART }

private enum class Shine { NONE, FLAME, MOON }

private class Keepsake(val image: Int, val shine: Shine = Shine.NONE, val glow: Offset = Offset.Zero)

private class StepCard(
    val step: Step,
    val title: String,
    val reason: String,
    val hint: String,
    val action: String,
    val keepsake: Keepsake,
    val granted: Boolean,
    val run: () -> Unit,
    val manual: Pair<String, () -> Unit>? = null
)

private val DoneKeepsake = Keepsake(R.drawable.permission_jar, Shine.MOON, Offset(0.52f, 0.42f))

@Composable
fun PermissionGate(
    status: PermissionStatus,
    revoked: Boolean,
    onRefresh: () -> Unit,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = LocalActivity.current

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        onRefresh()
        val permanentlyDenied = !granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
        if (permanentlyDenied) {
            PermissionUtils.open(context, PermissionUtils.notificationSettingsIntent(context))
        }
    }

    val steps = buildList {
        if (status.restrictionApplies) {
            val tried = status.restrictionTried
            add(
                StepCard(
                    step = Step.RESTRICTED,
                    title = "Ajustes restringidos",
                    reason = "Android bloquea algunos permisos en las apps instaladas desde un APK. Se desbloquea una sola vez.",
                    hint = if (tried) {
                        "Busca DarmixNight en la lista de apps y entra. Toca ⋮ arriba a la derecha, o baja hasta el final, y activa «Permitir ajustes restringidos»."
                    } else {
                        "Primero toca DarmixNight en la lista de accesibilidad y cierra el aviso que aparece."
                    },
                    action = if (tried) "Abrir lista de apps" else "Abrir accesibilidad",
                    keepsake = Keepsake(R.drawable.permission_padlock),
                    granted = status.restrictionCleared,
                    run = {
                        if (tried) {
                            PermissionUtils.open(
                                context,
                                PermissionUtils.appListSettingsIntent(),
                                PermissionUtils.allAppsSettingsIntent()
                            )
                        } else {
                            PermissionUtils.markRestrictionTried(context)
                            PermissionUtils.open(context, PermissionUtils.accessibilitySettingsIntent())
                        }
                    },
                    manual = if (status.restriction == RestrictionState.UNKNOWN) {
                        "Ya lo permití" to {
                            SetupPrefs.setRestrictionAcknowledged(context, true)
                            onRefresh()
                        }
                    } else {
                        null
                    }
                )
            )
        }
        add(
            StepCard(
                step = Step.ACCESSIBILITY,
                title = "Servicio de accesibilidad",
                reason = "Detecta qué app se abre en tu horario de descanso para cubrirla. No lee lo que escribes.",
                hint = "Busca DarmixNight en «Apps descargadas», actívalo y confirma con «Permitir».",
                action = "Abrir accesibilidad",
                keepsake = Keepsake(R.drawable.permission_key),
                granted = status.accessibilityEnabled,
                run = {
                    PermissionUtils.open(
                        context,
                        PermissionUtils.accessibilityServiceIntent(context),
                        PermissionUtils.accessibilitySettingsIntent()
                    )
                }
            )
        )
        add(
            StepCard(
                step = Step.OVERLAY,
                title = "Mostrar sobre otras apps",
                reason = "Cubre cualquier app con la pantalla de bloqueo cuando llega tu hora de dormir.",
                hint = "Activa «Permitir mostrar sobre otras apps» para DarmixNight.",
                action = "Abrir ajustes",
                keepsake = Keepsake(R.drawable.permission_snuffer),
                granted = status.overlayGranted,
                run = { PermissionUtils.open(context, PermissionUtils.overlaySettingsIntent(context)) }
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(
                StepCard(
                    step = Step.NOTIFICATIONS,
                    title = "Notificaciones",
                    reason = "Un aviso discreto que mantiene a DarmixNight despierto toda la noche.",
                    hint = "Elige «Permitir» en el mensaje que aparecerá.",
                    action = "Permitir notificaciones",
                    keepsake = Keepsake(R.drawable.permission_bell),
                    granted = status.notificationsGranted,
                    run = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                )
            )
        }
        add(
            StepCard(
                step = Step.BATTERY,
                title = "Batería sin restricciones",
                reason = "Evita que el ahorro de energía apague DarmixNight de madrugada.",
                hint = "Elige «Permitir». Si aparece una lista, marca DarmixNight como «Sin restricciones».",
                action = "Quitar restricción",
                keepsake = Keepsake(R.drawable.permission_lantern, Shine.FLAME, Offset(0.503f, 0.575f)),
                granted = status.batteryExempted,
                run = {
                    PermissionUtils.open(
                        context,
                        PermissionUtils.batteryOptimizationIntent(context),
                        PermissionUtils.batteryOptimizationListIntent()
                    )
                }
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(
                StepCard(
                    step = Step.EXACT_ALARM,
                    title = "Alarmas exactas",
                    reason = "El bloqueo empieza y termina en el minuto exacto que elegiste.",
                    hint = "Activa «Permitir alarmas y recordatorios».",
                    action = "Abrir ajustes",
                    keepsake = Keepsake(R.drawable.permission_clock),
                    granted = status.exactAlarmsGranted,
                    run = { PermissionUtils.open(context, PermissionUtils.exactAlarmSettingsIntent(context)) }
                )
            )
        }
        if (status.xiaomiRequired) {
            add(
                StepCard(
                    step = Step.AUTOSTART,
                    title = "Inicio automático",
                    reason = "HyperOS y MIUI cierran apps por su cuenta. Así el bloqueo no falla ninguna noche.",
                    hint = "Activa DarmixNight en la lista. Luego, en Recientes, fíjala con el candado.",
                    action = "Abrir inicio automático",
                    keepsake = Keepsake(R.drawable.permission_musicbox),
                    granted = status.xiaomiDone,
                    run = { PermissionUtils.open(context, PermissionUtils.xiaomiAutoStartIntent(context)) },
                    manual = if (status.xiaomiDone) {
                        null
                    } else {
                        "Ya lo hice" to {
                            SetupPrefs.setXiaomiAutostartDone(context, true)
                            onRefresh()
                        }
                    }
                )
            )
        }
    }

    val done = status.readyForMain
    var viewing by rememberSaveable { mutableStateOf<Step?>(null) }
    LaunchedEffect(status) { viewing = null }
    val firstPending = steps.firstOrNull { !it.granted }
    val current = steps.firstOrNull { it.step == viewing } ?: firstPending
    val index = if (current == null) steps.size else steps.indexOf(current)
    val clock = rememberSceneClock()

    SkyBackdrop(modifier = modifier) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .pointerInput(done) {
                    if (done) detectTapGestures { onEnter() }
                }
        ) {
            val unit = max(maxWidth.value / ROOM_W, maxHeight.value * DESK_SHARE / DESK_Y)
            val deskY = (DESK_Y * unit).dp
            val objectHeight = (deskY - 128.dp).coerceIn(120.dp, 270.dp)
            val objectWidth = maxWidth * 0.64f

            RoomLayer(unit = unit, modifier = Modifier.fillMaxSize())

            AnimatedContent(
                targetState = if (done) null else current?.step,
                transitionSpec = {
                    (fadeIn(tween(520, delayMillis = 160)) +
                            slideInVertically(tween(620, easing = FastOutSlowInEasing)) { -it / 14 } +
                            scaleIn(tween(620, easing = FastOutSlowInEasing), initialScale = 0.94f)) togetherWith
                            fadeOut(tween(260))
                },
                modifier = Modifier
                    .offset(y = deskY - objectHeight)
                    .align(Alignment.TopCenter),
                label = "keepsake"
            ) { step ->
                val keepsake = steps.firstOrNull { it.step == step }?.keepsake ?: DoneKeepsake
                KeepsakeView(
                    keepsake = keepsake,
                    clock = clock,
                    modifier = Modifier.size(objectWidth, objectHeight)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MainHeader(modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(deskY - 74.dp))
                AnimatedContent(
                    targetState = if (done) null else current?.step,
                    transitionSpec = {
                        (fadeIn(tween(480, delayMillis = 200)) +
                                slideInVertically(tween(560, easing = FastOutSlowInEasing)) { it / 16 }) togetherWith
                                fadeOut(tween(220))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    label = "stepText"
                ) { step ->
                    val card = steps.firstOrNull { it.step == step }
                    StepBody(
                        card = card,
                        warn = revoked && card != null && !card.granted,
                        onContinue = { viewing = null }
                    )
                }
                Progress(
                    lit = steps.map { it.granted },
                    current = if (done) -1 else index,
                    clock = clock,
                    canGoBack = !done && index > 0,
                    onBack = { if (index > 0) viewing = steps[index - 1].step }
                )
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun RoomLayer(unit: Float, modifier: Modifier = Modifier) {
    val room = ImageBitmap.imageResource(R.drawable.permission_room)
    Spacer(
        modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawBehind {
                val scale = unit * density
                val width = ROOM_W * scale
                val height = ROOM_H * scale
                val left = (size.width - width) / 2f
                drawImage(
                    room,
                    dstOffset = IntOffset(left.roundToInt(), 0),
                    dstSize = IntSize(width.roundToInt(), height.roundToInt())
                )
                val desk = DESK_Y * scale
                drawRect(
                    Brush.verticalGradient(
                        0f to Color.Black,
                        1f to Color.Transparent,
                        startY = desk + 20f * density,
                        endY = desk + 90f * density
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
    )
}

@Composable
private fun KeepsakeView(keepsake: Keepsake, clock: State<Float>, modifier: Modifier = Modifier) {
    val image = ImageBitmap.imageResource(keepsake.image)
    Spacer(
        modifier.drawWithCache {
            val ratio = min(size.width / image.width, size.height / image.height)
            val w = image.width * ratio
            val h = image.height * ratio
            val left = (size.width - w) / 2f
            val top = size.height - h
            val shadow = unitRadial(0f to Color.Black.copy(alpha = 0.55f), 1f to Color.Black.copy(alpha = 0f))
            val flame = unitRadial(0f to FlameLight, 0.4f to FlameLight.copy(alpha = 0.35f), 1f to FlameLight.copy(alpha = 0f))
            val moon = unitRadial(0f to MoonLight, 0.5f to MoonLight.copy(alpha = 0.3f), 1f to MoonLight.copy(alpha = 0f))
            val glow = Offset(left + w * keepsake.glow.x, top + h * keepsake.glow.y)
            onDrawBehind {
                translate(size.width / 2f, size.height - 2.dp.toPx()) {
                    scale(w * 0.42f, 10.dp.toPx(), Offset.Zero) {
                        drawCircle(shadow, 1f, Offset.Zero)
                    }
                }
                drawImage(
                    image,
                    dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                    dstSize = IntSize(w.roundToInt(), h.roundToInt())
                )
                val t = clock.value
                when (keepsake.shine) {
                    Shine.FLAME -> {
                        val flicker = lampFlicker(t * 1.6f)
                        drawGlow(flame, glow, w * 0.55f, w * 0.6f, 0.12f + 0.16f * flicker)
                    }
                    Shine.MOON -> drawGlow(moon, glow, w * 0.5f, w * 0.5f, 0.12f + 0.08f * sin(t * 1.2f))
                    Shine.NONE -> Unit
                }
            }
        }
    )
}

@Composable
private fun StepBody(card: StepCard?, warn: Boolean, onContinue: () -> Unit) {
    val colors = DnTheme.colors
    val f = DnTheme.scale.factor
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (warn) {
            Text(
                "Se desactivó. Vuelve a encenderlo.",
                style = DnType.noteSmall.copy(fontSize = (16 * f).sp, lineHeight = (20 * f).sp),
                color = colors.ember
            )
            Spacer(Modifier.height(4.dp))
        }
        Text(
            card?.title ?: "Todo listo",
            style = TextStyle(fontFamily = InstrumentSerif, fontSize = (36 * f).sp, lineHeight = (40 * f).sp),
            color = colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height((8 * f).dp))
        Text(
            card?.reason ?: "Tu noche ya está protegida. DarmixNight cuidará tu descanso.",
            style = DnType.note.copy(fontSize = (19 * f).sp, lineHeight = (24 * f).sp),
            color = colors.textSoft,
            textAlign = TextAlign.Center,
            maxLines = 3
        )
        if (card != null) {
            Spacer(Modifier.height((16 * f).dp))
            Spacer(
                Modifier
                    .width(64.dp)
                    .height(1.dp)
                    .drawBehind {
                        drawRect(
                            Brush.horizontalGradient(
                                listOf(colors.ember.copy(alpha = 0f), colors.ember.copy(alpha = 0.7f), colors.ember.copy(alpha = 0f))
                            )
                        )
                    }
            )
            Spacer(Modifier.height((16 * f).dp))
            Text(
                if (card.granted) "Este ya está encendido." else card.hint,
                style = TextStyle(fontFamily = InstrumentSerif, fontSize = (18 * f).sp, lineHeight = (23 * f).sp),
                color = colors.clay,
                textAlign = TextAlign.Center,
                maxLines = 3
            )
        }
        Spacer(Modifier.weight(1f))
        when {
            card == null -> Spacer(Modifier.height(54.dp))
            card.granted -> EmberButton("Continuar", onClick = onContinue)
            else -> EmberButton(card.action, onClick = card.run)
        }
        val manual = card?.manual
        if (manual != null && !card.granted) {
            Spacer(Modifier.height(4.dp))
            QuietLink(manual.first, onClick = manual.second)
        } else {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Progress(
    lit: List<Boolean>,
    current: Int,
    clock: State<Float>,
    canGoBack: Boolean,
    onBack: () -> Unit
) {
    val colors = DnTheme.colors
    val back by animateFloatAsState(if (canGoBack) 1f else 0f, tween(300), label = "back")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "‹ Volver",
            style = DnType.noteSmall.copy(fontSize = 16.sp, lineHeight = 20.sp),
            color = colors.clayDim,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .graphicsLayer { alpha = back }
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = canGoBack, onClick = onBack)
                .padding(horizontal = 6.dp, vertical = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics {
                contentDescription = "${lit.count { it }} de ${lit.size} permisos activos"
            }
        ) {
            lit.forEachIndexed { i, on ->
                Ember(on = on, active = i == current, clock = clock, size = 9.dp)
            }
        }
    }
}

@Composable
private fun Ember(on: Boolean, active: Boolean, clock: State<Float>, size: Dp) {
    val colors = DnTheme.colors
    val level by animateFloatAsState(if (on) 1f else 0f, tween(600), label = "ember")
    Spacer(
        Modifier
            .size(size)
            .drawBehind {
                val r = this.size.minDimension / 2f
                val pulse = if (active) 0.5f + 0.5f * sin(clock.value * 3f) else 0f
                if (level > 0f) {
                    drawCircle(colors.ember, r * 2.2f, alpha = 0.18f * level)
                    drawCircle(colors.ember, r, alpha = level)
                }
                if (active) {
                    drawCircle(colors.amber, r * (2.1f + 0.5f * pulse), alpha = 0.14f + 0.1f * pulse)
                    drawCircle(colors.amber, r * 1.45f, style = Stroke(1.4.dp.toPx()))
                    drawCircle(colors.amber, r * 0.8f)
                }
                if (level < 1f && !active) {
                    drawCircle(colors.ash, r - 0.5.dp.toPx(), alpha = 1f - level, style = Stroke(1.2.dp.toPx()))
                }
            }
    )
}
