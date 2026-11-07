package dev.dvach.client

import dev.dvach.client.util.safeFileName
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Имя файла из ответа движка идёт в `File(dir, name)` и в `MediaStore`.
 * Если в нём окажется путь (`../../…`, `C:\…`), запись уйдёт за пределы
 * папки приложения, поэтому проверяем, что остаётся только сам файл.
 */
class FileNameTest {

    @Test
    fun `drops unix path from name`() {
        assertEquals("render.webp", safeFileName("../../Pictures/render.webp"))
    }

    @Test
    fun `drops windows path from name`() {
        assertEquals("photo.jpg", safeFileName("C:\\Users\\anon\\photo.jpg"))
    }

    @Test
    fun `replaces dot segments with fallback`() {
        assertEquals("file", safeFileName(".."))
        assertEquals("file", safeFileName("."))
        assertEquals("file", safeFileName(""))
    }

    @Test
    fun `removes control characters`() {
        assertEquals("abcdef.jpg", safeFileName("abc\u0000def.jpg"))
        assertEquals("file.jpg", safeFileName("\u0000file.jpg\n"))
    }

    @Test
    fun `keeps extension when cutting a long name`() {
        val long = "a".repeat(300) + ".webm"
        val safe = safeFileName(long)
        assertEquals(120, safe.length)
        assertEquals(".webm", safe.takeLast(5))
    }

    @Test
    fun `usual name stays as is`() {
        assertEquals("17667019796322.mp4", safeFileName("17667019796322.mp4"))
    }
}
