package io.github.kashimkhv.vibe2ch

import io.github.kashimkhv.vibe2ch.util.quoteText
import org.junit.Assert.assertEquals
import org.junit.Test

/** Текст поста для «Ответить с цитатой»: абзацы сохраняются, разметка — нет. */
class HtmlQuoteTest {

    @Test
    fun `line breaks survive as new lines`() {
        assertEquals("первая\nвторая", quoteText("первая<br>вторая"))
    }

    @Test
    fun `tags are stripped and entities decoded`() {
        assertEquals("кот & пёс", quoteText("<b>кот</b> &amp; пёс"))
    }

    @Test
    fun `paragraphs and extra whitespace are normalized`() {
        assertEquals("абзац\nвторой", quoteText("<p>абзац</p><p>  второй  </p>"))
        // Пустые строки схлопываются в один перевод строки, а не исчезают.
        assertEquals("одна\nстрока", quoteText("одна\n\n\nстрока"))
    }
}
