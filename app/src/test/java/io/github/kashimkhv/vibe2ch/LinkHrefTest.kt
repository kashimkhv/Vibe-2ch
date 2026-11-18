package io.github.kashimkhv.vibe2ch

import io.github.kashimkhv.vibe2ch.util.decodeLinkHref
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
    fun `board relative url points to the chosen domain`() {
        assertEquals(
            "https://2ch.life/b/res/123.html#45",
            decodeLinkHref("/b/res/123.html#45", "https://2ch.life"),
        )
    }

    @Test
    fun `absolute mirror link is moved to the chosen domain`() {
        assertEquals(
            "https://2ch.life/b/res/1.html?x=2#5",
            decodeLinkHref("https://2ch.hk/b/res/1.html?x=2#5", "https://2ch.life"),
        )
        // Ссылка на www-зеркало — тот же движок, хост тоже подменяется.
        assertEquals(
            "https://2ch.life/e/index.html",
            decodeLinkHref("https://www.2ch.su/e/index.html", "https://2ch.life"),
        )
    }

    @Test
    fun `foreign link keeps its host`() {
        assertEquals(
            "https://life.ru/p/1",
            decodeLinkHref("https://life.ru/p/1", "https://2ch.life"),
        )
        // 2ch.org — не зеркало движка, а другой сайт.
        assertEquals(
            "https://2ch.org/b/res/1.html",
            decodeLinkHref("https://2ch.org/b/res/1.html", "https://2ch.life"),
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
