package io.github.kashimkhv.vibe2ch.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.kashimkhv.vibe2ch.ui.bookmarks.BookmarksScreen
import io.github.kashimkhv.vibe2ch.ui.boards.BoardsScreen
import io.github.kashimkhv.vibe2ch.ui.hidden.HiddenThreadsScreen
import io.github.kashimkhv.vibe2ch.ui.media.MediaScreen
import io.github.kashimkhv.vibe2ch.ui.posting.ComposerScreen
import io.github.kashimkhv.vibe2ch.ui.settings.SettingsScreen
import io.github.kashimkhv.vibe2ch.ui.settings.ThemesScreen
import io.github.kashimkhv.vibe2ch.ui.thread.ThreadScreen
import io.github.kashimkhv.vibe2ch.ui.threads.ThreadsScreen

@Composable
fun DvachNavHost(
    deepLink: DvachLink? = null,
    onDeepLinkHandled: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = BoardsRoute,
        enterTransition = {
            slideInHorizontally(tween(260)) { it / 6 } + fadeIn(tween(260))
        },
        exitTransition = {
            slideOutHorizontally(tween(260)) { -it / 8 } + fadeOut(tween(200))
        },
        popEnterTransition = {
            slideInHorizontally(tween(260)) { -it / 8 } + fadeIn(tween(260))
        },
        popExitTransition = {
            slideOutHorizontally(tween(260)) { it / 6 } + fadeOut(tween(200))
        },
    ) {
        composable<BoardsRoute> {
            BoardsScreen(
                onBoardClick = { board ->
                    navController.navigate(ThreadsRoute(board.id, board.name))
                },
                onOpenBookmarks = { navController.navigate(BookmarksRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
            )
        }

        composable<ThreadsRoute> {
            ThreadsScreen(
                onBack = navController::popBackStack,
                onThreadClick = { boardId, boardName, threadNum ->
                    navController.navigate(ThreadRoute(boardId, boardName, threadNum))
                },
                onOpenMedia = { file ->
                    navController.navigate(MediaRoute(file.url, file.name, file.isVideo))
                },
                onCreateThread = {
                    val route = it.toRoute<ThreadsRoute>()
                    navController.navigate(ComposerRoute(route.boardId, route.boardName))
                },
            )
        }

        composable<ThreadRoute> {
            ThreadScreen(
                onBack = navController::popBackStack,
                onOpenMedia = { file ->
                    navController.navigate(MediaRoute(file.url, file.name, file.isVideo))
                },
                onReply = { replyTo ->
                    val route = it.toRoute<ThreadRoute>()
                    navController.navigate(
                        ComposerRoute(
                            boardId = route.boardId,
                            boardName = route.boardName,
                            threadNum = route.threadNum,
                            quoteNum = replyTo,
                        ),
                    )
                },
                onOpenThread = { threadNum, highlight ->
                    val route = it.toRoute<ThreadRoute>()
                    navController.navigate(
                        ThreadRoute(route.boardId, route.boardName, threadNum, highlight),
                    )
                },
            )
        }

        composable<ComposerRoute> {
            ComposerScreen(
                onBack = navController::popBackStack,
                onPosted = navController::popBackStack,
                onThreadCreated = { threadNum ->
                    val route = it.toRoute<ComposerRoute>()
                    // Форма уходит из бэкстека: возвращаться в неё после отправки
                    // незачем, а «назад» из нового треда ведёт к списку тредов.
                    navController.popBackStack()
                    navController.navigate(
                        ThreadRoute(route.boardId, route.boardName, threadNum, threadNum),
                    )
                },
            )
        }

        composable<MediaRoute> {
            MediaScreen(onBack = navController::popBackStack)
        }

        composable<BookmarksRoute> {
            BookmarksScreen(
                onBack = navController::popBackStack,
                onOpenThread = { boardId, threadNum ->
                    navController.navigate(ThreadRoute(boardId, boardId, threadNum))
                },
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(
                onBack = navController::popBackStack,
                onOpenHiddenThreads = { navController.navigate(HiddenThreadsRoute) },
                onOpenThemes = { navController.navigate(ThemesRoute) },
            )
        }

        composable<ThemesRoute> {
            ThemesScreen(onBack = navController::popBackStack)
        }

        composable<HiddenThreadsRoute> {
            HiddenThreadsScreen(onBack = navController::popBackStack)
        }
    }

    // Ссылка из чужого приложения открывается поверх текущего экрана: «назад»
    // ведёт туда, где человек был, а не закрывает приложение. Эффект стоит
    // после NavHost: до установки графа navigate() бросил бы исключение.
    LaunchedEffect(deepLink) {
        val link = deepLink ?: return@LaunchedEffect
        navController.open(link)
        onDeepLinkHandled()
    }
}

/**
 * Открывает ссылку из чужого приложения.
 *
 * `launchSingleTop` здесь не годится: он переиспользует запись бэкстека вместе
 * с её `ViewModel`, а `ViewModel` читает аргументы маршрута один раз в
 * конструкторе. Вторая ссылка «доска → доска» или «тред → тред» показала бы
 * старые аргументы. Поэтому такой же экран сначала снимаем с бэкстека
 * (`popBackStack`), а потом кладём новый — с чистыми состоянием и `ViewModel`.
 * Экран другого типа просто встаёт сверху.
 */
private fun NavHostController.open(link: DvachLink) {
    val current = currentBackStackEntry
    when (link) {
        is DvachLink.Board -> {
            if (current != null && current.destination.hasRoute<ThreadsRoute>()) {
                // Эта доска уже открыта — второй такой же экран не нужен.
                if (current.toRoute<ThreadsRoute>().boardId == link.boardId) return
                popBackStack()
            }
            navigate(ThreadsRoute(link.boardId, ""))
        }

        is DvachLink.Thread -> {
            if (current != null && current.destination.hasRoute<ThreadRoute>()) {
                val open = current.toRoute<ThreadRoute>()
                val sameThread = open.boardId == link.boardId && open.threadNum == link.threadNum
                // Тот же тред и тот же якорь: повторно открывать нечего.
                // Другой якорь — пересоздаём экран, чтобы прокрутить к посту.
                if (sameThread && (link.postNum == 0 || link.postNum == open.highlight)) return
                popBackStack()
            }
            navigate(ThreadRoute(link.boardId, "", link.threadNum, link.postNum))
        }
    }
}
