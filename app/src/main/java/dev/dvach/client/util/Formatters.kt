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
