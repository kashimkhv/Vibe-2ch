package dev.dvach.client.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.dvach.client.data.settings.FontChoice
import dev.dvach.client.data.settings.ThemeMode
import dev.dvach.client.util.FONT_SCALE_DEFAULT

/**
 * Палитра старого Material Design (2016–2018): плоские белые строки,
 * серые подзаголовки-полосы и тонкие разделители. Никаких тональных
 * поверхностей и скруглений в 16 dp — квадратные углы и границы.
 *
 * Светлая тема: светло-голубая шапка (`primaryContainer`), белые списки
 * на сером фоне, разделитель `#DDE3E8`.
 *
 * Динамических цветов Material You здесь нет намеренно: палитра брендовая, и
 * шапка должна оставаться голубой независимо от обоев.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E4F4),
    onPrimaryContainer = Color(0xFF0D2A44),

    secondary = Color(0xFF546E7A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E7F6),
    onSecondaryContainer = Color(0xFF0A1E2C),

    tertiary = Color(0xFF5C6B7A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE3E7EB),
    onTertiaryContainer = Color(0xFF1B242C),

    background = Color(0xFFF5F6F7),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFECEFF1),
    onSurfaceVariant = Color(0xFF5F6B75),
    surfaceTint = Color(0xFF1976D2),

    // Старый Material строит иерархию на границах, а не на тоне, поэтому
    // все «контейнеры» — либо белые, либо светло-серые.
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFAFBFC),
    surfaceContainerHigh = Color(0xFFF0F2F4),
    surfaceContainerHighest = Color(0xFFE6EAEE),

    outline = Color(0xFF90A4AE),
    outlineVariant = Color(0xFFDDE3E8),

    error = Color(0xFFC62828),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF7DCDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF0D2A44),
    primaryContainer = Color(0xFF1B3A52),
    onPrimaryContainer = Color(0xFFD3E4F4),

    secondary = Color(0xFFB0BEC5),
    onSecondary = Color(0xFF23323E),
    secondaryContainer = Color(0xFF34495A),
    onSecondaryContainer = Color(0xFFD7E7F6),

    tertiary = Color(0xFFBFC8D2),
    onTertiary = Color(0xFF2C3540),
    tertiaryContainer = Color(0xFF424B55),
    onTertiaryContainer = Color(0xFFE3E7EB),

    background = Color(0xFF111315),
    onBackground = Color(0xFFE3E5E8),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE3E5E8),
    surfaceVariant = Color(0xFF2A2E31),
    onSurfaceVariant = Color(0xFFB6BFC7),
    surfaceTint = Color(0xFF90CAF9),

    surfaceContainerLowest = Color(0xFF0C0E0F),
    surfaceContainerLow = Color(0xFF1A1C1E),
    surfaceContainer = Color(0xFF1E2124),
    surfaceContainerHigh = Color(0xFF25282B),
    surfaceContainerHighest = Color(0xFF2E3235),

    outline = Color(0xFF8B939C),
    outlineVariant = Color(0xFF34383C),

    error = Color(0xFFEF9A9A),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

/**
 * Квадратные углы. В старом Material карточки были почти прямоугольными
 * (радиус 2 dp), а списки — вообще без скруглений; кнопки и поля ввода
 * подчёркивались линией, а не «пилюлей».
 */
private val DvachShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(2.dp),
    large = RoundedCornerShape(2.dp),
    extraLarge = RoundedCornerShape(2.dp),
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
    themeMode: ThemeMode = ThemeMode.LIGHT,
    headingFont: FontChoice = FontChoice.SYSTEM,
    metaFont: FontChoice = FontChoice.SYSTEM,
    bodyFont: FontChoice = FontChoice.SYSTEM,
    headingScalePercent: Int = FONT_SCALE_DEFAULT,
    metaScalePercent: Int = FONT_SCALE_DEFAULT,
    bodyScalePercent: Int = FONT_SCALE_DEFAULT,
    content: @Composable () -> Unit,
) {
    // Шрифты и их размеры меняются в настройках — типографику пересобираем
    // только когда выбор действительно другой, а не на каждую рекомпозицию.
    val typography = remember(
        headingFont,
        metaFont,
        bodyFont,
        headingScalePercent,
        metaScalePercent,
        bodyScalePercent,
    ) {
        dvachTypography(
            heading = headingFont,
            meta = metaFont,
            body = bodyFont,
            headingScalePercent = headingScalePercent,
            metaScalePercent = metaScalePercent,
            bodyScalePercent = bodyScalePercent,
        )
    }
    MaterialTheme(
        colorScheme = if (rememberDarkTheme(themeMode)) DarkColors else LightColors,
        typography = typography,
        shapes = DvachShapes,
        content = content,
    )
}
