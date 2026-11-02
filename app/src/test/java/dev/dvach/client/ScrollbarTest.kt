package dev.dvach.client

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import dev.dvach.client.ui.components.ItemHeightSample
import dev.dvach.client.ui.components.scrollProgress
import dev.dvach.client.ui.components.scrollSeek
import dev.dvach.client.ui.components.scrollSpan
import dev.dvach.client.ui.components.thumbFractionOf
import dev.dvach.client.ui.components.viewportFractionOf
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

    @Test
    fun `thumb length is the visible share of the content`() {
        // Экран 500 px, двадцать постов по 100 px: видно четверть содержимого.
        assertEquals(0.25f, viewportFractionOf(500f, 100f, 20), 0.001f)
        // Содержимое короче экрана — бегунок занимает всю дорожку.
        assertEquals(1f, viewportFractionOf(500f, 100f, 5), 0.001f)
        // Оценки нет вовсе: размеры ещё не пришли — тоже вся дорожка.
        assertEquals(1f, viewportFractionOf(500f, 0f, 0), 0.001f)
        assertEquals(1f, viewportFractionOf(0f, 100f, 20), 0.001f)
    }

    @Test
    fun `thumb length shows the live estimate until it is locked`() {
        // Пока длину не зафиксировали (первый кадр), показываем то, что есть,
        // иначе при открытии треда бегунок появился бы из нуля.
        assertEquals(0.2f, thumbFractionOf(locked = -1f, live = 0.2f), 0f)
        assertEquals(0.35f, thumbFractionOf(locked = -1f, live = 0.35f), 0f)
    }

    @Test
    fun `locked thumb length does not follow later estimates`() {
        // Перемотка меняет то, какие посты в кадре, а с ними и живую оценку.
        // Показанная длина после фиксации не должна за ней ходить.
        val locked = 0.14f
        assertEquals(locked, thumbFractionOf(locked, live = 0.44f), 0f)
        assertEquals(locked, thumbFractionOf(locked, live = 0.05f), 0f)
        // Ноль — это тоже фиксация (бегунок занимает всю дорожку в коротком
        // треде), а не «ещё не фиксировали».
        assertEquals(0f, thumbFractionOf(locked = 0f, live = 0.4f), 0f)
    }

    @Test
    fun `height sample remembers every post it has seen`() {
        val sample = ItemHeightSample()
        sample.observe(listOf(0 to 400f, 1 to 200f, 2 to 900f, 3 to 300f))
        assertEquals(450f, sample.average(), 0.001f)
        // Тот же пост, попавший в кадр ещё раз, не должен тянуть среднюю к себе.
        sample.observe(listOf(0 to 400f, 1 to 200f, 2 to 900f, 3 to 300f))
        assertEquals(450f, sample.average(), 0.001f)
    }

    @Test
    fun `height sample does not depend on where the list is`() {
        // Середина треда: рядом простыня с картинками. Оценка по видимым
        // прыгнула бы здесь вверх, и бегунок стал бы короче.
        val sample = ItemHeightSample()
        sample.observe((0..15).map { it to 300f })
        val before = viewportFractionOf(2000f, sample.average(), count = 502)

        // Перемотка в конец: в кадр попали совсем другие посты — короткие.
        sample.observe((480..501).map { it to 120f })
        val after = viewportFractionOf(2000f, sample.average(), count = 502)

        // Средняя по всему треду почти не двинулась — и длина бегунка тоже.
        assertTrue("до перемотки $before, после $after", abs(after - before) < 0.01f)

        // Для сравнения: та же оценка только по новым видимым постам
        // промахнулась бы мимо прежней в разы.
        val onlyVisible = viewportFractionOf(2000f, 120f, count = 502)
        assertTrue(onlyVisible > before * 2f)
    }

    @Test
    fun `empty height sample gives no estimate`() {
        val sample = ItemHeightSample()
        assertEquals(0f, sample.average(), 0f)
        sample.observe(listOf(0 to 0f, 1 to -5f))
        assertEquals(0f, sample.average(), 0f)
    }
}
