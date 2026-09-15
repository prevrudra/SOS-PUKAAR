package com.pukaar.highalert.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val PukaarColorScheme = lightColorScheme(
    primary = PukaarRed,
    onPrimary = SurfaceWhite,
    secondary = CallGreen,
    onSecondary = SurfaceWhite,
    tertiary = InfoBlue,
    onTertiary = SurfaceWhite,
    background = SurfaceWhite,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceGrey,
    onSurfaceVariant = TextSecondary,
    outline = OutlineGrey,
    error = PukaarRed,
    onError = SurfaceWhite
)

/**
 * The alert UI is intentionally light-only: an emergency screen has to look the same
 * for every recipient, so no dark or dynamic colour variants are applied.
 */
@Composable
fun PukaarAlertTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PukaarColorScheme,
        typography = Typography,
        content = content
    )
}
