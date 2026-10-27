package dev.dvach.client.ui.navigation

import kotlinx.serialization.Serializable

/** Type-safe маршруты: навигация проверяется компилятором, а не строками. */
@Serializable
data object BoardsRoute

@Serializable
data class ThreadsRoute(val boardId: String, val boardName: String)

@Serializable
data class ThreadRoute(val boardId: String, val boardName: String, val threadNum: Int)

@Serializable
data object BookmarksRoute

@Serializable
data object SettingsRoute
