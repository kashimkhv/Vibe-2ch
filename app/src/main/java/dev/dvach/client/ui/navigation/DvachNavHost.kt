package dev.dvach.client.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.dvach.client.ui.bookmarks.BookmarksScreen
import dev.dvach.client.ui.boards.BoardsScreen
import dev.dvach.client.ui.hidden.HiddenThreadsScreen
import dev.dvach.client.ui.media.MediaScreen
import dev.dvach.client.ui.posting.ComposerScreen
import dev.dvach.client.ui.settings.SettingsScreen
import dev.dvach.client.ui.settings.ThemesScreen
import dev.dvach.client.ui.thread.ThreadScreen
import dev.dvach.client.ui.threads.ThreadsScreen

@Composable
fun DvachNavHost(
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
}
