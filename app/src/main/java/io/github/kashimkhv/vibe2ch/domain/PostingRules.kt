package io.github.kashimkhv.vibe2ch.domain

/**
 * Правила формы поста, вынесенные из UI: их видно в тестах, и они одинаковы
 * для ответа в тред и для нового треда.
 */

/** Жёсткий предел, который клиент не даёт превысить даже при лимите доски выше. */
const val MAX_UPLOAD_BYTES = 32 * 1024 * 1024

/**
 * Цитата, с которой начинается ответ на пост. `0` — обычный ответ без цитаты.
 * Движок понимает `>>123` только в начале строки, поэтому перевод строки обязателен.
 */
fun quotePrefix(postNum: Int): String = if (postNum > 0) ">>$postNum\n" else ""

/**
 * Ошибка текста поста. Пустой пост допустим только с файлом: движок принимает
 * «только картинку» (с добавленным им самим текстом).
 */
fun postingCommentError(comment: String, hasFile: Boolean, maxComment: Int): String? = when {
    comment.isBlank() && !hasFile ->
        "Введите текст поста или прикрепите файл"
    maxComment > 0 && comment.length > maxComment ->
        "Текст длиннее предела доски: ${comment.length} из $maxComment символов"
    else -> null
}

/**
 * Ошибка вложения: расширение должно быть среди разрешённых доской, размер —
 * в пределах доски и клиентского потолка [MAX_UPLOAD_BYTES].
 *
 * @param maxFilesSizeKb лимит движка в килобайтах, 0 — не задан.
 */
fun uploadError(
    fileName: String,
    sizeBytes: Int,
    allowedExtensions: List<String>,
    maxFilesSizeKb: Int,
): String? {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    if (extension.isEmpty()) {
        return "У файла нет расширения — движок его не примет"
    }
    val allowed = allowedExtensions
        .map { it.trim().removePrefix(".").lowercase() }
        .filter { it.isNotEmpty() }
    if (allowed.isNotEmpty() && extension !in allowed) {
        return "Файлы .$extension на этой доске не принимаются. Можно: ${allowed.joinToString(", ")}"
    }
    val boardLimit = maxFilesSizeKb.toLong() * 1024
    if (boardLimit > 0 && sizeBytes > boardLimit) {
        return "Файл больше ${maxFilesSizeKb / 1024} МБ — это предел доски"
    }
    if (sizeBytes > MAX_UPLOAD_BYTES) {
        return "Файл больше ${MAX_UPLOAD_BYTES / (1024 * 1024)} МБ"
    }
    return null
}

/** Подсказка под кнопкой выбора файла: что и сколько можно прикрепить. */
fun uploadHint(allowedExtensions: List<String>, maxFilesSizeKb: Int): String {
    val types = allowedExtensions
        .map { it.trim().removePrefix(".").lowercase() }
        .filter { it.isNotEmpty() }
        .distinct()
    val size = when {
        maxFilesSizeKb >= 1024 -> "до ${maxFilesSizeKb / 1024} МБ"
        maxFilesSizeKb > 0 -> "до $maxFilesSizeKb КБ"
        else -> "до ${MAX_UPLOAD_BYTES / (1024 * 1024)} МБ"
    }
    return if (types.isEmpty()) size else "${types.joinToString(", ")} · $size"
}
