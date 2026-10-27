package dev.dvach.client

import org.junit.Assert.assertEquals
import org.junit.Test
import dev.dvach.client.ui.thread.plainText
import dev.dvach.client.util.attachmentFileName
import dev.dvach.client.util.decodeHtmlEntities
import dev.dvach.client.util.kilobytesToBytes
import dev.dvach.client.util.plural
import dev.dvach.client.util.formatCount
import dev.dvach.client.util.formatFileSize
import dev.dvach.client.util.postUrl

class FormattersTest {

    @Test
    fun `file size is human readable`() {
        assertEquals("", formatFileSize(0))
        assertEquals("512 Б", formatFileSize(512))
        assertEquals("1.0 КБ", formatFileSize(1024))
        assertEquals("1.5 МБ", formatFileSize(1024L * 1024 + 512 * 1024))
    }

    @Test
    fun `counts are compact`() {
        assertEquals("999", formatCount(999))
        assertEquals("1.2К", formatCount(1234))
        assertEquals("12.3К", formatCount(12_345))
    }

    @Test
    fun `post url points to the post anchor`() {
        assertEquals(
            "https://2ch.hk/b/res/123.html#456",
            postUrl("b", 123, 456),
        )
    }

    @Test
    fun `html entities decode numerically and by name`() {
        assertEquals("WebM/MP4", decodeHtmlEntities("WebM&#47;MP4"))
        assertEquals("a b", decodeHtmlEntities("a&nbsp;b"))
        assertEquals("«цитата»", decodeHtmlEntities("&laquo;цитата&raquo;"))
        assertEquals("—", decodeHtmlEntities("&#8212;"))
    }

    @Test
    fun `russian plurals follow the count`() {
        assertEquals("пост", plural(1, "пост", "поста", "постов"))
        assertEquals("поста", plural(3, "пост", "поста", "постов"))
        assertEquals("постов", plural(11, "пост", "поста", "постов"))
        assertEquals("постов", plural(25, "пост", "поста", "постов"))
        assertEquals("пост", plural(21, "пост", "поста", "постов"))
    }

    @Test
    fun `attachment name falls back to server name`() {
        val path = "/media/src/1170867/17917395327230382621.webm"
        // Файл загрузили без имени: движок отдаёт одно расширение.
        assertEquals(
            "17917395327230382621.webm",
            attachmentFileName(fullName = ".webm", displayName = ".webm", name = "17917395327230382621.webm", path = path),
        )
        // Имя без расширения — дописываем из пути.
        assertEquals(
            "image.png",
            attachmentFileName(fullName = "image", displayName = "image", name = "1791.png", path = "/b/src/1/1791.png"),
        )
        // Обрезанный displayname чистим от «[...]», если fullname пустой.
        assertEquals(
            "179300057599200.mp4",
            attachmentFileName("", "179300057599200[...].mp4", "1793.mp4", "/b/src/1/1793.mp4"),
        )
        // Обычный случай — исходное имя как есть.
        assertEquals(
            "кот.jpg",
            attachmentFileName("кот.jpg", "кот.jpg", "1791.jpg", "/b/src/1/1791.jpg"),
        )
    }

    @Test
    fun `makaba file size is kilobytes`() {
        // В API `size` = 632 для файла на 610 КБ, а не байты.
        assertEquals(647_168L, kilobytesToBytes(632))
        assertEquals("632.0 КБ", formatFileSize(kilobytesToBytes(632)))
        assertEquals("17.3 МБ", formatFileSize(kilobytesToBytes(17_720)))
    }

    @Test
    fun `html is stripped to plain text`() {
        assertEquals(
            "Привет мир",
            plainText("<p>Привет&nbsp;<b>мир</b></p>"),
        )
        assertEquals(
            "a > b & c",
            plainText("a &gt; b &amp; c"),
        )
    }
}
