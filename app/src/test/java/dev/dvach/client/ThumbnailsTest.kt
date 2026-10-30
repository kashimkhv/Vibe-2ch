package dev.dvach.client

import dev.dvach.client.util.THUMBNAIL_BASE_SIDE_DP
import dev.dvach.client.util.THUMBNAIL_LIST_BASE_SIDE_DP
import dev.dvach.client.util.THUMBNAIL_SCALE_DEFAULT
import dev.dvach.client.util.THUMBNAIL_SCALE_MAX
import dev.dvach.client.util.THUMBNAIL_SCALE_MIN
import dev.dvach.client.util.fitThumbnailSide
import dev.dvach.client.util.scaleThumbnail
import org.junit.Assert.assertEquals
import org.junit.Test

class ThumbnailsTest {

    @Test
    fun `list base is half of the thread base`() {
        // Список тредов не хранит своё число: он всегда вдвое меньше треда,
        // поэтому размеры на экранах не разъезжаются.
        assertEquals(150, THUMBNAIL_BASE_SIDE_DP)
        assertEquals(75, THUMBNAIL_LIST_BASE_SIDE_DP)
    }

    @Test
    fun `both sliders scale their own base`() {
        // Настройки раздельные, но процент по умолчанию общий: тред 150 → 180 dp,
        // список 75 → 90 dp.
        assertEquals(180, scaleThumbnail(THUMBNAIL_BASE_SIDE_DP, THUMBNAIL_SCALE_DEFAULT))
        assertEquals(90, scaleThumbnail(THUMBNAIL_LIST_BASE_SIDE_DP, THUMBNAIL_SCALE_DEFAULT))
    }

    @Test
    fun `hundred percent keeps base size`() {
        assertEquals(THUMBNAIL_BASE_SIDE_DP, scaleThumbnail(THUMBNAIL_BASE_SIDE_DP, 100))
        assertEquals(THUMBNAIL_LIST_BASE_SIDE_DP, scaleThumbnail(THUMBNAIL_LIST_BASE_SIDE_DP, 100))
    }

    @Test
    fun `values outside the range are clamped`() {
        assertEquals(scaleThumbnail(76, THUMBNAIL_SCALE_MIN), scaleThumbnail(76, 0))
        assertEquals(scaleThumbnail(76, THUMBNAIL_SCALE_MAX), scaleThumbnail(76, 1000))
        // Отрицательный процент не должен давать отрицательный размер.
        assertEquals(scaleThumbnail(76, THUMBNAIL_SCALE_MIN), scaleThumbnail(76, -100))
    }

    @Test
    fun `result is rounded to nearest dp`() {
        // 77 * 1.1 = 84.7 → 85, а не 84.
        assertEquals(85, scaleThumbnail(77, 110))
        // 75 * 1.5 = 112.5 → 113.
        assertEquals(113, scaleThumbnail(75, 150))
    }

    @Test
    fun `four thumbs fit one row on a phone card`() {
        // Карточка на типичном телефоне: 393 dp экрана минус 12 dp полей с каждой
        // стороны — 369 dp. Четыре превью по 90 dp и три промежутка по 4 dp
        // занимают 372 dp и на волосок не влезают, поэтому сторона — 89 dp;
        // на чуть более широком экране (400 dp) размер остаётся прежним.
        assertEquals(89, fitThumbnailSide(sideDp = 90, availableDp = 369, visible = 4))
        assertEquals(90, fitThumbnailSide(sideDp = 90, availableDp = 400, visible = 4))
    }

    @Test
    fun `large thumbs shrink to fit the row instead of wrapping`() {
        // При 200 % превью в списке — 150 dp, четыре штуки не влезают.
        // Сторона ужимается до (369 - 3 * 4) / 4 = 89 dp: одна строка, без переноса.
        assertEquals(89, fitThumbnailSide(sideDp = 150, availableDp = 369, visible = 4))
        // Три превью: (369 - 2 * 4) / 3 = 120 dp.
        assertEquals(120, fitThumbnailSide(sideDp = 150, availableDp = 369, visible = 3))
    }

    @Test
    fun `narrow card and empty list do not break the size`() {
        // Одна картинка на узкой карточке ужимается до её ширины, а не вылезает.
        assertEquals(90, fitThumbnailSide(sideDp = 150, availableDp = 90, visible = 1))
        // Нечего показывать — возвращаем исходный размер, а не мусор.
        assertEquals(150, fitThumbnailSide(sideDp = 150, availableDp = 369, visible = 0))
        // Нулевая ширина (до первого замера) тоже не должна давать 0 dp.
        assertEquals(150, fitThumbnailSide(sideDp = 150, availableDp = 0, visible = 4))
    }
}
