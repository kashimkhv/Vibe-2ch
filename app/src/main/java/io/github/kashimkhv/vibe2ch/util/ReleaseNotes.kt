package io.github.kashimkhv.vibe2ch.util

/**
 * Release notes с GitHub приходят в Markdown. В диалоге обновления показывать
 * звёздочки и решётки незачем — читаем их как разметку, а не как текст.
 *
 * Поддерживается ровно то, что встречается в заметках проекта: заголовки,
 * списки, выделение, ссылки и блоки кода. Полноценный Markdown-рендерер здесь
 * был бы лишней зависимостью.
 */
fun releaseNotesText(markdown: String): String {
    if (markdown.isBlank()) return ""
    val text = markdown
        .replace("\r\n", "\n")
        .replace(CODE_FENCE, "")
        .replace(LINK, "$1 ($2)")
        .replace(HEADING, "")
        .replace(BULLET, "• ")
        .replace(EMPHASIS, "")
        .replace("`", "")
    return text
        .replace(EXTRA_BLANK_LINES, "\n\n")
        .trim()
}

private val CODE_FENCE = Regex("(?m)^[ \\t]*```[^\\n]*$")
private val HEADING = Regex("(?m)^#{1,6}[ \\t]*")
private val BULLET = Regex("(?m)^[ \\t]*[-*][ \\t]+")
private val LINK = Regex("\\[([^]]+)]\\(([^)]+)\\)")
private val EMPHASIS = Regex("\\*\\*|__")
private val EXTRA_BLANK_LINES = Regex("\\n{3,}")
