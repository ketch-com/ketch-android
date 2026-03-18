package com.ketch.android.sample.compose.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val KetchPurple = Color(0xFF6B4EE6)
val KetchPurpleDark = Color(0xFF5438C5)

val LightBackground = Color(0xFFF5F5F7)
val LightSurface = Color(0xFFFFFFFF)
val LightCardStroke = Color(0xFFE0D9F5)
val LightTextPrimary = Color(0xFF1A1A2E)
val LightTextSecondary = Color(0xFF6E6E80)
val LightLogBackground = Color(0xFFF0ECF9)
val LightLogText = Color(0xFF3D3D50)
val LightDivider = Color(0xFFE8E5F0)
val LightToggleTrack = Color(0xFFE0D9F5)

val DarkBackground = Color(0xFF0D0B1E)
val DarkSurface = Color(0xFF1C1A35)
val DarkCardStroke = Color(0xFF2D2A4A)
val DarkTextPrimary = Color(0xFFE8E6F0)
val DarkTextSecondary = Color(0xFF9896A8)
val DarkLogBackground = Color(0xFF151330)
val DarkLogText = Color(0xFFC0BED0)
val DarkDivider = Color(0xFF2D2A4A)
val DarkToggleTrack = Color(0xFF2D2A4A)

private val LightColorScheme = lightColorScheme(
    primary = KetchPurple,
    onPrimary = Color.White,
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    outline = LightCardStroke,
    surfaceVariant = LightLogBackground,
    onSurfaceVariant = LightTextSecondary,
)

private val DarkColorScheme = darkColorScheme(
    primary = KetchPurple,
    onPrimary = Color.White,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    outline = DarkCardStroke,
    surfaceVariant = DarkLogBackground,
    onSurfaceVariant = DarkTextSecondary,
)

@Composable
fun KetchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}
