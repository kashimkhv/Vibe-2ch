package dev.dvach.client.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm", Locale("ru"))

fun formatTimestamp(seconds: Long): String =
    if (seconds <= 0L) {
        ""
    } else {
        runCatching {
            Instant.ofEpochSecond(seconds).atZone(ZoneId.systemDefault()).format(DATE_FORMAT)
        }.getOrDefault("")
    }

/**
 * Имя вложения для показа. `fullname` — исходное имя, но у части постов оно
 * приходит без расширения (`image`) или вообще пустое (`""`, `".webm"` — если
 * файл загрузили без имени), а `displayname` бывает обрезанным (`1793000[...].mp4`).
 * В последнем случае берём серверное имя из `name`.
 *
 * @param path путь файла: из него достаём расширение.
 */
fun attachmentFileName(fullName: String, displayName: String, name: String, path: String): String {
    val ext = path.substringAfterLast('.', "")
    val candidates = listOf(fullName, displayName, name)
        .map { it.replace("[...]", "").trim() }
        .filter { it.isNotEmpty() }
        .map { candidate -> if (ext.isEmpty() || candidate.contains('.')) candidate else "$candidate.$ext" }
    return candidates.firstOrNull { it.substringBeforeLast('.').isNotEmpty() }
        ?: candidates.firstOrNull()
        .orEmpty()
}

/**
 * Makaba отдаёт размер файла в килобайтах (632 для картинки на 610 КБ),
 * поэтому в байты переводим сами.
 */
fun kilobytesToBytes(kilobytes: Long): Long = kilobytes * 1024L

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val units = listOf("Б", "КБ", "МБ", "ГБ")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (unit == 0) {
        "${value.roundToInt()} ${units[unit]}"
    } else {
        String.format(Locale.US, "%.1f %s", value, units[unit])
    }
}

fun formatCount(value: Int): String = when {
    value < 1000 -> value.toString()
    value < 100_000 -> String.format(Locale.US, "%.1fК", value / 1000.0)
    else -> String.format(Locale.US, "%.0fК", value / 1000.0)
}

fun formatRelativeTime(timestampSeconds: Long, nowMillis: Long = System.currentTimeMillis()): String {
    if (timestampSeconds <= 0) return ""
    val diff = (nowMillis / 1000 - timestampSeconds).coerceAtLeast(0)
    return when {
        diff < 60 -> "только что"
        diff < 3600 -> "${diff / 60} мин назад"
        diff < 86_400 -> "${diff / 3600} ч назад"
        diff < 2_592_000 -> "${diff / 86_400} дн назад"
        else -> formatTimestamp(timestampSeconds).substringBefore(' ')
    }
}

fun postUrl(boardId: String, threadNum: Int, postNum: Int = threadNum): String =
    "https://2ch.hk/$boardId/res/$threadNum.html#${postNum}"

/** Русские склонения: 1 пост, 2 поста, 11 постов. */
fun plural(count: Int, one: String, few: String, many: String): String {
    val mod100 = count % 100
    val mod10 = count % 10
    return when {
        mod100 in 11..14 -> many
        mod10 == 1 -> one
        mod10 in 2..4 -> few
        else -> many
    }
}

fun postsLabel(count: Int): String = "${formatCount(count)} ${plural(count, "пост", "поста", "постов")}"

fun filesLabel(count: Int): String = "${formatCount(count)} ${plural(count, "файл", "файла", "файлов")}"

fun imagesLabel(count: Int): String =
    "${formatCount(count)} ${plural(count, "картинка", "картинки", "картинок")}"

/** «Видео» не склоняется: 1 видео, 5 видео. */
fun videosLabel(count: Int): String = "${formatCount(count)} видео"
