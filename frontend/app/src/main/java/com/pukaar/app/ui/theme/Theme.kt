package com.pukaar.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Pukaar is always dark and always the same red — an emergency screen has to look
 * identical every time it is opened, so the system theme and dynamic color are
 * deliberately ignored.
 */
private val PukaarColorScheme = darkColorScheme(
    primary = PukaarRed,
    onPrimary = TextPrimary,
    secondary = PukaarRedDark,
    onSecondary = TextPrimary,
    tertiary = SuccessGreen,
    onTertiary = Black,
    background = Black,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = Outline,
    error = PukaarRedBright,
    onError = TextPrimary
)

@Composable
fun PukaarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PukaarColorScheme,
        typography = Typography,
        content = content
    )
}
