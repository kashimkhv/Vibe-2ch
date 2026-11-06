package dev.dvach.client.ui.media

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import dev.dvach.client.domain.model.Attachment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Полноэкранный просмотр вложений треда: картинка с зумом и панорамированием
 * или видео-плеер. Вложения листаются свайпом — вбок, как в галерее, или
 * вверх-вниз, как в TikTok: направление задаётся настройкой.
 */
@OptIn(UnstableApi::class, ExperimentalFoundationApi::class)
@Composable
fun MediaScreen(
    onBack: () -> Unit,
    viewModel: MediaViewModel = hiltViewModel(),
) {
    val items = viewModel.items
    val page by viewModel.page.collectAsStateWithLifecycle()
    val player by viewModel.playerFlow.collectAsStateWithLifecycle()
    val renderedUrl by viewModel.renderedUrl.collectAsStateWithLifecycle()
    val failedUrl by viewModel.failedUrl.collectAsStateWithLifecycle()
    val downloadState by viewModel.download.collectAsStateWithLifecycle()
    val verticalPaging by viewModel.verticalPaging.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val pagerState = rememberPagerState(initialPage = viewModel.initialPage) { items.size }
    // Пока картинка приближена, свайп панорамирует её, а не листает страницы.
    var zoomed by remember { mutableStateOf(false) }
    // Одинарный тап по медиа прячет верхнюю панель и контролы видео,
    // повторный — возвращает. Так экран можно освободить от интерфейса,
    // не выходя из галереи.
    var chromeVisible by remember { mutableStateOf(true) }
    val toggleChrome: () -> Unit = { chromeVisible = !chromeVisible }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { viewModel.onPageChanged(it) }
    }
    LaunchedEffect(page) { zoomed = false }

    LaunchedEffect(downloadState) {
        when (val state = downloadState) {
            is DownloadState.Done -> snackbarHostState.showSnackbar("Сохранено: ${state.where}")
            is DownloadState.Failed -> snackbarHostState.showSnackbar(state.message)
            else -> Unit
        }
        if (downloadState !is DownloadState.Idle) viewModel.clearDownloadState()
    }

    // В фоне видео не должно продолжать играть в кармане.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { player?.pause() }

    val current = items.getOrNull(page) ?: items.first()

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Пейджер компонует только текущую страницу, поэтому соседние картинки
        // прогреваем сами — иначе они начинают грузиться, уже попав на экран.
        PrefetchMedia(
            items = items,
            page = page,
            sizePx = IntSize(constraints.maxWidth, constraints.maxHeight),
        )

        // Направление листания зависит от настройки: вбок — как в галерее,
        // вверх-вниз — как в TikTok. Пейджеры отличаются только осью, поэтому
        // страница вынесена в [MediaPage] и собирается одинаково.
        if (verticalPaging) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !zoomed,
                beyondViewportPageCount = 1,
                key = { index -> items[index].url },
            ) { index ->
                MediaPage(
                    item = items[index],
                    isCurrent = index == pagerState.currentPage,
                    player = player,
                    videoReady = renderedUrl == items[index].url,
                    videoFailed = failedUrl == items[index].url,
                    onZoomChange = { zoom ->
                        if (index == pagerState.currentPage) zoomed = zoom
                    },
                    onTap = toggleChrome,
                )
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !zoomed,
                // Соседи держим в композиции: их картинки грузятся в том же размере,
                // что и текущая, и к моменту свайпа уже лежат в памяти Coil.
                beyondViewportPageCount = 1,
                key = { index -> items[index].url },
            ) { index ->
                MediaPage(
                    item = items[index],
                    isCurrent = index == pagerState.currentPage,
                    player = player,
                    videoReady = renderedUrl == items[index].url,
                    videoFailed = failedUrl == items[index].url,
                    onZoomChange = { zoom ->
                        if (index == pagerState.currentPage) zoomed = zoom
                    },
                    onTap = toggleChrome,
                )
            }
        }

        if (chromeVisible) {
            OverlayBar(
                title = current.name,
                counter = if (items.size > 1) "${page + 1} / ${items.size}" else null,
                downloading = downloadState is DownloadState.InProgress,
                onBack = onBack,
                onDownload = viewModel::download,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }

        // У PlayerView отключён встроенный контроллер: он перехватывает касания
        // и не даёт листать вложения. Вместо него — свои кнопки и перемотка.
        if (chromeVisible && current.isVideo && player != null) {
            VideoControls(
                player = player!!,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp),
        )
    }
}

/** Страница галереи: видео с плеером, заглушка соседнего видео или картинка. */
@OptIn(UnstableApi::class)
@Composable
private fun MediaPage(
    item: Attachment,
    isCurrent: Boolean,
    player: ExoPlayer?,
    videoReady: Boolean,
    videoFailed: Boolean,
    onZoomChange: (Boolean) -> Unit,
    onTap: () -> Unit,
) {
    when {
        // Видео приближается так же, как картинка: те же жесты и тот же
        // слой трансформации, просто под ним не `AsyncImage`, а плеер.
        item.isVideo && isCurrent && player != null -> Zoomable(
            resetKey = item.url,
            onZoomChange = onZoomChange,
            onTap = onTap,
        ) {
            VideoPage(
                item = item,
                player = player,
                ready = videoReady,
                failed = videoFailed,
            )
        }
        item.isVideo -> VideoPlaceholder(item)
        else -> ZoomableImage(
            url = item.url,
            contentDescription = item.name,
            onZoomChange = onZoomChange,
            onTap = onTap,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun OverlayBar(
    title: String,
    counter: String?,
    downloading: Boolean,
    onBack: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.55f))
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = Color.White,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (counter != null) {
            Text(
                text = counter,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        if (downloading) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp).size(24.dp),
            )
        } else {
            IconButton(onClick = onDownload) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = "Скачать",
                    tint = Color.White,
                )
            }
        }
    }
}

/** Видео без встроенного контроллера: касания уходят пейджеру, а не плееру. */
@OptIn(UnstableApi::class)
@Composable
private fun VideoSurface(player: ExoPlayer) {
    AndroidView(
        factory = { context: Context ->
            PlayerView(context).apply {
                this.player = player
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setShutterBackgroundColor(AndroidColor.BLACK)
                keepScreenOn = true
            }
        },
        update = { view -> view.player = player },
        modifier = Modifier.fillMaxSize(),
    )
}

/**
 * Текущая видео-страница: плеер и превью поверх него, пока не отрисован
 * первый кадр. Так свайп на видео не показывает чёрный прямоугольник —
 * видно то же превью, что и на соседней странице, но с индикатором ожидания.
 */
@Composable
private fun VideoPage(
    item: Attachment,
    player: ExoPlayer,
    ready: Boolean,
    failed: Boolean,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        VideoSurface(player)
        AnimatedVisibility(
            visible = !ready,
            enter = EnterTransition.None,
            exit = fadeOut(tween(150)),
        ) {
            VideoPreview(item = item, failed = failed)
        }
    }
}

/** Превью видео и индикатор ожидания (или ошибка) вместо чёрного экрана. */
@Composable
private fun VideoPreview(item: Attachment, failed: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().background(Color.Black),
    ) {
        if (item.thumbnailUrl.isNotBlank()) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = item.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (failed) {
            Text(
                text = "Не удалось воспроизвести",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        } else {
            // Тёмная подложка: на светлом превью белый индикатор иначе терялся бы.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape),
            ) {
                CircularProgressIndicator(
                    strokeWidth = 3.dp,
                    color = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
}

/** Заглушка для соседней видео-страницы: превью и значок «play». */
@Composable
private fun VideoPlaceholder(item: Attachment) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        if (item.thumbnailUrl.isNotBlank()) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = item.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.75f),
            modifier = Modifier.size(72.dp),
        )
    }
}

/** Пауза/воспроизведение и перемотка: своих контролов хватает для просмотра. */
@Composable
private fun VideoControls(player: ExoPlayer, modifier: Modifier = Modifier) {
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var duration by remember { mutableLongStateOf(player.duration.coerceAtLeast(0L)) }
    var position by remember { mutableLongStateOf(player.currentPosition) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(player) {
        while (true) {
            isPlaying = player.isPlaying
            duration = player.duration.coerceAtLeast(0L)
            if (!dragging) position = player.currentPosition
            delay(400)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.55f))
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        IconButton(onClick = { if (player.isPlaying) player.pause() else player.play() }) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Пауза" else "Воспроизвести",
                tint = Color.White,
            )
        }
        Text(
            text = formatTime(if (dragging) dragValue.toLong() else position),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
        Slider(
            value = if (dragging) dragValue else position.toFloat(),
            onValueChange = {
                dragging = true
                dragValue = it
            },
            onValueChangeFinished = {
                player.seekTo(dragValue.toLong())
                position = dragValue.toLong()
                dragging = false
            },
            valueRange = 0f..(duration.coerceAtLeast(1L)).toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.35f),
            ),
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        )
        Text(
            text = formatTime(duration),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
    }
}

private fun formatTime(millis: Long): String {
    if (millis <= 0L) return "0:00"
    val totalSeconds = (millis / 1000f).roundToInt()
    return String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

/**
 * Картинка с зумом: двойной тап приближает и отдаляет, двумя пальцами —
 * масштаб и панорама. Одиночный свайп при 100 % не перехватывается, поэтому
 * его получает пейджер и листает соседние вложения.
 */
@Composable
private fun ZoomableImage(
    url: String,
    contentDescription: String,
    onZoomChange: (Boolean) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Zoomable(resetKey = url, onZoomChange = onZoomChange, onTap = onTap, modifier = modifier) {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
            loading = {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            },
            error = {
                Text(
                    text = "Не удалось загрузить файл",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            success = { SubcomposeAsyncImageContent() },
        )
    }
}

/**
 * Зум страницы галереи: масштаб и смещение живут здесь, а [content] рисуется
 * внутри трансформированного слоя. Под ним может быть что угодно — картинка
 * или `AndroidView` с плеером.
 *
 * Жест устроен так, чтобы не мешать листанию: при 100 % один палец никого не
 * перехватывает, и свайп достаётся пейджеру. Взяли двумя пальцами — забираем
 * жест себе, а пейджер на это время выключается через [onZoomChange].
 *
 * @param onTap одинарный тап по медиа: просмотрщик прячет и показывает свой
 *   интерфейс. Ждёт окончания двойного тапа, поэтому срабатывает с небольшой
 *   задержкой — иначе невозможно было бы отличить его от приближения. Протяжка
 *   тапом не считается: свайп вдоль пейджера и поперёк него интерфейс не трогает.
 */
@Composable
private fun Zoomable(
    resetKey: Any,
    onZoomChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    var scale by remember(resetKey) { mutableFloatStateOf(1f) }
    var offset by remember(resetKey) { mutableStateOf(Offset.Zero) }
    // Протяжка уже началась: тапом это считать нельзя. Флаг ставится на
    // Initial-проходе, поэтому детектор тапов видит его раньше, чем обработает
    // отпускание пальца.
    val dragged = remember(resetKey) { mutableStateOf(false) }

    fun applyScale(next: Float, pan: Offset, viewSize: IntSize) {
        val clamped = next.coerceIn(MIN_SCALE, MAX_SCALE)
        val maxX = viewSize.width * (clamped - 1f) / 2f
        val maxY = viewSize.height * (clamped - 1f) / 2f
        offset = if (clamped <= MIN_SCALE) {
            Offset.Zero
        } else {
            Offset(
                x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                y = (offset.y + pan.y).coerceIn(-maxY, maxY),
            )
        }
        scale = clamped
        onZoomChange(clamped > MIN_SCALE)
    }

    Box(
        modifier = modifier
            // Следим за протяжкой отдельно от потребления жеста: при листании
            // вбок вертикальный свайп, а при листании вверх-вниз горизонтальный
            // никто не потребляет, и без этой проверки детектор тапов принял бы
            // такой свайп за тап и спрятал интерфейс.
            .pointerInput(resetKey) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial,
                        )
                        dragged.value = false
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!dragged.value &&
                                (change.position - down.position).getDistance() >
                                viewConfiguration.touchSlop
                            ) {
                                dragged.value = true
                            }
                            if (!change.pressed) break
                        }
                    }
                }
            }
            .pointerInput(resetKey) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val multiTouch = event.changes.count { it.pressed } > 1
                        // Один палец без зума — жест остаётся пейджеру.
                        if (!multiTouch && scale <= MIN_SCALE) continue
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        if (zoom != 1f || pan != Offset.Zero) {
                            applyScale(scale * zoom, pan, size)
                            event.changes.forEach { change ->
                                if (change.positionChanged()) change.consume()
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(resetKey) {
                detectTapGestures(
                    onTap = { if (!dragged.value) onTap() },
                    onDoubleTap = { tap ->
                        if (scale > MIN_SCALE) {
                            applyScale(MIN_SCALE, Offset.Zero, size)
                        } else {
                            scale = DOUBLE_TAP_SCALE
                            // Приближаем к точке тапа, а не к центру экрана.
                            offset = Offset(
                                x = (size.width / 2f - tap.x) * (DOUBLE_TAP_SCALE - 1f),
                                y = (size.height / 2f - tap.y) * (DOUBLE_TAP_SCALE - 1f),
                            )
                            onZoomChange(true)
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        ) {
            content()
        }
    }
}


private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f
