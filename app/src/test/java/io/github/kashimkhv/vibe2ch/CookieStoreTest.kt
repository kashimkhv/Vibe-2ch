package io.github.kashimkhv.vibe2ch

import io.github.kashimkhv.vibe2ch.data.remote.CookieStore
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Куки движка: то, что пришло в `Set-Cookie`, должно уходить обратно — в том
 * числе на картинки и медиа — и переживать перезапуск. До этого OkHttp работал
 * с `CookieJar.NO_COOKIES`, и доступ к закрытым доскам терялся сразу после
 * ответа.
 *
 * Правила проверяются на нейтральном хосте, чтобы копия на зеркале (она есть
 * только у 2ch) не мешала считать куки; зеркала — отдельными тестами.
 */
class CookieStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val board = "https://board.test".toHttpUrl()

    @Test
    fun `cookie from the response goes back to the same host`() {
        val store = CookieStore(file())

        store.saveFromResponse(board, listOf(cookie("passcode=abc; Path=/; Max-Age=31536000")))

        val sent = store.loadForRequest("https://board.test/sex/index.json".toHttpUrl())
        assertEquals(1, sent.size)
        assertEquals("passcode", sent[0].name)
        assertEquals("abc", sent[0].value)
        assertEquals(1, store.count.value)
    }

    @Test
    fun `cookies survive a restart`() {
        val file = file()
        CookieStore(file).saveFromResponse(
            board,
            listOf(cookie("passcode=abc; Path=/; Max-Age=31536000")),
        )

        // Новый экземпляр — это ровно то, что делает перезапуск приложения.
        val afterRestart = CookieStore(file)

        assertEquals(1, afterRestart.count.value)
        assertEquals(
            "abc",
            afterRestart.loadForRequest("https://board.test/b/index.json".toHttpUrl())
                .single()
                .value,
        )
    }

    @Test
    fun `cookie is not sent to another host`() {
        val store = CookieStore(file())
        store.saveFromResponse(board, listOf(cookie("passcode=abc; Path=/; Max-Age=31536000")))

        assertTrue(store.loadForRequest("https://example.com/".toHttpUrl()).isEmpty())
    }

    @Test
    fun `cookie with a path is sent only inside that path`() {
        val store = CookieStore(file())
        store.saveFromResponse(board, listOf(cookie("access=1; Path=/sex; Max-Age=31536000")))

        assertEquals(
            1,
            store.loadForRequest("https://board.test/sex/res/1.json".toHttpUrl()).size,
        )
        assertTrue(store.loadForRequest("https://board.test/b/index.json".toHttpUrl()).isEmpty())
    }

    @Test
    fun `repeated cookie with the same name replaces the value`() {
        val store = CookieStore(file())
        store.saveFromResponse(board, listOf(cookie("passcode=old; Path=/; Max-Age=31536000")))
        store.saveFromResponse(board, listOf(cookie("passcode=new; Path=/; Max-Age=31536000")))

        assertEquals(1, store.count.value)
        assertEquals(
            "new",
            store.loadForRequest("https://board.test/b/index.json".toHttpUrl()).single().value,
        )
    }

    @Test
    fun `expired cookie from the file is dropped on load`() {
        val file = file()
        file.writeText(
            "https://board.test\tpasscode=old; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT\n",
        )

        val store = CookieStore(file)

        assertEquals(0, store.count.value)
        assertTrue(store.loadForRequest("https://board.test/b/index.json".toHttpUrl()).isEmpty())
        // Из файла просроченная строка тоже ушла, а не осталась до следующей записи:
        // кук не осталось — значит, и файла нет.
        assertFalse(file.exists())
    }

    @Test
    fun `clear removes cookies from memory and from the file`() {
        val file = file()
        val store = CookieStore(file)
        store.saveFromResponse(board, listOf(cookie("passcode=abc; Path=/; Max-Age=31536000")))

        store.clear()

        assertEquals(0, store.count.value)
        assertTrue(store.loadForRequest("https://board.test/b/index.json".toHttpUrl()).isEmpty())
        assertFalse(file.exists())
        assertEquals(0, CookieStore(file).count.value)
    }

    /**
     * Сам баг: `2ch.hk` отдаёт превью `301` на `2ch.su`, а кука доступа —
     * host-only для `2ch.hk`. Без учёта зеркала картинки закрытой доски
     * приходили `500` с HTML «Not found» вместо превью.
     */
    @Test
    fun `cookie saved on 2ch hk is also sent to the mirror`() {
        val store = CookieStore(file())
        store.saveFromResponse(
            "https://2ch.hk/user/posting?nc=1".toHttpUrl(),
            listOf(cookie("usercode_auth=abc; Path=/; Max-Age=31536000", "https://2ch.hk")),
        )

        val onMirror = store.loadForRequest(
            "https://2ch.su/e/thumb/856629/17735724925800067619s.jpg".toHttpUrl(),
        )

        assertEquals(1, onMirror.size)
        assertEquals("usercode_auth", onMirror[0].name)
        assertEquals("abc", onMirror[0].value)
        // Копии в хранилище нет: одна кука — одна строка в файле и одна в счётчике.
        assertEquals(1, store.count.value)
    }

    @Test
    fun `cookie saved on the mirror is also sent to 2ch hk`() {
        val store = CookieStore(file())
        store.saveFromResponse(
            "https://2ch.su".toHttpUrl(),
            listOf(cookie("usercode_auth=abc; Path=/; Max-Age=31536000", "https://2ch.su")),
        )

        assertEquals(
            1,
            store.loadForRequest("https://2ch.hk/e/index.json".toHttpUrl()).size,
        )
    }

    @Test
    fun `mirror works after a restart and with a file written earlier`() {
        val file = file()
        // Ровно то, что лежало на диске до этой правки: одна строка на 2ch.hk.
        file.writeText(
            "https://2ch.hk/user/posting?nc=1" +
                "\tusercode_auth=abc; expires=Thu, 04 Nov 2027 01:16:49 GMT; path=/\n",
        )

        val store = CookieStore(file)

        assertEquals(
            "abc",
            store.loadForRequest("https://2ch.su/e/thumb/1/2s.jpg".toHttpUrl()).single().value,
        )
        assertEquals(1, store.count.value)
    }

    @Test
    fun `cookie of a foreign host does not reach the mirror`() {
        val store = CookieStore(file())
        store.saveFromResponse(
            "https://example.com".toHttpUrl(),
            listOf(cookie("usercode_auth=abc; Path=/; Max-Age=31536000", "https://example.com")),
        )

        assertTrue(store.loadForRequest("https://2ch.su/e/index.json".toHttpUrl()).isEmpty())
        assertTrue(store.loadForRequest("https://2ch.hk/e/index.json".toHttpUrl()).isEmpty())
    }

    private fun file(): File = File(tmp.root, "cookies.txt")

    private fun cookie(header: String, url: String = "https://board.test"): Cookie =
        Cookie.parse(url.toHttpUrl(), header)!!
}
