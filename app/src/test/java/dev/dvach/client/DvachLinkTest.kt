package dev.dvach.client

import dev.dvach.client.ui.navigation.DvachLink
import dev.dvach.client.ui.navigation.DvachLinks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Ссылки из чужих приложений. Разбор намеренно не зависит от `android.net.Uri`,
 * поэтому проверяется обычным JUnit: ошибка в номере треда отправила бы
 * человека не туда, а не просто показала некрасивый экран.
 */
class DvachLinkTest {

    @Test
    fun `thread link keeps board, number and post anchor`() {
        assertEquals(
            DvachLink.Thread("b", 337975790, 337975861),
            DvachLinks.parse("https://2ch.hk/b/res/337975790.html#337975861"),
        )
        // Без якоря пост не выделяем.
        assertEquals(
            DvachLink.Thread("dev", 123, 0),
            DvachLinks.parse("https://2ch.hk/dev/res/123.html"),
        )
        // Расширение не обязательно: ссылки на API и старые копии бывают без него.
        assertEquals(
            DvachLink.Thread("po", 42, 0),
            DvachLinks.parse("https://2ch.hk/po/res/42"),
        )
    }

    @Test
    fun `board links open the thread list`() {
        listOf(
            "https://2ch.hk/b/",
            "https://2ch.hk/b",
            "https://2ch.hk/b/index.html",
            "https://2ch.hk/b/catalog.html",
            // Старая постраничная адресация: каталога страниц у нас нет,
            // открываем первую.
            "https://2ch.hk/b/3.html",
        ).forEach { url ->
            assertEquals(url, DvachLink.Board("b"), DvachLinks.parse(url))
        }
    }

    @Test
    fun `mirror, www and mobile addresses are the same thread`() {
        val expected = DvachLink.Thread("b", 100, 0)
        listOf(
            "http://2ch.hk/b/res/100.html",
            "https://2ch.su/b/res/100.html",
            "https://www.2ch.hk/b/res/100.html",
            "https://www.2ch.su/b/res/100.html?mobile=1",
            "https://2ch.hk/mobile/b/res/100.html",
            "https://2ch.hk/b/res/100.html?from=share",
        ).forEach { url ->
            assertEquals(url, expected, DvachLinks.parse(url))
        }
    }

    @Test
    fun `board id is normalised to lower case`() {
        assertEquals(DvachLink.Board("b"), DvachLinks.parse("https://2ch.hk/B/"))
        assertEquals(
            DvachLink.Thread("s4s", 7, 0),
            DvachLinks.parse("https://2ch.hk/S4S/res/7.html"),
        )
    }

    @Test
    fun `foreign and unsupported links are ignored`() {
        listOf(
            null,
            "",
            "   ",
            "https://example.com/b/res/1.html",
            "https://2ch.hk.evil.com/b/res/1.html",
            "ftp://2ch.hk/b/res/1.html",
            "javascript:alert(1)",
            "https://2ch.hk/",                 // доски нет
            "https://2ch.hk/b/user/profile",   // служебный путь движка
            "https://2ch.hk/b/1.json",         // API, а не страница
            "https://2ch.hk/b/res/",           // треда нет
            "https://2ch.hk/b/res/abc.html",   // номер не число
            "https://2ch.hk/b/res/0.html",     // нулевого треда не бывает
            "https://2ch.hk/b/res/99999999999.html", // больше Int
        ).forEach { url ->
            assertNull(url, DvachLinks.parse(url))
        }
    }

    @Test
    fun `garbage anchor is ignored but the thread still opens`() {        assertEquals(
            DvachLink.Thread("b", 5, 0),
            DvachLinks.parse("https://2ch.hk/b/res/5.html#top"),
        )
        // Якорь может быть не числом поста, а служебным словом.
        assertEquals(
            DvachLink.Thread("b", 5, 0),
            DvachLinks.parse("https://2ch.hk/b/res/5.html#"),
        )
    }

    @Test
    fun `shared text is parsed by its first engine link`() {
        // Так выглядит «Поделиться» из браузера: заголовок и адрес в одном тексте.
        assertEquals(
            DvachLink.Thread("b", 337975790, 337975929),
            DvachLinks.parseFromText(
                "Александр Ладан — новый нетсталкинг\nhttps://2ch.hk/b/res/337975790.html#337975929",
            ),
        )
        // Ссылка может стоять в середине текста и в скобках.
        assertEquals(
            DvachLink.Board("soc"),
            DvachLinks.parseFromText("смотри (https://2ch.hk/soc/) — там всё"),
        )
        // Хвостовая пунктуация не должна ломать номер треда.
        assertEquals(
            DvachLink.Thread("dev", 42, 0),
            DvachLinks.parseFromText("тред: https://2ch.hk/dev/res/42.html."),
        )
    }

    @Test
    fun `shared text without an engine link is ignored`() {
        listOf(
            null,
            "",
            "   ",
            "просто текст без ссылки",
            "https://example.com/b/res/1.html",
            "ftp://2ch.hk/b/res/1.html",
        ).forEach { text ->
            assertNull(text, DvachLinks.parseFromText(text))
        }
    }
}
