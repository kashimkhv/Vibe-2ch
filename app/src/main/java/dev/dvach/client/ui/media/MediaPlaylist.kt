package dev.dvach.client.ui.media

import javax.inject.Inject
import javax.inject.Singleton
import dev.dvach.client.domain.model.Attachment

/**
 * Список вложений, открытых из треда, и позиция в нём.
 *
 * Маршрут навигации переносит только одно вложение: список из десятков файлов
 * в аргументах маршрута раздул бы бэкстек и потребовал бы сериализации модели.
 * Поэтому экран треда кладёт сюда все файлы и индекс нажатого, а просмотрщик
 * читает их при создании. При пересоздании процесса список теряется, и
 * просмотрщик просто откроет одно вложение из маршрута.
 */
@Singleton
class MediaPlaylist @Inject constructor() {

    var items: List<Attachment> = emptyList()
        private set

    var index: Int = 0
        private set

    fun set(items: List<Attachment>, index: Int) {
        this.items = items
        this.index = index.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    }
}
