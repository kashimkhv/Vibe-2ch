package dev.dvach.client.domain.model

/** Доменные модели: UI и репозитории не знают про DTO и про то, как устроен JSON движка. */
data class Board(
    val id: String,
    val name: String,
    val category: String,
    val info: String,
    val threadsPerPage: Int,
    val maxPages: Int,
    val bumpLimit: Int,
    val maxComment: Int,
    val enablePosting: Boolean,
    val enableNames: Boolean,
    val enableSubject: Boolean,
    val enableTrips: Boolean,
    val enableFlags: Boolean,
    val enableIcons: Boolean,
    val enableLikes: Boolean,
    val enableSage: Boolean,
    val fileTypes: List<String>,
)

data class ThreadsPage(
    /** Номер страницы с нуля, как в UI. */
    val currentPage: Int,
    val isIndex: Boolean,
    val threads: List<ThreadSummary>,
    val threadsPerPage: Int,
    val maxPages: Int,
)

data class ThreadSummary(
    val boardId: String,
    val num: Int,
    val subject: String,
    val commentHtml: String,
    val name: String,
    val date: String,
    val timestamp: Long,
    val postsCount: Int,
    val filesCount: Int,
    val views: Int,
    val isSticky: Boolean,
    val isClosed: Boolean,
    /** Все вложения ОП-поста: в превью треда показываем каждое. */
    val attachments: List<Attachment>,
    val replies: List<Post>,
)

data class Post(
    val boardId: String,
    val num: Int,
    val parent: Int,
    val name: String,
    val subject: String,
    val commentHtml: String,
    val date: String,
    val timestamp: Long,
    val trip: String?,
    val isOp: Boolean,
    val isSticky: Boolean,
    val isClosed: Boolean,
    val isBanned: Boolean,
    val files: List<Attachment>,
) {
    val hasSubject: Boolean get() = subject.isNotBlank() && subject != "null"
}

data class Attachment(
    val url: String,
    val thumbnailUrl: String,
    val name: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val duration: String? = null,
    val isVideo: Boolean,
    /** Расширение файла из `path` — у видео это webm, mp4, mov; в метке показываем его. */
    val extension: String = "",
)
