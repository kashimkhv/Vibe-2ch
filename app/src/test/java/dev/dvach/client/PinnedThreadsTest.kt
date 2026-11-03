package dev.dvach.client

import dev.dvach.client.data.withPinned
import dev.dvach.client.domain.model.ThreadSummary
import dev.dvach.client.domain.model.ThreadsPage
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Закреплённые треды живут только в `index.json`, поэтому первая страница —
 * это страница из `1.json` плюс пины, подставленные перед ней.
 */
class PinnedThreadsTest {

    @Test
    fun `pinned threads go before the page`() {
        val page = threadsPage(10, 20)
        val merged = page.withPinned(listOf(thread(1, sticky = true), thread(2, sticky = true)))

        assertEquals(listOf(1, 2, 10, 20), merged.threads.map { it.num })
    }

    @Test
    fun `a pinned thread already on the page is not duplicated`() {
        // index.json и 1.json — разные снимки движка, тред может быть в обоих.
        // Остаётся вариант из pins: у него точно выставлен признак закрепа.
        val page = threadsPage(10, 20)
        val merged = page.withPinned(listOf(thread(20, sticky = true)))

        assertEquals(listOf(20, 10), merged.threads.map { it.num })
        assertEquals(listOf(true, false), merged.threads.map { it.isSticky })
    }

    @Test
    fun `no pins leave the page untouched`() {
        val page = threadsPage(10, 20)
        val merged = page.withPinned(emptyList())

        assertEquals(page.threads, merged.threads)
        assertEquals(page.currentPage, merged.currentPage)
        assertEquals(page.threadsPerPage, merged.threadsPerPage)
        assertEquals(page.maxPages, merged.maxPages)
    }

    @Test
    fun `page counters survive the merge`() {
        val merged = threadsPage(10).withPinned(listOf(thread(1, sticky = true)))

        assertEquals(2, merged.currentPage)
        assertEquals(30, merged.threadsPerPage)
        assertEquals(7, merged.maxPages)
    }

    private fun threadsPage(vararg nums: Int) = ThreadsPage(
        currentPage = 2,
        isIndex = false,
        threads = nums.map { thread(it) },
        threadsPerPage = 30,
        maxPages = 7,
    )

    private fun thread(
        num: Int,
        sticky: Boolean = false,
        endless: Boolean = false,
    ) = ThreadSummary(
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
        isSticky = sticky,
        isClosed = false,
        attachments = emptyList(),
        replies = emptyList(),
        isEndless = endless,
    )
}
