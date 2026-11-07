package io.github.kashimkhv.vibe2ch

import io.github.kashimkhv.vibe2ch.data.cache.CacheUsage
import io.github.kashimkhv.vibe2ch.ui.settings.cacheDetails
import io.github.kashimkhv.vibe2ch.util.formatCacheSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Строка про кэш в настройках: сколько занято, из какого предела, и что
 * показывать, когда очищать нечего.
 */
class CacheTest {

    private val mb = 1024L * 1024

    @Test
    fun `empty cache is called empty`() {
        assertEquals("пусто", formatCacheSize(0))
    }

    @Test
    fun `non empty cache is formatted like a file size`() {
        assertEquals("1.5 МБ", formatCacheSize(mb * 3 / 2))
    }

    @Test
    fun `usage sums both caches`() {
        val usage = CacheUsage(imagesBytes = 10 * mb, networkBytes = 2 * mb)
        assertEquals(12 * mb, usage.totalBytes)
        assertFalse(usage.isEmpty)
    }

    @Test
    fun `zero usage is empty`() {
        val usage = CacheUsage()
        assertEquals(0L, usage.totalBytes)
        assertTrue(usage.isEmpty)
    }

    @Test
    fun `details show both caches with their limits`() {
        val usage = CacheUsage(
            imagesBytes = 12 * mb,
            imagesLimitBytes = 256 * mb,
            networkBytes = 3 * mb,
            networkLimitBytes = 64 * mb,
        )
        assertEquals(
            "Картинки: 12.0 МБ из 256.0 МБ · сеть: 3.0 МБ из 64.0 МБ",
            cacheDetails(usage),
        )
    }

    @Test
    fun `missing limit is not shown`() {
        // Кэш без заданного предела (например, если его не удалось прочитать).
        val usage = CacheUsage(imagesBytes = 2 * mb, networkBytes = 0)
        assertEquals("Картинки: 2.0 МБ · сеть: пусто", cacheDetails(usage))
    }
}
