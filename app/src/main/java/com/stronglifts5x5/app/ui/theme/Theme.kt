package com.stronglifts5x5.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val StrongRed = Color(0xFFEF4444)
val StrongRedDark = Color(0xFFB91C1C)
val SlateBackground = Color(0xFF0F172A)
val SlateSurface = Color(0xFF1E293B)
val SlateCard = Color(0xFF26334D)
val EmeraldSuccess = Color(0xFF10B981)
val AmberWarning = Color(0xFFF59E0B)
val AccentBlue = Color(0xFF3B82F6)

private val DarkPalette = darkColorScheme(
    primary = StrongRed,
    onPrimary = Color.White,
    primaryContainer = StrongRedDark,
    onPrimaryContainer = Color.White,
    secondary = AccentBlue,
    onSecondary = Color.White,
    tertiary = EmeraldSuccess,
    background = SlateBackground,
    onBackground = Color(0xFFF8FAFC),
    surface = SlateSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = SlateCard,
    onSurfaceVariant = Color(0xFFCBD5E1)
)

@Composable
fun StrongLifts5x5Theme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkPalette,
        content = content
    )
}
