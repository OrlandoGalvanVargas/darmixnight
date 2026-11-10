package com.darmix.darmixnight.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
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
    val isDark: Boolean,
    val ink: Color,
    val umber: Color,
    val hairline: Color,
    val hairlineSoft: Color,
    val textSoft: Color,
    val clay: Color,
    val clayDim: Color,
    val textMute: Color,
    val ash: Color,
    val ember: Color,
    val emberDeep: Color,
    val emberCore: Color,
    val amber: Color,
    val gold: Color,
    val flame: Color,
    val whiteHot: Color,
    val bronze: Color,
    val bronzeDark: Color
)

val BrasaColors = DnColors(
    bgTop = BrasaBgTop,
    bg = BrasaBg,
    card = BrasaSurface,
    raised = BrasaRaised,
    text = BrasaCream,
    textDim = BrasaDim,
    accent = BrasaEmber,
    onAccent = BrasaInk,
    accentAlt = BrasaAmber,
    glow = BrasaFlame,
    success = BrasaSage,
    warn = BrasaAmber,
    danger = BrasaDanger,
    outline = BrasaHairline,
    isDark = true,
    ink = BrasaInk,
    umber = BrasaUmber,
    hairline = BrasaHairline,
    hairlineSoft = BrasaHairlineSoft,
    textSoft = BrasaCreamSoft,
    clay = BrasaClay,
    clayDim = BrasaClayDim,
    textMute = BrasaMute,
    ash = BrasaAsh,
    ember = BrasaEmber,
    emberDeep = BrasaEmberDeep,
    emberCore = BrasaEmberCore,
    amber = BrasaAmber,
    gold = BrasaGold,
    flame = BrasaFlame,
    whiteHot = BrasaWhiteHot,
    bronze = BrasaBronze,
    bronzeDark = BrasaBronzeDark
)

@Immutable
class DnScale(val factor: Float) {
    fun dp(value: Float): Dp = (value * factor).dp
    fun dp(value: Int): Dp = (value * factor).dp
    fun sp(value: TextUnit): TextUnit = value * factor
}

val LocalDnColors = staticCompositionLocalOf { BrasaColors }
val LocalDnScale = staticCompositionLocalOf { DnScale(1f) }

object DnTheme {
    val colors: DnColors
        @Composable @ReadOnlyComposable get() = LocalDnColors.current
    val scale: DnScale
        @Composable @ReadOnlyComposable get() = LocalDnScale.current
}

val DnShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

private const val DesignHeightDp = 844f
private const val MinScale = 0.74f

private val BrasaScheme = darkColorScheme(
    primary = BrasaEmber,
    onPrimary = BrasaInk,
    primaryContainer = BrasaRaised,
    onPrimaryContainer = BrasaCream,
    secondary = BrasaAmber,
    onSecondary = BrasaInk,
    tertiary = BrasaFlame,
    onTertiary = BrasaInk,
    background = BrasaBg,
    onBackground = BrasaCream,
    surface = BrasaSurface,
    onSurface = BrasaCream,
    surfaceVariant = BrasaRaised,
    onSurfaceVariant = BrasaDim,
    surfaceContainerLowest = BrasaInk,
    surfaceContainerLow = BrasaBg,
    surfaceContainer = BrasaSurface,
    surfaceContainerHigh = BrasaRaised,
    surfaceContainerHighest = BrasaRaised,
    inverseSurface = BrasaCream,
    inverseOnSurface = BrasaInk,
    error = BrasaDanger,
    onError = BrasaInk,
    outline = BrasaDim,
    outlineVariant = BrasaHairline,
    scrim = Color.Black
)

@Composable
fun DarmixNightTheme(content: @Composable () -> Unit) {
    val heightDp = LocalConfiguration.current.screenHeightDp
    val scale = remember(heightDp) {
        DnScale((heightDp / DesignHeightDp).coerceIn(MinScale, 1f))
    }
    CompositionLocalProvider(
        LocalDnColors provides BrasaColors,
        LocalDnScale provides scale
    ) {
        MaterialTheme(
            colorScheme = BrasaScheme,
            typography = DnTypography,
            shapes = DnShapes,
            content = content
        )
    }
}
