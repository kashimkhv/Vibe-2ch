package io.github.kashimkhv.vibe2ch

import io.github.kashimkhv.vibe2ch.data.settings.AppSettings
import io.github.kashimkhv.vibe2ch.data.settings.decodeBoardSorts
import io.github.kashimkhv.vibe2ch.data.settings.encodeBoardSorts
import io.github.kashimkhv.vibe2ch.domain.BoardSort
import io.github.kashimkhv.vibe2ch.domain.ThreadSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Порядок тредов хранится по доскам одной строкой в DataStore. Здесь
 * проверяется и сама строка (её пишет и читает одно и то же приложение),
 * и то, какая доска какой порядок получает.
 */
class BoardSortSettingsTest {

    @Test
    fun `board sorts survive encoding and decoding`() {
        val saved = mapOf(
            "b" to BoardSort(ThreadSort.CREATED, false),
            "po" to BoardSort(ThreadSort.BUMP, false),
            "de" to BoardSort(ThreadSort.ALPHABET, true),
        )

        assertEquals(saved, decodeBoardSorts(encodeBoardSorts(saved)))
    }

    @Test
    fun `broken entries are skipped and good ones stay`() {
        // Обрезанная запись, неизвестное имя порядка и пустой id доски —
        // всё это не должно ронять чтение настроек целиком.
        val raw = "b=REPLIES:0;po=НЕТ_ТАКОГО:1;=BUMP:0;de;au=ALPHABET:1;rf=CREATED"

        val decoded = decodeBoardSorts(raw)

        assertEquals(BoardSort(ThreadSort.REPLIES, false), decoded["b"])
        assertEquals(BoardSort(ThreadSort.ALPHABET, true), decoded["au"])
        // Без направления в записи считается «по умолчанию» — не возрастание.
        assertEquals(BoardSort(ThreadSort.CREATED, false), decoded["rf"])
        assertEquals(3, decoded.size)
    }

    @Test
    fun `empty settings mean no board has its own order`() {
        assertTrue(decodeBoardSorts(null).isEmpty())
        assertTrue(decodeBoardSorts("").isEmpty())
    }

    @Test
    fun `board with its own order ignores the common one`() {
        val settings = AppSettings(
            threadSort = ThreadSort.REPLIES,
            threadSortAscending = false,
            threadSortByBoard = mapOf("po" to BoardSort(ThreadSort.CREATED, true)),
        )

        assertEquals(BoardSort(ThreadSort.CREATED, true), settings.sortFor("po"))
        // Доска без своей записи берёт общий порядок.
        assertEquals(BoardSort(ThreadSort.REPLIES, false), settings.sortFor("b"))
    }

    @Test
    fun `board id case does not create a second entry`() {
        val settings = AppSettings(
            threadSortByBoard = mapOf("b" to BoardSort(ThreadSort.IMAGES, true)),
        )

        assertEquals(BoardSort(ThreadSort.IMAGES, true), settings.sortFor("B"))
        assertEquals(BoardSort(ThreadSort.IMAGES, true), settings.sortFor("b"))
    }

    @Test
    fun `encoding keeps board ids lowercase`() {
        val raw = encodeBoardSorts(mapOf("b" to BoardSort(ThreadSort.RATE, false)))

        assertEquals("b=RATE:0", raw)
    }
}
