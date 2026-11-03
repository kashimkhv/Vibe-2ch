package dev.dvach.client.domain

import dev.dvach.client.domain.model.ThreadSummary
import dev.dvach.client.util.stripHtml

/**
 * Порядок тредов в списке доски. Движок отдаёт их в порядке бампа, остальные
 * варианты клиент считает сам по уже загруженным страницам.
 *
 * @property label подпись строки в меню сортировки.
 * @property defaultAscending направление по умолчанию: «сначала новые» для дат,
 *   «сначала большие» для счётчиков и А→Я для алфавита.
 */
enum class ThreadSort(val label: String, val defaultAscending: Boolean) {
    /** Как отдаёт движок: закреплённые сверху, дальше по времени последнего бампа. */
    BUMP("Порядок бампа", false),
    REPLIES("По количеству ответов", false),
    CREATED("По дате создания", false),
    RATE("По скорости ответов", false),
    LAST_REPLY("По последнему ответу", false),
    IMAGES("По количеству картинок", false),
    ALPHABET("По алфавиту", true),
    ;

    /** У бампа направления нет: это порядок самого движка. */
    val canReverse: Boolean get() = this != BUMP
}

/**
 * Сортировка списка тредов.
 *
 * Закреплённые треды всегда остаются сверху — так их показывает и движок,
 * и любая другая сортировка иначе утопила бы их в середине списка.
 *
 * @param ascending направление для счётчиков и дат; для [ThreadSort.ALPHABET]
 *   это А→Я, для [ThreadSort.BUMP] не используется.
 */
fun sortThreads(
    threads: List<ThreadSummary>,
    sort: ThreadSort,
    ascending: Boolean,
): List<ThreadSummary> {
    // Порядок движка сохраняем как есть: сортировка только поднимает закреплённые,
    // а их движок и так отдаёт первыми — список остаётся нетронутым.
    if (sort == ThreadSort.BUMP) return threads.sortedWith(compareByDescending { it.isSticky })

    // Номер треда — тай-брейк: при равных значениях порядок не должен «прыгать»
    // от перерисовки к перерисовке. Он входит в сравнение до разворота, чтобы
    // при обратном порядке список оставался зеркальным.
    val byValue: Comparator<ThreadSummary> = when (sort) {
        ThreadSort.BUMP -> error("обработан выше")
        ThreadSort.REPLIES -> compareBy<ThreadSummary> { it.postsCount }
        ThreadSort.CREATED -> compareBy<ThreadSummary> { it.timestamp }
        ThreadSort.RATE -> compareBy<ThreadSummary> { replyRate(it) }
        ThreadSort.LAST_REPLY -> compareBy<ThreadSummary> { lastReplyAt(it) }
        ThreadSort.IMAGES -> compareBy<ThreadSummary> { it.filesCount }
        ThreadSort.ALPHABET -> Comparator { first: ThreadSummary, second: ThreadSummary ->
            String.CASE_INSENSITIVE_ORDER.compare(alphabetKey(first), alphabetKey(second))
        }
    }.thenBy { it.num }

    val ordered = if (ascending) byValue else byValue.reversed()
    return threads.sortedWith(compareByDescending<ThreadSummary> { it.isSticky }.then(ordered))
}

/** Время последнего ответа; у треда без ответов — время ОП-поста. */
fun lastReplyAt(thread: ThreadSummary): Long =
    thread.replies.maxOfOrNull { it.timestamp } ?: thread.timestamp

/**
 * Ответов в сутки за время жизни треда: от ОП-поста до последнего ответа.
 * Считаем не от «сейчас», а от активности треда, иначе старые обсуждения
 * с сотней постов всегда висели бы внизу.
 */
fun replyRate(thread: ThreadSummary): Double {
    val spanHours = ((lastReplyAt(thread) - thread.timestamp).coerceAtLeast(0L)) / 3_600_000.0
    return thread.postsCount / spanHours.coerceAtLeast(1.0)
}

/** Ключ алфавитной сортировки: тема, а без темы — начало текста ОП-поста. */
private fun alphabetKey(thread: ThreadSummary): String {
    val subject = thread.subject.trim()
    if (subject.isNotEmpty()) return subject
    return stripHtml(thread.commentHtml).take(120)
}
