package io.github.kashimkhv.vibe2ch

import io.github.kashimkhv.vibe2ch.data.remote.rewriteDvachUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Подмена домена в перехватчике: запросы к движку уходят на выбранный домен,
 * а чужие (GitHub, внешние картинки) не трогаются. Путь, параметры и схема
 * остаются прежними — иначе запрос перестал бы быть тем же запросом.
 */
class DvachUrlsTest {

    private val aliases = setOf("2ch.hk", "2ch.life", "2ch.su")

    @Test
    fun `engine request goes to the chosen host`() {
        val url = rewriteDvachUrl(
            "https://2ch.hk/b/index.json?page=2".toHttpUrl(),
            "2ch.life",
            aliases,
        )

        assertEquals("https://2ch.life/b/index.json?page=2", url.toString())
    }

    @Test
    fun `image on another mirror goes to the chosen host`() {
        val url = rewriteDvachUrl(
            "https://2ch.su/e/thumb/1/2s.jpg".toHttpUrl(),
            "2ch.life",
            aliases,
        )

        assertEquals("https://2ch.life/e/thumb/1/2s.jpg", url.toString())
    }

    @Test
    fun `request already on the chosen host is left alone`() {
        assertNull(
            rewriteDvachUrl("https://2ch.life/b/index.json".toHttpUrl(), "2ch.life", aliases),
        )
    }

    @Test
    fun `foreign host is left alone`() {
        assertNull(
            rewriteDvachUrl("https://api.github.com/repos/1".toHttpUrl(), "2ch.life", aliases),
        )
        assertNull(
            rewriteDvachUrl("https://example.com/pic.jpg".toHttpUrl(), "2ch.life", aliases),
        )
    }

    @Test
    fun `custom host works the same way`() {
        val custom = setOf("2ch.hk", "2ch.example")
        assertEquals(
            "https://2ch.example/b/res/1.json",
            rewriteDvachUrl(
                "https://2ch.hk/b/res/1.json".toHttpUrl(),
                "2ch.example",
                custom,
            ).toString(),
        )
    }
}
