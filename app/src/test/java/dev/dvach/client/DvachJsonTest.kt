package dev.dvach.client

import dev.dvach.client.data.remote.dto.BoardDto
import dev.dvach.client.data.remote.dto.ThreadsPageDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Фикстуры сняты с живого API 2ch.hk в октябре 2026:
 *  - boards.json — GET /api/mobile/v2/boards
 *  - index.json  — GET /b/index.json
 *  - thread.json — GET /b/res/{num}.json
 *
 * Тест ловит расхождения между моделями и реальным ответом движка
 * (переименованные поля, null вместо строк и т.п.).
 */
class DvachJsonTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        explicitNulls = false
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/$name")) { "fixture $name not found" }
            .readBytes()
            .decodeToString()

    @Test
    fun `boards endpoint decodes`() {
        val boards = json.decodeFromString<List<BoardDto>>(fixture("boards.json"))

        assertTrue("ожидали много досок, получили ${boards.size}", boards.size > 50)
        val random = boards.first { it.id == "b" }
        assertTrue(random.enablePosting)
        assertTrue(random.threadsPerPage in 5..100)
        assertTrue(random.category.isNotBlank())
    }

    @Test
    fun `index page decodes with threads and posts`() {
        val page = json.decodeFromString<ThreadsPageDto>(fixture("index.json"))

        assertTrue(page.isBoard || page.isIndex)
        assertTrue(page.threads.isNotEmpty())
        // Активность доски движок присылает в каждом файле страницы: у фикстуры
        // `index.json` это положительное число, и оно должно дожить до UI.
        assertTrue("board_speed должен разбираться", page.boardSpeed > 0)
        val thread = page.threads.first()
        assertTrue("у треда должен быть ОП-пост", thread.posts.isNotEmpty())
        val op = thread.posts.first()
        assertTrue("номер поста должен быть положительным", op.num > 0)
        assertTrue("у ОП обычно есть html-комментарий", op.comment.isNotEmpty())
    }

    @Test
    fun `thread page decodes with files`() {
        val page = json.decodeFromString<ThreadsPageDto>(fixture("thread.json"))

        val thread = page.threads.first()
        assertTrue(thread.posts.isNotEmpty())
        assertEquals(page.currentThread, thread.posts.first().num)

        val withFiles = thread.posts.firstOrNull { it.files.isNotEmpty() }
        if (withFiles != null) {
            val file = withFiles.files.first()
            assertTrue("путь к файлу абсолютный или относительный", file.path.isNotEmpty())
            assertTrue(file.size > 0)
        }
    }
}
