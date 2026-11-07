package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004E57),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = K8sBlueLight,
    onSecondary = Color(0xFF002B73),
    secondaryContainer = Color(0xFF1345A4),
    onSecondaryContainer = Color(0xFFD9E2FF),
    tertiary = PrometheusOrange,
    onTertiary = Color(0xFF452B00),
    tertiaryContainer = Color(0xFF643F00),
    onTertiaryContainer = Color(0xFFFFDDB3),
    background = TechDarkBg,
    onBackground = TextPrimaryDark,
    surface = TechDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = TechDarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = TechDarkBorder,
    error = DangerRed
)

private val LightColorScheme = lightColorScheme(
    primary = K8sBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E2FF),
    onPrimaryContainer = Color(0xFF001945),
    secondary = FluentdCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC7F0FF),
    onSecondaryContainer = Color(0xFF001F29),
    tertiary = PrometheusOrange,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB3),
    onTertiaryContainer = Color(0xFF2B1700),
    background = TechLightBg,
    onBackground = TextPrimaryLight,
    surface = TechLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = TechLightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = TechLightBorder,
    error = DangerRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to dark theme for authentic cloud ops terminal aesthetic
    dynamicColor: Boolean = false, // Keep distinctive DevOps palette
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
