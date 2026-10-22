package com.studyapp.pomodoro.nativeapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val BaseStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

private fun textStyle(
    fontSize: Int,
    fontWeight: FontWeight,
    lineHeight: Int,
    letterSpacing: Float = 0f,
) = BaseStyle.copy(
    fontSize = fontSize.sp,
    fontWeight = fontWeight,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

val PomodoroTypography = Typography(
    headlineLarge = textStyle(32, FontWeight.Bold, 38, -0.6f),
    headlineMedium = textStyle(20, FontWeight.Bold, 26),
    titleLarge = textStyle(20, FontWeight.Bold, 24),
    titleMedium = textStyle(15, FontWeight.Bold, 22),
    bodyLarge = textStyle(14, FontWeight.Normal, 20),
    bodyMedium = textStyle(13, FontWeight.Normal, 19),
    bodySmall = textStyle(11, FontWeight.Normal, 16),
    labelLarge = textStyle(15, FontWeight.Bold, 20, 0.2f),
    labelMedium = textStyle(13, FontWeight.SemiBold, 18, 0.2f),
    labelSmall = textStyle(9, FontWeight.Bold, 14, 1.2f),
)
