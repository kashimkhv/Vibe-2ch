package dev.dvach.client.ui.theme

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
import dev.dvach.client.data.settings.ThemeMode

private val Green = Color(0xFF2F6B3A)
private val GreenLight = Color(0xFF8FD49A)
private val Ochre = Color(0xFF8A6A1F)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3F0D6),
    onPrimaryContainer = Color(0xFF0B2010),
    secondary = Ochre,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4E7C4),
    onSecondaryContainer = Color(0xFF2A1F00),
    background = Color(0xFFFDFCF8),
    onBackground = Color(0xFF1B1C18),
    surface = Color(0xFFFDFCF8),
    onSurface = Color(0xFF1B1C18),
    surfaceVariant = Color(0xFFE7E4DC),
    onSurfaceVariant = Color(0xFF474740),
    outline = Color(0xFF79786F),
)

private val DarkColors = darkColorScheme(
    primary = GreenLight,
    onPrimary = Color(0xFF003912),
    primaryContainer = Color(0xFF14521F),
    onPrimaryContainer = Color(0xFFC6F2CA),
    secondary = Color(0xFFE0C77E),
    onSecondary = Color(0xFF3B2F00),
    secondaryContainer = Color(0xFF544600),
    onSecondaryContainer = Color(0xFFF4E7C4),
    background = Color(0xFF121310),
    onBackground = Color(0xFFE4E3DA),
    surface = Color(0xFF121310),
    onSurface = Color(0xFFE4E3DA),
    surfaceVariant = Color(0xFF2A2B26),
    onSurfaceVariant = Color(0xFFC8C7BC),
    outline = Color(0xFF92918A),
)

/**
 * Единая точка правды о том, тёмная ли тема: приложению важно не только
 * для палитры, но и для цвета системных баров, когда тема выбрана вручную.
 */
@Composable
fun rememberDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun DvachTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = rememberDarkTheme(themeMode)
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
