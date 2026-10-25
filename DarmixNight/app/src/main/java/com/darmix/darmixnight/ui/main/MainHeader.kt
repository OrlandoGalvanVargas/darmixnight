package com.darmix.darmixnight.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darmix.darmixnight.R
import com.darmix.darmixnight.ui.components.ThemeToggleButton
import com.darmix.darmixnight.ui.components.rememberSceneClock
import com.darmix.darmixnight.ui.theme.DnTheme
import com.darmix.darmixnight.ui.theme.DnType
import com.darmix.darmixnight.ui.theme.Sora
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val HeaderHeight = 46.dp
private val WeekDays = listOf("LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM")
private val NightBlue = Color(0xFF2E7FD0)

private val Wordmark = TextStyle(
    fontFamily = Sora,
    fontWeight = FontWeight.SemiBold,
    fontSize = 18.sp,
    lineHeight = 22.sp,
    letterSpacing = (-0.4).sp
)

private fun greetingFor(time: LocalDateTime): String {
    val part = when (time.hour) {
        in 5..11 -> "BUENOS DÍAS"
        in 12..18 -> "BUENAS TARDES"
        else -> "BUENAS NOCHES"
    }
    return "$part · ${WeekDays[time.dayOfWeek.value - 1]} ${time.dayOfMonth}"
}

@Composable
internal fun MainHeader(onToggleTheme: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DnTheme.colors
    val clock = rememberSceneClock()
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(60_000L - System.currentTimeMillis() % 60_000L + 50L)
        }
    }
    val nightBrush = if (colors.isDark) {
        Brush.horizontalGradient(listOf(colors.accent, colors.glow, colors.accentAlt))
    } else {
        Brush.horizontalGradient(listOf(colors.accent, NightBlue))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(HeaderHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OrbitMark(clock)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            AnimatedContent(
                targetState = greetingFor(now),
                transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(300)) },
                label = "greeting"
            ) { text ->
                Text(
                    text,
                    style = DnType.overline.copy(fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 1.6.sp),
                    color = colors.textDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row {
                Text("Darmix", style = Wordmark, color = colors.text)
                Text(
                    "Night",
                    style = Wordmark.copy(brush = nightBrush),
                    modifier = Modifier.shimmer(clock, if (colors.isDark) Color.White else Color(0xFFBFD9F5))
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        ThemeToggleButton(onClick = onToggleTheme)
    }
}

private fun Modifier.shimmer(clock: State<Float>, highlight: Color): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val p = (clock.value % 6.5f) / 1.3f
        if (p < 1f) {
            val band = size.width * 0.55f
            val x = -band + (size.width + band * 2f) * p
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, highlight.copy(alpha = 0.85f), Color.Transparent),
                    start = Offset(x - band / 2f, 0f),
                    end = Offset(x + band / 2f, size.height)
                ),
                blendMode = BlendMode.SrcAtop
            )
        }
    }

private fun sparklePath(radius: Float): Path = Path().apply {
    for (i in 0 until 10) {
        val a = -PI / 2 + i * PI / 5
        val r = if (i % 2 == 0) radius else radius * 0.42f
        val x = r * cos(a).toFloat()
        val y = r * sin(a).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

@Composable
private fun OrbitMark(clock: State<Float>) {
    val colors = DnTheme.colors
    val ringColor = if (colors.isDark) colors.glow else colors.accent
    Box(Modifier.size(HeaderHeight), contentAlignment = Alignment.Center) {
        Spacer(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val orbit = size.minDimension / 2f - 1.5.dp.toPx()
                    drawCircle(colors.accent.copy(alpha = 0.14f), orbit - 4.dp.toPx(), c)
                    drawCircle(
                        colors.accent.copy(alpha = 0.45f),
                        orbit,
                        c,
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.5.dp.toPx(), 3.5.dp.toPx()))
                        )
                    )
                }
        )
        Image(
            painter = painterResource(R.drawable.dn_panda_avatar),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .border(1.5.dp, ringColor.copy(alpha = 0.6f), CircleShape)
        )
        Spacer(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val orbit = size.minDimension / 2f - 1.5.dp.toPx()
                    val a = clock.value * (2f * PI.toFloat() / 16f) - PI.toFloat() / 4f
                    val p = Offset(c.x + orbit * cos(a), c.y + orbit * sin(a))
                    val pulse = 0.85f + 0.15f * sin(clock.value * 3f)
                    drawCircle(colors.accent.copy(alpha = 0.25f), 4.5.dp.toPx() * pulse, p)
                    val star = sparklePath(3.6.dp.toPx() * pulse)
                    drawPath(star.apply { translate(p) }, colors.accent)
                }
        )
    }
}
