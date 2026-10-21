package com.darmix.darmixnight.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.darmix.darmixnight.R

val Sora = FontFamily(
    Font(R.font.sora_light, FontWeight.Light),
    Font(R.font.sora_semibold, FontWeight.SemiBold),
    Font(R.font.sora_bold, FontWeight.Bold)
)

val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold)
)

private fun sora(weight: FontWeight, size: Int, line: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = Sora,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp
)

private fun manrope(weight: FontWeight, size: Int, line: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = Manrope,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp
)

val DnTypography = Typography(
    displayLarge = sora(FontWeight.Light, 56, 60, -1.5),
    displayMedium = sora(FontWeight.Light, 44, 50, -1.0),
    displaySmall = sora(FontWeight.SemiBold, 34, 40, -0.6),
    headlineLarge = sora(FontWeight.SemiBold, 30, 36, -0.5),
    headlineMedium = sora(FontWeight.SemiBold, 26, 32, -0.4),
    headlineSmall = sora(FontWeight.SemiBold, 22, 28, -0.2),
    titleLarge = sora(FontWeight.SemiBold, 20, 26, -0.1),
    titleMedium = manrope(FontWeight.Bold, 16, 22),
    titleSmall = manrope(FontWeight.SemiBold, 14, 20),
    bodyLarge = manrope(FontWeight.Normal, 16, 24, 0.1),
    bodyMedium = manrope(FontWeight.Normal, 14, 20, 0.1),
    bodySmall = manrope(FontWeight.Normal, 12, 17, 0.2),
    labelLarge = manrope(FontWeight.Bold, 14, 20, 0.2),
    labelMedium = manrope(FontWeight.SemiBold, 12, 16, 0.3),
    labelSmall = manrope(FontWeight.Bold, 11, 14, 1.2)
)

object DnType {
    val clock = TextStyle(
        fontFamily = Sora,
        fontWeight = FontWeight.Light,
        fontSize = 64.sp,
        lineHeight = 66.sp,
        letterSpacing = (-2).sp,
        fontFeatureSettings = "tnum"
    )
    val clockMedium = TextStyle(
        fontFamily = Sora,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-1).sp,
        fontFeatureSettings = "tnum"
    )
    val wordmark = TextStyle(
        fontFamily = Sora,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.6).sp
    )
    val overline = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 2.sp
    )
    val numeric = TextStyle(
        fontFamily = Sora,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum"
    )
}
