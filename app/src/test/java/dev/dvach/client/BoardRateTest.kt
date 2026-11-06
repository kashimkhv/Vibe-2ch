package dev.dvach.client

import dev.dvach.client.domain.boardActivity
import dev.dvach.client.domain.model.Post
import dev.dvach.client.domain.model.ThreadSummary
import dev.dvach.client.domain.model.ThreadsPage
import dev.dvach.client.domain.postsPerHourOf
import dev.dvach.client.domain.threadPostsPerHour
import dev.dvach.client.util.postsPerHourExactLabel
import dev.dvach.client.util.postsPerHourLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Активность доски: посты в час из трёх последних постов каждого треда.
 * Время синтетическое (в секундах, как у движка) — проверяется арифметика.
 */
class BoardRateTest {

    /** 2026-01-01 00:00:00 UTC: дальше считаем минутами и часами от него. */
    private val base = 1_767_225_600L
    private fun minutes(value: Long) = base + value * 60

    private fun reply(num: Int, at: Long) = Post(
        boardId = "b",
        num = num,
        parent = 0,
        name = "Аноним",
        subject = "",
        commentHtml = "",
        date = "",
        timestamp = at,
        trip = null,
        isOp = false,
        isSticky = false,
        isClosed = false,
        isBanned = false,
        files = emptyList(),
    )

    private fun thread(
        num: Int = 1,
        createdAt: Long = base,
        posts: Int = 4,
        replyTimes: List<Long> = emptyList(),
    ) = ThreadSummary(
        boardId = "b",
        num = num,
        subject = "",
        commentHtml = "",
        name = "Аноним",
        date = "",
        timestamp = createdAt,
        postsCount = posts,
        filesCount = 0,
        views = 0,
        isSticky = false,
        isClosed = false,
        attachments = emptyList(),
        replies = replyTimes.mapIndexed { index, at -> reply(num * 100 + index, at) },
    )

    @Test
    fun `steady thread keeps its average rate`() {
        // Четыре поста за два часа, последние три — через полчаса друг от друга:
        // и хвост, и средняя дают ровно 2 поста в час.
        val steady = thread(posts = 4, replyTimes = listOf(minutes(60), minutes(90), minutes(120)))
        assertEquals(2.0, threadPostsPerHour(steady), 1e-9)
    }

    @Test
    fun `board rate is the sum of thread rates`() {
        // 2 поста в час и 3 поста в час — вместе 5, а не «среднее по доске».
        val slow = thread(posts = 4, replyTimes = listOf(minutes(60), minutes(90), minutes(120)))
        val fast = thread(
            num = 2,
            posts = 12,
            replyTimes = listOf(minutes(200), minutes(220), minutes(240)),
        )
        assertEquals(5.0, postsPerHourOf(listOf(slow, fast)), 1e-9)
    }

    @Test
    fun `a burst is damped by the thread's own average`() {
        // Тред живёт двое суток и пишет ~10 постов в час, но выдал три поста
        // за 58 секунд. Без сглаживания это 124 поста в час — такая вспышка
        // одна перевесила бы всю доску. С априорным интервалом — около 27.
        val burst = thread(
            createdAt = base - 48 * 3_600L,
            posts = 497,
            replyTimes = listOf(base, base + 29, base + 58),
        )
        val rate = threadPostsPerHour(burst)
        assertTrue("вспышка не должна давать больше 40: $rate", rate < 40)
        assertTrue("но и среднюю она поднимает: $rate", rate > 10)
    }

    @Test
    fun `only the last three posts matter`() {
        // Старые ответы в хвост не попадают: у обоих тредов он один и тот же.
        val withEarlier = thread(
            replyTimes = listOf(minutes(0), minutes(60), minutes(120), minutes(123), minutes(126)),
        )
        val onlyTail = thread(replyTimes = listOf(minutes(120), minutes(123), minutes(126)))
        assertEquals(
            threadPostsPerHour(onlyTail),
            threadPostsPerHour(withEarlier),
            1e-9,
        )
    }

    @Test
    fun `without two timestamps the lifetime average is used`() {
        // Один ответ через 5 часов: интервала нет, берём среднюю за жизнь.
        // 11 постов за 5 часов = 2.2 в час.
        val single = thread(posts = 11, replyTimes = listOf(minutes(300)))
        assertEquals(2.2, threadPostsPerHour(single), 1e-9)
    }

    @Test
    fun `thread with only its opening post adds nothing`() {
        assertEquals(0.0, threadPostsPerHour(thread(posts = 1, replyTimes = emptyList())), 1e-9)
        assertEquals(0.0, postsPerHourOf(emptyList()), 1e-9)
    }

    @Test
    fun `zero and duplicate timestamps are dropped`() {
        // Нулевая отметка (нет времени) и повтор не должны дать бесконечность:
        // остаётся один настоящий интервал на полчаса плюс априорный.
        val zero = threadPostsPerHour(thread(replyTimes = listOf(0, minutes(10), minutes(40))))
        val duplicate = threadPostsPerHour(
            thread(replyTimes = listOf(minutes(10), minutes(10), minutes(40))),
        )
        assertEquals(8.0 / 3.0, zero, 1e-9)
        assertEquals(zero, duplicate, 1e-9)
    }

    @Test
    fun `one usable timestamp falls back to the lifetime average`() {
        // Из трёх отметок пригодна одна — интервала нет, значит средняя за жизнь
        // с тем же часовым полом, что и в сортировке: 4 поста / 1 ч = 4 в час.
        assertEquals(
            4.0,
            threadPostsPerHour(thread(posts = 4, replyTimes = listOf(0, minutes(10), minutes(10)))),
            1e-9,
        )
    }

    @Test
    fun `engine number wins over the client estimate`() {
        // Движок считает по всей доске — его число точнее и показывается как есть.
        val page = ThreadsPage(
            currentPage = 0,
            isIndex = true,
            threads = listOf(thread(posts = 4, replyTimes = listOf(minutes(60), minutes(90), minutes(120)))),
            threadsPerPage = 20,
            maxPages = 10,
            boardSpeed = 472,
        )
        val activity = boardActivity(page)
        assertEquals(472.0, activity.postsPerHour, 1e-9)
        assertEquals(false, activity.isEstimate)
    }

    @Test
    fun `without engine number the page estimate is used`() {
        // Ноль вместо board_speed (зеркало, старый движок) — считаем сами и
        // помечаем число как оценку: в интерфейсе у неё будет «≈».
        val page = ThreadsPage(
            currentPage = 0,
            isIndex = true,
            threads = listOf(thread(posts = 4, replyTimes = listOf(minutes(60), minutes(90), minutes(120)))),
            threadsPerPage = 20,
            maxPages = 10,
            boardSpeed = 0,
        )
        val activity = boardActivity(page)
        assertEquals(2.0, activity.postsPerHour, 1e-9)
        assertTrue(activity.isEstimate)
    }

    @Test
    fun `label rounds to the precision the number deserves`() {
        assertEquals("330 постов/ч", postsPerHourLabel(327.4))
        assertEquals("210 постов/ч", postsPerHourLabel(208.0))
        assertEquals("21 пост/ч", postsPerHourLabel(21.4))
        assertEquals("4,6 поста/ч", postsPerHourLabel(4.56))
        assertEquals("1,0 поста/ч", postsPerHourLabel(1.0))
        assertEquals("0 постов/ч", postsPerHourLabel(0.01))
        // Число движка точное — без округления до десятков и без «≈».
        assertEquals("472 поста/ч", postsPerHourExactLabel(472.0))
        assertEquals("1 пост/ч", postsPerHourExactLabel(1.0))
    }
}
