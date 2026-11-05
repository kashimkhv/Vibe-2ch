package dev.dvach.client.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import dev.dvach.client.data.settings.DvachTheme
import dev.dvach.client.data.settings.FontChoice
import dev.dvach.client.util.FONT_SCALE_DEFAULT
import dev.dvach.client.util.LINE_SPACING_DEFAULT

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
 *
 * «Как в системе» — это пара Material You, поэтому её решает настройка
 * Android, а не выбранная человеком тема.
 */
@Composable
fun rememberDarkTheme(theme: DvachTheme): Boolean = when (theme) {
    DvachTheme.SYSTEM -> isSystemInDarkTheme()
    else -> theme.palette().isDark
}

/** Палитра темы с учётом того, светлая или тёмная система. */
@Composable
fun rememberDvachPalette(theme: DvachTheme): DvachPalette {
    val systemDark = isSystemInDarkTheme()
    return remember(theme, systemDark) { theme.palette(systemDark) }
}

@Composable
fun DvachTheme(
    theme: DvachTheme = DvachTheme.MATERIAL_LIGHT,
    headingFont: FontChoice = FontChoice.SYSTEM,
    metaFont: FontChoice = FontChoice.SYSTEM,
    bodyFont: FontChoice = FontChoice.SYSTEM,
    headingScalePercent: Int = FONT_SCALE_DEFAULT,
    metaScalePercent: Int = FONT_SCALE_DEFAULT,
    bodyScalePercent: Int = FONT_SCALE_DEFAULT,
    headingBold: Boolean = false,
    metaBold: Boolean = false,
    bodyBold: Boolean = false,
    bodyLineSpacingPercent: Int = LINE_SPACING_DEFAULT,
    content: @Composable () -> Unit,
) {
    // Шрифты, их размеры и жирность меняются в настройках — типографику
    // пересобираем только когда выбор действительно другой, а не на каждую
    // рекомпозицию.
    val typography = remember(
        headingFont,
        metaFont,
        bodyFont,
        headingScalePercent,
        metaScalePercent,
        bodyScalePercent,
        headingBold,
        metaBold,
        bodyBold,
        bodyLineSpacingPercent,
    ) {
        dvachTypography(
            heading = headingFont,
            meta = metaFont,
            body = bodyFont,
            headingScalePercent = headingScalePercent,
            metaScalePercent = metaScalePercent,
            bodyScalePercent = bodyScalePercent,
            headingBold = headingBold,
            metaBold = metaBold,
            bodyBold = bodyBold,
            bodyLineSpacingPercent = bodyLineSpacingPercent,
        )
    }
    val palette = rememberDvachPalette(theme)
    val colorScheme = remember(palette) { palette.toColorScheme() }

    // LocalDvachPalette нужен там, где ролей Material не хватает: подсветка
    // цитаты, тон тени, точки в превью темы. Через CompositionLocal, чтобы
    // не протаскивать палитру аргументом до каждого листа дерева.
    CompositionLocalProvider(LocalDvachPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = DvachShapes,
            content = content,
        )
    }
}
