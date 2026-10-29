package dev.dvach.client

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import dev.dvach.client.domain.model.Attachment
import dev.dvach.client.ui.media.MediaPlaylist

class MediaPlaylistTest {

    private fun file(name: String, video: Boolean = false) = Attachment(
        url = "https://2ch.hk/b/src/$name",
        thumbnailUrl = "",
        name = name,
        sizeBytes = 0,
        width = 0,
        height = 0,
        isVideo = video,
    )

    @Test
    fun `playlist remembers items and the opened index`() {
        val playlist = MediaPlaylist()
        val files = listOf(file("1.jpg"), file("2.webm", video = true))
        playlist.set(files, 1)

        assertEquals(2, playlist.items.size)
        assertEquals(1, playlist.index)
    }

    @Test
    fun `index is clamped to the playlist`() {
        val playlist = MediaPlaylist()
        playlist.set(listOf(file("1.jpg")), 5)
        assertEquals(0, playlist.index)

        playlist.set(listOf(file("1.jpg")), -3)
        assertEquals(0, playlist.index)
    }

    @Test
    fun `swiping to another file is remembered for the thread`() {
        val playlist = MediaPlaylist()
        val files = listOf(file("1.jpg"), file("2.jpg"), file("3.webm", video = true))
        playlist.set(files, 0)

        playlist.select(2)

        assertEquals(2, playlist.index)
        // Тред должен встать на пост этого файла.
        assertEquals(files[2], playlist.consumeSwiped())
    }

    @Test
    fun `without swiping the thread position is left alone`() {
        val playlist = MediaPlaylist()
        playlist.set(listOf(file("1.jpg"), file("2.jpg")), 1)

        // Просмотрщик открыли и закрыли на том же файле.
        assertNull(playlist.consumeSwiped())
    }

    @Test
    fun `the swiped file is consumed once`() {
        val playlist = MediaPlaylist()
        playlist.set(listOf(file("1.jpg"), file("2.jpg")), 0)
        playlist.select(1)

        assertEquals("2.jpg", playlist.consumeSwiped()?.name)
        // Повторный возврат в тред уже ничего не прокручивает.
        assertNull(playlist.consumeSwiped())
    }

    @Test
    fun `select on an empty playlist does nothing`() {
        // Процесс пересоздали: в плейлисте пусто, а маршрут остался.
        val playlist = MediaPlaylist()
        playlist.select(3)
        assertEquals(0, playlist.index)
        assertNull(playlist.consumeSwiped())
    }

    @Test
    fun `empty playlist keeps index at zero`() {
        val playlist = MediaPlaylist()
        playlist.set(emptyList(), 4)
        assertEquals(0, playlist.index)
        assertEquals(0, playlist.items.size)
    }
}
