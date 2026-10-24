package com.studyapp.pomodoro.nativeapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.studyapp.pomodoro.nativeapp.R

val Cormorant = FontFamily(
    Font(R.font.cormorant_light, FontWeight.Light),
    Font(R.font.cormorant_regular, FontWeight.Normal),
    Font(R.font.cormorant_medium, FontWeight.Medium),
)

private val SansStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

private val SerifStyle = TextStyle(
    fontFamily = Cormorant,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

private fun sans(
    fontSize: Int,
    fontWeight: FontWeight,
    lineHeight: Int,
    letterSpacing: Float = 0f,
) = SansStyle.copy(
    fontSize = fontSize.sp,
    fontWeight = fontWeight,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

private fun serif(
    fontSize: Int,
    fontWeight: FontWeight,
    lineHeight: Int,
    letterSpacing: Float = 0f,
) = SerifStyle.copy(
    fontSize = fontSize.sp,
    fontWeight = fontWeight,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

val PomodoroTypography = Typography(
    displayLarge = serif(56, FontWeight.Light, 58, -0.4f),
    displayMedium = serif(42, FontWeight.Normal, 44),
    displaySmall = serif(34, FontWeight.Normal, 38),
    headlineLarge = serif(30, FontWeight.Normal, 34),
    headlineMedium = serif(26, FontWeight.Medium, 30),
    headlineSmall = serif(22, FontWeight.Medium, 26),
    titleLarge = serif(19, FontWeight.Medium, 24, 0.2f),
    titleMedium = sans(14, FontWeight.SemiBold, 20),
    titleSmall = sans(12, FontWeight.SemiBold, 16),
    bodyLarge = sans(14, FontWeight.Normal, 21),
    bodyMedium = sans(13, FontWeight.Normal, 19),
    bodySmall = sans(11, FontWeight.Normal, 16),
    labelLarge = sans(11, FontWeight.SemiBold, 16),
    labelMedium = sans(11, FontWeight.Medium, 16, 0.1f),
    labelSmall = sans(11, FontWeight.Bold, 15, 1.4f),
)
