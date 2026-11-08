package io.github.kashimkhv.vibe2ch.util

private val TAG_REGEX = Regex("<[^>]+>")
private val SPACES = Regex("\\s+")
private val ENTITY_REGEX = Regex("&(#x?[0-9a-fA-F]+|[a-zA-Z][a-zA-Z0-9]*);")

private val NAMED_ENTITIES = mapOf(
    "amp" to "&",
    "lt" to "<",
    "gt" to ">",
    "quot" to "\"",
    "apos" to "'",
    "nbsp" to " ",
    "hellip" to "…",
    "mdash" to "—",
    "ndash" to "–",
    "laquo" to "«",
    "raquo" to "»",
    "copy" to "©",
    "reg" to "®",
    "deg" to "°",
    "middot" to "·",
    "bull" to "•",
    "lsquo" to "‘",
    "rsquo" to "’",
    "ldquo" to "“",
    "rdquo" to "”",
    "euro" to "€",
    "trade" to "™",
)

/**
 * Раскрывает HTML-сущности. Движок отдаёт и именованные (`&nbsp;`),
 * и числовые (`&#47;`, `&#x2F;`) — без этого в тексте видны `WebM&#47;MP4`.
 */
fun decodeHtmlEntities(text: String): String {
    if (!text.contains('&')) return text
    return ENTITY_REGEX.replace(text) { match ->
        val entity = match.groupValues[1]
        when {
            entity.startsWith("#x", ignoreCase = true) ->
                codePoint(entity.substring(2), 16) ?: match.value
            entity.startsWith("#") ->
                codePoint(entity.substring(1), 10) ?: match.value
            else -> NAMED_ENTITIES[entity.lowercase()] ?: match.value
        }
    }
}

private fun codePoint(raw: String, radix: Int): String? =
    raw.toIntOrNull(radix)
        ?.takeIf { it in 1..0x10FFFF }
        ?.let { runCatching { String(Character.toChars(it)) }.getOrNull() }

/**
 * Приводит `href` из поста к адресу, который можно отдать системе.
 *
 * Движок экранирует в ссылке слэши: `https:&#47;&#47;life.ru&#47;p&#47;…`.
 * Если открыть такую строку как есть, браузер получает
 * `https://&#47;&#47;life.ru…` и не может разрешить домен — ссылка «ломается».
 * Поэтому сущности раскрываются до запуска `ACTION_VIEW`.
 *
 * Заодно отбрасываются схемы, которым не место в клиенте (`javascript:`,
 * `data:`), протокол-относительные `//host/path` получают `https:`, а
 * внутренние пути движка (`/b/res/123.html`) — хост `2ch.hk`.
 */
fun decodeLinkHref(raw: String?): String? {
    val text = decodeHtmlEntities(raw ?: return null).trim()
    if (text.isEmpty()) return null

    val normalized = when {
        text.startsWith("//") -> "https:$text"
        text.startsWith("/") -> "$DVACH_ORIGIN$text"
        else -> text
    }
    val scheme = normalized.substringBefore(':', missingDelimiterValue = "").lowercase()
    return normalized.takeIf { it.contains(':') && scheme in LINK_SCHEMES }
}

private const val DVACH_ORIGIN = "https://2ch.hk"

/** Схемы, которые открывает система: браузер или почтовый клиент. */
private val LINK_SCHEMES = setOf("http", "https", "mailto")

/** Убирает HTML-разметку: движок отдаёт `info` досок и часть постов с тегами. */
fun stripHtml(html: String): String =
    decodeHtmlEntities(html.replace(TAG_REGEX, " "))
        .replace(SPACES, " ")
        .trim()

private val QUOTE_BREAKS = Regex("(?i)<br\\s*/?>|</(?:p|div|blockquote|li|tr|h[1-6])\\s*>")
private val INLINE_SPACES = Regex("[ \\t\\u00A0]+")
private val NEWLINE_PADDING = Regex("[ \\t]*\\n[ \\t]*")
private val EXTRA_NEWLINES = Regex("\\n{2,}")

/**
 * Текст поста для цитаты: блочные теги становятся переводами строк, остальная
 * разметка убирается. В отличие от [stripHtml] сохраняет абзацы — иначе
 * цитата длинного поста склеивается в одну строку.
 */
fun quoteText(html: String): String =
    decodeHtmlEntities(html.replace(QUOTE_BREAKS, "\n").replace(TAG_REGEX, " "))
        .replace(INLINE_SPACES, " ")
        .replace(NEWLINE_PADDING, "\n")
        .replace(EXTRA_NEWLINES, "\n")
        .trim()
