package dev.dvach.client

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.dvach.client.data.settings.FontChoice
import dev.dvach.client.data.settings.fontChoice
import dev.dvach.client.ui.theme.dvachTypography
import dev.dvach.client.ui.theme.fontFamily
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
    fun `every choice has a note and a distinct family`() {
        val families = FontChoice.entries.map { it.fontFamily }
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
