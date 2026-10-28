package dev.dvach.client

import org.junit.Assert.assertEquals
import org.junit.Test
import dev.dvach.client.domain.model.ThreadSummary
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
}
