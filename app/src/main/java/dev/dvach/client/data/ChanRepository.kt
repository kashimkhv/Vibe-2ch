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
        api.index(boardId, page).toDomain(boardId)

    override suspend fun thread(boardId: String, threadNum: Int): List<Post> =
        api.thread(boardId, threadNum)
            .threads
            .firstOrNull()
            ?.posts
            ?.map { it.toDomain(boardId) }
            .orEmpty()
}

private fun ThreadsPageDto.toDomain(boardId: String) = ThreadsPage(
    currentPage = currentPage,
    isIndex = isIndex,
    threads = threads.mapNotNull { it.toDomain(boardId) },
    threadsPerPage = board?.threadsPerPage?.takeIf { it > 0 } ?: 20,
)

private fun ThreadDto.toDomain(boardId: String): ThreadSummary? {
    val op = posts.firstOrNull() ?: return null
    return ThreadSummary(
        boardId = boardId,
        num = op.num,
        subject = op.subject,
        commentHtml = op.comment,
        name = op.name,
        date = op.date,
        timestamp = op.timestamp,
        postsCount = if (postsCount > 0) postsCount else posts.size,
        filesCount = filesCount,
        views = views,
        isSticky = sticky == 1,
        isClosed = closed == 1,
        thumbnailUrl = op.files.firstOrNull()?.let { it.thumbnailPath.ifBlank { it.path } }?.absoluteUrl(),
        replies = posts.drop(1).takeLast(3).map { it.toDomain(boardId) },
    )
}

private fun PostDto.toDomain(boardId: String) = Post(
    boardId = board,
    num = num,
    parent = parent,
    name = name,
    subject = subject,
    commentHtml = comment,
    date = date,
    timestamp = timestamp,
    trip = trip.ifBlank { null },
    isOp = isOp == 1,
    isSticky = sticky == 1,
    isClosed = closed == 1,
    isBanned = banned == 1,
    files = files.map { it.toDomain() },
)

private fun FileDto.toDomain(): Attachment = Attachment(
    url = path.absoluteUrl(),
    thumbnailUrl = (thumbnailPath.ifBlank { path }).absoluteUrl(),
    name = fullName.ifBlank { name },
    sizeBytes = size,
    width = width,
    height = height,
    isVideo = type == TYPE_VIDEO || name.endsWith(".webm") || name.endsWith(".mp4"),
)

private fun BoardDto.toDomain() = Board(
    id = id,
    name = name,
    category = category,
    info = info,
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

private const val TYPE_VIDEO = 10
