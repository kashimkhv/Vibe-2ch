package io.github.kashimkhv.vibe2ch

import io.github.kashimkhv.vibe2ch.util.releaseNotesText
import org.junit.Assert.assertEquals
import org.junit.Test

/** Release notes приходят в Markdown — в диалоге их надо показать читаемым текстом. */
class ReleaseNotesTest {

    @Test
    fun `headings, bold and bullets lose their markers`() {
        val notes = """
            ## Что нового

            - **Ответ с цитатой** — текст поста в форме.
            - Выделение текста.
        """.trimIndent()
        assertEquals(
            "Что нового\n\n• Ответ с цитатой — текст поста в форме.\n• Выделение текста.",
            releaseNotesText(notes),
        )
    }

    @Test
    fun `code fences are dropped but code stays`() {
        val notes = """
            Скачать:

            ```bash
            adb install Vibe-2ch.apk
            ```
        """.trimIndent()
        assertEquals("Скачать:\n\nadb install Vibe-2ch.apk", releaseNotesText(notes))
    }

    @Test
    fun `links read as text with the address in parentheses`() {
        assertEquals(
            "Код на GitHub (https://github.com/kashimkhv/Vibe-2ch).",
            releaseNotesText("Код на [GitHub](https://github.com/kashimkhv/Vibe-2ch)."),
        )
    }

    @Test
    fun `empty notes stay empty`() {
        assertEquals("", releaseNotesText(""))
        assertEquals("", releaseNotesText("  \n "))
    }

    @Test
    fun `extra blank lines collapse`() {
        assertEquals("первый\n\nвторой", releaseNotesText("первый\n\n\n\nвторой"))
    }
}
