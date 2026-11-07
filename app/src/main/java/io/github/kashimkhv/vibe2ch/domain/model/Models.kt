package io.github.kashimkhv.vibe2ch.domain.model

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
    /** Предел размера файла в килобайтах, как его отдаёт движок. 0 — не задан. */
    val maxFilesSize: Int = 0,
)

data class ThreadsPage(
    /** Номер страницы с нуля, как в UI. */
    val currentPage: Int,
    val isIndex: Boolean,
    val threads: List<ThreadSummary>,
    val threadsPerPage: Int,
    val maxPages: Int,
    /**
     * Активность доски в постах в час от самого движка (`board_speed`).
     * 0 — движок число не прислал, тогда клиент считает своё приближение.
     */
    val boardSpeed: Int = 0,
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
    /**
     * Тред достиг бамп-лимита: движок держит его сверху, новые посты его
     * больше не поднимают. Это не модераторский закреп, поэтому и пометка
     * в ленте другая.
     */
    val isEndless: Boolean = false,
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
    /** Расширение файла из `path`: у видео это webm, mp4, mov. */
    val extension: String = "",
) {
    /**
     * Анимированная картинка. Отдельного признака в API нет, а из картинок
     * двач анимирует только gif — превью такой файл листает сам.
     */
    val isAnimated: Boolean get() = extension.equals("gif", ignoreCase = true)
}
