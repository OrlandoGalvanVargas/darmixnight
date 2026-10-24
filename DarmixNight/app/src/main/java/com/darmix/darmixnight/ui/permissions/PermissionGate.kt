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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.darmix.darmixnight.PermissionStatus
import com.darmix.darmixnight.PermissionUtils
import com.darmix.darmixnight.RestrictionState
import com.darmix.darmixnight.SetupPrefs
import com.darmix.darmixnight.ui.components.ConstellationProgress
import com.darmix.darmixnight.ui.components.Crescent
import com.darmix.darmixnight.ui.components.DnGlyph
import com.darmix.darmixnight.ui.components.DnPrimaryButton
import com.darmix.darmixnight.ui.components.DnSecondaryButton
import com.darmix.darmixnight.ui.components.Glyph
import com.darmix.darmixnight.ui.components.MoonMark
import com.darmix.darmixnight.ui.components.SkyBackdrop
import com.darmix.darmixnight.ui.components.ThemeToggleButton
import com.darmix.darmixnight.ui.theme.DnTheme
import com.darmix.darmixnight.ui.theme.DnType

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

    SkyBackdrop(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MoonMark()
                Spacer(Modifier.width(10.dp))
                Text(
                    "DarmixNight",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.text,
                    modifier = Modifier.weight(1f)
                )
                ThemeToggleButton(onClick = onToggleTheme)
            }

            Spacer(Modifier.height(28.dp))

            AnimatedContent(
                targetState = done,
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(250)) },
                label = "gateHero"
            ) { complete ->
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = when {
                            complete -> "LISTO"
                            revoked -> "PERMISO DESACTIVADO"
                            else -> "PREPARACIÓN"
                        },
                        style = DnType.overline,
                        color = when {
                            complete -> colors.success
                            revoked -> colors.warn
                            else -> colors.accent
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = when {
                            complete -> "Todo listo para la noche"
                            revoked -> "Algo se apagó"
                            else -> "Antes de que caiga la noche"
                        },
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.text,
                        modifier = Modifier.semantics { heading() }
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = when {
                            complete -> "DarmixNight ya puede cuidar tu horario de descanso."
                            revoked -> "Un permiso que DarmixNight necesita se desactivó. Suele pasar si la app se detuvo por completo (desde Ajustes o con «Cerrar todo»). Vuelve a activarlo para que el bloqueo funcione."
                            else -> "Concede estos permisos una sola vez. DarmixNight los usa únicamente para proteger tu horario de descanso."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textDim
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            ConstellationProgress(lit = lit)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "$grantedCount de ${required.size} listos",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (done) colors.success else colors.textDim
                )
                Text(
                    if (done) "Abriendo…" else "Se revisa al volver de Ajustes",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textDim.copy(alpha = 0.8f)
                )
            }

            Spacer(Modifier.height(22.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                required.forEachIndexed { index, card ->
                    StaggeredItem(enter.value, index) {
                        PermissionCard(
                            card = card,
                            expanded = expanded == card.item,
                            expandableWhenGranted = card.item == GateItem.XIAOMI,
                            onToggle = { toggle(card.item) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(26.dp))
            PrivacyNote()
            Spacer(Modifier.height(28.dp))
        }
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

@Composable
private fun PermissionCard(
    card: GateCard,
    expanded: Boolean,
    onToggle: () -> Unit,
    expandableWhenGranted: Boolean = false
) {
    val colors = DnTheme.colors
    val open = expanded && (!card.granted || expandableWhenGranted)
    val borderColor = when {
        card.item == GateItem.RESTRICTED -> colors.warn.copy(alpha = 0.45f)
        open -> colors.accent.copy(alpha = 0.40f)
        else -> colors.outline
    }
    val chevron by animateFloatAsState(if (open) 180f else 0f, label = "chevron")

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (open) colors.card else colors.card.copy(alpha = if (colors.isDark) 0.72f else 0.85f),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Surface(
                onClick = onToggle,
                enabled = !card.granted || expandableWhenGranted,
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { role = Role.Button }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlyphTile(card)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(card.title, style = MaterialTheme.typography.titleMedium, color = colors.text)
                        Spacer(Modifier.height(2.dp))
                        StatusLabel(card)
                    }
                    Spacer(Modifier.width(8.dp))
                    if (card.granted && !expandableWhenGranted) {
                        Box(
                            Modifier
                                .size(28.dp)
                                .background(colors.success.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Glyph(DnGlyph.CHECK, colors.success, size = 18.dp, strokeWidth = 2.4f)
                        }
                    } else {
                        Glyph(
                            DnGlyph.CHEVRON,
                            colors.textDim,
                            size = 22.dp,
                            strokeWidth = 2f,
                            modifier = Modifier.graphicsLayer { rotationZ = chevron }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = open,
                enter = expandVertically(tween(320, easing = FastOutSlowInEasing)) + fadeIn(tween(260, 80)),
                exit = shrinkVertically(tween(260, easing = FastOutSlowInEasing)) + fadeOut(tween(160))
            ) {
                CardDetails(card)
            }
        }
    }
}

@Composable
private fun StatusLabel(card: GateCard) {
    val colors = DnTheme.colors
    val (text, color) = when {
        card.granted && card.item == GateItem.XIAOMI -> "Hecho" to colors.success
        card.granted -> "Concedido" to colors.success
        card.item == GateItem.RESTRICTED -> card.pendingLabel to colors.warn
        else -> card.pendingLabel to colors.textDim
    }
    Text(text, style = MaterialTheme.typography.labelMedium, color = color)
}

@Composable
private fun GlyphTile(card: GateCard) {
    val colors = DnTheme.colors
    val tint = when {
        card.granted -> colors.success
        card.item == GateItem.RESTRICTED -> colors.warn
        else -> colors.accent
    }
    Box(
        modifier = Modifier
            .size(46.dp)
            .background(tint.copy(alpha = if (colors.isDark) 0.12f else 0.10f), RoundedCornerShape(15.dp)),
        contentAlignment = Alignment.Center
    ) {
        Glyph(card.glyph, tint, size = 24.dp)
    }
}

@Composable
private fun CardDetails(card: GateCard) {
    val colors = DnTheme.colors
    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        Text(card.reason, style = MaterialTheme.typography.bodyMedium, color = colors.textDim)
        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.raised.copy(alpha = if (colors.isDark) 0.7f else 1f), RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            card.steps.forEachIndexed { i, step ->
                StepRow(number = i + 1, text = step)
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
private fun StepRow(
    number: Int,
    text: String,
    active: Boolean = false,
    past: Boolean = false,
    tint: Color = DnTheme.colors.accent
) {
    val colors = DnTheme.colors
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    when {
                        active -> tint
                        past -> colors.success.copy(alpha = 0.18f)
                        else -> tint.copy(alpha = 0.14f)
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (past) {
                Glyph(DnGlyph.CHECK, colors.success, size = 15.dp, strokeWidth = 2.6f)
            } else {
                Text(
                    "$number",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) colors.onAccent else tint,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (active) colors.text else colors.text.copy(alpha = if (past) 0.55f else 0.85f),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun PrivacyNote() {
    val colors = DnTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.raised.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Glyph(DnGlyph.SHIELD, colors.accentAlt, size = 20.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            "DarmixNight solo se conecta a internet para buscar actualizaciones en GitHub. Tu horario y tu uso nunca salen del teléfono.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textDim
        )
    }
}
