package dev.dvach.client.data

import dev.dvach.client.data.remote.DvachApi
import dev.dvach.client.data.remote.dto.BoardDto
import dev.dvach.client.data.remote.dto.FileDto
import dev.dvach.client.data.remote.dto.PostDto
import dev.dvach.client.data.remote.dto.ThreadDto
import dev.dvach.client.data.remote.dto.ThreadsPageDto
import dev.dvach.client.di.BASE_URL
import dev.dvach.client.domain.model.Attachment
import dev.dvach.client.domain.model.Board
import dev.dvach.client.domain.model.Post
import dev.dvach.client.domain.model.ThreadSummary
import dev.dvach.client.domain.model.ThreadsPage
import dev.dvach.client.util.attachmentFileName
import dev.dvach.client.util.decodeHtmlEntities
import dev.dvach.client.util.kilobytesToBytes
import dev.dvach.client.util.stripHtml
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
        // В приложении страницы с нуля, в API — с единицы.
        api.indexPage(boardId, page + 1).toDomain(boardId)

    override suspend fun thread(boardId: String, threadNum: Int): List<Post> =
        api.thread(boardId, threadNum)
            .threads
            .firstOrNull()
            ?.posts
            ?.toThreadPosts(boardId)
            .orEmpty()
}

private fun ThreadsPageDto.toDomain(boardId: String) = ThreadsPage(
    currentPage = (currentPage - 1).coerceAtLeast(0),
    isIndex = isIndex,
    threads = threads.mapNotNull { it.toDomain(boardId) },
    threadsPerPage = board?.threadsPerPage?.takeIf { it > 0 } ?: 20,
    maxPages = board?.maxPages?.takeIf { it > 0 } ?: Int.MAX_VALUE,
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
        postsCount = if (postsCount > 0) postsCount else posts.size,
        filesCount = filesCount,
        views = views,
        isSticky = sticky == 1,
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
    isSticky = sticky == 1,
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
