package dev.dvach.client.ui.thread

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dev.dvach.client.data.ChanRepository
import dev.dvach.client.data.local.BookmarkEntity
import dev.dvach.client.data.local.ChanDao
import dev.dvach.client.data.local.HistoryEntity
import dev.dvach.client.data.settings.AppSettings
import dev.dvach.client.data.settings.SettingsRepository
import dev.dvach.client.domain.model.Attachment
import dev.dvach.client.domain.model.Post
import dev.dvach.client.ui.media.MediaPlaylist
import dev.dvach.client.ui.navigation.ThreadRoute
import dev.dvach.client.util.stripHtml
import javax.inject.Inject

data class ThreadUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val posts: List<Post> = emptyList(),
)

@HiltViewModel
class ThreadViewModel @Inject constructor(
    private val repository: ChanRepository,
    private val dao: ChanDao,
    private val mediaPlaylist: MediaPlaylist,
    settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val route: ThreadRoute = savedStateHandle.toRoute()

    private val _state = MutableStateFlow(ThreadUiState())
    val state: StateFlow<ThreadUiState> = _state.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val isBookmarked: StateFlow<Boolean> = dao.isBookmarked(route.boardId, route.threadNum)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.thread(route.boardId, route.threadNum) }
                .onSuccess { posts ->
                    _state.update { it.copy(isLoading = false, posts = posts, error = null) }
                    rememberVisit(posts)
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(isLoading = false, error = error.message ?: "Ошибка сети")
                    }
                }
        }
    }

    /**
     * Кладём в просмотрщик все вложения треда, чтобы их можно было листать
     * свайпом, и запоминаем, какое открыли.
     */
    fun prepareMedia(file: Attachment) {
        val files = _state.value.posts.flatMap { it.files }
        val index = files.indexOfFirst { it.url == file.url && it.name == file.name }
        if (index >= 0) {
            mediaPlaylist.set(files, index)
        } else {
            mediaPlaylist.set(listOf(file), 0)
        }
    }

    fun toggleBookmark() {
        val bookmarked = isBookmarked.value
        viewModelScope.launch {
            if (bookmarked) {
                dao.removeBookmark(route.boardId, route.threadNum)
            } else {
                dao.addBookmark(
                    BookmarkEntity(
                        boardId = route.boardId,
                        threadNum = route.threadNum,
                        subject = threadTitle(),
                        createdAt = System.currentTimeMillis(),
                    ),
                )
            }
        }
    }

    fun threadTitle(): String =
        _state.value.posts.firstOrNull()?.let { op ->
            op.subject.ifBlank { plainText(op.commentHtml) }
        }?.take(140).orEmpty().ifBlank { "№${route.threadNum}" }

    private suspend fun rememberVisit(posts: List<Post>) {
        val op = posts.firstOrNull() ?: return
        dao.touchHistory(
            HistoryEntity(
                threadKey = "${route.boardId}/${route.threadNum}",
                boardId = route.boardId,
                threadNum = route.threadNum,
                subject = op.subject.ifBlank { plainText(op.commentHtml) }.take(140),
                visitedAt = System.currentTimeMillis(),
            ),
        )
    }
}

internal fun plainText(html: String): String = stripHtml(html)
