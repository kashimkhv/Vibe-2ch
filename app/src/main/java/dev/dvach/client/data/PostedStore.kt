package dev.dvach.client.data

import javax.inject.Inject
import javax.inject.Singleton

/**
 * «Только что отправил пост» — мостик между формой и тредом.
 *
 * Форма закрывается, тред остаётся на бэкстеке, и его `ViewModel` живёт
 * отдельно. Через этот объект тред узнаёт, что в него только что написали:
 * перезагружается и прокручивается к новому посту. Хранится в памяти: после
 * перезапуска процесса прокручивать уже некуда.
 */
@Singleton
class PostedStore @Inject constructor() {

    private val pending = HashMap<String, Int>()

    fun remember(boardId: String, threadNum: Int, postNum: Int) {
        pending[key(boardId, threadNum)] = postNum
    }

    /** Забирает номер поста один раз: повторный возврат в тред ничего не двигает. */
    fun consume(boardId: String, threadNum: Int): Int? =
        pending.remove(key(boardId, threadNum))

    private fun key(boardId: String, threadNum: Int) = "$boardId/$threadNum"
}
