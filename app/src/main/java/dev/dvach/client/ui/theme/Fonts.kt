package dev.dvach.client.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.dvach.client.R
import dev.dvach.client.data.settings.FontChoice

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
): Typography {
    val base = Typography()
    val headingFamily = heading.fontFamily
    val metaFamily = meta.fontFamily
    val bodyFamily = body.fontFamily
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = headingFamily),
        displayMedium = base.displayMedium.copy(fontFamily = headingFamily),
        displaySmall = base.displaySmall.copy(fontFamily = headingFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = headingFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = headingFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = headingFamily),
        titleLarge = base.titleLarge.copy(fontFamily = headingFamily),
        titleMedium = base.titleMedium.copy(fontFamily = headingFamily),
        titleSmall = base.titleSmall.copy(fontFamily = headingFamily),
        labelLarge = base.labelLarge.copy(fontFamily = metaFamily),
        labelMedium = base.labelMedium.copy(fontFamily = metaFamily),
        labelSmall = base.labelSmall.copy(fontFamily = metaFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = bodyFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = bodyFamily),
        bodySmall = base.bodySmall.copy(fontFamily = bodyFamily),
    )
}
