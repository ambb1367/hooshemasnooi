package ir.hoshemasnooi.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8C9EFF),
    onPrimary = Color(0xFF0B1026),
    primaryContainer = Color(0xFF2A2F5A),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFF4FC3F7),
    onSecondary = Color(0xFF001E2B),
    tertiary = Color(0xFFB388FF),
    background = Color(0xFF0B1026),
    onBackground = Color(0xFFE6E8F5),
    surface = Color(0xFF121834),
    onSurface = Color(0xFFE6E8F5),
    surfaceVariant = Color(0xFF1E2547),
    onSurfaceVariant = Color(0xFFC3C8E8),
    outline = Color(0xFF3A4270),
    error = Color(0xFFFF8A80)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF3D5AFE),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF00105C),
    secondary = Color(0xFF0288D1),
    onSecondary = Color.White,
    tertiary = Color(0xFF7C4DFF),
    background = Color(0xFFF7F8FE),
    onBackground = Color(0xFF12142A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF12142A),
    surfaceVariant = Color(0xFFE6E8F5),
    onSurfaceVariant = Color(0xFF454B6B),
    outline = Color(0xFFC3C8E8),
    error = Color(0xFFB3261E)
)

@Composable
fun HooshYarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
