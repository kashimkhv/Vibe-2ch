package dev.dvach.client

import dev.dvach.client.ui.components.PREFETCH_AHEAD_ITEMS
import dev.dvach.client.ui.components.PREFETCH_BEHIND_ITEMS
import dev.dvach.client.ui.components.prefetchRange
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Диапазон предзагрузки: превью грузятся не только для видимых постов, но и
 * на [PREFETCH_AHEAD_ITEMS] вперёд — иначе при быстром флинге картинки не
 * успевают и посты остаются серыми.
 */
class ThumbnailPrefetchTest {

    @Test
    fun `range covers visible items plus a margin ahead`() {
        // Экран показывает посты 10..14 из ста: вперёд 12, назад 3.
        assertEquals(7..26, prefetchRange(first = 10, last = 14, total = 100, ahead = 12, behind = 3))
    }

    @Test
    fun `range starts at the first item`() {
        // В начале списка назад отступать некуда.
        assertEquals(0..12, prefetchRange(first = 0, last = 0, total = 100, ahead = 12, behind = 3))
    }

    @Test
    fun `range stops at the last item`() {
        // Конец списка: вперёд ничего нет, «до конца» не выходим, а назад
        // по-прежнему забираем пару постов.
        assertEquals(94..99, prefetchRange(first = 97, last = 99, total = 100, ahead = 12, behind = 3))
    }

    @Test
    fun `empty list gives empty range`() {
        assertEquals(IntRange.EMPTY, prefetchRange(first = 0, last = 0, total = 0, ahead = 12, behind = 3))
    }

    @Test
    fun `single item list asks for that item only`() {
        assertEquals(0..0, prefetchRange(first = 0, last = 0, total = 1, ahead = 12, behind = 3))
    }
}
