package io.github.kashimkhv.vibe2ch

import io.github.kashimkhv.vibe2ch.domain.DEFAULT_DVACH_HOST
import io.github.kashimkhv.vibe2ch.domain.DVACH_HOSTS
import io.github.kashimkhv.vibe2ch.domain.DVACH_MIRRORS
import io.github.kashimkhv.vibe2ch.domain.baseUrlOf
import io.github.kashimkhv.vibe2ch.domain.hostOfBaseUrl
import io.github.kashimkhv.vibe2ch.domain.normalizeDvachHost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Домен движка из настроек: в поле обычно вставляют адрес из браузера целиком,
 * а хранить и подставлять в запросы надо только хост. Заодно проверяем, что
 * мусор и чужие домены не принимаются молча.
 */
class DvachDomainTest {

    @Test
    fun `plain host stays as is`() {
        assertEquals("2ch.hk", normalizeDvachHost("2ch.hk"))
        assertEquals("2ch.life", normalizeDvachHost("2ch.life"))
    }

    @Test
    fun `full browser url is reduced to host`() {
        assertEquals("2ch.life", normalizeDvachHost("https://2ch.life/b/res/123.html?x=1#45"))
        assertEquals("2ch.su", normalizeDvachHost("http://2ch.su/"))
        assertEquals("2ch.hk", normalizeDvachHost("  https://2ch.hk  "))
    }

    @Test
    fun `www scheme and case are normalized`() {
        assertEquals("2ch.hk", normalizeDvachHost("HTTPS://WWW.2CH.HK/b/"))
        assertEquals("2ch.life", normalizeDvachHost("www.2ch.life"))
        // Точка в конце — как в DNS-записи, к домену не относится.
        assertEquals("2ch.hk", normalizeDvachHost("2ch.hk."))
    }

    @Test
    fun `port and credentials are dropped`() {
        assertEquals("2ch.hk", normalizeDvachHost("2ch.hk:8080"))
        assertEquals("2ch.hk", normalizeDvachHost("https://user:pass@2ch.hk/b/"))
    }

    @Test
    fun `cyrillic domain becomes punycode`() {
        assertEquals("xn--e1afmkfd.xn--p1ai", normalizeDvachHost("пример.рф"))
    }

    @Test
    fun `not a host is rejected`() {
        assertNull(normalizeDvachHost(null))
        assertNull(normalizeDvachHost(""))
        assertNull(normalizeDvachHost("   "))
        assertNull(normalizeDvachHost("localhost"))
        assertNull(normalizeDvachHost("192.168.1.1"))
        assertNull(normalizeDvachHost("2ch_hk"))
        assertNull(normalizeDvachHost("-2ch.hk"))
        assertNull(normalizeDvachHost("2ch..hk"))
        assertNull(normalizeDvachHost("https://"))
    }

    @Test
    fun `base url is https and host comes back`() {
        assertEquals("https://2ch.hk", baseUrlOf(DEFAULT_DVACH_HOST))
        assertEquals("2ch.life", hostOfBaseUrl("https://2ch.life"))
        assertEquals("2ch.hk", hostOfBaseUrl(null))
        // Битое значение в настройках — не повод остаться без движка.
        assertEquals(DEFAULT_DVACH_HOST, hostOfBaseUrl("не домен"))
    }

    @Test
    fun `mirror list is consistent`() {
        assertTrue(DEFAULT_DVACH_HOST in DVACH_HOSTS)
        assertTrue(DVACH_HOSTS.containsAll(DVACH_MIRRORS.map { it.host }))
        // В списке нет повторов: иначе в диалоге было бы две одинаковые строки.
        assertEquals(DVACH_MIRRORS.size, DVACH_HOSTS.size)
    }
}
