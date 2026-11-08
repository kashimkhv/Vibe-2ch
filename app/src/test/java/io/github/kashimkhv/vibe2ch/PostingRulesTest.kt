package io.github.kashimkhv.vibe2ch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.kashimkhv.vibe2ch.domain.MAX_UPLOAD_BYTES
import io.github.kashimkhv.vibe2ch.domain.postingCommentError
import io.github.kashimkhv.vibe2ch.domain.quotePrefix
import io.github.kashimkhv.vibe2ch.domain.uploadError
import io.github.kashimkhv.vibe2ch.domain.uploadHint

/** Правила формы поста: что можно отправить и как это объяснить человеку. */
class PostingRulesTest {

    @Test
    fun `reply starts with a quote on its own line`() {
        // Движок понимает `>>123` только в начале строки.
        assertEquals(">>123\n", quotePrefix(123))
        assertEquals("", quotePrefix(0))
    }

    @Test
    fun `quote with text puts every line after a marker`() {
        assertEquals(">>7\n>привет\n>мир\n\n", quotePrefix(7, "привет\nмир"))
    }

    @Test
    fun `long quote is clipped and marked with an ellipsis`() {
        assertEquals(">>1\n>" + "а".repeat(10) + "…\n\n", quotePrefix(1, "а".repeat(20), limit = 10))
        // Режем по code points, чтобы не разорвать эмодзи на половинки.
        assertEquals(">>1\n>😀…\n\n", quotePrefix(1, "😀😀", limit = 1))
    }

    @Test
    fun `blank quote text leaves only the post number`() {
        assertEquals(">>5\n", quotePrefix(5, "  \n "))
    }

    @Test
    fun `empty comment is allowed only with a file`() {
        val maxComment = 15_000
        assertEquals(
            "Введите текст поста или прикрепите файл",
            postingCommentError(comment = "  \n ", hasFile = false, maxComment = maxComment),
        )
        // «Только картинку» движок принимает.
        assertNull(postingCommentError(comment = "", hasFile = true, maxComment = maxComment))
        assertNull(postingCommentError(comment = "текст", hasFile = false, maxComment = maxComment))
    }

    @Test
    fun `comment longer than the board limit is refused`() {
        val problem = postingCommentError(
            comment = "а".repeat(20),
            hasFile = false,
            maxComment = 10,
        )
        assertTrue(problem.orEmpty().contains("20 из 10"))
        // 0 — лимит неизвестен, длину не проверяем.
        assertNull(postingCommentError(comment = "а".repeat(1000), hasFile = false, maxComment = 0))
    }

    @Test
    fun `file without an extension is refused`() {
        assertEquals(
            "У файла нет расширения — движок его не примет",
            uploadError("screenshot", 1024, listOf("jpg", "png"), 0),
        )
    }

    @Test
    fun `file type must be allowed by the board`() {
        val problem = uploadError("clip.mkv", 1024, listOf(".jpg", "PNG", "webm"), 0)
        // Расширения в списке доски приходят и с точкой, и в разном регистре.
        assertTrue(problem.orEmpty().contains(".mkv"))
        assertTrue(problem.orEmpty().contains("jpg, png, webm"))
        assertNull(uploadError("cat.PNG", 1024, listOf("jpg", "png"), 0))
        // Пустой список — доска не ограничивает типы.
        assertNull(uploadError("cat.whatever", 1024, emptyList(), 0))
    }

    @Test
    fun `file size is checked against the board and the client limit`() {
        val twentyMbKb = 20 * 1024
        assertTrue(
            uploadError("video.webm", 21 * 1024 * 1024, listOf("webm"), twentyMbKb)
                .orEmpty()
                .contains("больше 20 МБ"),
        )
        assertNull(uploadError("video.webm", 19 * 1024 * 1024, listOf("webm"), twentyMbKb))

        // Доска лимит не задала (0) — остаётся клиентский потолок.
        assertTrue(
            uploadError("video.webm", MAX_UPLOAD_BYTES + 1, listOf("webm"), 0)
                .orEmpty()
                .contains("больше ${MAX_UPLOAD_BYTES / (1024 * 1024)} МБ"),
        )
    }

    @Test
    fun `upload hint lists types and size`() {
        assertEquals("jpg, png · до 20 МБ", uploadHint(listOf(".jpg", "png", "jpg"), 20 * 1024))
        // Лимит меньше мегабайта показываем в килобайтах.
        assertEquals("gif · до 512 КБ", uploadHint(listOf("gif"), 512))
        // Лимита нет — говорим про клиентский потолок.
        assertEquals(
            "до ${MAX_UPLOAD_BYTES / (1024 * 1024)} МБ",
            uploadHint(emptyList(), 0),
        )
    }
}
