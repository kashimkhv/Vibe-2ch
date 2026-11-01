package dev.dvach.client.ui.thread

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import dev.dvach.client.domain.model.Attachment
import dev.dvach.client.ui.components.dvachShadow
import dev.dvach.client.ui.components.dvachTopAppBarColors
import dev.dvach.client.ui.components.DvachScrollbar
import dev.dvach.client.ui.components.SCROLLBAR_WIDTH
import dev.dvach.client.ui.components.ErrorBox
import dev.dvach.client.ui.components.LoadingBox
import dev.dvach.client.ui.components.PrefetchThumbnails
import dev.dvach.client.util.imagesLabel
import dev.dvach.client.util.postsLabel
import dev.dvach.client.util.threadUrl
import dev.dvach.client.util.videosLabel

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

    // Дерево ответов: пост встаёт сразу под тем, кого цитирует.
    val nodes = remember(state.posts) { buildPostTree(state.posts) }
    // Порядковые номера считаются по хронологии, а не по месту в дереве.
    val ordinals = remember(state.posts) { postOrdinals(state.posts) }

    // Пост, к которому надо прокрутить после пересборки дерева: при переходе
    // по цитате ветка может быть свёрнута, и позиция в списке ещё не та.
    var pendingScroll by remember { mutableStateOf<PendingScroll?>(null) }

    // Позицию восстанавливаем один раз: дальше список живёт своей жизнью.
    var restoredScroll by remember { mutableStateOf(false) }

    val refreshing = state.isLoading && state.posts.isNotEmpty()

    // «Потянуть вверх в конце треда» — обновление: тянем список за последний
    // пост, отпускаем — тред перезагружается.
    var endPull by remember { mutableFloatStateOf(0f) }
    var refreshingFromEnd by remember { mutableStateOf(false) }
    val endRefreshThreshold = with(LocalDensity.current) { END_REFRESH_THRESHOLD_DP.dp.toPx() }
    // Правая полоса прокрутки сама ловит палец: жесты, начавшиеся в её зоне,
    // не должны считаться вытягиванием конца треда.
    val scrollbarZone = with(LocalDensity.current) { SCROLLBAR_WIDTH.toPx() }

    LaunchedEffect(refreshing) {
        if (!refreshing) refreshingFromEnd = false
    }

    LaunchedEffect(state.posts) {
        if (!appliedInitialHighlight && initialHighlight > 0 && state.posts.isNotEmpty()) {
            appliedInitialHighlight = true
            pendingScroll = PendingScroll(initialHighlight, animate = false)
        }
    }

    // Возврат в тред: прокручиваем к посту, на котором остановились.
    // Переход по цитате важнее — если он есть, позицию не трогаем.
    LaunchedEffect(nodes, state.scrollLoaded, state.savedScroll) {
        if (restoredScroll || !state.scrollLoaded || nodes.isEmpty()) return@LaunchedEffect
        restoredScroll = true
        if (initialHighlight > 0) return@LaunchedEffect
        // Если в просмотрщике долистали до другого поста — встаём на него:
        // позиция в треде следует за листанием медиа.
        val viewedPost = viewModel.consumeMediaPost()
        if (viewedPost != null) {
            val index = nodes.indexOfFirst { it.post.num == viewedPost }
            // Пост уже на экране — не дёргаем список зря.
            if (index >= 0 && listState.layoutInfo.visibleItemsInfo.none { it.index == index }) {
                listState.scrollToItem(index)
                return@LaunchedEffect
            }
        }
        val saved = state.savedScroll ?: return@LaunchedEffect
        // Прокрутка без анимации: экран ещё не показан, анимация была бы рывком.
        listState.scrollToItem(
            scrollIndexFor(nodes, saved.postNum, saved.index),
            saved.offset,
        )
    }

    // Запоминаем место при уходе с экрана. latestNodes — чтобы обработчик видел
    // актуальный список, а не тот, что был на первом кадре.
    val latestNodes by rememberUpdatedState(nodes)
    val savePosition: () -> Unit = {
        val index = listState.firstVisibleItemIndex
        val postNum = latestNodes.getOrNull(index)?.post?.num
        if (postNum != null) {
            viewModel.saveScroll(
                postNum = postNum,
                index = index,
                offset = listState.firstVisibleItemScrollOffset,
            )
        }
    }
    DisposableEffect(Unit) {
        onDispose { savePosition() }
    }
    // И в фон: если систему прибьёт процесс, позиция останется на последнем
    // месте, а не там, где тред закрывали в прошлый раз.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { savePosition() }

    // Прокрутка выполняется уже по готовому дереву, поэтому цитата попадает
    // точно в пост, даже если ветка была свёрнута и только что раскрылась.
    LaunchedEffect(nodes, pendingScroll) {
        val target = pendingScroll ?: return@LaunchedEffect
        val index = nodes.indexOfFirst { it.post.num == target.num }
        if (index >= 0) {
            if (target.animate) {
                listState.animateScrollToItem(index)
            } else {
                listState.scrollToItem(index)
            }
            highlighted = target.num
        }
        // Сбрасываем запрос последним: если сделать это до прокрутки, смена
        // ключа перезапустит эффект и отменит анимацию.
        pendingScroll = null
    }

    // Подсветка гаснет сама, отдельным эффектом: перезапуск прокрутки не должен
    // оставлять пост подсвеченным навсегда.
    LaunchedEffect(highlighted) {
        if (highlighted > 0) {
            delay(HIGHLIGHT_MS)
            highlighted = -1
        }
    }

    val boardId = viewModel.route.boardId
    val threadNum = viewModel.route.threadNum

    // Счётчики вложений в шапке: сколько в треде картинок и сколько видео.
    val imagesCount = remember(state.posts) { state.posts.sumOf { post -> post.files.count { !it.isVideo } } }
    val videosCount = remember(state.posts) { state.posts.sumOf { post -> post.files.count { it.isVideo } } }

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
                colors = dvachTopAppBarColors(),
                // Шапка чуть выступает над списком — как app bar в старом Material.
                modifier = Modifier.dvachShadow(),
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
                                    if (imagesCount > 0) {
                                        append(" · ")
                                        append(imagesLabel(imagesCount))
                                    }
                                    if (videosCount > 0) {
                                        append(" · ")
                                        append(videosLabel(videosCount))
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        ThreadMenu(
                            expanded = titleMenuOpen,
                            bookmarked = bookmarked,
                            shareUrl = threadUrl(boardId, threadNum),
                            shareTitle = title,
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
                // Кнопок в шапке нет: обновление — свайпом сверху вниз (или снизу
                // вверх в конце треда), закладки — через долгое нажатие на заголовок
                // или на пост.
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                // Жест только читаем (Initial pass, ничего не потребляем), поэтому
                // список продолжает скроллиться и «пружинить» как обычно.
                .pointerInput(endRefreshThreshold, scrollbarZone) {
                    awaitEachGesture {
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial,
                        )
                        // Палец на полосе прокрутки — её дело, не наше.
                        if (down.position.x >= size.width - scrollbarZone) return@awaitEachGesture
                        var pull = 0f
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            // Копим только когда список уже в самом низу: до этого
                            // жест — обычная прокрутка.
                            if (listState.canScrollForward) {
                                pull = 0f
                            } else {
                                pull = (pull - change.positionChange().y).coerceAtLeast(0f)
                            }
                            endPull = pull
                        }
                        if (pull >= endRefreshThreshold && !refreshing) {
                            refreshingFromEnd = true
                            viewModel.load()
                        }
                        endPull = 0f
                    }
                },
        ) {
            PullToRefreshBox(
                isRefreshing = refreshing && !refreshingFromEnd,
                onRefresh = viewModel::load,
                modifier = Modifier.fillMaxSize(),
            ) {
                // Превью следующих постов ставим в очередь заранее: иначе при
                // быстром флинге список уезжает быстрее, чем подгружаются картинки.
                if (settings.showThumbnails) {
                    PrefetchThumbnails(
                        listState = listState,
                        urlsOf = { index ->
                            nodes.getOrNull(index)?.post?.files
                                ?.map { it.thumbnailUrl }
                                .orEmpty()
                        },
                    )
                }
                when {
                    state.isLoading && state.posts.isEmpty() -> LoadingBox()
                    state.error != null && state.posts.isEmpty() ->
                        ErrorBox(message = state.error.orEmpty(), onRetry = viewModel::load)
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(items = nodes, key = { it.post.num }) { node ->
                            val post = node.post
                            PostCard(
                                post = post,
                                isHighlighted = post.num == highlighted,
                                bookmarked = bookmarked,
                                onToggleBookmark = toggleBookmark,
                                shareUrl = threadUrl(boardId, threadNum),
                                shareTitle = title,
                                showThumbnails = settings.showThumbnails,
                                thumbnailScalePercent = settings.thumbnailScalePercent,
                                cropThumbnails = settings.cropThumbnails,
                                thumbnailBackground = settings.thumbnailBackground,
                                autoRevealSpoilers = settings.autoRevealSpoilers,
                                textMargins = settings.textMarginsDp.dp,
                                ordinal = ordinals[post.num],
                                depth = node.depth,
                                onPostClick = { ref, refThread ->
                                    if (refThread != null && refThread != threadNum) {
                                        // Цитата из другого треда — открываем его и подсвечиваем пост.
                                        onOpenThread(refThread, ref)
                                    } else if (state.posts.any { it.num == ref }) {
                                        pendingScroll = PendingScroll(ref, animate = true)
                                    } else {
                                        // Пост мог быть удалён или ещё не догружен.
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Пост $ref не найден в треде")
                                        }
                                    }
                                },
                                onOpenAttachment = { file ->
                                    viewModel.prepareMedia(file)
                                    onOpenMedia(file)
                                },
                            )
                        }
                        item(key = "thread-end") {
                            EndOfThreadHint()
                        }
                    }
                }

                // Полоса прокрутки поверх списка: правый край экрана.
                DvachScrollbar(
                    state = listState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                )
            }

            // Индикатор вытягивания снизу: появляется, только когда тянут конец треда.
            if (endPull > 0f || refreshingFromEnd) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 4.dp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                ) {
                    if (refreshingFromEnd) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.padding(10.dp).size(24.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowUp,
                            contentDescription = "Обновить тред",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(24.dp)
                                .graphicsLayer {
                                    alpha = (endPull / endRefreshThreshold.coerceAtLeast(1f))
                                        .coerceIn(0.3f, 1f)
                                    rotationZ = 180f * (endPull / endRefreshThreshold.coerceAtLeast(0.01f))
                                        .coerceIn(0f, 1f)
                                },
                        )
                    }
                }
            }
        }
    }
}

/** Подсказка в конце треда: свайп вверх перезагружает тред. */
@Composable
private fun EndOfThreadHint() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 22.dp),
    ) {
        Text(
            text = "Конец треда",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Потяните вверх, чтобы обновить",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Сколько подсвечивается пост, к которому перешли по цитате. */
private const val HIGHLIGHT_MS = 1500L

/** Насколько надо вытянуть конец треда вверх, чтобы он обновился. */
private const val END_REFRESH_THRESHOLD_DP = 96

/** Запрос на прокрутку к посту: номер и нужно ли анимировать переход. */
private data class PendingScroll(val num: Int, val animate: Boolean)
