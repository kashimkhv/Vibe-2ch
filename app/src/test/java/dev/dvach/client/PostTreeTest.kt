package dev.dvach.client

import org.junit.Assert.assertEquals
import org.junit.Test
import dev.dvach.client.domain.model.Post
import dev.dvach.client.ui.thread.MAX_TREE_DEPTH
import dev.dvach.client.ui.thread.buildPostTree
import dev.dvach.client.ui.thread.scrollIndexFor

class PostTreeTest {

    private fun post(num: Int, quotes: List<Int> = emptyList(), isOp: Boolean = false) = Post(
        boardId = "b",
        num = num,
        parent = 0,
        name = "Аноним",
        subject = "",
        commentHtml = quotes.joinToString("") { "&gt;&gt;$it<br>" } + "текст",
        date = "",
        timestamp = 0,
        trip = null,
        isOp = isOp,
        isSticky = false,
        isClosed = false,
        isBanned = false,
        files = emptyList(),
    )

    /** ОП + три независимых поста, которые отвечают ему. */
    private fun flatThread() = listOf(
        post(1, isOp = true),
        post(2, quotes = listOf(1)),
        post(3, quotes = listOf(1)),
    )

    @Test
    fun `reply to another anon is nested under it`() {
        val posts = listOf(
            post(1, isOp = true),
            post(2, quotes = listOf(1)),
            post(3, quotes = listOf(2)),
        )
        val tree = buildPostTree(posts)

        assertEquals(listOf(1, 2, 3), tree.map { it.post.num })
        assertEquals(listOf(0, 0, 1), tree.map { it.depth })
    }

    @Test
    fun `replies to OP stay on the root level`() {
        // В 2ch почти все отвечают ОПу — иначе весь тред уехал бы на один уровень.
        val tree = buildPostTree(flatThread())
        assertEquals(listOf(0, 0, 0), tree.map { it.depth })
    }

    @Test
    fun `first quote to an earlier post wins`() {
        val posts = listOf(
            post(1, isOp = true),
            post(2),
            post(3),
            post(4, quotes = listOf(2, 3)),
        )
        val tree = buildPostTree(posts)

        // 4 цитирует и 2, и 3, но родителем становится первая цитата,
        // поэтому 4 встаёт под 2, а 3 остаётся отдельным корнем.
        assertEquals(listOf(1, 2, 4, 3), tree.map { it.post.num })
        assertEquals(listOf(0, 0, 1, 0), tree.map { it.depth })
    }

    @Test
    fun `links to missing, future and own posts are ignored`() {
        val posts = listOf(
            post(1, isOp = true),
            post(2, quotes = listOf(999, 2, 3, 1)),
            post(3),
        )
        val tree = buildPostTree(posts)
        assertEquals(listOf(0, 0, 0), tree.map { it.depth })
    }

    @Test
    fun `subtree follows its parent keeping chronological order`() {
        val posts = listOf(
            post(1, isOp = true),
            post(2),
            post(3, quotes = listOf(2)),
            post(4),
            post(5, quotes = listOf(3)),
        )
        val tree = buildPostTree(posts)
        assertEquals(listOf(1, 2, 3, 5, 4), tree.map { it.post.num })
        assertEquals(listOf(0, 0, 1, 2, 0), tree.map { it.depth })
    }

    @Test
    fun `indentation is capped so deep chains stay readable`() {
        val posts = buildList {
            add(post(1, isOp = true))
            (2..MAX_TREE_DEPTH + 3).forEach { add(post(it, quotes = listOf(it - 1))) }
        }
        val tree = buildPostTree(posts)
        assertEquals(MAX_TREE_DEPTH, tree.last().depth)
    }

    @Test
    fun `saved position is found by post number even when posts moved`() {
        // Пока тред читали, ответы переехали — индексы другие, номер тот же.
        val tree = buildPostTree(
            listOf(
                post(1, isOp = true),
                post(2),
                post(3, quotes = listOf(2)),
            ),
        )
        assertEquals(2, scrollIndexFor(tree, postNum = 3, fallbackIndex = 0))
    }

    @Test
    fun `deleted post falls back to the saved index clamped to the list`() {
        val tree = buildPostTree(listOf(post(1, isOp = true), post(2)))
        assertEquals(1, scrollIndexFor(tree, postNum = 999, fallbackIndex = 1))
        assertEquals(1, scrollIndexFor(tree, postNum = 999, fallbackIndex = 50))
        assertEquals(0, scrollIndexFor(tree, postNum = 999, fallbackIndex = -5))
    }
}
