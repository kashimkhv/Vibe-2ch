package io.github.kashimkhv.vibe2ch

import io.github.kashimkhv.vibe2ch.domain.compareVersions
import io.github.kashimkhv.vibe2ch.domain.isNewerVersion
import io.github.kashimkhv.vibe2ch.domain.versionNumbers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Сравнение версий: по нему решается, показать ли человеку обновление.
 * Ошибка в любую сторону заметна сразу — либо ложное «доступно», либо
 * пропущенный релиз.
 */
class UpdateVersionTest {

    @Test
    fun `version numbers ignore prefix and debug suffix`() {
        assertEquals(listOf(3, 1, 0), versionNumbers("v3.1.0"))
        assertEquals(listOf(3, 1, 0), versionNumbers("3.1.0-debug"))
        assertEquals(listOf(3, 2), versionNumbers("V3.2"))
    }

    @Test
    fun `patch and minor bumps are newer`() {
        assertTrue(isNewerVersion("3.1.1", "3.1.0"))
        assertTrue(isNewerVersion("3.2.0", "3.1.9"))
        assertTrue(isNewerVersion("4.0.0", "3.99.0"))
    }

    @Test
    fun `numbers compare by value, not by text`() {
        // Строковое сравнение поставило бы 3.10.0 раньше 3.9.1.
        assertTrue(isNewerVersion("3.10.0", "3.9.1"))
        assertEquals(0, compareVersions("3.10.0", "3.10"))
    }

    @Test
    fun `same release and debug build are equal`() {
        assertFalse(isNewerVersion("3.1.0", "3.1.0-debug"))
        assertFalse(isNewerVersion("v3.1.0", "3.1.0"))
    }

    @Test
    fun `older release is not offered`() {
        assertFalse(isNewerVersion("3.0.1", "3.1.0"))
    }

    @Test
    fun `pre-release suffix does not hide a release`() {
        // Предрелизы проект не публикует: 3.2.0-rc1 не считается новее 3.2.0.
        assertFalse(isNewerVersion("3.2.0-rc1", "3.2.0"))
    }
}
