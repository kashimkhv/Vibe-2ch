package dev.dvach.client.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import dev.dvach.client.R
import dev.dvach.client.data.settings.FontChoice
import dev.dvach.client.util.FONT_SCALE_DEFAULT
import dev.dvach.client.util.LINE_SPACING_DEFAULT
import dev.dvach.client.util.clampFontScale
import dev.dvach.client.util.clampLineSpacing

/**
 * Шрифты из Google Fonts, у которых есть полноценная кириллица, а не только
 * латиница с парой русских букв. Файлы лежат в `res/font` (Regular, Medium,
 * Bold), поэтому шрифты работают без сети — качать ничего не нужно.
 *
 * Системный вариант — `FontFamily.Default`: это Roboto, и он ничего не стоит
 * по размеру APK.
 */
val FontChoice.fontFamily: FontFamily
    get() = when (this) {
        FontChoice.SYSTEM -> FontFamily.Default
        FontChoice.PT_SANS -> ptSans
        FontChoice.PT_SERIF -> ptSerif
        FontChoice.OPEN_SANS -> openSans
        FontChoice.IBM_PLEX_SANS -> ibmPlexSans
        FontChoice.GOLOS_TEXT -> golosText
        FontChoice.MONTSERRAT -> montserrat
        FontChoice.LORA -> lora
        FontChoice.MERRIWEATHER -> merriweather
    }

private fun fontFamily(regular: Int, medium: Int, bold: Int): FontFamily = FontFamily(
    Font(regular, FontWeight.Normal),
    Font(medium, FontWeight.Medium),
    Font(bold, FontWeight.Bold),
)

private val ptSans = fontFamily(
    R.font.pt_sans_regular,
    R.font.pt_sans_medium,
    R.font.pt_sans_bold,
)

private val ptSerif = fontFamily(
    R.font.pt_serif_regular,
    R.font.pt_serif_medium,
    R.font.pt_serif_bold,
)

private val openSans = fontFamily(
    R.font.open_sans_regular,
    R.font.open_sans_medium,
    R.font.open_sans_bold,
)

private val ibmPlexSans = fontFamily(
    R.font.ibm_plex_sans_regular,
    R.font.ibm_plex_sans_medium,
    R.font.ibm_plex_sans_bold,
)

private val golosText = fontFamily(
    R.font.golos_text_regular,
    R.font.golos_text_medium,
    R.font.golos_text_bold,
)

private val montserrat = fontFamily(
    R.font.montserrat_regular,
    R.font.montserrat_medium,
    R.font.montserrat_bold,
)

private val lora = fontFamily(
    R.font.lora_regular,
    R.font.lora_medium,
    R.font.lora_bold,
)

private val merriweather = fontFamily(
    R.font.merriweather_regular,
    R.font.merriweather_medium,
    R.font.merriweather_bold,
)

/**
 * Типографика приложения: три независимых роли.
 *
 * Заголовки — `display*`, `headline*`, `title*`: заголовок треда в шапке,
 * название доски, тема ОП-поста, заголовок карточки в ленте.
 *
 * Мета-данные — `label*`: ник, трип, дата и номер поста, счётчики постов
 * и файлов, подписи под настройками.
 *
 * Текст — `body*`: сам текст поста и описания в настройках.
 *
 * Роли разведены по стилям, а не по экранам, поэтому одного выбора хватает
 * всему приложению: экраны и так берут готовый стиль из темы.
 */
fun dvachTypography(
    heading: FontChoice,
    meta: FontChoice,
    body: FontChoice,
    headingScalePercent: Int = FONT_SCALE_DEFAULT,
    metaScalePercent: Int = FONT_SCALE_DEFAULT,
    bodyScalePercent: Int = FONT_SCALE_DEFAULT,
    headingBold: Boolean = false,
    metaBold: Boolean = false,
    bodyBold: Boolean = false,
    bodyLineSpacingPercent: Int = LINE_SPACING_DEFAULT,
): Typography {
    val base = Typography()
    val headingFamily = heading.fontFamily
    val metaFamily = meta.fontFamily
    val bodyFamily = body.fontFamily
    val headingScale = clampFontScale(headingScalePercent)
    val metaScale = clampFontScale(metaScalePercent)
    val bodyScale = clampFontScale(bodyScalePercent)
    val bodyLineSpacing = clampLineSpacing(bodyLineSpacingPercent)
    return Typography(
        displayLarge = base.displayLarge.forRole(headingFamily, headingScale, headingBold),
        displayMedium = base.displayMedium.forRole(headingFamily, headingScale, headingBold),
        displaySmall = base.displaySmall.forRole(headingFamily, headingScale, headingBold),
        headlineLarge = base.headlineLarge.forRole(headingFamily, headingScale, headingBold),
        headlineMedium = base.headlineMedium.forRole(headingFamily, headingScale, headingBold),
        headlineSmall = base.headlineSmall.forRole(headingFamily, headingScale, headingBold),
        titleLarge = base.titleLarge.forRole(headingFamily, headingScale, headingBold),
        titleMedium = base.titleMedium.forRole(headingFamily, headingScale, headingBold),
        titleSmall = base.titleSmall.forRole(headingFamily, headingScale, headingBold),
        labelLarge = base.labelLarge.forRole(metaFamily, metaScale, metaBold),
        labelMedium = base.labelMedium.forRole(metaFamily, metaScale, metaBold),
        labelSmall = base.labelSmall.forRole(metaFamily, metaScale, metaBold),
        // Интервал применяется после масштаба: ползунок размера уже растянул
        // lineHeight вместе с кеглем, а здесь он тянется ещё раз — отдельно.
        bodyLarge = base.bodyLarge.forRole(bodyFamily, bodyScale, bodyBold)
            .lineSpaced(bodyLineSpacing),
        bodyMedium = base.bodyMedium.forRole(bodyFamily, bodyScale, bodyBold)
            .lineSpaced(bodyLineSpacing),
        bodySmall = base.bodySmall.forRole(bodyFamily, bodyScale, bodyBold)
            .lineSpaced(bodyLineSpacing),
    )
}

/** Семейство, размер и начертание одной роли — в одном месте, чтобы не забыть. */
private fun TextStyle.forRole(
    family: FontFamily,
    scalePercent: Int,
    bold: Boolean,
): TextStyle {
    val styled = copy(fontFamily = family).scaled(scalePercent)
    return if (bold) styled.copy(fontWeight = FontWeight.Bold) else styled
}

/**
 * Масштабирует размер и межстрочный интервал стиля. Интервал растёт вместе
 * с кеглем, иначе крупный текст слипается, а мелкий «разъезжается».
 *
 * Отступы между буквами тоже в sp, поэтому тянутся следом — иначе после
 * увеличения подписи выглядели бы собранными в кучу.
 */
internal fun TextStyle.scaled(percent: Int): TextStyle {
    if (percent == FONT_SCALE_DEFAULT) return this
    val factor = percent / 100f
    return copy(
        fontSize = fontSize.scaledBy(factor),
        lineHeight = lineHeight.scaledBy(factor),
        letterSpacing = letterSpacing.scaledBy(factor),
    )
}

/**
 * Меняет только межстрочный интервал стиля, не трогая кегль.
 *
 * Отдельная настройка чтения: размер строки выбирается ползунком шрифта, а
 * плотность вёрстки — этим множителем. Стили без заданного `lineHeight`
 * (`TextUnit.Unspecified`) пропускаются: выдумывать интервал там не из чего.
 */
internal fun TextStyle.lineSpaced(percent: Int): TextStyle {
    if (percent == LINE_SPACING_DEFAULT) return this
    if (lineHeight == TextUnit.Unspecified) return this
    return copy(lineHeight = lineHeight * (percent / 100f))
}

private fun TextUnit.scaledBy(factor: Float): TextUnit =
    if (this == TextUnit.Unspecified) this else this * factor
