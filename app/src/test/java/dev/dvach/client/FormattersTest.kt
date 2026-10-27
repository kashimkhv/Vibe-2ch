package dev.dvach.client

import org.junit.Assert.assertEquals
import org.junit.Test
import dev.dvach.client.ui.thread.plainText
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
