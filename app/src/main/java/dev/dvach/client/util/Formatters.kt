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
 * Имя файла для записи в галерею. Имя приходит от движка (это `fullname`
 * загрузившего), поэтому в нём может оказаться что угодно: `/`, `\`, `..`
 * и управляющие символы. На старых версиях Android файл пишется через
 * `File(dir, name)`, где `..` выводит за пределы папки приложения, — поэтому
 * от имени оставляем только последний сегмент и вычищаем управляющие символы.
 * Точки `.` и `..` заменяются запасным именем: это не файлы, а ссылки на
 * каталоги.
 */
fun safeFileName(raw: String, fallback: String = "file"): String {
    val base = raw.substringAfterLast('/').substringAfterLast('\\')
    val cleaned = base.filterNot { it.isISOControl() }.trim()
    if (cleaned.isEmpty() || cleaned == "." || cleaned == "..") return fallback
    if (cleaned.length <= MAX_FILE_NAME_LENGTH) return cleaned

    // Длинное имя обрезаем, но расширение сохраняем: по нему галерея выбирает,
    // чем открывать файл.
    val ext = cleaned.substringAfterLast('.', "")
    return if (ext.length in 1..8 && cleaned.length > ext.length + 1) {
        cleaned.take(MAX_FILE_NAME_LENGTH - ext.length - 1) + "." + ext
    } else {
        cleaned.take(MAX_FILE_NAME_LENGTH)
    }
}

/** Предел имени файла: 255 байт в MediaStore, кириллица занимает по два. */
private const val MAX_FILE_NAME_LENGTH = 120

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

/**
 * Размер кэша словами: у пустого кэша «пусто» понятнее, чем «0 Б»,
 * а нулевой предел роста («сколько влезет») в интерфейсе не показываем.
 */
fun formatCacheSize(bytes: Long): String = if (bytes <= 0) "пусто" else formatFileSize(bytes)

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

/**
 * Время в шапке поста и карточки треда: точная дата или относительная, если
 * настройка включена. `fallback` — строка `date` от движка (московское время),
 * она нужна только когда `timestamp` не пришёл вовсе.
 *
 * @param nowMillis текущее время для относительных подписей; экран передаёт
 *   его из одного тикера на весь список.
 */
fun formatPostTime(
    timestampSeconds: Long,
    fallback: String,
    relative: Boolean,
    nowMillis: Long = System.currentTimeMillis(),
): String {
    val formatted = if (relative) {
        formatRelativeTime(timestampSeconds, nowMillis)
    } else {
        formatTimestamp(timestampSeconds)
    }
    return formatted.ifBlank { fallback }
}

fun postUrl(boardId: String, threadNum: Int, postNum: Int = threadNum): String =
    "${threadUrl(boardId, threadNum)}#${postNum}"

/** Ссылка на тред целиком — для «Поделиться». */
fun threadUrl(boardId: String, threadNum: Int): String =
    "https://2ch.hk/$boardId/res/$threadNum.html"

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

/**
 * Активность доски словами: «≈ 330 постов/ч». До десяти — с десятой долей
 * («4,5 поста/ч»): у тихих досок округление до целого давало бы «4» и «5»
 * у совсем разных досок. Дробное число в русском требует «поста», целое —
 * обычного склонения. Выше сотни округляем до десятков: точность своей
 * оценки всё равно в разы.
 */
fun postsPerHourLabel(rate: Double): String = when {
    rate < 0.05 -> "0 постов/ч"
    rate < 10 -> String.format(Locale.US, "%.1f", rate).replace('.', ',') + " поста/ч"
    else -> {
        val rounded = if (rate >= 100) (rate / 10).roundToInt() * 10 else rate.roundToInt()
        postsLabel(rounded) + "/ч"
    }
}

/**
 * Число движка (`board_speed`) — точное и по всей доске, поэтому без
 * округления до десятков: 472 так и остаётся 472.
 */
fun postsPerHourExactLabel(rate: Double): String = postsLabel(rate.roundToInt()) + "/ч"

/** «Видео» не склоняется: 1 видео, 5 видео. */
fun videosLabel(count: Int): String = "${formatCount(count)} видео"
