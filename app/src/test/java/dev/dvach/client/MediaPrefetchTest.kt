package dev.dvach.client

import dev.dvach.client.ui.media.MEDIA_PREFETCH_AHEAD
import dev.dvach.client.ui.media.MEDIA_PREFETCH_BEHIND
import dev.dvach.client.ui.media.mediaPrefetchOrder
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Порядок прогрева вложений в просмотрщике: соседние страницы грузятся заранее,
 * ближайшие — первыми, потому что очередь загрузчика не бесконечна.
 */
class MediaPrefetchTest {

    @Test
    fun `nearest pages come first`() {
        // Десятая страница из ста: назад 2, вперёд 3 — начинаем с соседей.
        assertEquals(
            listOf(9, 11, 8, 12, 13),
            mediaPrefetchOrder(page = 10, total = 100, ahead = 3, behind = 2),
        )
        // Значения по умолчанию — шире: при быстром листании вперёд успевает
        // уехать не одна страница.
        assertEquals(
            listOf(9, 11, 8, 12, 13, 14, 15),
            mediaPrefetchOrder(page = 10, total = 100, ahead = 5, behind = 2),
        )
    }

    @Test
    fun `current page is not prefetched`() {
        assertEquals(
            listOf(1, 2, 3),
            mediaPrefetchOrder(page = 0, total = 100, ahead = 3, behind = 2),
        )
    }

    @Test
    fun `end of the list is not crossed`() {
        assertEquals(
            listOf(98, 97),
            mediaPrefetchOrder(page = 99, total = 100, ahead = 3, behind = 2),
        )
    }

    @Test
    fun `empty playlist asks for nothing`() {
        assertEquals(emptyList<Int>(), mediaPrefetchOrder(page = 0, total = 0, ahead = 3, behind = 2))
    }

    @Test
    fun `single item gives nothing to preload`() {
        assertEquals(emptyList<Int>(), mediaPrefetchOrder(page = 0, total = 1, ahead = 3, behind = 2))
    }

    @Test
    fun `defaults cover both directions`() {
        // Значения по умолчанию: вперёд дальше, чем назад, но сосед сзади есть.
        assertEquals(5, MEDIA_PREFETCH_AHEAD)
        assertEquals(2, MEDIA_PREFETCH_BEHIND)
    }
}
