package dev.dvach.client

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import dev.dvach.client.ui.components.scrollProgress
import dev.dvach.client.ui.components.scrollSeek
import dev.dvach.client.ui.components.scrollSpan
import kotlin.math.abs
import kotlin.math.roundToInt

class ScrollbarTest {

    /** Десять постов по 100 px, в окне видно четыре: низ списка — индекс 6. */
    private val count = 10
    private val visibleCount = 4
    private val itemSize = 100f

    @Test
    fun `start of the rail scrolls to the first item`() {
        val seek = scrollSeek(0f, count, visibleCount, itemSize)
        assertEquals(0, seek.index)
        assertEquals(0, seek.offset)
    }

    @Test
    fun `end of the rail scrolls to the last page`() {
        val seek = scrollSeek(1f, count, visibleCount, itemSize)
        // Низ списка — пост, с которого начинается последний экран (6), а не
        // последний пост: иначе бегунок не доезжал бы до края.
        assertEquals(6, seek.index)
        assertEquals(0, seek.offset)
    }

    @Test
    fun `middle of the rail keeps the position inside an item`() {
        // Доля 0.4 при низе списка на 6 — это позиция 2.4: пост 2, 40 px внутри.
        val seek = scrollSeek(0.4f, count, visibleCount, itemSize)
        assertEquals(2, seek.index)
        assertEquals(40, seek.offset)
    }

    @Test
    fun `positions outside the rail are clamped`() {
        assertEquals(0, scrollSeek(-1f, count, visibleCount, itemSize).index)
        assertEquals(6, scrollSeek(2f, count, visibleCount, itemSize).index)
    }

    @Test
    fun `offset never goes past the item height`() {
        repeat(50) { step ->
            val seek = scrollSeek(step / 49f, count, visibleCount, itemSize)
            assertTrue(seek.index in 0 until count)
            assertTrue(seek.offset in 0 until itemSize.toInt())
        }
    }

    @Test
    fun `unmeasurable list gives a safe fallback`() {
        val seek = scrollSeek(0.5f, count = 0, visibleCount = 0, itemSize = 0f)
        assertEquals(0, seek.index)
        assertEquals(0, seek.offset)
    }

    @Test
    fun `progress at the top and at the bottom spans the whole rail`() {
        assertEquals(0f, scrollProgress(0, 0, itemSize, count, visibleCount), 0.001f)
        assertEquals(1f, scrollProgress(6, 0, itemSize, count, visibleCount), 0.001f)
    }

    @Test
    fun `progress counts the scrolling inside an item`() {
        // Половина поста 2 из 6 возможных позиций.
        assertEquals(2.5f / 6f, scrollProgress(2, 50, itemSize, count, visibleCount), 0.001f)
    }

    @Test
    fun `seek and progress agree with each other`() {
        // Главное свойство: куда тянуть — туда и прокрутится, и бегунок встанет
        // под палец. Раньше из-за пиксельной оценки он уезжал на другой пост.
        repeat(101) { step ->
            val fraction = step / 100f
            val seek = scrollSeek(fraction, count, visibleCount, itemSize)
            val back = scrollProgress(seek.index, seek.offset, itemSize, count, visibleCount)
            assertTrue(
                "доля $fraction → ${seek.index}+${seek.offset} → $back",
                abs(back - fraction) <= 0.02f,
            )
        }
    }

    @Test
    fun `tall posts still move smoothly inside one item`() {
        // Пост в 1200 px на экране с четырьмя видимыми из десяти: дорожка
        // внутри одного поста должна давать разные смещения, а не стоять на месте.
        val offsets = (0..20).map { step ->
            scrollSeek(step / 20f, count, visibleCount, itemSize = 1200f).offset
        }
        // Дорожка прошла всю длину, а смещения внутри постов — разные: значит,
        // бегунок не залипает на границах постов.
        assertTrue(offsets.distinct().size >= 9)
    }

    @Test
    fun `dragging the rail moves the list by the same fraction`() {
        // Протяжка двигает содержимое относительно: 100 px поста × 6 позиций =
        // 600 px на всю дорожку. Протащили долю — список проехал ту же долю,
        // без пересчёта в «пост + смещение» и без скачков на границах постов.
        val span = scrollSpan(count, visibleCount, itemSize)
        assertEquals(600f, span, 0.001f)

        repeat(101) { step ->
            val fraction = step / 100f
            val px = fraction * span
            val index = (px / itemSize).toInt().coerceAtMost(count - 1)
            val offset = (px - index * itemSize).roundToInt()
            assertEquals(
                "доля $fraction → $px px",
                fraction,
                scrollProgress(index, offset, itemSize, count, visibleCount),
                0.001f,
            )
        }
    }

    @Test
    fun `taller posts give a longer scroll span`() {
        // Те же четыре позиции, но посты по 600 px: дорожка должна проезжать
        // вчетверо больше содержимого, иначе в треде с картинками бегунок
        // обгонял бы список.
        assertEquals(1200f, scrollSpan(count = 4, visibleCount = 2, itemSize = 600f), 0.001f)
    }
}
