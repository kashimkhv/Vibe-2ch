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
 *
 * Здесь же хранится, до какого файла долистали: по возврате в тред он решает,
 * к какому посту прокрутить список (см. [consumeSwiped]).
 */
@Singleton
class MediaPlaylist @Inject constructor() {

    var items: List<Attachment> = emptyList()
        private set

    var index: Int = 0
        private set

    /** Файл, с которого открыли просмотрщик: по нему видно, листали ли вообще. */
    private var openedWith: Attachment? = null

    /** Файл, который просмотрщик показывает сейчас. */
    var lastViewed: Attachment? = null
        private set

    fun set(items: List<Attachment>, index: Int) {
        this.items = items
        this.index = index.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        lastViewed = this.items.getOrNull(this.index)
        openedWith = lastViewed
    }

    /**
     * Свайп в просмотрщике: запоминаем файл, на котором остановились.
     *
     * Плейлист пуст, если процесс пересоздали, а маршрут остался: тогда
     * листать нечего и трогать индекс незачем.
     */
    fun select(index: Int) {
        if (items.isEmpty()) return
        this.index = index.coerceIn(0, items.lastIndex)
        lastViewed = items[this.index]
    }

    /**
     * Файл, до которого долистали, если он не тот, с которого открывали.
     *
     * Заодно забывает позицию: возврат в тред бывает один, и второй раз
     * прокручивать к тому же посту не нужно. `null` — листали в пределах
     * того же вложения или не листали вовсе.
     */
    fun consumeSwiped(): Attachment? {
        val last = lastViewed
        val swiped = last != null && last.url != openedWith?.url
        openedWith = null
        lastViewed = null
        return last.takeIf { swiped }
    }
}
