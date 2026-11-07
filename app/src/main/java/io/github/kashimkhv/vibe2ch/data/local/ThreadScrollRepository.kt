package io.github.kashimkhv.vibe2ch.data.local

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Позиция чтения тредов: где пользователь остановился в каждом треде.
 *
 * Хранится в Room, а не в памяти экрана, потому что экран треда при возврате
 * с доски создаётся заново, а вместе с ним и `LazyListState`.
 *
 * Запись идёт в собственном [scope]: сохранять позицию нужно в момент, когда
 * экран закрывается, — к этому времени `viewModelScope` уже отменён.
 */
@Singleton
class ThreadScrollRepository @Inject constructor(
    private val dao: ChanDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun load(threadKey: String): ThreadScrollEntity? = dao.threadScroll(threadKey)

    /**
     * @param postNum первый видимый пост: по нему позиция и восстанавливается.
     * @param index резервный индекс в списке, если пост успели удалить.
     * @param offset смещение внутри поста в пикселях.
     */
    fun save(threadKey: String, postNum: Int, index: Int, offset: Int) {
        scope.launch {
            dao.saveThreadScroll(
                ThreadScrollEntity(
                    threadKey = threadKey,
                    postNum = postNum,
                    scrollIndex = index,
                    scrollOffset = offset,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            dao.trimThreadScroll(MAX_THREADS)
        }
    }

    private companion object {
        /** Сколько последних тредов помнить; более старые позиции бесполезны. */
        const val MAX_THREADS = 200
    }
}
