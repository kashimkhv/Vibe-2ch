package dev.dvach.client

import dev.dvach.client.domain.model.ThreadSummary
import dev.dvach.client.ui.threads.withoutHidden
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Скрытые треды вычитаются из ленты доски до сортировки: иначе карточка
 * осталась бы в списке, а закреплённый скрытый тред продолжал бы занимать
 * верхнюю строку.
 */
class HiddenThreadsTest {

    @Test
    fun `hidden threads are removed from the feed`() {
        val threads = listOf(thread(10), thread(20), thread(30))

        assertEquals(
            listOf(10, 30),
            threads.withoutHidden(setOf(20)).map { it.num },
        )
    }

    @Test
    fun `order of the remaining threads is kept`() {
        val threads = listOf(thread(30), thread(10), thread(20))

        assertEquals(
            listOf(30, 20),
            threads.withoutHidden(setOf(10)).map { it.num },
        )
    }

    @Test
    fun `empty hidden set returns the same list`() {
        val threads = listOf(thread(10), thread(20))

        assertEquals(threads, threads.withoutHidden(emptySet()))
    }

    @Test
    fun `numbers of other boards do not hide anything`() {
        // В наборе лежат номера только текущей доски, но проверяем, что
        // чужой номер без совпадения ничего не вычитает.
        val threads = listOf(thread(10), thread(20))

        assertEquals(threads, threads.withoutHidden(setOf(99)))
    }

    private fun thread(num: Int) = ThreadSummary(
        boardId = "b",
        num = num,
        subject = "",
        commentHtml = "",
        name = "Аноним",
        date = "",
        timestamp = 0,
        postsCount = 0,
        filesCount = 0,
        views = 0,
        isSticky = false,
        isClosed = false,
        attachments = emptyList(),
        replies = emptyList(),
    )
}
