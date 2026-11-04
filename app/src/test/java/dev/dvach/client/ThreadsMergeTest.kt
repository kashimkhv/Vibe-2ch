package dev.dvach.client

import org.junit.Assert.assertEquals
import org.junit.Test
import dev.dvach.client.domain.model.ThreadSummary
import dev.dvach.client.ui.threads.mergeFirstPage
import dev.dvach.client.ui.threads.mergeThreads

class ThreadsMergeTest {

    private fun thread(num: Int) = ThreadSummary(
        boardId = "b",
        num = num,
        subject = "Тред $num",
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

    @Test
    fun `next page is appended after the loaded one`() {
        val merged = mergeThreads(listOf(thread(1), thread(2)), listOf(thread(3)))
        assertEquals(listOf(1, 2, 3), merged.map { it.num })
    }

    @Test
    fun `thread that moved between pages is not duplicated`() {
        // Пока листали, тред с бампом переехал на следующую страницу.
        val merged = mergeThreads(listOf(thread(1), thread(2)), listOf(thread(2), thread(3)))
        assertEquals(listOf(1, 2, 3), merged.map { it.num })
    }

    @Test
    fun `empty page keeps the list as is`() {
        val loaded = listOf(thread(1), thread(2))
        assertEquals(loaded, mergeThreads(loaded, emptyList()))
    }

    @Test
    fun `quiet refresh replaces the list when only the first page is loaded`() {
        // Тред 2 умер на движке: без догруженных страниц свежая первая
        // страница — это весь список, поэтому карточка должна исчезнуть.
        val merged = mergeFirstPage(
            loaded = listOf(thread(1), thread(2), thread(3)),
            fresh = listOf(thread(1), thread(3)),
            keepLoadedTail = false,
        )
        assertEquals(listOf(1, 3), merged.map { it.num })
    }

    @Test
    fun `quiet refresh puts the fresh page first and keeps loaded pages behind it`() {
        val merged = mergeFirstPage(
            loaded = listOf(thread(1), thread(2), thread(3), thread(4)),
            fresh = listOf(thread(2), thread(1)),
            keepLoadedTail = true,
        )
        assertEquals(listOf(2, 1, 3, 4), merged.map { it.num })
    }

    @Test
    fun `quiet refresh takes moved threads from the fresh page`() {
        // Тред 3 догрузили второй страницей, но с бампом он приехал в первую:
        // показываем его на новом месте, а не дважды.
        val merged = mergeFirstPage(
            loaded = listOf(thread(1), thread(2), thread(3)),
            fresh = listOf(thread(3), thread(1)),
            keepLoadedTail = true,
        )
        assertEquals(listOf(3, 1, 2), merged.map { it.num })
    }
}
