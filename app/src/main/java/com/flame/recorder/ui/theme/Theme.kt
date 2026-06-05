package com.flame.recorder.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF4511E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5E1B00),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFF77574B),
    onSecondary = Color.White,
    background = Color(0xFF1B1412),
    onBackground = Color(0xFFF0DFDA),
    surface = Color(0xFF231A17),
    onSurface = Color(0xFFF0DFDA),
    error = Color(0xFFFFB4AB)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFF4511E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF380C00),
    secondary = Color(0xFF77574B),
    onSecondary = Color.White,
    background = Color(0xFFFEF1EB),
    onBackground = Color(0xFF201A18),
    surface = Color(0xFFF9EDE7),
    onSurface = Color(0xFF201A18),
    error = Color(0xFFBA1A1A)
)

@Composable
fun FlameRecorderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}