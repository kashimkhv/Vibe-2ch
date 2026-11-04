package dev.dvach.client

import dev.dvach.client.data.remote.CookieStore
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
 */
class CookieStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val board = "https://2ch.hk".toHttpUrl()

    @Test
    fun `cookie from the response goes back to the same host`() {
        val store = CookieStore(file())

        store.saveFromResponse(board, listOf(cookie("passcode=abc; Path=/; Max-Age=31536000")))

        val sent = store.loadForRequest("https://2ch.hk/sex/index.json".toHttpUrl())
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
            afterRestart.loadForRequest("https://2ch.hk/b/index.json".toHttpUrl()).single().value,
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

        assertEquals(1, store.loadForRequest("https://2ch.hk/sex/res/1.json".toHttpUrl()).size)
        assertTrue(store.loadForRequest("https://2ch.hk/b/index.json".toHttpUrl()).isEmpty())
    }

    @Test
    fun `repeated cookie with the same name replaces the value`() {
        val store = CookieStore(file())
        store.saveFromResponse(board, listOf(cookie("passcode=old; Path=/; Max-Age=31536000")))
        store.saveFromResponse(board, listOf(cookie("passcode=new; Path=/; Max-Age=31536000")))

        assertEquals(1, store.count.value)
        assertEquals(
            "new",
            store.loadForRequest("https://2ch.hk/b/index.json".toHttpUrl()).single().value,
        )
    }

    @Test
    fun `expired cookie from the file is dropped on load`() {
        val file = file()
        file.writeText(
            "https://2ch.hk\tpasscode=old; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT\n",
        )

        val store = CookieStore(file)

        assertEquals(0, store.count.value)
        assertTrue(store.loadForRequest("https://2ch.hk/b/index.json".toHttpUrl()).isEmpty())
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
        assertTrue(store.loadForRequest("https://2ch.hk/b/index.json".toHttpUrl()).isEmpty())
        assertFalse(file.exists())
        assertEquals(0, CookieStore(file).count.value)
    }

    private fun file(): File = File(tmp.root, "cookies.txt")

    private fun cookie(header: String): Cookie = Cookie.parse(board, header)!!
}
