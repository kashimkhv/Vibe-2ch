package dev.dvach.client

import dev.dvach.client.data.remote.dto.PostDto
import dev.dvach.client.data.remote.dto.ThreadDto
import dev.dvach.client.data.toDomain
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
}
