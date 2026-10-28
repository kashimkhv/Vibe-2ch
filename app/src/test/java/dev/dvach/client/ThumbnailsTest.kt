package dev.dvach.client

import dev.dvach.client.util.THUMBNAIL_SCALE_DEFAULT
import dev.dvach.client.util.THUMBNAIL_SCALE_MAX
import dev.dvach.client.util.THUMBNAIL_SCALE_MIN
import dev.dvach.client.util.scaleThumbnail
import org.junit.Assert.assertEquals
import org.junit.Test

class ThumbnailsTest {

    @Test
    fun `default scale makes previews larger`() {
        // 76 dp в списке тредов и 260 dp в треде при 120 %.
        assertEquals(91, scaleThumbnail(76, THUMBNAIL_SCALE_DEFAULT))
        assertEquals(312, scaleThumbnail(260, THUMBNAIL_SCALE_DEFAULT))
    }

    @Test
    fun `hundred percent keeps base size`() {
        assertEquals(76, scaleThumbnail(76, 100))
        assertEquals(260, scaleThumbnail(260, 100))
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
}
