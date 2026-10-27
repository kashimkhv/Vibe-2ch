package dev.dvach.client.util

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

/** Убирает HTML-разметку: движок отдаёт `info` досок и часть постов с тегами. */
fun stripHtml(html: String): String =
    decodeHtmlEntities(html.replace(TAG_REGEX, " "))
        .replace(SPACES, " ")
        .trim()
