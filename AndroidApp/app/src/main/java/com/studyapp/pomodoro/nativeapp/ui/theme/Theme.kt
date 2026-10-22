package com.studyapp.pomodoro.nativeapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val PomodoroColors = lightColorScheme(
    primary = DeepTeal,
    onPrimary = OnTeal,
    primaryContainer = DeepTeal,
    onPrimaryContainer = OnTeal,
    secondary = WarmAmber,
    onSecondary = Charcoal,
    secondaryContainer = AmberTint,
    background = BaseCream,
    onBackground = Charcoal,
    surface = RaisedCream,
    onSurface = Charcoal,
    surfaceVariant = InsetCream,
    onSurfaceVariant = MutedGrey,
    outline = BorderCream,
    error = MutedCrimson,
)

@Composable
fun PomodoroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PomodoroColors,
        typography = PomodoroTypography,
        content = content,
    )
}
