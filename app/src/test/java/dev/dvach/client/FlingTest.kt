package dev.dvach.client

import dev.dvach.client.ui.components.FLING_BOOST
import dev.dvach.client.ui.components.FLING_BOOST_FULL_SPEED
import dev.dvach.client.ui.components.FLING_BOOST_MIN_SPEED
import dev.dvach.client.ui.components.FLING_MAX_SPEED
import dev.dvach.client.ui.components.boostedFlingVelocity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Усиление флинга — чистая функция, поэтому её и проверяем: что медленное
 * движение не трогается, резкое усиливается, направление сохраняется, а
 * потолок скорости соблюдается.
 */
class FlingTest {

    @Test
    fun `zero velocity stays zero`() {
        assertEquals(0f, boostedFlingVelocity(0f), 0f)
    }

    @Test
    fun `slow drag is not boosted`() {
        // Медленное движение пальцем — это не бросок: список не должен «уезжать».
        assertEquals(120f, boostedFlingVelocity(120f), 0f)
        assertEquals(
            FLING_BOOST_MIN_SPEED,
            boostedFlingVelocity(FLING_BOOST_MIN_SPEED),
            0f,
        )
    }

    @Test
    fun `sharp swipe is boosted`() {
        val boosted = boostedFlingVelocity(2800f)
        assertTrue("флинг должен усиливаться: $boosted", boosted > 2800f)
    }

    @Test
    fun `boost keeps the direction`() {
        assertTrue(boostedFlingVelocity(-2800f) < -2800f)
        assertTrue(boostedFlingVelocity(2800f) > 2800f)
        assertTrue(boostedFlingVelocity(-80f) == -80f)
    }

    @Test
    fun `the sharper the swipe the farther the fling`() {
        // Сила инерции должна зависеть от скорости жеста, а не быть константой.
        val slow = boostedFlingVelocity(1500f)
        val medium = boostedFlingVelocity(2800f)
        val sharp = boostedFlingVelocity(4200f)
        assertTrue("$slow < $medium", slow < medium)
        assertTrue("$medium < $sharp", medium < sharp)
        // Прирост именно от усиления, а не только от исходной скорости.
        assertTrue((medium / 2800f) > (slow / 1500f))
    }

    @Test
    fun `boost is limited and saturates at full speed`() {
        val full = boostedFlingVelocity(FLING_BOOST_FULL_SPEED)
        assertEquals(FLING_BOOST_FULL_SPEED * (1f + FLING_BOOST), full, 0.01f)
        // Дальше усиление не растёт: «чирк» по экрану не должен улетать на тред.
        val beyond = boostedFlingVelocity(FLING_BOOST_FULL_SPEED * 1.5f)
        assertEquals(
            FLING_BOOST_FULL_SPEED * 1.5f * (1f + FLING_BOOST),
            beyond,
            0.01f,
        )
    }

    @Test
    fun `absurd velocity is capped`() {
        assertEquals(FLING_MAX_SPEED, boostedFlingVelocity(FLING_MAX_SPEED * 4f), 0f)
        assertEquals(-FLING_MAX_SPEED, boostedFlingVelocity(-FLING_MAX_SPEED * 4f), 0f)
        assertTrue(abs(boostedFlingVelocity(FLING_MAX_SPEED * 4f)) <= FLING_MAX_SPEED)
    }

    @Test
    fun `boost turns on smoothly at the threshold`() {
        // Ровно на пороге усиления ещё нет, чуть выше — уже есть, без скачка.
        val input = FLING_BOOST_MIN_SPEED + 1f
        val justAbove = boostedFlingVelocity(input)
        assertTrue(justAbove > input)
        assertTrue(justAbove - input < 1f)
    }
}
