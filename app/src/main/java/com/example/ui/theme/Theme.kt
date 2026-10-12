package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val CosmicColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF0F172A),
    secondary = CosmicPurple,
    onSecondary = Color.White,
    tertiary = NeonMagenta,
    onTertiary = Color.White,
    background = CosmicNavy,
    onBackground = Color.White,
    surface = CosmicDark,
    onSurface = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = CosmicColorScheme,
        typography = Typography,
        content = content
    )
}
