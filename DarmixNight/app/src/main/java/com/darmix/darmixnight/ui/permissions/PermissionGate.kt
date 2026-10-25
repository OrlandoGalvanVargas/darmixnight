package com.darmix.darmixnight.ui.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.darmix.darmixnight.PermissionStatus
import com.darmix.darmixnight.PermissionUtils
import com.darmix.darmixnight.RestrictionState
import com.darmix.darmixnight.SetupPrefs
import com.darmix.darmixnight.ui.components.DnGlyph
import com.darmix.darmixnight.ui.components.DnPrimaryButton
import com.darmix.darmixnight.ui.components.DnSecondaryButton
import com.darmix.darmixnight.ui.components.Glyph
import com.darmix.darmixnight.ui.components.SkyBackdrop
import com.darmix.darmixnight.ui.components.rememberSceneClock
import com.darmix.darmixnight.ui.main.MainHeader
import com.darmix.darmixnight.ui.theme.DnTheme
import com.darmix.darmixnight.ui.theme.DnType
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

private val StarGold = Color(0xFFE8C796)
private val MoonLight = Color(0xFFFFF4D6)
private val MoonDeep = Color(0xFFD9AE6A)
private val MoonCrater = Color(0xFFC99A55)
private const val CELEBRATION_MS = 2500

private enum class GateItem {
    RESTRICTED, ACCESSIBILITY, OVERLAY, NOTIFICATIONS, BATTERY, EXACT_ALARM, XIAOMI
}

private class GateAction(
    val label: String,
    val glyph: DnGlyph? = DnGlyph.OPEN,
    val primary: Boolean = true,
    val run: () -> Unit
)

private class GateCard(
    val item: GateItem,
    val glyph: DnGlyph,
    val title: String,
    val reason: String,
    val steps: List<String>,
    val actions: List<GateAction>,
    val granted: Boolean,
    val pendingLabel: String = "Pendiente"
)

@Composable
fun PermissionGate(
    status: PermissionStatus,
    revoked: Boolean,
    onRefresh: () -> Unit,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val colors = DnTheme.colors

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

    val required = buildList {
        if (status.restrictionApplies) {
            add(
                GateCard(
                    item = GateItem.RESTRICTED,
                    glyph = DnGlyph.SHIELD,
                    title = "Permitir ajustes restringidos",
                    reason = "Android protege algunos permisos en las apps instaladas desde un APK. Autoriza a DarmixNight una sola vez para poder activar los siguientes.",
                    steps = listOf(
                        "Toca «Abrir info de la app».",
                        "Abre el menú ⋮ de arriba a la derecha, elige «Permitir ajustes restringidos» y confirma con tu PIN, patrón o huella.",
                        "Regresa a DarmixNight para continuar con el servicio de accesibilidad."
                    ),
                    actions = buildList {
                        add(
                            GateAction("Abrir info de la app") {
                                PermissionUtils.open(context, PermissionUtils.appDetailsSettingsIntent(context))
                            }
                        )
                        if (status.restriction == RestrictionState.UNKNOWN) {
                            add(
                                GateAction("Ya lo permití", glyph = DnGlyph.CHECK, primary = false) {
                                    SetupPrefs.setRestrictionAcknowledged(context, true)
                                    onRefresh()
                                }
                            )
                        }
                    },
                    granted = status.restrictionCleared,
                    pendingLabel = "Primero esto"
                )
            )
        }
        add(
            GateCard(
                item = GateItem.ACCESSIBILITY,
                glyph = DnGlyph.ACCESSIBILITY,
                title = "Servicio de accesibilidad",
                reason = "Detecta qué app se abre durante tu horario de descanso para cubrirla con la pantalla de bloqueo. No lee lo que escribes.",
                steps = listOf(
                    "Toca «Abrir accesibilidad».",
                    "Si no se abre directo, entra a «Apps descargadas» o «Servicios instalados» y elige DarmixNight.",
                    "Activa el interruptor y confirma con «Permitir»."
                ),
                actions = listOf(
                    GateAction("Abrir accesibilidad") {
                        PermissionUtils.open(
                            context,
                            PermissionUtils.accessibilityServiceIntent(context),
                            PermissionUtils.accessibilitySettingsIntent()
                        )
                    }
                ),
                granted = status.accessibilityEnabled
            )
        )
        add(
            GateCard(
                item = GateItem.OVERLAY,
                glyph = DnGlyph.OVERLAY,
                title = "Mostrar sobre otras apps",
                reason = "Permite dibujar la pantalla de bloqueo encima de cualquier app cuando llega tu hora.",
                steps = listOf(
                    "Toca «Abrir ajustes».",
                    "Si aparece una lista, busca DarmixNight y entra.",
                    "Activa «Permitir mostrar sobre otras apps»."
                ),
                actions = listOf(
                    GateAction("Abrir ajustes") {
                        PermissionUtils.open(context, PermissionUtils.overlaySettingsIntent(context))
                    }
                ),
                granted = status.overlayGranted
            )
        )
        add(
            GateCard(
                item = GateItem.NOTIFICATIONS,
                glyph = DnGlyph.BELL,
                title = "Notificaciones",
                reason = "Muestra el aviso permanente que mantiene a DarmixNight activo toda la noche.",
                steps = listOf(
                    "Toca «Permitir notificaciones» y elige «Permitir».",
                    "Si antes lo rechazaste, se abrirán los ajustes: activa las notificaciones de DarmixNight."
                ),
                actions = listOf(
                    GateAction("Permitir notificaciones", glyph = null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            PermissionUtils.open(context, PermissionUtils.notificationSettingsIntent(context))
                        }
                    }
                ),
                granted = status.notificationsGranted
            )
        )
        add(
            GateCard(
                item = GateItem.BATTERY,
                glyph = DnGlyph.BATTERY,
                title = "Sin restricciones de batería",
                reason = "Evita que Android detenga DarmixNight de madrugada para ahorrar energía.",
                steps = listOf(
                    "Toca «Quitar restricción».",
                    "En el aviso del sistema elige «Permitir».",
                    "Si se abre una lista, busca DarmixNight y marca «No optimizar» o «Sin restricciones»."
                ),
                actions = listOf(
                    GateAction("Quitar restricción") {
                        PermissionUtils.open(
                            context,
                            PermissionUtils.batteryOptimizationIntent(context),
                            PermissionUtils.batteryOptimizationListIntent()
                        )
                    }
                ),
                granted = status.batteryExempted
            )
        )
        add(
            GateCard(
                item = GateItem.EXACT_ALARM,
                glyph = DnGlyph.ALARM,
                title = "Alarmas exactas",
                reason = "Hace que el bloqueo empiece y termine en el minuto exacto que elegiste.",
                steps = listOf(
                    "Toca «Abrir ajustes».",
                    "Activa «Permitir alarmas y recordatorios»."
                ),
                actions = listOf(
                    GateAction("Abrir ajustes") {
                        PermissionUtils.open(context, PermissionUtils.exactAlarmSettingsIntent(context))
                    }
                ),
                granted = status.exactAlarmsGranted
            )
        )
        if (status.xiaomiRequired) {
            add(
                GateCard(
                    item = GateItem.XIAOMI,
                    glyph = DnGlyph.POWER,
                    title = "Inicio automático",
                    reason = "HyperOS y MIUI cierran apps en segundo plano por su cuenta. Esto evita que el bloqueo falle una noche.",
                    steps = listOf(
                        "Toca «Abrir inicio automático» y activa DarmixNight en la lista.",
                        "Abre Recientes, mantén presionada la tarjeta de DarmixNight y toca el candado. Así «Cerrar todo» no la detiene.",
                        "Vuelve y toca «Ya lo hice». Android no permite comprobarlo automáticamente."
                    ),
                    actions = listOf(
                        GateAction("Abrir inicio automático") {
                            PermissionUtils.open(context, PermissionUtils.xiaomiAutoStartIntent(context))
                        },
                        GateAction(
                            label = if (status.xiaomiDone) "Marcar como pendiente" else "Ya lo hice",
                            glyph = if (status.xiaomiDone) null else DnGlyph.CHECK,
                            primary = false
                        ) {
                            SetupPrefs.setXiaomiAutostartDone(context, !status.xiaomiDone)
                            onRefresh()
                        }
                    ),
                    granted = status.xiaomiDone
                )
            )
        }
    }


    val firstPending = required.firstOrNull { !it.granted }?.item
    var manualExpanded by rememberSaveable { mutableStateOf<GateItem?>(null) }
    var userCollapsed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(status) {
        userCollapsed = false
        val target = manualExpanded
        if (target != null && required.any { it.item == target && it.granted }) manualExpanded = null
    }
    val expanded = if (userCollapsed) null else manualExpanded ?: firstPending
    val toggle: (GateItem) -> Unit = { item ->
        if (expanded == item) {
            userCollapsed = true
            manualExpanded = null
        } else {
            userCollapsed = false
            manualExpanded = item
        }
    }

    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }

    val lit = required.map { it.granted }
    val grantedCount = lit.count { it }
    val done = status.readyForMain
    val warnIndex = if (revoked && !done) required.indexOfFirst { !it.granted } else -1
    val clock = rememberSceneClock()

    val fill = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }
    val entering = remember { Animatable(0f) }
    LaunchedEffect(done) {
        if (done) {
            launch { entering.animateTo(1f, tween(CELEBRATION_MS, easing = LinearEasing)) }
            launch { fill.animateTo(1f, tween(1200, delayMillis = 300, easing = FastOutSlowInEasing)) }
            launch { flash.animateTo(1f, tween(900, delayMillis = 1550, easing = FastOutSlowInEasing)) }
        } else {
            fill.snapTo(0f)
            flash.snapTo(0f)
            entering.snapTo(0f)
        }
    }

    SkyBackdrop(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            MainHeader(onToggleTheme = onToggleTheme)
            MoonConstellation(
                lit = lit,
                warnIndex = warnIndex,
                fill = fill,
                clock = clock,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )
            AnimatedContent(
                targetState = when {
                    done -> 2
                    revoked -> 1
                    else -> 0
                },
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(250)) },
                label = "gateHero"
            ) { mode ->
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when (mode) {
                            2 -> "TODO LISTO"
                            1 -> "ALGO SE APAGÓ"
                            else -> "PREPARACIÓN"
                        },
                        style = DnType.overline.copy(letterSpacing = 2.6.sp),
                        color = when (mode) {
                            2 -> colors.success
                            1 -> colors.warn
                            else -> colors.accent
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = when (mode) {
                            2 -> "Tu noche está lista"
                            1 -> "Una estrella se apagó"
                            else -> "Enciende tu noche"
                        },
                        style = DnType.clockMedium.copy(fontSize = 26.sp, lineHeight = 32.sp),
                        color = colors.text,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() }
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = when (mode) {
                            2 -> "DarmixNight ya cuida tu descanso."
                            1 -> "Un permiso se desactivó. Suele pasar si la app se detuvo por completo (desde Ajustes o con «Cerrar todo»). Vuelve a activarlo para que el bloqueo funcione."
                            else -> "Cada permiso enciende una estrella. Concédelos una sola vez; solo se usan para proteger tu descanso."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textDim,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "$grantedCount de ${required.size} estrellas",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = StarGold,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.07f), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }

            AnimatedVisibility(
                visible = done,
                enter = fadeIn(tween(400, delayMillis = 300)) + expandVertically(tween(400)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(300))
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Entrando a tu noche…", style = MaterialTheme.typography.labelLarge, color = colors.textDim)
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .width(140.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .drawBehind {
                                drawRect(StarGold, size = size.copy(width = size.width * entering.value))
                            }
                    )
                }
            }

            AnimatedVisibility(
                visible = !done,
                enter = fadeIn(tween(300)) + expandVertically(tween(320)),
                exit = fadeOut(tween(250)) + shrinkVertically(tween(450, easing = FastOutSlowInEasing))
            ) {
                Column {
                    Spacer(Modifier.height(24.dp))
                    required.forEachIndexed { index, card ->
                        StaggeredItem(enter.value, index) {
                            TimelineRow(
                                card = card,
                                first = index == 0,
                                last = index == required.lastIndex,
                                current = card.item == firstPending,
                                warn = index == warnIndex,
                                expanded = expanded == card.item,
                                clock = clock,
                                onToggle = { toggle(card.item) }
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    PrivacyNote()
                }
            }
            Spacer(Modifier.height(28.dp))
        }

        Spacer(
            Modifier
                .matchParentSize()
                .drawBehind {
                    val f = flash.value
                    if (f > 0f) {
                        val origin = Offset(size.width / 2f, size.height * 0.27f)
                        val reach = hypot(size.width, size.height) * 1.1f * f
                        drawCircle(
                            Brush.radialGradient(
                                0f to MoonLight.copy(alpha = 0.95f * f),
                                0.6f to StarGold.copy(alpha = 0.8f * f),
                                1f to StarGold.copy(alpha = 0f),
                                center = origin,
                                radius = reach.coerceAtLeast(1f)
                            ),
                            radius = reach.coerceAtLeast(1f),
                            center = origin
                        )
                    }
                }
        )
    }
}

@Composable
private fun StaggeredItem(progress: Float, index: Int, content: @Composable () -> Unit) {
    Box(
        Modifier.graphicsLayer {
            val start = 0.12f + index * 0.08f
            val t = ((progress - start) / 0.45f).coerceIn(0f, 1f)
            alpha = t
            translationY = (1f - t) * 26.dp.toPx()
        }
    ) { content() }
}

private class CrescentGeometry(
    val points: List<Offset>,
    val outer: Path,
    val innerArc: Path,
    val shape: Path
)

private const val TWO_PI = 6.2831855f

private fun normAngle(x: Float): Float = ((x % TWO_PI) + TWO_PI) % TWO_PI

private fun angleGap(a: Float, b: Float): Float {
    val d = normAngle(a - b)
    return minOf(d, TWO_PI - d)
}

private fun sweepToward(start: Float, end: Float, target: Float): Float {
    val forward = normAngle(end - start)
    val backward = forward - TWO_PI
    return if (angleGap(start + forward / 2f, target) <= angleGap(start + backward / 2f, target)) forward else backward
}

private fun crescentGeometry(cx: Float, cy: Float, r: Float, count: Int): CrescentGeometry {
    val bx = cx + 0.42f * r
    val by = cy - 0.36f * r
    val br = 0.86f * r
    val d = hypot(bx - cx, by - cy)
    val a = (r * r - br * br + d * d) / (2f * d)
    val h = sqrt((r * r - a * a).coerceAtLeast(0f))
    val ux = (bx - cx) / d
    val uy = (by - cy) / d
    val px = cx + a * ux
    val py = cy + a * uy
    val t1 = Offset(px - h * uy, py + h * ux)
    val t2 = Offset(px + h * uy, py - h * ux)

    val awayFromBite = atan2(cy - by, cx - bx)
    val s = atan2(t1.y - cy, t1.x - cx)
    val sweep = sweepToward(s, atan2(t2.y - cy, t2.x - cx), awayFromBite)
    val points = List(count) { i ->
        val ang = if (count == 1) s + sweep / 2f else s + sweep * i / (count - 1)
        Offset(cx + r * cos(ang), cy + r * sin(ang))
    }

    val outerCircle = Path().apply { addOval(Rect(Offset(cx, cy), r)) }
    val bite = Path().apply { addOval(Rect(Offset(bx, by), br)) }
    val shape = Path().apply { op(outerCircle, bite, PathOperation.Difference) }

    val towardMoon = atan2(cy - by, cx - bx)
    val sb = atan2(t1.y - by, t1.x - bx)
    val innerSweep = sweepToward(sb, atan2(t2.y - by, t2.x - bx), towardMoon)
    val innerArc = Path().apply {
        arcTo(
            Rect(Offset(bx, by), br),
            Math.toDegrees(sb.toDouble()).toFloat(),
            Math.toDegrees(innerSweep.toDouble()).toFloat(),
            true
        )
    }
    return CrescentGeometry(points, outerCircle, innerArc, shape)
}

@Composable
private fun MoonConstellation(
    lit: List<Boolean>,
    warnIndex: Int,
    fill: Animatable<Float, *>,
    clock: State<Float>,
    modifier: Modifier = Modifier
) {
    val count = lit.size
    val glows = remember(count) { List(count) { i -> Animatable(if (lit.getOrElse(i) { false }) 1f else 0f) } }
    LaunchedEffect(lit) {
        glows.forEachIndexed { i, glow ->
            launch {
                glow.animateTo(if (lit.getOrElse(i) { false }) 1f else 0f, tween(700, delayMillis = i * 90, easing = FastOutSlowInEasing))
            }
        }
    }
    val warnColor = DnTheme.colors.warn
    val backdrop = DnTheme.colors.bg
    Spacer(
        modifier
            .semantics { role = Role.Image }
            .drawWithCache {
                val r = 86.dp.toPx()
                val c = Offset(size.width / 2f, size.height / 2f + 6.dp.toPx())
                val geo = crescentGeometry(c.x, c.y, r, count)
                val halo = Brush.radialGradient(
                    0f to StarGold.copy(alpha = 0.5f),
                    1f to Color.Transparent,
                    center = c,
                    radius = r * 1.9f
                )
                val moonBrush = Brush.linearGradient(
                    listOf(MoonLight, MoonDeep),
                    start = Offset(c.x - r, c.y - r),
                    end = Offset(c.x + r, c.y + r)
                )
                onDrawBehind {
                    drawMoonConstellation(geo, c, r, glows.map { it.value }, warnIndex, fill.value, clock.value, halo, moonBrush, warnColor, backdrop)
                }
            }
    )
}

private fun DrawScope.drawMoonConstellation(
    geo: CrescentGeometry,
    c: Offset,
    r: Float,
    glows: List<Float>,
    warnIndex: Int,
    fill: Float,
    t: Float,
    halo: Brush,
    moonBrush: Brush,
    warnColor: Color,
    backdrop: Color
) {
    val u = density
    val avg = if (glows.isEmpty()) 0f else glows.sum() / glows.size
    drawCircle(halo, r * 1.9f, c, alpha = 0.25f + 0.75f * avg)

    if (fill > 0f) {
        drawPath(geo.shape, moonBrush, alpha = fill)
        listOf(Triple(-0.46f, 0.23f, 0.1f), Triple(-0.23f, 0.6f, 0.07f), Triple(-0.67f, -0.12f, 0.06f)).forEach { (dx, dy, cr) ->
            drawCircle(MoonCrater, cr * r, Offset(c.x + dx * r, c.y + dy * r), alpha = 0.35f * fill)
        }
        for (k in 0 until 2) {
            val p = ((fill * 1.4f) - k * 0.35f).coerceIn(0f, 1f)
            if (p in 0.01f..0.99f) {
                drawCircle(StarGold, r * (1f + 0.9f * p), c, alpha = 0.5f * (1f - p), style = Stroke((3f * (1f - p) + 0.5f) * u))
            }
        }
    }

    drawPath(
        geo.innerArc,
        StarGold,
        alpha = 0.35f + 0.25f * fill,
        style = Stroke(1f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f * u, 5f * u)))
    )

    val pts = geo.points
    for (i in 0 until pts.size - 1) {
        drawLine(
            Color.White.copy(alpha = 0.3f),
            pts[i],
            pts[i + 1],
            strokeWidth = 1f * u,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f * u, 4f * u))
        )
        val on = minOf(glows[i], glows[i + 1])
        if (on > 0f) {
            val end = Offset(pts[i].x + (pts[i + 1].x - pts[i].x) * on, pts[i].y + (pts[i + 1].y - pts[i].y) * on)
            drawLine(StarGold, pts[i], end, strokeWidth = 1.6f * u, cap = StrokeCap.Round, alpha = 0.85f)
        }
    }

    pts.forEachIndexed { i, p ->
        val g = glows[i]
        if (i == warnIndex) {
            val pulse = 0.5f + 0.5f * sin(t * 3f)
            drawCircle(warnColor, (12f + 4f * pulse) * u, p, alpha = 0.18f)
            drawCircle(warnColor, 6f * u, p, style = Stroke(1.6f * u))
        } else {
            if (g < 1f) {
                drawCircle(backdrop, 4.5f * u, p, alpha = 1f - g)
                drawCircle(Color.White, 4.5f * u, p, alpha = 0.55f * (1f - g), style = Stroke(1.4f * u))
            }
            if (g > 0f) {
                val twinkle = 0.85f + 0.15f * sin(t * 2.4f + i * 1.3f)
                drawCircle(StarGold, 15f * u * g, p, alpha = 0.18f * g)
                drawSparkle(p, 9f * u * g * twinkle, StarGold, g)
                drawCircle(Color.White, 2f * u * g, p, alpha = g)
            }
        }
    }
}

private fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color, alpha: Float) {
    if (radius <= 0f) return
    val thin = radius * 0.32f
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
    drawPath(sparkle, color, alpha = alpha)
}

@Composable
private fun TimelineRow(
    card: GateCard,
    first: Boolean,
    last: Boolean,
    current: Boolean,
    warn: Boolean,
    expanded: Boolean,
    clock: State<Float>,
    onToggle: () -> Unit
) {
    val colors = DnTheme.colors
    val tone = if (warn) colors.warn else colors.accent
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Spacer(
            Modifier
                .width(34.dp)
                .fillMaxHeight()
                .drawBehind {
                    val x = size.width / 2f - 4.dp.toPx()
                    val nodeY = 30.dp.toPx()
                    val dash = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 5.dp.toPx()))
                    val top = if (first) nodeY else 0f
                    val bottom = if (last) nodeY else size.height
                    if (bottom > top) {
                        drawLine(Color.White.copy(alpha = 0.16f), Offset(x, top), Offset(x, bottom), 1.4.dp.toPx(), pathEffect = dash)
                    }
                    val node = Offset(x, nodeY)
                    when {
                        card.granted -> {
                            drawCircle(StarGold, 12.dp.toPx(), node, alpha = 0.16f)
                            drawSparkle(node, 8.dp.toPx(), StarGold, 1f)
                        }
                        current || warn -> {
                            val pulse = 0.5f + 0.5f * sin(clock.value * 3f)
                            drawCircle(tone, (11f + 4f * pulse).dp.toPx(), node, alpha = 0.18f)
                            drawCircle(tone, 6.5.dp.toPx(), node, style = Stroke(2.dp.toPx()))
                            drawCircle(tone, 2.4.dp.toPx(), node)
                        }
                        else -> {
                            drawCircle(colors.bg, 4.5.dp.toPx(), node)
                            drawCircle(Color.White.copy(alpha = 0.35f), 4.5.dp.toPx(), node, style = Stroke(1.3.dp.toPx()))
                        }
                    }
                }
        )
        Box(
            Modifier
                .weight(1f)
                .padding(bottom = 10.dp)
        ) {
            PermissionCard(
                card = card,
                expanded = expanded,
                current = current,
                warn = warn,
                expandableWhenGranted = card.item == GateItem.XIAOMI,
                onToggle = onToggle
            )
        }
    }
}

@Composable
private fun PermissionCard(
    card: GateCard,
    expanded: Boolean,
    current: Boolean,
    warn: Boolean,
    onToggle: () -> Unit,
    expandableWhenGranted: Boolean = false
) {
    val colors = DnTheme.colors
    val open = expanded && (!card.granted || expandableWhenGranted)
    val tone = when {
        warn -> colors.warn
        card.item == GateItem.RESTRICTED && !card.granted -> colors.warn
        else -> colors.accent
    }
    val shape = RoundedCornerShape(if (open) 22.dp else 18.dp)
    val faded = !card.granted && !open && !current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (open) colors.card.copy(alpha = if (colors.isDark) 0.92f else 0.97f)
                else Color.White.copy(alpha = if (card.granted) 0.07f else 0.04f),
                shape
            )
            .border(1.dp, if (open) tone.copy(alpha = 0.45f) else colors.text.copy(alpha = 0.08f), shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !card.granted || expandableWhenGranted, onClick = onToggle)
                .semantics { role = Role.Button }
                .padding(horizontal = 14.dp, vertical = if (open) 14.dp else 13.dp)
                .alpha(if (faded) 0.6f else 1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(if (open) 38.dp else 30.dp)
                    .background(
                        (if (card.granted) StarGold else tone).copy(alpha = if (open) 0.14f else 0.0f),
                        RoundedCornerShape(11.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Glyph(
                    card.glyph,
                    when {
                        card.granted -> StarGold
                        open -> tone
                        else -> colors.textDim
                    },
                    size = if (open) 22.dp else 20.dp,
                    strokeWidth = 1.9f
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    card.title,
                    style = (if (open) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall),
                    color = colors.text
                )
                if (open || card.item == GateItem.RESTRICTED && !card.granted) {
                    Text(
                        when {
                            warn -> "Se apagó · actívalo de nuevo"
                            card.item == GateItem.RESTRICTED -> card.pendingLabel
                            else -> "Siguiente estrella"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = tone
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            when {
                card.granted -> Box(
                    Modifier
                        .size(22.dp)
                        .background(colors.success, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Glyph(DnGlyph.CHECK, colors.card, size = 14.dp, strokeWidth = 3f)
                }
                !open -> Text("Pendiente", style = MaterialTheme.typography.labelMedium, color = colors.textDim)
            }
        }

        AnimatedVisibility(
            visible = open,
            enter = expandVertically(tween(320, easing = FastOutSlowInEasing)) + fadeIn(tween(260, 80)),
            exit = shrinkVertically(tween(260, easing = FastOutSlowInEasing)) + fadeOut(tween(160))
        ) {
            CardDetails(card, tone)
        }
    }
}

@Composable
private fun CardDetails(card: GateCard, tone: Color) {
    val colors = DnTheme.colors
    Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
        Text(card.reason, style = MaterialTheme.typography.bodyMedium, color = colors.textDim)
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            card.steps.forEachIndexed { i, step ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier
                            .size(22.dp)
                            .background(Color.White.copy(alpha = 0.08f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${i + 1}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = tone,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        step,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.text.copy(alpha = 0.9f),
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            card.actions.forEach { action ->
                if (action.primary) {
                    DnPrimaryButton(label = action.label, onClick = action.run, glyph = action.glyph)
                } else {
                    DnSecondaryButton(label = action.label, onClick = action.run, glyph = action.glyph)
                }
            }
        }
    }
}

@Composable
private fun PrivacyNote() {
    val colors = DnTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Glyph(DnGlyph.LOCK, colors.textDim, size = 18.dp, strokeWidth = 2f)
        Spacer(Modifier.width(12.dp))
        Text(
            "DarmixNight solo se conecta a internet para buscar actualizaciones en GitHub. Tu horario y tu uso nunca salen del teléfono.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textDim
        )
    }
}
