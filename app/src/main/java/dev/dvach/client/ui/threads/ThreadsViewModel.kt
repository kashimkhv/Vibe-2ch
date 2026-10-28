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
import dev.dvach.client.domain.model.ThreadSummary
import dev.dvach.client.ui.navigation.ThreadsRoute
import dev.dvach.client.util.THUMBNAIL_SCALE_DEFAULT
import javax.inject.Inject

data class ThreadsUiState(
    val boardId: String,
    val boardName: String,
    val page: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val threads: List<ThreadSummary> = emptyList(),
    val canGoNext: Boolean = false,
    /** Размер превью вложений в процентах, из настроек. */
    val thumbnailScalePercent: Int = THUMBNAIL_SCALE_DEFAULT,
    /** Спойлеры в превью ОП-поста раскрыты сразу. */
    val autoRevealSpoilers: Boolean = false,
)

@HiltViewModel
class ThreadsViewModel @Inject constructor(
    private val repository: ChanRepository,
    private val settings: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<ThreadsRoute>()

    private val _state = MutableStateFlow(
        ThreadsUiState(boardId = route.boardId, boardName = route.boardName),
    )
    val state: StateFlow<ThreadsUiState> = _state.asStateFlow()

    init {
        loadPage(0)
        // Размер превью меняется в настройках — держим его в состоянии экрана,
        // чтобы список перерисовался сразу после возврата.
        viewModelScope.launch {
            settings.settings.collect { appSettings ->
                _state.update {
                    it.copy(
                        thumbnailScalePercent = appSettings.thumbnailScalePercent,
                        autoRevealSpoilers = appSettings.autoRevealSpoilers,
                    )
                }
            }
        }
    }

    fun loadPage(page: Int) {
        _state.update { it.copy(isLoading = true, error = null, page = page) }
        viewModelScope.launch {
            runCatching { repository.index(_state.value.boardId, page) }
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            threads = result.threads,
                            page = result.currentPage,
                            // Страница заполнена целиком и лимит страниц доски не достигнут.
                            canGoNext = result.threads.size >= result.threadsPerPage &&
                                result.currentPage + 1 < result.maxPages,
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

    fun refresh() = loadPage(_state.value.page)

    fun nextPage() {
        if (_state.value.canGoNext) loadPage(_state.value.page + 1)
    }

    fun previousPage() {
        if (_state.value.page > 0) loadPage(_state.value.page - 1)
    }
}
