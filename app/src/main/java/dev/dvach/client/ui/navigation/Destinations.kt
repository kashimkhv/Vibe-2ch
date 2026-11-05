package dev.dvach.client.ui.navigation

import kotlinx.serialization.Serializable

/** Type-safe маршруты: навигация проверяется компилятором, а не строками. */
@Serializable
data object BoardsRoute

@Serializable
data class ThreadsRoute(val boardId: String, val boardName: String)

@Serializable
data class ThreadRoute(
    val boardId: String,
    val boardName: String,
    val threadNum: Int,
    /** Номер поста, на который надо прокрутить при открытии (переход по цитате из другого треда). */
    val highlight: Int = 0,
)

/** Полноэкранный просмотр вложения: картинка с зумом или видео-плеер. */
@Serializable
data class MediaRoute(
    val url: String,
    val name: String,
    val isVideo: Boolean,
)

@Serializable
data object BookmarksRoute

@Serializable
data object SettingsRoute

/** Скрытые треды: список, из которого их можно вернуть в ленты досок. */
@Serializable
data object HiddenThreadsRoute

/** Выбор цветовой темы: список с образцами, применяется сразу. */
@Serializable
data object ThemesRoute

/**
 * Форма поста. `threadNum = 0` — создание нового треда; `quoteNum` — пост,
 * на который отвечаем (попадёт в начало комментария as `>>123`).
 */
@Serializable
data class ComposerRoute(
    val boardId: String,
    val boardName: String,
    val threadNum: Int = 0,
    val quoteNum: Int = 0,
)
