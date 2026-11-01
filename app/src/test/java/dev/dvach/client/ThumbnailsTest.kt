package dev.dvach.client

import dev.dvach.client.util.THUMBNAIL_BASE_SIDE_DP
import dev.dvach.client.util.THUMBNAIL_LIST_BASE_SIDE_DP
import dev.dvach.client.util.THUMBNAIL_SCALE_DEFAULT
import dev.dvach.client.util.THUMBNAIL_SCALE_MAX
import dev.dvach.client.util.THUMBNAIL_SCALE_MIN
import dev.dvach.client.util.listThumbnailSide
import dev.dvach.client.util.scaleThumbnail
import dev.dvach.client.util.thumbnailRowGap
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
    fun `four thumbs squeeze the gap to stay in one row`() {
        // Карточка на телефоне: 393 dp экрана минус 12 dp полей с каждой стороны —
        // 369 dp. Четыре превью по 90 dp и обычные 4 dp между ними занимают
        // 372 dp, поэтому зазор ужимается до (369 - 360) / 3 = 3 dp.
        assertEquals(3, thumbnailRowGap(sideDp = 90, availableDp = 369, visible = 4))
        // На широком экране ничего ужимать не нужно.
        assertEquals(4, thumbnailRowGap(sideDp = 90, availableDp = 400, visible = 4))
    }

    @Test
    fun `large thumbs keep their size and wrap instead of shrinking`() {
        // При 200 % превью в списке — 150 dp: четыре штуки не влезают даже без
        // зазора. Размер не трогаем (он задан настройкой), ряд переносится,
        // а промежуток остаётся обычным.
        assertEquals(4, thumbnailRowGap(sideDp = 150, availableDp = 369, visible = 4))
    }

    @Test
    fun `few attachments double the list thumb`() {
        // Один-два файла не заполняют строку: при включённой настройке ячейка
        // вдвое крупнее — 90 dp при обычных 120 % превращаются в 180 dp.
        assertEquals(
            180,
            listThumbnailSide(THUMBNAIL_LIST_BASE_SIDE_DP, THUMBNAIL_SCALE_DEFAULT, 1, true),
        )
        assertEquals(
            180,
            listThumbnailSide(THUMBNAIL_LIST_BASE_SIDE_DP, THUMBNAIL_SCALE_DEFAULT, 2, true),
        )
        // Три и больше — обычный размер: ряд и так заполнен.
        assertEquals(
            90,
            listThumbnailSide(THUMBNAIL_LIST_BASE_SIDE_DP, THUMBNAIL_SCALE_DEFAULT, 3, true),
        )
        assertEquals(
            90,
            listThumbnailSide(THUMBNAIL_LIST_BASE_SIDE_DP, THUMBNAIL_SCALE_DEFAULT, 4, true),
        )
    }

    @Test
    fun `few attachments stay normal when the setting is off`() {
        assertEquals(
            90,
            listThumbnailSide(THUMBNAIL_LIST_BASE_SIDE_DP, THUMBNAIL_SCALE_DEFAULT, 1, false),
        )
        // Выключенная настройка не зависит от числа файлов.
        assertEquals(
            90,
            listThumbnailSide(THUMBNAIL_LIST_BASE_SIDE_DP, THUMBNAIL_SCALE_DEFAULT, 2, false),
        )
    }

    @Test
    fun `doubling applies after the slider`() {
        // Сначала процент, потом множитель: 75 * 150 % = 113 dp, вдвое — 226 dp.
        // Иначе шаг зависел бы от ползунка: 200 % давало бы уже 300 dp.
        assertEquals(226, listThumbnailSide(THUMBNAIL_LIST_BASE_SIDE_DP, 150, 2, true))
        assertEquals(113, listThumbnailSide(THUMBNAIL_LIST_BASE_SIDE_DP, 150, 3, true))
        // Процент зажимается и здесь — через scaleThumbnail.
        assertEquals(300, listThumbnailSide(THUMBNAIL_LIST_BASE_SIDE_DP, 1000, 1, true))
    }

    @Test
    fun `gap stays normal for one thumb and empty card`() {
        // Одна картинка: ужимать нечего, зазор обычный.
        assertEquals(4, thumbnailRowGap(sideDp = 150, availableDp = 369, visible = 1))
        // Нулевая ширина (до первого замера) не должна давать отрицательный зазор.
        assertEquals(4, thumbnailRowGap(sideDp = 90, availableDp = 0, visible = 4))
        assertEquals(4, thumbnailRowGap(sideDp = 90, availableDp = 369, visible = 0))
    }
}
