package dev.dvach.client.ui.media

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent

/**
 * Полноэкранный просмотр вложения: картинка с зумом и панорамированием или
 * видео-плеер Media3. Вместо внешнего браузера — как в вебе, но без чужих
 * таб-баров и рекламы.
 */
@OptIn(UnstableApi::class)
@Composable
fun MediaScreen(
    onBack: () -> Unit,
    viewModel: MediaViewModel = hiltViewModel(),
) {
    val route = viewModel.route
    val downloadState by viewModel.download.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(downloadState) {
        when (val state = downloadState) {
            is DownloadState.Done -> snackbarHostState.showSnackbar("Сохранено: ${state.where}")
            is DownloadState.Failed -> snackbarHostState.showSnackbar(state.message)
            else -> Unit
        }
        if (downloadState !is DownloadState.Idle) viewModel.clearDownloadState()
    }

    // В фоне видео не должно продолжать играть в кармане.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.player()?.pause() }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (route.isVideo) {
            val player = viewModel.player()
            if (player != null) {
                AndroidView(
                    factory = { context ->
                        PlayerView(context).apply {
                            this.player = player
                            useController = true
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                            setShutterBackgroundColor(AndroidColor.BLACK)
                            keepScreenOn = true
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            ZoomableImage(
                url = route.url,
                contentDescription = route.name,
                modifier = Modifier.fillMaxSize(),
            )
        }

        OverlayBar(
            title = route.name,
            downloading = downloadState is DownloadState.InProgress,
            onBack = onBack,
            onDownload = viewModel::download,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp),
        )
    }
}

@Composable
private fun OverlayBar(
    title: String,
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

@Composable
private fun ZoomableImage(
    url: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val next = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                    val maxX = size.width * (next - 1f) / 2f
                    val maxY = size.height * (next - 1f) / 2f
                    offset = if (next <= MIN_SCALE) {
                        Offset.Zero
                    } else {
                        Offset(
                            x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                            y = (offset.y + pan.y).coerceIn(-maxY, maxY),
                        )
                    }
                    scale = next
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale > MIN_SCALE) MIN_SCALE else DOUBLE_TAP_SCALE
                        if (scale == MIN_SCALE) offset = Offset.Zero
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
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

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f
