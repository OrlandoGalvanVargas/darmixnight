package com.darmix.darmixnight.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class DnColors(
    val bgTop: Color,
    val bg: Color,
    val card: Color,
    val raised: Color,
    val text: Color,
    val textDim: Color,
    val accent: Color,
    val onAccent: Color,
    val accentAlt: Color,
    val glow: Color,
    val success: Color,
    val warn: Color,
    val danger: Color,
    val outline: Color,
    val isDark: Boolean
)

val NightColors = DnColors(
    bgTop = NightBgTop,
    bg = NightBg,
    card = NightCard,
    raised = NightRaised,
    text = NightText,
    textDim = NightTextDim,
    accent = NightAccent,
    onAccent = NightOnAccent,
    accentAlt = NightAccentAlt,
    glow = NightGlow,
    success = NightSuccess,
    warn = NightWarn,
    danger = NightDanger,
    outline = Color.White.copy(alpha = 0.07f),
    isDark = true
)

val DayColors = DnColors(
    bgTop = DayBgTop,
    bg = DayBg,
    card = DayCard,
    raised = DayRaised,
    text = DayText,
    textDim = DayTextDim,
    accent = DayAccent,
    onAccent = DayOnAccent,
    accentAlt = DayAccentAlt,
    glow = DayGlow,
    success = DaySuccess,
    warn = DayWarn,
    danger = DayDanger,
    outline = DayText.copy(alpha = 0.09f),
    isDark = false
)

val LocalDnColors = staticCompositionLocalOf { NightColors }

object DnTheme {
    val colors: DnColors
        @Composable @ReadOnlyComposable get() = LocalDnColors.current
}

val DnShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

private val ThemeSwapSpec = tween<Color>(durationMillis = 450, easing = FastOutSlowInEasing)

@Composable
private fun swap(target: Color, label: String): Color =
    animateColorAsState(targetValue = target, animationSpec = ThemeSwapSpec, label = label).value

@Composable
private fun DnColors.animated(): DnColors = DnColors(
    bgTop = swap(bgTop, "bgTop"),
    bg = swap(bg, "bg"),
    card = swap(card, "card"),
    raised = swap(raised, "raised"),
    text = swap(text, "text"),
    textDim = swap(textDim, "textDim"),
    accent = swap(accent, "accent"),
    onAccent = swap(onAccent, "onAccent"),
    accentAlt = swap(accentAlt, "accentAlt"),
    glow = swap(glow, "glow"),
    success = swap(success, "success"),
    warn = swap(warn, "warn"),
    danger = swap(danger, "danger"),
    outline = swap(outline, "outline"),
    isDark = isDark
)

private fun materialScheme(c: DnColors): ColorScheme = if (c.isDark) {
    darkColorScheme(
        primary = c.accent,
        onPrimary = c.onAccent,
        primaryContainer = c.raised,
        onPrimaryContainer = c.text,
        secondary = c.accentAlt,
        onSecondary = c.bg,
        tertiary = c.glow,
        background = c.bg,
        onBackground = c.text,
        surface = c.card,
        onSurface = c.text,
        surfaceVariant = c.raised,
        onSurfaceVariant = c.textDim,
        surfaceContainer = c.card,
        surfaceContainerHigh = c.raised,
        error = c.danger,
        onError = c.onAccent,
        outline = c.textDim,
        outlineVariant = c.outline
    )
} else {
    lightColorScheme(
        primary = c.accent,
        onPrimary = c.onAccent,
        primaryContainer = c.raised,
        onPrimaryContainer = c.text,
        secondary = c.accentAlt,
        onSecondary = Color.White,
        tertiary = c.glow,
        background = c.bg,
        onBackground = c.text,
        surface = c.card,
        onSurface = c.text,
        surfaceVariant = c.raised,
        onSurfaceVariant = c.textDim,
        surfaceContainer = c.card,
        surfaceContainerHigh = c.raised,
        error = c.danger,
        onError = Color.White,
        outline = c.textDim,
        outlineVariant = c.outline
    )
}

@Composable
fun DarmixNightTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = (if (darkTheme) NightColors else DayColors).animated()
    val scheme = remember(colors) { materialScheme(colors) }
    CompositionLocalProvider(LocalDnColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = DnTypography,
            shapes = DnShapes,
            content = content
        )
    }
}
