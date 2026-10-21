package com.darmix.darmixnight

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.darmix.darmixnight.ui.splash.NightIntro
import com.darmix.darmixnight.ui.theme.DarmixNightTheme
import com.darmix.darmixnight.ui.theme.DnTheme
import com.darmix.darmixnight.ui.theme.DnType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private val bgColor: Color @Composable get() = DnTheme.colors.bg
private val cardColor: Color @Composable get() = DnTheme.colors.card
private val raisedColor: Color @Composable get() = DnTheme.colors.raised
private val textColor: Color @Composable get() = DnTheme.colors.text
private val dimColor: Color @Composable get() = DnTheme.colors.textDim
private val accentColor: Color @Composable get() = DnTheme.colors.accent
private val successColor: Color @Composable get() = DnTheme.colors.success
private val warnColor: Color @Composable get() = DnTheme.colors.warn
private val dangerColor: Color @Composable get() = DnTheme.colors.danger
private val outlineColor: Color @Composable get() = DnTheme.colors.outline

class MainActivity : ComponentActivity() {

    private var themeReady by mutableStateOf(false)
    private var storedDark by mutableStateOf<Boolean?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splash.setKeepOnScreenCondition { !themeReady }
        splash.setOnExitAnimationListener { provider ->
            provider.view.animate()
                .alpha(0f)
                .setDuration(220L)
                .withEndAction { provider.remove() }
                .start()
        }

        ContextCompat.startForegroundService(this, Intent(this, KeepAliveService::class.java))

        lifecycleScope.launch {
            ThemePrefs.darkModeFlow(applicationContext).collect {
                storedDark = it
                themeReady = true
            }
        }

        val playIntro = savedInstanceState == null

        setContent {
            val systemDark = isSystemInDarkTheme()
            val darkTheme = storedDark ?: systemDark
            var introVisible by rememberSaveable { mutableStateOf(playIntro) }

            val view = LocalView.current
            val lightBars = !darkTheme && !introVisible
            SideEffect {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = lightBars
                    isAppearanceLightNavigationBars = lightBars
                }
            }

            DarmixNightTheme(darkTheme = darkTheme) {
                Box(Modifier.fillMaxSize()) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = DnTheme.colors.bg
                    ) { innerPadding ->
                        MainScreen(
                            modifier = Modifier.padding(innerPadding),
                            onToggleTheme = {
                                lifecycleScope.launch {
                                    ThemePrefs.setDarkMode(applicationContext, !darkTheme)
                                }
                            }
                        )
                    }
                    if (introVisible) {
                        NightIntro(
                            start = themeReady,
                            onFinished = { introVisible = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onToggleTheme: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var permissionStatus by remember { mutableStateOf(PermissionUtils.checkAll(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionStatus = PermissionUtils.checkAll(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        permissionStatus = PermissionUtils.checkAll(context)
    }

    val startMinutes by remember(context) { BlockPrefs.startMinutesFlow(context) }
        .collectAsStateWithLifecycle(initialValue = BlockPrefs.DEFAULT_START)
    val endMinutes by remember(context) { BlockPrefs.endMinutesFlow(context) }
        .collectAsStateWithLifecycle(initialValue = BlockPrefs.DEFAULT_END)
    val emergencyDurationMinutes by remember(context) { BlockPrefs.emergencyDurationMinutesFlow(context) }
        .collectAsStateWithLifecycle(initialValue = BlockPrefs.DEFAULT_EMERGENCY_DURATION_MINUTES)
    val weeklyEmergencyCount by remember(context) { EmergencyUnlockManager.weeklyUsageCountFlow(context) }
        .collectAsStateWithLifecycle(initialValue = 0)
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            tick = System.currentTimeMillis()
            delay(15_000)
        }
    }
    val withinBlockRange = remember(startMinutes, endMinutes, tick) {
        ScheduleUtils.isCurrentlyWithinBlockRange(startMinutes, endMinutes)
    }

    val permissions = listOf(
        PermissionItem(
            "Servicio de accesibilidad",
            "Permite detectar y cerrar las apps bloqueadas.",
            permissionStatus.accessibilityEnabled
        ) { context.startActivity(PermissionUtils.accessibilitySettingsIntent()) },
        PermissionItem(
            "Mostrar sobre otras apps",
            "Necesario para cubrir la pantalla durante el bloqueo.",
            permissionStatus.overlayGranted
        ) { context.startActivity(PermissionUtils.overlaySettingsIntent(context)) },
        PermissionItem(
            "Sin restricciones de batería",
            "Evita que el sistema apague el servicio de noche.",
            permissionStatus.batteryExempted
        ) { context.startActivity(PermissionUtils.batteryOptimizationIntent(context)) },
        PermissionItem(
            "Notificaciones",
            "Muestra el aviso permanente del servicio activo.",
            permissionStatus.notificationsGranted
        ) { notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS) },
        PermissionItem(
            "Alarmas exactas",
            "Inicia el bloqueo al minuto exacto.",
            permissionStatus.exactAlarmsGranted
        ) { context.startActivity(PermissionUtils.exactAlarmSettingsIntent(context)) }
    )
    val grantedCount = permissions.count { it.granted }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to DnTheme.colors.bgTop,
                    0.45f to bgColor,
                    1f to bgColor
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        Header(
            allGranted = permissionStatus.allGranted,
            blocking = withinBlockRange,
            onToggleTheme = onToggleTheme
        )

        ScheduleHero(
            startMinutes = startMinutes,
            endMinutes = endMinutes,
            blocking = withinBlockRange,
            editable = !withinBlockRange,
            onStartPicked = { scope.launch { BlockPrefs.setStartMinutes(context, it) } },
            onEndPicked = { scope.launch { BlockPrefs.setEndMinutes(context, it) } }
        )

        PermissionsSection(
            permissions = permissions,
            grantedCount = grantedCount,
            onXiaomiClick = { context.startActivity(PermissionUtils.xiaomiAutoStartIntent(context)) }
        )

        EmergencySection(
            weeklyCount = weeklyEmergencyCount,
            durationMinutes = emergencyDurationMinutes,
            editable = !withinBlockRange,
            onChangeDuration = { scope.launch { BlockPrefs.setEmergencyDurationMinutes(context, it) } }
        )

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun Header(
    allGranted: Boolean,
    blocking: Boolean,
    onToggleTheme: () -> Unit
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                Text("Darmix", style = DnType.wordmark, color = textColor)
                Text("Night", style = DnType.wordmark, color = DnTheme.colors.glow)
            }
            ThemeToggleButton(onClick = onToggleTheme)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Tu teléfono se apaga cuando tú deberías dormir.",
            color = dimColor,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(14.dp))

        val (dot, label) = when {
            !allGranted -> warnColor to "Faltan permisos por conceder"
            blocking -> dangerColor to "Bloqueando ahora mismo"
            else -> successColor to "Servicio activo y en guardia"
        }
        StatusPill(color = dot, label = label)
    }
}

@Composable
private fun ThemeToggleButton(onClick: () -> Unit) {
    val goingToLight = DnTheme.colors.isDark
    val iconColor = if (goingToLight) warnColor else accentColor
    val buttonColor = raisedColor

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = buttonColor,
        border = BorderStroke(1.dp, outlineColor),
        modifier = Modifier
            .size(46.dp)
            .semantics {
                contentDescription =
                    if (goingToLight) "Cambiar a tema claro" else "Cambiar a tema oscuro"
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(22.dp)) {
                val r = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                if (goingToLight) {
                    drawCircle(iconColor, radius = r * 0.5f, center = center)
                    repeat(8) { i ->
                        val a = (Math.PI / 4.0) * i
                        val dx = cos(a).toFloat()
                        val dy = sin(a).toFloat()
                        drawLine(
                            color = iconColor,
                            start = Offset(center.x + dx * r * 0.72f, center.y + dy * r * 0.72f),
                            end = Offset(center.x + dx * r, center.y + dy * r),
                            strokeWidth = r * 0.22f,
                            cap = StrokeCap.Round
                        )
                    }
                } else {
                    drawCircle(iconColor, radius = r, center = center)
                    drawCircle(
                        color = buttonColor,
                        radius = r * 0.86f,
                        center = Offset(center.x + r * 0.45f, center.y - r * 0.32f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPill(color: Color, label: String) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), CircleShape)
            .border(1.dp, color.copy(alpha = 0.35f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(9.dp))
        Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ScheduleHero(
    startMinutes: Int,
    endMinutes: Int,
    blocking: Boolean,
    editable: Boolean,
    onStartPicked: (Int) -> Unit,
    onEndPicked: (Int) -> Unit
) {
    val duration = ((endMinutes - startMinutes) + 1440) % 1440

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = cardColor,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (blocking) dangerColor.copy(alpha = 0.45f) else outlineColor
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Horario de bloqueo",
                    color = textColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatDuration(duration),
                    color = accentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                TimeTile(
                    caption = "Empieza",
                    minutes = startMinutes,
                    enabled = editable,
                    modifier = Modifier.weight(1f),
                    onTimePicked = onStartPicked
                )
                Box(
                    modifier = Modifier.width(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("→", color = dimColor, fontSize = 20.sp)
                }
                TimeTile(
                    caption = "Termina",
                    minutes = endMinutes,
                    enabled = editable,
                    modifier = Modifier.weight(1f),
                    onTimePicked = onEndPicked
                )
            }

            if (blocking) {
                Spacer(Modifier.height(16.dp))
                NoticeBar(
                    color = dangerColor,
                    text = "El bloqueo está activo. Podrás editar el horario a las " +
                            "${formatTime12(endMinutes)}."
                )
            } else {
                Spacer(Modifier.height(14.dp))
                Text(
                    "Toca una hora para cambiarla.",
                    color = dimColor,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun TimeTile(
    caption: String,
    minutes: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onTimePicked: (Int) -> Unit
) {
    val context = LocalContext.current
    val isDark = DnTheme.colors.isDark
    val hour12 = hourLabel(minutes)
    val suffix = suffixLabel(minutes)

    Surface(
        onClick = {
            val dialogTheme =
                if (isDark) R.style.Theme_DarmixNight_TimePicker_Dark
                else R.style.Theme_DarmixNight_TimePicker_Light
            TimePickerDialog(
                context,
                dialogTheme,
                { _, selectedHour, selectedMinute ->
                    onTimePicked(selectedHour * 60 + selectedMinute)
                },
                minutes / 60,
                minutes % 60,
                false
            ).show()
        },
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        color = raisedColor,
        modifier = modifier.alpha(if (enabled) 1f else 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(caption, color = dimColor, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    hour12,
                    color = textColor,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    suffix,
                    color = accentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

private data class PermissionItem(
    val label: String,
    val detail: String,
    val granted: Boolean,
    val onGrantClick: () -> Unit
)

@Composable
private fun PermissionsSection(
    permissions: List<PermissionItem>,
    grantedCount: Int,
    onXiaomiClick: () -> Unit
) {
    val total = permissions.size
    val complete = grantedCount == total

    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Permisos",
                color = textColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "$grantedCount de $total",
                color = if (complete) successColor else warnColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { grantedCount / total.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = if (complete) successColor else warnColor,
            trackColor = dimColor.copy(alpha = 0.20f),
            gapSize = 0.dp,
            drawStopIndicator = {}
        )

        Spacer(Modifier.height(6.dp))
        Text(
            text = if (complete) {
                "Todo listo. El bloqueo se aplicará a la hora programada."
            } else {
                "Sin estos permisos el bloqueo no se aplicará."
            },
            color = if (complete) dimColor else warnColor,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(4.dp))

        permissions.forEachIndexed { index, item ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 2.dp),
                    color = outlineColor
                )
            }
            PermissionRow(item)
        }

        Spacer(Modifier.height(8.dp))
        Surface(
            onClick = onXiaomiClick,
            shape = RoundedCornerShape(16.dp),
            color = raisedColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    "Abrir inicio automático (Xiaomi)",
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Solo en MIUI/HyperOS: activa DarmixNight para que no se cierre solo.",
                    color = dimColor,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(item: PermissionItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusBadge(granted = item.granted)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.label,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(item.detail, color = dimColor, fontSize = 12.sp)
        }
        if (!item.granted) {
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = item.onGrantClick) {
                Text("Conceder", color = accentColor, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StatusBadge(granted: Boolean) {
    val color = if (granted) successColor else warnColor
    Box(
        modifier = Modifier
            .size(26.dp)
            .background(color.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (granted) "✓" else "!",
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmergencySection(
    weeklyCount: Int,
    durationMinutes: Int,
    editable: Boolean,
    onChangeDuration: (Int) -> Unit
) {
    SectionCard {
        Text(
            "Desbloqueo de emergencia",
            color = textColor,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Disponible solo entre ${formatTime12(ScheduleUtils.EMERGENCY_WINDOW_START)} y " +
                    "${formatTime12(ScheduleUtils.EMERGENCY_WINDOW_END)}.",
            color = dimColor,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(raisedColor, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Usos esta semana", color = dimColor, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "$weeklyCount",
                    color = if (weeklyCount == 0) successColor else warnColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = if (weeklyCount == 0) {
                    "Ninguno todavía. Buen trabajo."
                } else {
                    "Cada uso acorta tu descanso."
                },
                color = dimColor,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        DurationStepperRow(
            label = "Duración del desbloqueo",
            minutes = durationMinutes,
            enabled = editable,
            onChange = onChangeDuration
        )

        if (!editable) {
            Spacer(Modifier.height(14.dp))
            NoticeBar(
                color = dangerColor,
                text = "La duración no se puede cambiar mientras el bloqueo está activo, " +
                        "ni siquiera durante un desbloqueo de emergencia."
            )
        }
    }
}

@Composable
private fun DurationStepperRow(
    label: String,
    minutes: Int,
    enabled: Boolean,
    minMinutes: Int = 5,
    maxMinutes: Int = 60,
    stepMinutes: Int = 5,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = textColor, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepperButton(
                symbol = "−",
                enabled = enabled && minutes > minMinutes,
                onClick = { onChange((minutes - stepMinutes).coerceAtLeast(minMinutes)) }
            )
            Box(
                modifier = Modifier.width(72.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$minutes min",
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            StepperButton(
                symbol = "+",
                enabled = enabled && minutes < maxMinutes,
                onClick = { onChange((minutes + stepMinutes).coerceAtMost(maxMinutes)) }
            )
        }
    }
}

@Composable
private fun StepperButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (enabled) accentColor.copy(alpha = 0.18f) else dimColor.copy(alpha = 0.12f),
        modifier = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                symbol,
                color = if (enabled) accentColor else dimColor.copy(alpha = 0.5f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = cardColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, outlineColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) { content() }
    }
}

@Composable
private fun NoticeBar(color: Color, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier
                .padding(top = 5.dp)
                .size(6.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(10.dp))
        Text(text, color = color, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

private fun hourLabel(minutes: Int): String {
    val h24 = (minutes / 60) % 24
    val h12 = if (h24 % 12 == 0) 12 else h24 % 12
    return "%d:%02d".format(h12, minutes % 60)
}

private fun suffixLabel(minutes: Int): String =
    if ((minutes / 60) % 24 < 12) "AM" else "PM"

private fun formatTime12(minutes: Int): String =
    "${hourLabel(minutes)} ${suffixLabel(minutes)}"

private fun formatDuration(totalMinutes: Int): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> "$h h de bloqueo"
        else -> "$h h $m min de bloqueo"
    }
}