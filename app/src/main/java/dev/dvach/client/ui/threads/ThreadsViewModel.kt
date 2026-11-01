package dev.dvach.client.ui.threads

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dev.dvach.client.data.ChanRepository
import dev.dvach.client.data.settings.SettingsRepository
import dev.dvach.client.domain.model.Attachment
import dev.dvach.client.domain.model.ThreadSummary
import dev.dvach.client.domain.model.ThreadsPage
import dev.dvach.client.ui.media.MediaPlaylist
import dev.dvach.client.ui.navigation.ThreadsRoute
import dev.dvach.client.util.THUMBNAIL_SCALE_DEFAULT
import javax.inject.Inject

data class ThreadsUiState(
    val boardId: String,
    val boardName: String,
    /** Номер последней загруженной страницы (с нуля). */
    val page: Int = 0,
    val isLoading: Boolean = true,
    /** Догрузка следующей страницы: список уже на экране, внизу крутится спиннер. */
    val isLoadingMore: Boolean = false,
    /** Ошибка догрузки: список остался, внизу — кнопка «Повторить». */
    val loadMoreError: String? = null,
    val error: String? = null,
    val threads: List<ThreadSummary> = emptyList(),
    val canGoNext: Boolean = false,
    /** Размер превью ОП-поста в списке, в процентах; у него отдельная настройка. */
    val listThumbnailScalePercent: Int = THUMBNAIL_SCALE_DEFAULT,
    /** Превью ОП-поста вдвое крупнее, если вложений меньше трёх. */
    val enlargeFewListThumbnails: Boolean = true,
    /** Превью ОП-поста обрезается до квадрата, а не вписывается в него. */
    val cropThumbnails: Boolean = true,
    /** Подложка под превью ОП-поста в списке. */
    val thumbnailBackground: Boolean = true,
    /** Спойлеры в превью ОП-поста раскрыты сразу. */
    val autoRevealSpoilers: Boolean = false,
)

@HiltViewModel
class ThreadsViewModel @Inject constructor(
    private val repository: ChanRepository,
    private val settings: SettingsRepository,
    private val mediaPlaylist: MediaPlaylist,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<ThreadsRoute>()

    private val _state = MutableStateFlow(
        ThreadsUiState(boardId = route.boardId, boardName = route.boardName),
    )
    val state: StateFlow<ThreadsUiState> = _state.asStateFlow()

    init {
        loadFirstPage()
        // Размер превью меняется в настройках — держим его в состоянии экрана,
        // чтобы список перерисовался сразу после возврата.
        viewModelScope.launch {
            settings.settings.collect { appSettings ->
                _state.update {
                    it.copy(
                        listThumbnailScalePercent = appSettings.listThumbnailScalePercent,
                        enlargeFewListThumbnails = appSettings.enlargeFewListThumbnails,
                        cropThumbnails = appSettings.cropThumbnails,
                        thumbnailBackground = appSettings.thumbnailBackground,
                        autoRevealSpoilers = appSettings.autoRevealSpoilers,
                    )
                }
            }
        }
    }

    /** Первая страница: замена списка целиком (первый вход и обновление). */
    fun loadFirstPage() {
        _state.update { it.copy(isLoading = true, error = null, loadMoreError = null) }
        viewModelScope.launch {
            runCatching { repository.index(route.boardId, 0) }
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            threads = result.threads,
                            page = result.currentPage,
                            canGoNext = result.canGoNext(),
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(isLoading = false, error = error.message ?: "Ошибка сети")
                    }
                }
        }
    }

    /**
     * Догрузка следующей страницы — вызывается, когда список прокрутили
     * почти до конца. Повторный вызов во время загрузки игнорируется.
     *
     * @param force повтор после ошибки (кнопка «Повторить»), иначе авто-догрузка
     *   не долбит сервер, пока ошибка не снята.
     */
    fun loadMore(force: Boolean = false) {
        val current = _state.value
        if (current.isLoading || current.isLoadingMore || !current.canGoNext) return
        if (current.loadMoreError != null && !force) return

        _state.update { it.copy(isLoadingMore = true, loadMoreError = null) }
        viewModelScope.launch {
            runCatching { repository.index(route.boardId, current.page + 1) }
                .onSuccess { result ->
                    _state.update { state ->
                        state.copy(
                            isLoadingMore = false,
                            loadMoreError = null,
                            // Между страницами треды могли переехать (бамп), поэтому
                            // склеиваем без дублей по номеру треда.
                            threads = mergeThreads(state.threads, result.threads),
                            page = result.currentPage,
                            canGoNext = result.canGoNext(),
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoadingMore = false,
                            loadMoreError = error.message ?: "Ошибка сети",
                        )
                    }
                }
        }
    }

    fun refresh() = loadFirstPage()

    /**
     * Просмотрщик из списка тредов: листать предлагаем только вложения
     * ОП-поста — тот пост, чьи превью и нажали.
     */
    fun prepareMedia(thread: ThreadSummary, file: Attachment) {
        val files = thread.attachments
        val index = files.indexOfFirst { it.url == file.url && it.name == file.name }
        if (index >= 0) {
            mediaPlaylist.set(files, index)
        } else {
            mediaPlaylist.set(listOf(file), 0)
        }
    }

    /** Возврат из галереи: «долистали до файла» здесь ни на что не влияет. */
    fun discardMediaSwipe() {
        mediaPlaylist.consumeSwiped()
    }
}

/**
 * Страница считается заполненной и лимит страниц доски не достигнут — значит,
 * дальше есть что грузить. Та же проверка, что была у кнопки «вперёд».
 */
private fun ThreadsPage.canGoNext(): Boolean =
    threads.size >= threadsPerPage && currentPage + 1 < maxPages

/** Склейка страниц без дублей: порядок сохраняется, повторные треды отбрасываются. */
internal fun mergeThreads(
    loaded: List<ThreadSummary>,
    next: List<ThreadSummary>,
): List<ThreadSummary> = (loaded + next).distinctBy { it.num }
