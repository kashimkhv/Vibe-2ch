package io.github.kashimkhv.vibe2ch.domain

import io.github.kashimkhv.vibe2ch.domain.model.ThreadSummary
import io.github.kashimkhv.vibe2ch.util.stripHtml

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
    IMAGES("По количеству картинок", false),
    ALPHABET("По алфавиту", true),
    ;

    /** У бампа направления нет: это порядок самого движка. */
    val canReverse: Boolean get() = this != BUMP
}

/**
 * Выбранный порядок одной доски: режим и направление. Доски не делят эту
 * настройку: на /b/ можно держать «по дате», а на /po/ остаться на бампе.
 */
data class BoardSort(val sort: ThreadSort, val ascending: Boolean)

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
 * Постов в час за время жизни треда: от ОП-поста до последнего ответа.
 * Считаем не от «сейчас», а от активности треда, иначе старые обсуждения
 * с сотней постов всегда висели бы внизу. Час — нижняя граница интервала:
 * тред, проживший минуту, не должен раздуваться.
 */
fun replyRate(thread: ThreadSummary): Double {
    // Время движка — в секундах, поэтому и делим на секунды в часе.
    val spanHours = ((lastReplyAt(thread) - thread.timestamp).coerceAtLeast(0L)) / 3_600.0
    return thread.postsCount / spanHours.coerceAtLeast(1.0)
}

/** Ключ алфавитной сортировки: тема, а без темы — начало текста ОП-поста. */
private fun alphabetKey(thread: ThreadSummary): String {
    val subject = thread.subject.trim()
    if (subject.isNotEmpty()) return subject
    return stripHtml(thread.commentHtml).take(120)
}
