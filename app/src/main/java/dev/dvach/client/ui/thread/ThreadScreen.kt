package dev.dvach.client.ui.thread

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import dev.dvach.client.domain.model.Attachment
import dev.dvach.client.ui.components.ErrorBox
import dev.dvach.client.ui.components.LoadingBox
import dev.dvach.client.util.postsLabel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ThreadScreen(
    onBack: () -> Unit,
    onOpenThread: (threadNum: Int, highlight: Int) -> Unit = { _, _ -> },
    onOpenMedia: (Attachment) -> Unit = {},
    viewModel: ThreadViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val bookmarked by viewModel.isBookmarked.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var highlighted by remember { mutableIntStateOf(-1) }
    var appliedInitialHighlight by remember { mutableStateOf(false) }
    var titleMenuOpen by remember { mutableStateOf(false) }
    val initialHighlight = viewModel.route.highlight
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.posts) {
        if (!appliedInitialHighlight && initialHighlight > 0 && state.posts.isNotEmpty()) {
            appliedInitialHighlight = true
            val index = state.posts.indexOfFirst { it.num == initialHighlight }
            if (index >= 0) {
                listState.scrollToItem(index)
                highlighted = initialHighlight
                delay(HIGHLIGHT_MS)
                if (highlighted == initialHighlight) highlighted = -1
            }
        }
    }

    val boardId = viewModel.route.boardId
    val threadNum = viewModel.route.threadNum

    // В заголовке — тема ОП-поста, а не номер треда: номер и так виден в подзаголовке.
    val op = state.posts.firstOrNull()
    val title = remember(op, threadNum) {
        op?.let { post -> post.subject.trim().ifBlank { plainText(post.commentHtml) } }
            ?.take(140)
            ?.ifBlank { null }
            ?: "№$threadNum"
    }

    val toggleBookmark: () -> Unit = {
        val adding = !bookmarked
        viewModel.toggleBookmark()
        scope.launch {
            snackbarHostState.showSnackbar(
                if (adding) "Сохранено в закладки" else "Удалено из закладок",
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Box {
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {},
                                    onLongClick = { titleMenuOpen = true },
                                ),
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = buildString {
                                    append("/$boardId/ · №$threadNum")
                                    if (state.posts.isNotEmpty()) {
                                        append(" · ")
                                        append(postsLabel(state.posts.size))
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        BookmarkMenu(
                            expanded = titleMenuOpen,
                            bookmarked = bookmarked,
                            onDismiss = { titleMenuOpen = false },
                            onToggle = toggleBookmark,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                // Кнопок в шапке нет: обновление — свайпом сверху вниз, закладки —
                // через долгое нажатие на заголовок или на пост.
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading && state.posts.isNotEmpty(),
            onRefresh = viewModel::load,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            when {
                state.isLoading && state.posts.isEmpty() -> LoadingBox()
                state.error != null && state.posts.isEmpty() ->
                    ErrorBox(message = state.error.orEmpty(), onRetry = viewModel::load)
                else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(items = state.posts, key = { it.num }) { post ->
                        PostCard(
                            post = post,
                            isHighlighted = post.num == highlighted,
                            bookmarked = bookmarked,
                            onToggleBookmark = toggleBookmark,
                            showThumbnails = settings.showThumbnails,
                            thumbnailScalePercent = settings.thumbnailScalePercent,
                            onPostClick = { ref, refThread ->
                                val index = state.posts.indexOfFirst { it.num == ref }
                                if (refThread != null && refThread != threadNum) {
                                    // Цитата из другого треда — открываем его и подсвечиваем пост.
                                    onOpenThread(refThread, ref)
                                } else if (index >= 0) {
                                    highlighted = ref
                                    scope.launch {
                                        listState.animateScrollToItem(index)
                                        delay(HIGHLIGHT_MS)
                                        if (highlighted == ref) highlighted = -1
                                    }
                                } else {
                                    // Пост мог быть удалён или ещё не догружен.
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Пост $ref не найден в треде")
                                    }
                                }
                            },
                            onOpenAttachment = onOpenMedia,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

/** Сколько подсвечивается пост, к которому перешли по цитате. */
private const val HIGHLIGHT_MS = 1500L
