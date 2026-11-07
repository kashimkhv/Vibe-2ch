package io.github.kashimkhv.vibe2ch

import org.junit.Assert.assertEquals
import org.junit.Test
import io.github.kashimkhv.vibe2ch.ui.thread.plainText
import io.github.kashimkhv.vibe2ch.util.attachmentFileName
import io.github.kashimkhv.vibe2ch.util.decodeHtmlEntities
import io.github.kashimkhv.vibe2ch.util.kilobytesToBytes
import io.github.kashimkhv.vibe2ch.util.plural
import io.github.kashimkhv.vibe2ch.util.formatCount
import io.github.kashimkhv.vibe2ch.util.formatFileSize
import io.github.kashimkhv.vibe2ch.util.imagesLabel
import io.github.kashimkhv.vibe2ch.util.postUrl
import io.github.kashimkhv.vibe2ch.util.formatPostTime
import io.github.kashimkhv.vibe2ch.util.formatTimestamp
import io.github.kashimkhv.vibe2ch.util.threadUrl
import io.github.kashimkhv.vibe2ch.util.videosLabel

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
    fun `thread url is the post url without the anchor`() {
        // «Поделиться» в контекстном меню отдаёт тред целиком, а не пост,
        // на котором открыли меню: ссылка без якоря — тот же адрес, что и
        // у `postUrl` с номером ОП-поста.
        assertEquals("https://2ch.hk/b/res/123.html", threadUrl("b", 123))
        // Ссылка на пост — это та же ссылка на тред плюс якорь.
        assertEquals(threadUrl("asylum", 42), postUrl("asylum", 42).substringBefore('#'))
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
    fun `attachment counters in the thread header are pluralized`() {
        assertEquals("1 картинка", imagesLabel(1))
        assertEquals("3 картинки", imagesLabel(3))
        assertEquals("11 картинок", imagesLabel(11))
        assertEquals("1 видео", videosLabel(1))
        assertEquals("5 видео", videosLabel(5))
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

    @Test
    fun `absolute dates keep the exact time`() {
        val ts = 1_796_000_000L
        // Ожидание считаем тем же форматтером: тест не должен зависеть
        // от часового пояса машины, где он запускается.
        assertEquals(formatTimestamp(ts), formatPostTime(ts, "01/11/26", relative = false))
    }

    @Test
    fun `relative dates say how long ago`() {
        val now = 1_796_000_000_000L
        val fiveMinutes = now / 1000 - 5 * 60
        assertEquals(
            "5 мин назад",
            formatPostTime(fiveMinutes, "01/11/26", relative = true, nowMillis = now),
        )
        assertEquals(
            "только что",
            formatPostTime(now / 1000 - 10, "01/11/26", relative = true, nowMillis = now),
        )
        assertEquals(
            "2 ч назад",
            formatPostTime(now / 1000 - 2 * 3600, "01/11/26", relative = true, nowMillis = now),
        )
        assertEquals(
            "3 дн назад",
            formatPostTime(now / 1000 - 3 * 86_400, "01/11/26", relative = true, nowMillis = now),
        )
    }

    @Test
    fun `month old posts fall back to the date without the time`() {
        val now = 1_796_000_000_000L
        val old = now / 1000 - 45L * 86_400
        val expected = formatTimestamp(old).substringBefore(' ')
        assertEquals(
            expected,
            formatPostTime(old, "01/11/26", relative = true, nowMillis = now),
        )
    }

    @Test
    fun `missing timestamp falls back to the engine date string`() {
        // Строка движка — московское время и свой формат: показываем как есть.
        assertEquals("01/11/26 Сб 19:33:14", formatPostTime(0, "01/11/26 Сб 19:33:14", relative = false))
        assertEquals("01/11/26 Сб 19:33:14", formatPostTime(0, "01/11/26 Сб 19:33:14", relative = true, nowMillis = 1_796_000_000_000L))
    }
}
