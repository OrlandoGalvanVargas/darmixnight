package com.darmix.darmixnight.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.darmix.darmixnight.R

val InstrumentSerif = FontFamily(
    Font(R.font.instrument_serif_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic)
)

val InterTight = FontFamily(
    Font(R.font.inter_tight_regular, FontWeight.Normal),
    Font(R.font.inter_tight_medium, FontWeight.Medium),
    Font(R.font.inter_tight_semibold, FontWeight.SemiBold)
)

private val Tight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both
)

private fun serif(size: Int, line: Int, tracking: Double = 0.0, italic: Boolean = false) = TextStyle(
    fontFamily = InstrumentSerif,
    fontWeight = FontWeight.Normal,
    fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp
)

private fun inter(weight: FontWeight, size: Double, line: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = InterTight,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp
)

val DnTypography = Typography(
    displayLarge = serif(64, 64, -1.6),
    displayMedium = serif(52, 54, -1.2),
    displaySmall = serif(40, 44, -0.8),
    headlineLarge = serif(34, 38, -0.6),
    headlineMedium = serif(28, 32, -0.4),
    headlineSmall = serif(24, 28, -0.2),
    titleLarge = serif(22, 26, italic = true),
    titleMedium = inter(FontWeight.SemiBold, 16.0, 22),
    titleSmall = inter(FontWeight.Medium, 14.0, 20),
    bodyLarge = inter(FontWeight.Normal, 16.0, 24),
    bodyMedium = inter(FontWeight.Normal, 14.0, 20),
    bodySmall = inter(FontWeight.Normal, 12.5, 18),
    labelLarge = inter(FontWeight.Medium, 14.0, 20),
    labelMedium = inter(FontWeight.Medium, 12.0, 16),
    labelSmall = inter(FontWeight.Medium, 11.0, 14, 0.2)
)

object DnType {
    val countdown = TextStyle(
        fontFamily = InstrumentSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 150.sp,
        lineHeight = 130.sp,
        letterSpacing = (-0.035).em,
        lineHeightStyle = Tight
    )
    val lead = TextStyle(
        fontFamily = InstrumentSerif,
        fontStyle = FontStyle.Italic,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        lineHeightStyle = Tight
    )
    val note = TextStyle(
        fontFamily = InstrumentSerif,
        fontStyle = FontStyle.Italic,
        fontSize = 21.sp,
        lineHeight = 26.sp
    )
    val noteSmall = TextStyle(
        fontFamily = InstrumentSerif,
        fontStyle = FontStyle.Italic,
        fontSize = 15.sp,
        lineHeight = 18.sp
    )
    val timeLarge = TextStyle(
        fontFamily = InstrumentSerif,
        fontSize = 68.sp,
        lineHeight = 68.sp,
        letterSpacing = (-0.03).em,
        lineHeightStyle = Tight
    )
    val navLabel = TextStyle(
        fontFamily = InterTight,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 14.sp
    )
    val brand = TextStyle(
        fontFamily = InterTight,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.02).em
    )
    val brandAccent = TextStyle(
        fontFamily = InstrumentSerif,
        fontStyle = FontStyle.Italic,
        fontSize = 23.sp,
        lineHeight = 24.sp
    )
}
