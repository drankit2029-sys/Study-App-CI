package com.studyapp.decaycharge.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CalmColors = lightColorScheme(
    primary = Forest,
    onPrimary = Ivory,
    primaryContainer = MintLight,
    onPrimaryContainer = Forest,
    secondary = Mint,
    onSecondary = Ink,
    secondaryContainer = MintLight,
    onSecondaryContainer = Forest,
    background = Ivory,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Parchment,
    onSurfaceVariant = Muted,
    outline = Border,
    error = Error,
)

@Composable
fun DecayChargeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CalmColors,
        typography = DecayTypography,
        content = content,
    )
}
