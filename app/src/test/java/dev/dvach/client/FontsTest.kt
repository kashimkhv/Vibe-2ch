package dev.dvach.client

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.dvach.client.data.settings.FontChoice
import dev.dvach.client.data.settings.fontChoice
import dev.dvach.client.ui.theme.dvachTypography
import dev.dvach.client.ui.theme.fontFamily
import dev.dvach.client.util.FONT_SCALE_DEFAULT
import dev.dvach.client.util.FONT_SCALE_MAX
import dev.dvach.client.util.FONT_SCALE_MIN
import dev.dvach.client.util.LINE_SPACING_DEFAULT
import dev.dvach.client.util.LINE_SPACING_MAX
import dev.dvach.client.util.LINE_SPACING_MIN
import dev.dvach.client.util.TEXT_MARGIN_DEFAULT
import dev.dvach.client.util.TEXT_MARGIN_MAX
import dev.dvach.client.util.TEXT_MARGIN_MIN
import dev.dvach.client.util.clampFontScale
import dev.dvach.client.util.clampLineSpacing
import dev.dvach.client.util.clampTextMargin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FontsTest {

    private val key = stringPreferencesKey("font")

    @Test
    fun `saved choice is read back`() {
        val prefs = mutablePreferencesOf(key to "LORA")
        assertEquals(FontChoice.LORA, prefs.fontChoice(key))
    }

    @Test
    fun `missing key falls back to the system font`() {
        assertEquals(FontChoice.SYSTEM, mutablePreferencesOf().fontChoice(key))
    }

    @Test
    fun `unknown name falls back to the system font`() {
        // В DataStore могло остаться имя шрифта из будущей версии: настройки
        // должны открыться, а не упасть на valueOf.
        val prefs = mutablePreferencesOf(key to "COMIC_SANS")
        assertEquals(FontChoice.SYSTEM, prefs.fontChoice(key))
    }

    @Test
    fun `font size scales with the percent`() {
        // Кегль и межстрочный интервал растут вместе: иначе крупный текст
        // слипается. Мета-данные и текст живут по своим процентам.
        val base = Typography()
        val typography = dvachTypography(
            heading = FontChoice.SYSTEM,
            meta = FontChoice.SYSTEM,
            body = FontChoice.SYSTEM,
            headingScalePercent = 150,
            metaScalePercent = 75,
            bodyScalePercent = 100,
        )

        assertEquals(base.titleLarge.fontSize.value * 1.5f, typography.titleLarge.fontSize.value, 0.01f)
        assertEquals(base.titleLarge.lineHeight.value * 1.5f, typography.titleLarge.lineHeight.value, 0.01f)
        assertEquals(base.labelSmall.fontSize.value * 0.75f, typography.labelSmall.fontSize.value, 0.01f)
        // Текст постов остался на 100 % — чужие ползунки его не двигают.
        assertEquals(base.bodyMedium.fontSize.value, typography.bodyMedium.fontSize.value, 0.01f)
    }

    @Test
    fun `font scale is clamped`() {
        assertEquals(FONT_SCALE_MAX, clampFontScale(500))
        assertEquals(FONT_SCALE_MIN, clampFontScale(10))
        assertEquals(FONT_SCALE_DEFAULT, clampFontScale(100))
    }

    @Test
    fun `hundred percent keeps the default sizes`() {
        val base = Typography()
        val typography = dvachTypography(FontChoice.SYSTEM, FontChoice.SYSTEM, FontChoice.SYSTEM)
        assertEquals(base.titleMedium.fontSize.value, typography.titleMedium.fontSize.value, 0.01f)
        assertEquals(base.bodyLarge.lineHeight.value, typography.bodyLarge.lineHeight.value, 0.01f)
    }

    @Test
    fun `bold role is applied to its own styles only`() {
        // Жирность заголовков не должна жирнить мета-данные и текст постов:
        // иначе переключатель роли превращается в глобальный.
        val base = Typography()
        val typography = dvachTypography(
            heading = FontChoice.SYSTEM,
            meta = FontChoice.SYSTEM,
            body = FontChoice.SYSTEM,
            headingBold = true,
        )

        assertEquals(FontWeight.Bold, typography.titleLarge.fontWeight)
        assertEquals(FontWeight.Bold, typography.titleMedium.fontWeight)
        assertEquals(FontWeight.Bold, typography.headlineSmall.fontWeight)
        // У мета-данных и текста остались их собственные веса темы.
        assertEquals(base.labelLarge.fontWeight, typography.labelLarge.fontWeight)
        assertEquals(base.bodyLarge.fontWeight, typography.bodyLarge.fontWeight)
    }

    @Test
    fun `meta and body bold do not touch each other`() {
        val base = Typography()
        val typography = dvachTypography(
            heading = FontChoice.SYSTEM,
            meta = FontChoice.SYSTEM,
            body = FontChoice.SYSTEM,
            metaBold = true,
        )

        assertEquals(FontWeight.Bold, typography.labelSmall.fontWeight)
        assertEquals(FontWeight.Bold, typography.labelLarge.fontWeight)
        assertEquals(base.bodyMedium.fontWeight, typography.bodyMedium.fontWeight)
        assertEquals(base.titleLarge.fontWeight, typography.titleLarge.fontWeight)

        val bodyTypography = dvachTypography(
            heading = FontChoice.SYSTEM,
            meta = FontChoice.SYSTEM,
            body = FontChoice.SYSTEM,
            bodyBold = true,
        )
        assertEquals(FontWeight.Bold, bodyTypography.bodyLarge.fontWeight)
        assertEquals(base.labelLarge.fontWeight, bodyTypography.labelLarge.fontWeight)
    }

    @Test
    fun `bold off keeps the default weights`() {
        // Выключенный переключатель ничего не навязывает: остаются веса
        // типографики Material, где заголовки уже чуть плотнее текста.
        val base = Typography()
        val typography = dvachTypography(FontChoice.SYSTEM, FontChoice.SYSTEM, FontChoice.SYSTEM)
        assertEquals(base.titleLarge.fontWeight, typography.titleLarge.fontWeight)
        assertEquals(base.titleMedium.fontWeight, typography.titleMedium.fontWeight)
        assertEquals(base.labelLarge.fontWeight, typography.labelLarge.fontWeight)
        assertEquals(base.bodyLarge.fontWeight, typography.bodyLarge.fontWeight)
    }

    @Test
    fun `line spacing stretches body line height only`() {
        // Отдельная настройка чтения: плотность вёрстки меняется у текста постов,
        // а кегль, заголовки и мета-данные остаются как были.
        val base = Typography()
        val typography = dvachTypography(
            heading = FontChoice.SYSTEM,
            meta = FontChoice.SYSTEM,
            body = FontChoice.SYSTEM,
            bodyLineSpacingPercent = 150,
        )

        assertEquals(base.bodyLarge.fontSize.value, typography.bodyLarge.fontSize.value, 0.01f)
        assertEquals(
            base.bodyLarge.lineHeight.value * 1.5f,
            typography.bodyLarge.lineHeight.value,
            0.01f,
        )
        assertEquals(
            base.bodySmall.lineHeight.value * 1.5f,
            typography.bodySmall.lineHeight.value,
            0.01f,
        )
        // Заголовки и мета-данные настройка не трогает.
        assertEquals(base.titleLarge.lineHeight.value, typography.titleLarge.lineHeight.value, 0.01f)
        assertEquals(base.labelLarge.lineHeight.value, typography.labelLarge.lineHeight.value, 0.01f)
    }

    @Test
    fun `line spacing is applied after the font scale`() {
        // Сначала кегль, потом интервал: 150 % размера и 80 % интервала дают
        // строку короче обычной, хотя текст крупнее.
        val base = Typography()
        val typography = dvachTypography(
            heading = FontChoice.SYSTEM,
            meta = FontChoice.SYSTEM,
            body = FontChoice.SYSTEM,
            bodyScalePercent = 150,
            bodyLineSpacingPercent = 80,
        )

        assertEquals(base.bodyMedium.fontSize.value * 1.5f, typography.bodyMedium.fontSize.value, 0.01f)
        assertEquals(
            base.bodyMedium.lineHeight.value * 1.5f * 0.8f,
            typography.bodyMedium.lineHeight.value,
            0.01f,
        )
    }

    @Test
    fun `line spacing of a hundred percent changes nothing`() {
        val base = Typography()
        val typography = dvachTypography(
            heading = FontChoice.SYSTEM,
            meta = FontChoice.SYSTEM,
            body = FontChoice.SYSTEM,
            bodyLineSpacingPercent = 100,
        )

        assertEquals(base.bodyMedium.lineHeight.value, typography.bodyMedium.lineHeight.value, 0.01f)
        assertEquals(base.bodySmall.lineHeight.value, typography.bodySmall.lineHeight.value, 0.01f)
    }

    @Test
    fun `line spacing and text margins are clamped`() {
        // Значения приходят из DataStore, где могло остаться что угодно.
        assertEquals(LINE_SPACING_MAX, clampLineSpacing(1000))
        assertEquals(LINE_SPACING_MIN, clampLineSpacing(0))
        assertEquals(LINE_SPACING_DEFAULT, clampLineSpacing(100))
        assertEquals(TEXT_MARGIN_MAX, clampTextMargin(100))
        assertEquals(TEXT_MARGIN_MIN, clampTextMargin(-8))
        assertEquals(TEXT_MARGIN_DEFAULT, clampTextMargin(0))
        assertEquals(16, clampTextMargin(16))
    }

    @Test
    fun `every choice has a note and a distinct family`() {        val families = FontChoice.entries.map { it.fontFamily }
        assertTrue(FontChoice.entries.all { it.note.isNotBlank() })
        assertEquals("семейства не должны повторяться", families.size, families.toSet().size)
    }

    @Test
    fun `heading, meta and body families are applied independently`() {
        // Роли разведены по стилям темы: заголовки — title*, мета — label*,
        // текст — body*. Выбор одной роли не должен тянуть за собой две другие.
        val heading = FontChoice.MONTSERRAT
        val meta = FontChoice.OPEN_SANS
        val body = FontChoice.LORA
        val typography = dvachTypography(heading, meta, body)

        assertEquals(heading.fontFamily, typography.titleLarge.fontFamily)
        assertEquals(heading.fontFamily, typography.titleSmall.fontFamily)
        assertEquals(meta.fontFamily, typography.labelSmall.fontFamily)
        assertEquals(meta.fontFamily, typography.labelLarge.fontFamily)
        assertEquals(body.fontFamily, typography.bodyMedium.fontFamily)
        assertEquals(body.fontFamily, typography.bodyLarge.fontFamily)

        assertNotEquals(typography.titleLarge.fontFamily, typography.bodyMedium.fontFamily)
    }
}
