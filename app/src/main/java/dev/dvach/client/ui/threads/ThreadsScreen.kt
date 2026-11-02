package dev.dvach.client.ui.threads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import dev.dvach.client.domain.model.Attachment
import dev.dvach.client.domain.model.ThreadSummary
import dev.dvach.client.ui.components.dvachShadow
import dev.dvach.client.ui.components.dvachTopAppBarColors
import dev.dvach.client.ui.components.AttachmentThumbsRow
import dev.dvach.client.ui.components.DvachScrollbar
import dev.dvach.client.ui.components.PrefetchThumbnails
import dev.dvach.client.ui.components.EmptyBox
import dev.dvach.client.ui.components.ErrorBox
import dev.dvach.client.ui.components.HtmlText
import dev.dvach.client.ui.components.LoadingBox
import dev.dvach.client.util.filesLabel
import dev.dvach.client.util.formatCount
import dev.dvach.client.util.formatTimestamp
import dev.dvach.client.util.postsLabel
import dev.dvach.client.util.THUMBNAIL_LIST_BASE_SIDE_DP
import dev.dvach.client.util.listThumbnailSide
import dev.dvach.client.util.stripHtml
import dev.dvach.client.util.thumbnailRowGap

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ThreadsScreen(
    onBack: () -> Unit,
    onThreadClick: (boardId: String, boardName: String, threadNum: Int) -> Unit,
    onOpenMedia: (Attachment) -> Unit = {},
    /** Создать новый тред на этой доске: открывает форму поста. */
    onCreateThread: () -> Unit = {},
    viewModel: ThreadsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Плейлист просмотрщика общий на приложение. Возврат из галереи в список
    // не должен влиять на тред, который откроют позже: забываем, до чего
    // долистали, — прокрутка к «последнему файлу» нужна только в треде.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.discardMediaSwipe() }

    // Обновление — это новая лента с первой страницы, поэтому сначала наверх:
    // иначе список остался бы на прежней позиции и сразу догрузил бы всё обратно.
    val refreshFromTop: () -> Unit = {
        scope.launch { listState.scrollToItem(0) }
        viewModel.refresh()
    }

    // Бесконечная лента: страница догружается, когда до конца остаётся
    // пара карточек, — без кнопок «вперёд/назад» и без ожидания у пустого места.
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible >= state.threads.size - LOAD_MORE_PREFETCH) {
                    viewModel.loadMore()
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = dvachTopAppBarColors(),
                // Шапка чуть выступает над списком — как app bar в старом Material.
                modifier = Modifier.dvachShadow(),
                title = {
                    Column {
                        Text("/${state.boardId}/", fontFamily = FontFamily.Monospace)
                        Text(
                            text = state.boardName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onCreateThread) {
                        Icon(Icons.Default.Add, contentDescription = "Создать тред")
                    }
                    IconButton(onClick = refreshFromTop) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isLoading && state.threads.isNotEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            when {
                state.isLoading && state.threads.isEmpty() -> LoadingBox()
                state.error != null && state.threads.isEmpty() ->
                    ErrorBox(message = state.error.orEmpty(), onRetry = viewModel::refresh)
                state.threads.isEmpty() -> EmptyBox("На доске пока нет тредов")
                else -> Box(modifier = Modifier.fillMaxSize()) {
                    // Превью следующих карточек грузим заранее: при быстром флинге
                    // список успевает уехать раньше, чем картинки попросят сами.
                    PrefetchThumbnails(
                        listState = listState,
                        urlsOf = { index ->
                            state.threads.getOrNull(index)?.attachments
                                ?.take(THUMBNAILS_IN_LIST)
                                ?.map { it.thumbnailUrl }
                                .orEmpty()
                        },
                    )
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(items = state.threads, key = { it.num }) { thread ->
                            ThreadCard(
                                thread = thread,
                                listThumbnailScalePercent = state.listThumbnailScalePercent,
                                enlargeFewListThumbnails = state.enlargeFewListThumbnails,
                                autoRevealSpoilers = state.autoRevealSpoilers,
                                cropThumbnails = state.cropThumbnails,
                                thumbnailBackground = state.thumbnailBackground,
                                textMargins = state.textMarginsDp.dp,
                                onClick = {
                                    onThreadClick(thread.boardId, state.boardName, thread.num)
                                },
                                onOpenAttachment = { file ->
                                    viewModel.prepareMedia(thread, file)
                                    onOpenMedia(file)
                                },
                            )
                        }
                        item(key = "threads-footer") {
                            ListFooter(
                                isLoadingMore = state.isLoadingMore,
                                loadMoreError = state.loadMoreError,
                                canGoNext = state.canGoNext,
                                onRetry = { viewModel.loadMore(force = true) },
                            )
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
            }
        }
    }
}

/** Сколько карточек остаётся до конца, когда начинается догрузка. */
private const val LOAD_MORE_PREFETCH = 3

/** Низ списка: спиннер догрузки, кнопка повтора после ошибки или «конец списка». */
@Composable
private fun ListFooter(
    isLoadingMore: Boolean,
    loadMoreError: String?,
    canGoNext: Boolean,
    onRetry: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
    ) {
        when {
            loadMoreError != null -> TextButton(onClick = onRetry) {
                Text("Не удалось загрузить — повторить")
            }
            isLoadingMore -> CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(26.dp),
            )
            !canGoNext -> Text(
                text = "Конец списка",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> Text(
                text = "Загружаем ещё…",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Сколько превью ОП-поста показывать в списке; остальные — в плашке «+N». */
private const val THUMBNAILS_IN_LIST = 4

@Composable
private fun ThreadCard(
    thread: ThreadSummary,
    listThumbnailScalePercent: Int,
    enlargeFewListThumbnails: Boolean,
    autoRevealSpoilers: Boolean,
    cropThumbnails: Boolean,
    thumbnailBackground: Boolean,
    textMargins: Dp,
    onClick: () -> Unit,
    onOpenAttachment: (Attachment) -> Unit,
) {
    val plainComment = remember(thread.commentHtml) { stripHtml(thread.commentHtml) }
    val subject = thread.subject.trim()
    val showSubject = subject.isNotBlank() && !plainComment.startsWith(subject, ignoreCase = true)

    // Строка списка — плоская, во всю ширину, с разделителем снизу:
    // карточек со скруглением в старом Material не было.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Шапка карточки: слева имя ОП-поста, справа дата и номер треда —
            // так же, как в шапке поста в самом треде.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = thread.name.ifBlank { "Аноним" },
                    style = MaterialTheme.typography.labelLarge,
                    // Имя в карточке — ник ОП-поста. В самом треде он тоже
                    // жирный: это структурное выделение, а не настройка роли.
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text(
                        text = formatTimestamp(thread.timestamp).ifBlank { thread.date },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    Text(
                        text = thread.num.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Превью ОП-поста — тот же компонент, что и в треде: квадратные ячейки,
            // картинка заполняет их целиком или вписывается — по настройке.
            // В списке показываем не больше четырёх (остальные в плашке «+N»),
            // иначе карточка занимает весь экран. Размер ячейки один на все
            // карточки: его задаёт настройка, а вписывается ряд за счёт
            // промежутка (крупный масштаб переносит превью на вторую строку,
            // но размер остаётся прежним). Если вложений меньше трёх, ячейка
            // вдвое крупнее — одинокая картинка не оставляет полстроки пустой.
            if (thread.attachments.isNotEmpty()) {
                BoxWithConstraints {
                    val side = listThumbnailSide(
                        baseDp = THUMBNAIL_LIST_BASE_SIDE_DP,
                        percent = listThumbnailScalePercent,
                        fileCount = thread.attachments.size,
                        enlargeFew = enlargeFewListThumbnails,
                    )
                    val visible = minOf(thread.attachments.size, THUMBNAILS_IN_LIST)
                    val gap = thumbnailRowGap(
                        sideDp = side,
                        availableDp = maxWidth.value.toInt(),
                        visible = visible,
                    )
                    AttachmentThumbsRow(
                        files = thread.attachments,
                        side = side.dp,
                        maxVisible = THUMBNAILS_IN_LIST,
                        gap = gap.dp,
                        crop = cropThumbnails,
                        background = thumbnailBackground,
                        onClick = onOpenAttachment,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (showSubject) {
                    Text(
                        text = thread.subject.ifBlank { "Без темы" },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(
                            start = textMargins,
                            end = textMargins,
                        ),
                    )
                }
                HtmlText(
                    html = thread.commentHtml,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    linksEnabled = false,
                    autoRevealSpoilers = autoRevealSpoilers,
                    modifier = Modifier.padding(
                        start = textMargins,
                        end = textMargins,
                    ),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = postsLabel(thread.postsCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (thread.filesCount > 0) {
                        Text(
                            text = filesLabel(thread.filesCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (thread.views > 0) {
                        Text(
                            text = "${formatCount(thread.views)} просм.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (thread.isSticky) {
                        Text(
                            text = "закреплён",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (thread.isClosed) {
                        Text(
                            text = "закрыт",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}
