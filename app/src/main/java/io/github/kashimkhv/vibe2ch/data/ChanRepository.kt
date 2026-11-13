package io.github.kashimkhv.vibe2ch.data

import io.github.kashimkhv.vibe2ch.data.remote.DvachApi
import io.github.kashimkhv.vibe2ch.data.remote.dto.BoardDto
import io.github.kashimkhv.vibe2ch.data.remote.dto.FileDto
import io.github.kashimkhv.vibe2ch.data.remote.dto.PostDto
import io.github.kashimkhv.vibe2ch.data.remote.dto.ThreadDto
import io.github.kashimkhv.vibe2ch.data.remote.dto.ThreadsPageDto
import io.github.kashimkhv.vibe2ch.di.BASE_URL
import io.github.kashimkhv.vibe2ch.domain.model.Attachment
import io.github.kashimkhv.vibe2ch.domain.model.Board
import io.github.kashimkhv.vibe2ch.domain.model.Post
import io.github.kashimkhv.vibe2ch.domain.model.ThreadSummary
import io.github.kashimkhv.vibe2ch.domain.model.ThreadsPage
import io.github.kashimkhv.vibe2ch.util.attachmentFileName
import io.github.kashimkhv.vibe2ch.util.decodeHtmlEntities
import io.github.kashimkhv.vibe2ch.util.kilobytesToBytes
import io.github.kashimkhv.vibe2ch.util.stripHtml
import javax.inject.Inject
import javax.inject.Singleton

interface ChanRepository {
    suspend fun boards(): List<Board>

    /** Одна доска по id: нужна форме ответа (поля, лимиты, разрешённые файлы). */
    suspend fun board(boardId: String): Board?

    suspend fun index(boardId: String, page: Int): ThreadsPage
    suspend fun thread(boardId: String, threadNum: Int): List<Post>
}

@Singleton
class NetworkChanRepository @Inject constructor(
    private val api: DvachApi,
) : ChanRepository {

    override suspend fun boards(): List<Board> =
        api.boards()
            .filter { it.id.isNotBlank() }
            .map { it.toDomain() }
            .sortedWith(compareBy({ it.category }, { it.name }))

    override suspend fun board(boardId: String): Board? =
        api.boards().firstOrNull { it.id == boardId }?.toDomain()

    override suspend fun index(boardId: String, page: Int): ThreadsPage =
        if (page == 0) {
            // `index.json` — это и есть живая первая страница доски
            // (`current_page: 0`), вместе с закрепами. Постраничные файлы
            // начинаются **после** неё: `1.json` — уже вторая страница,
            // `2.json` — третья. Раньше клиент запрашивал `page + 1`, поэтому
            // первой страницы не показывал никогда: на /news/ не было видно
            // четырёх свежих новостей, а на /b/ лента начиналась со второй
            // страницы. Счётчик страниц движка это подтверждает: у index.json
            // `current_page: 0`, у 1.json — 1.
            api.index(boardId).toDomain(boardId)
        } else {
            api.indexPage(boardId, page).toDomain(boardId)
        }

    override suspend fun thread(boardId: String, threadNum: Int): List<Post> =
        api.thread(boardId, threadNum)
            .threads
            .firstOrNull()
            ?.posts
            ?.toThreadPosts(boardId)
            .orEmpty()
}

private fun ThreadsPageDto.toDomain(boardId: String) = ThreadsPage(
    // Движок нумерует страницы с нуля, как и UI: index.json — 0, 1.json — 1.
    currentPage = currentPage.coerceAtLeast(0),
    isIndex = isIndex,
    threads = threads.mapNotNull { it.toDomain(boardId) },
    threadsPerPage = board?.threadsPerPage?.takeIf { it > 0 } ?: 20,
    maxPages = board?.maxPages?.takeIf { it > 0 } ?: Int.MAX_VALUE,
    // Метрику движок присылает в каждом файле страницы, а не только в index.json:
    // отдельный запрос за ней не нужен.
    boardSpeed = boardSpeed,
)

internal fun ThreadDto.toDomain(boardId: String): ThreadSummary? {
    val op = posts.firstOrNull() ?: return null
    return ThreadSummary(
        boardId = boardId,
        num = op.num,
        // Движок отдаёт имя и тему с HTML-сущностями и разметкой
        // («Аноним&nbsp;ID:&nbsp;<span id=…>Имя</span>»), а в UI они выводятся
        // как обычный текст — чистим здесь, на границе DTO.
        subject = stripHtml(op.subject),
        commentHtml = op.comment,
        name = displayName(op.name),
        date = op.date,
        timestamp = op.timestamp,
        // Счётчики движка считают только посты вне блока `posts`. В index.json
        // тред приходит как ОП и три последних ответа, а `posts_count` — это
        // «сколько ещё постов в треде». То есть 15 в `posts_count` при четырёх
        // постах в ответе означают 19 постов, и именно 19 видно в треде.
        // Проверено на пятидесяти тредах пяти досок: `posts_count + posts.size`
        // всегда совпадает с числом постов в `res/<номер>.json`, а само
        // `posts_count` — нет. Без поправки последние три ответа не попадали
        // в счётчик: только что ответил — а в ленте по-прежнему старое число.
        postsCount = postsCount + posts.size,
        // У файлов та же природа (`files_count` — вложения вне блока `posts`),
        // но движок вдобавок отстаёт на больших тредах: на /b/ у треда с 396
        // файлами счётчик показывал 97. Поэтому сумма превью — нижняя граница,
        // а не точное число: она вытягивает свежие треды, где вложения есть
        // только в ОП и последних ответах (иначе в ленте пропадает
        // «3 картинки», а сортировка по картинкам ставит такие треды в самый
        // низ). Проверено на сотне тредов: сумма ни разу не превысила
        // настоящее число файлов.
        filesCount = filesCount + posts.sumOf { it.files.size },
        views = views,
        // `sticky` — не флаг, а ранг закрепа: движок ставит 1–69, и любое
        // ненулевое значение означает «держим сверху». На /po/ и /d/ ранг
        // лежит только в ОП-посте, на уровне треда его нет вовсе.
        isSticky = sticky > 0 || op.sticky > 0,
        isEndless = endless == 1 || op.endless == 1,
        isClosed = closed == 1,
        attachments = op.files.map { it.toDomain() },
        replies = posts.drop(1).takeLast(3).map { it.toDomain(boardId) },
    )
}

/**
 * Посты треда в доменные модели. ОП — всегда первый пост ответа: в API поле
 * `op` у постов треда равно 0 даже у стартового, поэтому по нему не отличить
 * автора треда от отвечающих.
 */
internal fun List<PostDto>.toThreadPosts(boardId: String): List<Post> =
    mapIndexed { index, dto ->
        dto.toDomain(boardId).let { if (index == 0) it.copy(isOp = true) else it }
    }

/**
 * Имя для шапки поста.
 *
 * `stripHtml` убирает разметку и сущности. Отдельно срезается приставка
 * «Аноним ID:» — но только если после неё есть что показать: на /d/ имя
 * анонима приходит как `Аноним ID: <span …>Ехидный Вертер</span>`, и в шапке
 * нужен сам ник, он и так занимает всю ширину. Обычный «Аноним», имя с
 * других досок и трип остаются как есть.
 */
internal fun displayName(raw: String): String {
    val plain = stripHtml(raw)
    val withoutPrefix = plain.replaceFirst(ANONYMOUS_ID_PREFIX, "")
    return when {
        withoutPrefix.isNotBlank() -> withoutPrefix
        // Осталась одна приставка без ника — считаем анонимом.
        plain.isNotBlank() -> "Аноним"
        else -> plain
    }
}

private val ANONYMOUS_ID_PREFIX = Regex("""^Аноним\s+ID:\s*""", RegexOption.IGNORE_CASE)

internal fun PostDto.toDomain(boardId: String) = Post(
    // В ответе треда поле `board` заполнено, но если сервер его не прислал —
    // берём доску из маршрута, чтобы пост не остался без идентификатора.
    boardId = board.ifBlank { boardId },
    num = num,
    parent = parent,
    name = displayName(name),
    subject = stripHtml(subject),
    commentHtml = comment,
    date = date,
    timestamp = timestamp,
    trip = stripHtml(trip).ifBlank { null },
    isOp = isOp == 1,
    // Как и у треда: ранг закрепа, а не булево 0/1.
    isSticky = sticky > 0,
    isClosed = closed == 1,
    isBanned = banned == 1,
    files = files.map { it.toDomain() },
)

private fun FileDto.toDomain(): Attachment = Attachment(
    url = path.absoluteUrl(),
    thumbnailUrl = listOf(thumbnail, thumbnailPath, path)
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
        .absoluteUrl(),
    name = decodeHtmlEntities(attachmentFileName(fullName, displayname, name, path)),
    sizeBytes = kilobytesToBytes(size),
    width = width,
    height = height,
    duration = duration.takeIf { it.isNotBlank() && it != "00:00:00" },
    isVideo = VIDEO_EXTENSIONS.any { path.endsWith(it, ignoreCase = true) },
    extension = path.substringAfterLast('.', "").takeIf { '.' in path && it.length in 2..4 }.orEmpty(),
)

private fun BoardDto.toDomain() = Board(
    id = id,
    name = name,
    category = category,
    info = stripHtml(info),
    threadsPerPage = threadsPerPage,
    maxPages = maxPages,
    bumpLimit = bumpLimit,
    maxComment = maxComment,
    enablePosting = enablePosting,
    enableNames = enableNames,
    enableSubject = enableSubject,
    enableTrips = enableTrips,
    enableFlags = enableFlags,
    enableIcons = enableIcons,
    enableLikes = enableLikes,
    enableSage = enableSage,
    fileTypes = fileTypes,
    maxFilesSize = maxFilesSize,
)

/**
 * В JSON пути файлов относительные (`/b/src/...`), а в превью иногда `../`.
 * Приводим к абсолютному виду для OkHttp/Coil.
 */
private fun String.absoluteUrl(): String = when {
    isBlank() -> ""
    startsWith("http://") || startsWith("https://") -> this
    startsWith("//") -> "https:$this"
    startsWith("/") -> BASE_URL.trimEnd('/') + this
    else -> BASE_URL.trimEnd('/') + "/" + this
}

/**
 * Имя файла для показа. `fullname` — исходное имя, но у части постов оно бывает
 * без расширения (`image`), а `displayname` приходит обрезанным (`1793000[...].mp4`).
 */

private val VIDEO_EXTENSIONS = listOf(".webm", ".mp4", ".mov", ".mkv")
