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

    override suspend fun index(boardId: String, page: Int): ThreadsPage =
        // В приложении страницы с нуля, в API — с единицы.
        api.indexPage(boardId, page + 1).toDomain(boardId)

    override suspend fun thread(boardId: String, threadNum: Int): List<Post> =
        api.thread(boardId, threadNum)
            .threads
            .firstOrNull()
            ?.posts
            ?.map { it.toDomain(boardId) }
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
        // Движок отдаёт имя и тему с HTML-сущностями («Аноним&nbsp;»), а в UI
        // они выводятся как обычный текст — раскрываем здесь, на границе DTO.
        subject = decodeHtmlEntities(op.subject).trim(),
        commentHtml = op.comment,
        name = decodeHtmlEntities(op.name).trim(),
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

internal fun PostDto.toDomain(boardId: String) = Post(
    boardId = board,
    num = num,
    parent = parent,
    name = decodeHtmlEntities(name).trim(),
    subject = decodeHtmlEntities(subject).trim(),
    commentHtml = comment,
    date = date,
    timestamp = timestamp,
    trip = decodeHtmlEntities(trip).trim().ifBlank { null },
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
