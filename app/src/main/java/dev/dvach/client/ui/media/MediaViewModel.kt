package dev.dvach.client.ui.media

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import dev.dvach.client.data.settings.SettingsRepository
import dev.dvach.client.di.MediaClient
import dev.dvach.client.domain.model.Attachment
import dev.dvach.client.ui.navigation.MediaRoute
import java.io.File
import javax.inject.Inject

sealed interface DownloadState {
    data object Idle : DownloadState

    data object InProgress : DownloadState

    /** [where] — путь или имя файла в галерее, чтобы показать пользователю. */
    data class Done(val where: String) : DownloadState

    data class Failed(val message: String) : DownloadState
}

@HiltViewModel
class MediaViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @MediaClient private val client: OkHttpClient,
    private val playlist: MediaPlaylist,
    settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val route: MediaRoute = savedStateHandle.toRoute()

    /**
     * Всё, что можно листать свайпом: файлы треда из [MediaPlaylist] или
     * единственное вложение из маршрута (если процесс пересоздали).
     */
    val items: List<Attachment> = playlist.items.ifEmpty {
        listOf(
            Attachment(
                url = route.url,
                thumbnailUrl = "",
                name = route.name,
                sizeBytes = 0,
                width = 0,
                height = 0,
                isVideo = route.isVideo,
            ),
        )
    }

    val initialPage: Int = playlist.index.coerceIn(0, (items.size - 1).coerceAtLeast(0))

    private val _page = MutableStateFlow(initialPage)
    val page: StateFlow<Int> = _page.asStateFlow()

    private val _download = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val download: StateFlow<DownloadState> = _download.asStateFlow()

    /**
     * Направление листания в галерее. По умолчанию вбок, как в обычной
     * галерее; включённая настройка делает ленту вертикальной, как в TikTok.
     */
    val verticalPaging: StateFlow<Boolean> = settingsRepository.settings
        .map { it.verticalMediaPaging }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * Плеер живёт во ViewModel, чтобы переживать поворот экрана, и создаётся
     * лениво — только когда на экране действительно видео. Flow нужен, чтобы
     * экран перерисовался в момент создания плеера.
     */
    private val _player = MutableStateFlow<ExoPlayer?>(null)
    val playerFlow: StateFlow<ExoPlayer?> = _player.asStateFlow()

    private var preparedUrl: String? = null

    init {
        syncPlayer()
    }

    /** Текущее вложение — от него зависят заголовок, счётчик и скачивание. */
    fun currentItem(): Attachment = items[_page.value.coerceIn(0, items.lastIndex)]

    /**
     * Переключение страницы пейджера: картинке плеер не нужен, а для видео
     * подменяем медиа-источник. Один плеер на весь просмотрщик — иначе на
     * каждом свайпе пришлось бы освобождать предыдущий.
     */
    fun onPageChanged(page: Int) {
        val index = page.coerceIn(0, items.lastIndex)
        _page.value = index
        // Плейлист помнит, до какого файла долистали: по возврате в тред
        // список прокрутится к посту с этим вложением.
        playlist.select(index)
        syncPlayer()
    }

    private fun syncPlayer() {
        val item = currentItem()
        val existing = _player.value
        if (!item.isVideo) {
            existing?.pause()
            return
        }
        val exo = existing ?: createPlayer().also { _player.value = it }
        if (preparedUrl != item.url) {
            preparedUrl = item.url
            exo.setMediaItem(MediaItem.fromUri(item.url))
            exo.prepare()
        }
        exo.playWhenReady = true
    }

    private fun createPlayer(): ExoPlayer {
        // Видео идёт через тот же OkHttp (UA для Cloudflare), но без callTimeout.
        val dataSourceFactory = DefaultDataSource.Factory(
            context,
            OkHttpDataSource.Factory(client),
        )
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    /* handleAudioFocus = */ true,
                )
                setHandleAudioBecomingNoisy(true)
            }
    }

    fun download() {
        if (_download.value is DownloadState.InProgress) return
        _download.value = DownloadState.InProgress
        val item = currentItem()
        viewModelScope.launch {
            _download.value = runCatching { saveToGallery(item) }
                .fold(
                    onSuccess = { DownloadState.Done(it) },
                    onFailure = { DownloadState.Failed(it.message ?: "Не удалось скачать файл") },
                )
        }
    }

    fun clearDownloadState() {
        _download.value = DownloadState.Idle
    }

    override fun onCleared() {
        _player.value?.release()
        _player.value = null
    }

    private fun fileName(item: Attachment): String {
        val name = item.name.ifBlank { route.name.ifBlank { item.url.substringAfterLast('/') } }
        val ext = item.url.substringAfterLast('.', "")
        return if (ext.isEmpty() || name.substringAfterLast('.', "").isNotEmpty()) {
            name
        } else {
            "$name.$ext"
        }
    }

    private fun mimeType(item: Attachment): String = when (item.url.substringAfterLast('.').lowercase()) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "mp4" -> "video/mp4"
        "webm" -> "video/webm"
        "mkv" -> "video/x-matroska"
        else -> if (item.isVideo) "video/*" else "image/jpeg"
    }

    /**
     * Сохраняем в общую галерею через MediaStore (Android 10+ не требует
     * разрешений). На старых версиях RELATIVE_PATH нет, поэтому кладём в
     * приватную папку приложения — она доступна без WRITE_EXTERNAL_STORAGE.
     */
    private suspend fun saveToGallery(item: Attachment): String =
        withContext(Dispatchers.IO) {
            val name = fileName(item)
            val request = Request.Builder().url(item.url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Сервер ответил " + response.code)
                val body = response.body ?: error("Пустой ответ")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val collection = if (item.isVideo) {
                        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    } else {
                        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    }
                    val relativePath = if (item.isVideo) {
                        Environment.DIRECTORY_MOVIES + "/Dvach"
                    } else {
                        Environment.DIRECTORY_PICTURES + "/Dvach"
                    }
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType(item))
                        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    val uri = context.contentResolver.insert(collection, values)
                        ?: error("Не удалось создать файл в галерее")
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            body.byteStream().copyTo(out)
                        } ?: error("Не удалось открыть файл для записи")
                        values.clear()
                        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        context.contentResolver.update(uri, values, null, null)
                    } catch (error: Throwable) {
                        context.contentResolver.delete(uri, null, null)
                        throw error
                    }
                    "$relativePath/$name"
                } else {
                    val dir = File(
                        context.getExternalFilesDir(
                            if (item.isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES,
                        ),
                        "Dvach",
                    ).apply { mkdirs() }
                    val file = File(dir, name)
                    file.outputStream().use { out -> body.byteStream().copyTo(out) }
                    file.absolutePath
                }
            }
        }
}
