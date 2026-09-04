package com.vigyan.juniorcollege.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val VigyanGreen = Color(0xFF1FAE38)
val VigyanGreenDark = Color(0xFF0C7A22)
val VigyanMagenta = Color(0xFFE8148C)
val VigyanCyan = Color(0xFF3AC7E0)
val VigyanOrange = Color(0xFFF5941F)
val VigyanGold = Color(0xFFF2C41A)
val SurfaceLight = Color(0xFFFFFBFE)

private val LightColors = lightColorScheme(
    primary = VigyanGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC6F2CE),
    onPrimaryContainer = VigyanGreenDark,
    secondary = VigyanMagenta,
    onSecondary = Color.White,
    tertiary = VigyanCyan,
    onTertiary = Color.Black,
    error = Color(0xFFBA1A1A),
    background = SurfaceLight,
    surface = SurfaceLight,
    surfaceVariant = Color(0xFFEFF3EE)
)

private val DarkColors = darkColorScheme(
    primary = VigyanGreen,
    onPrimary = Color.Black,
    secondary = VigyanMagenta,
    tertiary = VigyanCyan
)

@Composable
fun VigyanErpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = VigyanTypography,
        content = content
    )
}
