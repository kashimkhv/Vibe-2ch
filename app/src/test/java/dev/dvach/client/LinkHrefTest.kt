package dev.dvach.client

import dev.dvach.client.util.decodeLinkHref
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `href` из постов приходит с экранированными слэшами
 * (`https:&#47;&#47;life.ru&#47;…`): если отдать такую строку в `ACTION_VIEW`,
 * браузер ищет домен `&`, а не `life.ru`. Проверяем, что адрес раскрывается
 * до открытия, а мусорные схемы не уходят наружу.
 */
class LinkHrefTest {

    @Test
    fun `decodes numeric escapes in external url`() {
        val raw = "https:&#47;&#47;life.ru&#47;p&#47;svezhiy-opros-fom-vladimiru-putinu"
        assertEquals(
            "https://life.ru/p/svezhiy-opros-fom-vladimiru-putinu",
            decodeLinkHref(raw),
        )
    }

    @Test
    fun `decodes named escapes in query string`() {
        assertEquals(
            "https://example.com/search?q=test&lang=ru&page=2",
            decodeLinkHref("https://example.com/search?q=test&amp;lang=ru&amp;page=2"),
        )
    }

    @Test
    fun `keeps normal url as is`() {
        assertEquals(
            "https://2ch.org/b/res/337699284.html#bottom",
            decodeLinkHref("https://2ch.org/b/res/337699284.html#bottom"),
        )
    }

    @Test
    fun `protocol relative url gets https`() {
        assertEquals("https://example.com/p", decodeLinkHref("//example.com/p"))
    }

    @Test
    fun `board relative url points to dvach host`() {
        assertEquals(
            "https://2ch.hk/news/res/1.html",
            decodeLinkHref("/news/res/1.html"),
        )
    }

    @Test
    fun `mailto stays`() {
        assertEquals("mailto:someone@example.com", decodeLinkHref("mailto:someone@example.com"))
    }

    @Test
    fun `script and data schemes are dropped`() {
        assertNull(decodeLinkHref("javascript:alert(1)"))
        assertNull(decodeLinkHref("data:text/html;base64,AAAA"))
        assertNull(decodeLinkHref("file:///etc/passwd"))
    }

    @Test
    fun `blank href is dropped`() {
        assertNull(decodeLinkHref(null))
        assertNull(decodeLinkHref(""))
        assertNull(decodeLinkHref("   "))
        assertNull(decodeLinkHref("&nbsp;"))
    }
}
