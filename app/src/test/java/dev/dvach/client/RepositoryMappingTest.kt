package dev.dvach.client

import dev.dvach.client.data.remote.dto.FileDto
import dev.dvach.client.data.remote.dto.PostDto
import dev.dvach.client.data.remote.dto.ThreadDto
import dev.dvach.client.data.toDomain
import dev.dvach.client.data.toThreadPosts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Маппинг DTO → домен: движок отдаёт имена и темы с HTML-сущностями
 * («Аноним&nbsp;»), а в UI они рисуются как обычный текст — раскрывать их
 * нужно на границе, иначе в шапке поста видно `&nbsp;`.
 */
class RepositoryMappingTest {

    @Test
    fun `post name and subject are decoded from html entities`() {
        val post = PostDto(
            num = 1,
            board = "b",
            name = "Аноним&nbsp;",
            subject = "Тема&nbsp;треда",
            trip = "!!&amp;tri",
        ).toDomain("b")

        assertEquals("Аноним", post.name)
        assertEquals("Тема треда", post.subject)
        assertEquals("!!&tri", post.trip)
    }

    @Test
    fun `thread summary uses decoded name and subject of the op post`() {
        val thread = ThreadDto(
            posts = listOf(
                PostDto(
                    num = 42,
                    name = "Аноним&nbsp;",
                    subject = "Новости&nbsp;дня",
                    comment = "<p>Текст</p>",
                    isOp = 1,
                ),
            ),
        ).toDomain("b")

        assertNotNull(thread)
        assertEquals("Новости дня", thread!!.subject)
        assertEquals("Аноним", thread.name)
    }

    @Test
    fun `blank trip becomes null and blank name stays blank`() {
        val post = PostDto(num = 1, name = "", subject = "&nbsp;", trip = "&nbsp;").toDomain("b")

        assertEquals("", post.name)
        assertEquals("", post.subject)
        assertNull(post.trip)
    }

    @Test
    fun `span markup and anonymous id prefix are removed from name`() {
        // /d/ присылает имя анонима разметкой: «Аноним&nbsp;ID:&nbsp;<span …>Имя</span>».
        // В шапке поста это обычный текст, а приставка «Аноним ID:» одинакова у
        // всех и только съедает ширину — остаётся сам ник.
        val post = PostDto(
            num = 1,
            name = "Аноним&nbsp;ID:&nbsp;<span id=\"id_tag_c91eae86\" " +
                "style=\"color:rgb(86,168,79);\">Ехидный&nbsp;Вертер</span>",
        ).toDomain("d")

        assertEquals("Ехидный Вертер", post.name)
    }

    @Test
    fun `plain anonymous name is not touched`() {
        // На остальных досках имя — обычный «Аноним»; его трогать нельзя.
        assertEquals("Аноним", PostDto(num = 1, name = "Аноним&nbsp;").toDomain("b").name)
        assertEquals(
            "Аноним",
            PostDto(
                num = 1,
                name = "Аноним&nbsp;ID:&nbsp;",
            ).toDomain("d").name,
        )
    }

    @Test
    fun `anonymous id without nickname keeps its id`() {
        // Без ника (например, короткий ID в /b/) приставку тоже убираем,
        // но сам идентификатор показываем.
        assertEquals(
            "3fA2b",
            PostDto(num = 1, name = "Аноним&nbsp;ID:&nbsp;3fA2b").toDomain("b").name,
        )
    }

    @Test
    fun `attachment type is recognized by extension`() {
        val post = PostDto(
            num = 1,
            files = listOf(
                FileDto(path = "/b/src/1/2.gif", name = "анимация.gif"),
                FileDto(path = "/b/src/1/3.GIF", name = "верхний регистр.gif"),
                FileDto(path = "/b/src/1/4.webm", name = "видео.webm", duration = "00:00:12"),
                FileDto(path = "/b/src/1/5.jpg", name = "фото.jpg"),
            ),
        ).toDomain("b")

        // Оба значка в углу превью берутся из этих признаков: gif — анимация,
        // webm — видео, jpg не получает ни того, ни другого.
        assertEquals(listOf(true, true, false, false), post.files.map { it.isAnimated })
        assertEquals(listOf(false, false, true, false), post.files.map { it.isVideo })
        assertEquals("gif", post.files[0].extension)
    }

    @Test
    fun `first post of a thread is the op even though api sends op 0`() {
        // В ответе /res/{num}.json у всех постов `op`: 0, включая стартовый.
        val posts = listOf(
            PostDto(num = 100, name = "Аноним", isOp = 0),
            PostDto(num = 101, name = "Аноним", isOp = 0),
            PostDto(num = 102, name = "Аноним"),
        ).toThreadPosts("b")

        assertEquals(listOf(true, false, false), posts.map { it.isOp })
    }

    @Test
    fun `thread posts keep their order and board`() {
        val posts = listOf(
            PostDto(num = 100, comment = "<p>ОП</p>"),
            PostDto(num = 101, comment = "<p>ответ</p>"),
        ).toThreadPosts("b")

        assertEquals(listOf(100, 101), posts.map { it.num })
        assertEquals(listOf("b", "b"), posts.map { it.boardId })
    }
}
