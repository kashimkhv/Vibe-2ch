package dev.dvach.client

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import dev.dvach.client.ui.components.scrollSeek

class ScrollbarTest {

    /** Десять постов по 100 px в окне 400 px: прокрутить можно на 600 px. */
    private val travel = 600f
    private val averageSize = 100f
    private val count = 10

    @Test
    fun `start of the rail scrolls to the first item`() {
        val seek = scrollSeek(0f, travel, averageSize, count)
        assertEquals(0, seek.index)
        assertEquals(0, seek.offset)
    }

    @Test
    fun `end of the rail reaches the last page`() {
        val seek = scrollSeek(1f, travel, averageSize, count)
        // 600 px при высоте поста 100 px — это пост с индексом 6 (7-й),
        // при окне в 400 px именно он оказывается последним возможным верхом.
        assertEquals(6, seek.index)
        assertEquals(0, seek.offset)
    }

    @Test
    fun `middle of the rail keeps the position inside an item`() {
        // 240 px — это пост с индексом 2 и смещение 40 px внутри него.
        val seek = scrollSeek(240f / travel, travel, averageSize, count)
        assertEquals(2, seek.index)
        assertEquals(40, seek.offset)
    }

    @Test
    fun `positions outside the rail are clamped`() {
        assertEquals(0, scrollSeek(-1f, travel, averageSize, count).index)
        assertEquals(6, scrollSeek(2f, travel, averageSize, count).index)
    }

    @Test
    fun `offset never goes past the item height`() {
        // Дробный шаг: 5.9 поста — индекс 6, смещение не больше одного поста.
        repeat(50) { step ->
            val seek = scrollSeek(step / 49f, travel, averageSize, count)
            assertTrue(seek.index in 0 until count)
            assertTrue(seek.offset in 0..averageSize.toInt())
        }
    }

    @Test
    fun `unmeasurable list gives a safe fallback`() {
        val seek = scrollSeek(0.5f, travel = 0f, averageSize = 0f, count = 0)
        assertEquals(0, seek.index)
        assertEquals(0, seek.offset)
    }
}
