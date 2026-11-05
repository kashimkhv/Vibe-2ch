package dev.dvach.client.ui.navigation

import java.net.URI
import java.net.URISyntaxException

/**
 * Ссылка двача, открытая из другого приложения. Понимаем только то, что умеем
 * показать: доску и тред с необязательным номером поста.
 *
 * @property boardId доска в нижнем регистре: `b`, `dev`, `s4s`.
 * @property threadNum номер треда.
 * @property postNum номер поста из якоря `#123`; 0 — якоря не было.
 */
sealed interface DvachLink {
    data class Board(val boardId: String) : DvachLink
    data class Thread(
        val boardId: String,
        val threadNum: Int,
        val postNum: Int = 0,
    ) : DvachLink
}

/**
 * Разбор веб-ссылок движка. Работает на чистом `java.net.URI`, без
 * `android.net.Uri`, чтобы правила проверялись обычными unit-тестами.
 *
 * Поддерживаются адреса `2ch.hk` и зеркала `2ch.su` (в том числе с `www.`):
 *
 * - `/{доска}/` и `/{доска}/index.html` — список тредов;
 * - `/{доска}/catalog.html` — тоже список: каталога в клиенте пока нет;
 * - `/{доска}/{страница}.html` — список с первой страницы;
 * - `/{доска}/res/{тред}.html` — тред;
 * - `.../{тред}.html#123` — тред, прокрученный к посту 123.
 *
 * Старые мобильные адреса с ведущим `/mobile/` тоже понимаем. Чужие хосты,
 * не-http схемы и мусор возвращают `null`: приложение просто откроется как
 * обычно, а не покажет ошибку.
 */
object DvachLinks {

    /** Домены движка, которые клиент умеет открывать. */
    private val hosts = setOf("2ch.hk", "2ch.su")

    private val boardId = Regex("[a-z0-9]{1,16}")

    /** `/b/2.html` — старая постраничная адресация; открываем первую страницу. */
    private val legacyPage = Regex("\\d{1,4}\\.html")

    fun parse(raw: String?): DvachLink? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null

        val uri = try {
            URI(text)
        } catch (_: URISyntaxException) {
            return null
        }

        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return null

        val host = uri.host?.lowercase()?.removePrefix("www.")
        if (host !in hosts) return null

        // Пустые сегменты отбрасываем: `/b//`, `/b/` и `/b` — одна и та же доска.
        val segments = uri.path.orEmpty().split('/').filter { it.isNotEmpty() }
        // Старый мобильный вид: /mobile/b/res/123.html.
        val parts = if (segments.firstOrNull()?.equals("mobile", ignoreCase = true) == true) {
            segments.drop(1)
        } else {
            segments
        }

        val board = parts.firstOrNull()?.lowercase()?.takeIf { boardId.matches(it) }
            ?: return null
        if (parts.size == 1) return DvachLink.Board(board)

        return when (val section = parts[1].lowercase()) {
            "res" -> thread(board, parts, uri.fragment)
            "index.html", "catalog", "catalog.html" -> DvachLink.Board(board)
            else -> if (legacyPage.matches(section)) DvachLink.Board(board) else null
        }
    }

    private fun thread(board: String, parts: List<String>, fragment: String?): DvachLink? {
        val file = parts.getOrNull(2) ?: return null
        val num = file.substringBefore('.').toIntOrNull()?.takeIf { it > 0 } ?: return null
        val post = fragment?.toIntOrNull()?.takeIf { it > 0 } ?: 0
        return DvachLink.Thread(board, num, post)
    }

    /** Первый http(s)-адрес в произвольном тексте. */
    private val urlInText = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)

    /** Знаки препинания, которые прилипают к адресу в обычном тексте. */
    private const val TRAILING_PUNCTUATION = ".,;:!?)]}>»\"'"

    /**
     * Ссылка из «Поделиться»: чужие приложения присылают не `data`, а текст
     * вида «Заголовок https://2ch.hk/b/res/123.html#45». Берём первый адрес
     * и разбираем его обычным [parse].
     */
    fun parseFromText(text: String?): DvachLink? {
        val raw = text?.trim().orEmpty()
        if (raw.isEmpty()) return null
        val match = urlInText.find(raw) ?: return null
        return parse(match.value.trimEnd { it in TRAILING_PUNCTUATION })
    }
}
