package io.github.kashimkhv.vibe2ch.ui.thread

import io.github.kashimkhv.vibe2ch.domain.model.Post

/**
 * Пост в развёрнутом дереве треда.
 *
 * @param depth уровень вложенности: 0 — корень ветки, 1 — ответ на него и т. д.
 */
data class PostNode(
    val post: Post,
    val depth: Int,
)

/** Глубже шести уровней карточка становится слишком узкой для текста. */
internal const val MAX_TREE_DEPTH = 6

/**
 * Цитаты в тексте поста: `>>123456` (в сыром HTML может прийти `&gt;&gt;123456`).
 * Ссылки на другие треды тоже попадают сюда — их отсекает проверка «такой пост
 * уже есть выше в этом треде».
 */
private val REFERENCE = Regex("""(?:>>|&gt;&gt;)(\d+)""")

internal fun postReferences(html: String): List<Int> =
    REFERENCE.findAll(html).mapNotNull { it.groupValues[1].toIntOrNull() }.toList()

/**
 * Родитель для каждого поста: первая цитата на пост, который уже встречался
 * выше в треде. Ссылки на будущие посты, на себя и на другие треды во внимание
 * не принимаются — иначе получаются циклы и потерянные ветки.
 *
 * Ответы на ОП-пост родителем не считаются: иначе весь тред висел бы на одном
 * уровне вложенности, ведь в 2ch почти все отвечают ОПу. Такие посты — корни
 * веток, и дерево показывает именно переписку между анонами.
 */
internal fun parentsOf(posts: List<Post>, opNum: Int): Map<Int, Int?> {
    val indexOf = HashMap<Int, Int>(posts.size)
    posts.forEachIndexed { index, post -> indexOf[post.num] = index }

    val parents = HashMap<Int, Int?>(posts.size)
    posts.forEachIndexed { index, post ->
        parents[post.num] = postReferences(post.commentHtml)
            .asSequence()
            .filter { it != opNum }
            .firstOrNull { ref -> indexOf[ref]?.let { it < index } == true }
    }
    return parents
}

/**
 * Раскладывает посты в плоский список для `LazyColumn`: каждая ветка идёт
 * сразу за своим родителем, внутри ветки сохраняется исходный (хронологический)
 * порядок.
 */
internal fun buildPostTree(
    posts: List<Post>,
    opNum: Int = 0,
): List<PostNode> {
    if (posts.isEmpty()) return emptyList()

    val parents = parentsOf(posts, opNum.takeIf { it != 0 } ?: opNumOf(posts))
    val children = HashMap<Int, MutableList<Post>>()
    val roots = ArrayList<Post>()
    posts.forEach { post ->
        val parent = parents[post.num]
        if (parent == null) roots.add(post) else children.getOrPut(parent) { mutableListOf() }.add(post)
    }

    val result = ArrayList<PostNode>(posts.size)
    fun visit(post: Post, depth: Int) {
        result += PostNode(post = post, depth = depth.coerceAtMost(MAX_TREE_DEPTH))
        children[post.num].orEmpty().forEach { visit(it, depth + 1) }
    }
    roots.forEach { visit(it, 0) }
    return result
}

/**
 * Порядковый номер поста в треде: 1 — ОП, 2 — первый ответ и так далее.
 *
 * Считается по хронологическому порядку из API, а не по месту в дереве: дерево
 * переставляет ответы под цитируемые посты, и «номер на экране» менялся бы после
 * каждого обновления треда. По номеру поста (`post.num`) можно найти его
 * порядковый номер, не перестраивая список.
 */
internal fun postOrdinals(posts: List<Post>): Map<Int, Int> =
    posts.mapIndexed { index, post -> post.num to index + 1 }.toMap()

/**
 * Индекс поста, к которому надо прокрутить при возврате в тред. Ищем по номеру:
 * после обновления треда посты могли переехать (новые ответы, перестроенное
 * дерево), а номер остаётся тем же. Если пост удалили — берём сохранённый
 * индекс и зажимаем его в границы списка.
 */
internal fun scrollIndexFor(nodes: List<PostNode>, postNum: Int, fallbackIndex: Int): Int {
    val byNum = nodes.indexOfFirst { it.post.num == postNum }
    if (byNum >= 0) return byNum
    return fallbackIndex.coerceIn(0, (nodes.size - 1).coerceAtLeast(0))
}

/** Номер ОП-поста: сервер помечает его `op`, но на всякий случай берём первый. */
private fun opNumOf(posts: List<Post>): Int =
    posts.firstOrNull { it.isOp }?.num ?: posts.first().num
