package io.github.kashimkhv.vibe2ch

import io.github.kashimkhv.vibe2ch.domain.ThreadSort
import io.github.kashimkhv.vibe2ch.domain.lastReplyAt
import io.github.kashimkhv.vibe2ch.domain.replyRate
import io.github.kashimkhv.vibe2ch.domain.sortThreads
import io.github.kashimkhv.vibe2ch.domain.model.Post
import io.github.kashimkhv.vibe2ch.domain.model.ThreadSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Сортировка списка тредов: направление, закреплённые сверху и тай-брейк
 * по номеру. Данные синтетические — важна математика, а не живой движок.
 */
class ThreadSortTest {

    private fun thread(
        num: Int,
        subject: String = "",
        comment: String = "",
        posts: Int = 1,
        files: Int = 0,
        createdAt: Long = 0L,
        lastReply: Long = createdAt,
        sticky: Boolean = false,
    ) = ThreadSummary(
        boardId = "po",
        num = num,
        subject = subject,
        commentHtml = comment,
        name = "Аноним",
        date = "",
        timestamp = createdAt,
        postsCount = posts,
        filesCount = files,
        views = 0,
        isSticky = sticky,
        isClosed = false,
        attachments = emptyList(),
        replies = if (lastReply > createdAt) {
            listOf(
                Post(
                    boardId = "po",
                    num = num + 1,
                    parent = num,
                    name = "Аноним",
                    subject = "",
                    commentHtml = "",
                    date = "",
                    timestamp = lastReply,
                    trip = null,
                    isOp = false,
                    isSticky = false,
                    isClosed = false,
                    isBanned = false,
                    files = emptyList(),
                ),
            )
        } else {
            emptyList()
        },
    )

    private val threads = listOf(
        thread(num = 1, subject = "Бананы", posts = 10, files = 2, createdAt = 100, lastReply = 400),
        thread(num = 2, subject = "Автомобили", posts = 30, files = 0, createdAt = 300, lastReply = 700),
        thread(num = 3, subject = "Виноград", posts = 5, files = 7, createdAt = 200, lastReply = 250),
    )

    @Test
    fun `bump order keeps the engine order`() {
        assertEquals(listOf(1, 2, 3), sortThreads(threads, ThreadSort.BUMP, false).map { it.num })
        assertEquals(listOf(1, 2, 3), sortThreads(threads, ThreadSort.BUMP, true).map { it.num })
    }

    @Test
    fun `sticky threads stay on top in any order`() {
        val withSticky = threads + thread(num = 4, subject = "Якорь", posts = 0, sticky = true)
        /// Закреплённый не должен попадать в конец даже при сортировке по алфавиту.
        assertEquals(4, sortThreads(withSticky, ThreadSort.ALPHABET, true).first().num)
        assertEquals(4, sortThreads(withSticky, ThreadSort.REPLIES, false).first().num)
    }

    @Test
    fun `replies sort by count in both directions`() {
        assertEquals(
            listOf(2, 1, 3),
            sortThreads(threads, ThreadSort.REPLIES, ascending = false).map { it.num },
        )
        assertEquals(
            listOf(3, 1, 2),
            sortThreads(threads, ThreadSort.REPLIES, ascending = true).map { it.num },
        )
    }

    @Test
    fun `created date puts new threads first by default`() {
        assertEquals(
            listOf(2, 3, 1),
            sortThreads(threads, ThreadSort.CREATED, ascending = false).map { it.num },
        )
    }

    @Test
    fun `last reply is the newest reply and falls back to the op date`() {
        // На этом времени стоит «скорость ответов», отдельного порядка по нему нет.
        assertEquals(700L, lastReplyAt(threads[1]))
        assertEquals(300L, lastReplyAt(thread(9, createdAt = 300)))
    }

    @Test
    fun `images sort by file count`() {
        assertEquals(listOf(3, 1, 2), sortThreads(threads, ThreadSort.IMAGES, false).map { it.num })
    }

    @Test
    fun `alphabet ignores case and falls back to the comment`() {
        val unnamed = thread(num = 5, comment = "<b>ёжики</b> в тумане")
        val sorted = sortThreads(threads + unnamed, ThreadSort.ALPHABET, ascending = true).map { it.num }
        // «Автомобили», «Бананы», «Виноград», затем тред без темы — по тексту «ёжики».
        assertEquals(listOf(2, 1, 3, 5), sorted)
    }

    @Test
    fun `reply rate is posts per hour of the thread life`() {
        val lively = thread(num = 1, posts = 48, createdAt = 0, lastReply = 24 * 3_600L)
        val dead = thread(num = 2, posts = 48, createdAt = 0, lastReply = 24 * 100 * 3_600L)
        // 48 постов за сутки жизни — два в час; за сто суток — 0.02.
        assertEquals(2.0, replyRate(lively), 0.001)
        assertTrue(replyRate(lively) > replyRate(dead))
        // Тред, проживший меньше часа, не должен делить на ноль и раздуваться.
        val instant = thread(num = 3, posts = 3, createdAt = 5_000, lastReply = 5_000)
        assertEquals(3.0, replyRate(instant), 0.001)
    }

    @Test
    fun `equal values keep a stable order by thread number`() {
        val twins = listOf(
            thread(num = 30, posts = 5),
            thread(num = 10, posts = 5),
            thread(num = 20, posts = 5),
        )
        assertEquals(
            listOf(10, 20, 30),
            sortThreads(twins, ThreadSort.REPLIES, ascending = true).map { it.num },
        )
    }
}
