package com.pukaar.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val SosRed = Color(0xFFD32F2F)
val HelpOrange = Color(0xFFF57C00)
val PukaarBlack = Color(0xFF000000)
val PukaarWhite = Color(0xFFFFFFFF)
val PukaarMuted = Color(0xFFBDBDBD)
val TileGray = Color(0xFF1C1C1C)

private val scheme = darkColorScheme(
    primary = SosRed,
    secondary = HelpOrange,
    background = PukaarBlack,
    surface = PukaarBlack,
    onPrimary = PukaarWhite,
    onBackground = PukaarWhite,
    onSurface = PukaarWhite
)

@Composable
fun PukaarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = scheme,
        typography = MaterialTheme.typography.copy(
            displayLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = 48.sp,
                color = PukaarWhite
            ),
            headlineLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = PukaarWhite
            ),
            titleLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = PukaarWhite
            ),
            bodyLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                color = PukaarWhite
            ),
            bodyMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                color = PukaarMuted
            )
        ),
        content = content
    )
}
