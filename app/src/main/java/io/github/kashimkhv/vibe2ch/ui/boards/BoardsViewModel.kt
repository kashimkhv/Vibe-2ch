package io.github.kashimkhv.vibe2ch.ui.boards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.github.kashimkhv.vibe2ch.data.ChanRepository
import io.github.kashimkhv.vibe2ch.data.local.ChanDao
import io.github.kashimkhv.vibe2ch.data.local.PinnedBoardEntity
import io.github.kashimkhv.vibe2ch.domain.model.Board
import javax.inject.Inject

data class BoardsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val boards: List<Board> = emptyList(),
    val query: String = "",
    val error: String? = null,
    /** Закреплённые доски в порядке закрепления. */
    val pinnedBoardIds: List<String> = emptyList(),
) {
    val visibleBoards: List<Board>
        get() {
            if (query.isBlank()) return boards
            val q = query.trim().lowercase()
            return boards.filter {
                it.id.contains(q) ||
                    it.name.lowercase().contains(q) ||
                    it.category.lowercase().contains(q) ||
                    it.info.lowercase().contains(q)
            }
        }

    /** Закреплённые доски, которые проходят текущий поиск: они идут первыми. */
    val pinnedBoards: List<Board>
        get() {
            if (pinnedBoardIds.isEmpty()) return emptyList()
            val byId = visibleBoards.associateBy { it.id }
            return pinnedBoardIds.mapNotNull { byId[it] }
        }

    /**
     * Доски по категориям. Закреплённые остаются и в своей категории — так их
     * видно на привычном месте, а сверху список всегда под рукой.
     */
    val grouped: List<Pair<String, List<Board>>>
        get() = visibleBoards.groupBy { it.category.ifBlank { "Разное" } }.toList()
}

@HiltViewModel
class BoardsViewModel @Inject constructor(
    private val repository: ChanRepository,
    private val dao: ChanDao,
) : ViewModel() {

    private val _state = MutableStateFlow(BoardsUiState())
    val state: StateFlow<BoardsUiState> = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            dao.pinnedBoards().collect { rows ->
                _state.update { it.copy(pinnedBoardIds = rows.map(PinnedBoardEntity::boardId)) }
            }
        }
    }

    fun load() {
        if (_state.value.boards.isEmpty()) {
            _state.update { it.copy(isLoading = true, error = null) }
        } else {
            _state.update { it.copy(isRefreshing = true, error = null) }
        }
        viewModelScope.launch {
            runCatching { repository.boards() }
                .onSuccess { boards ->
                    _state.update {
                        it.copy(isLoading = false, isRefreshing = false, boards = boards, error = null)
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = error.message ?: error::class.simpleName ?: "Ошибка сети",
                        )
                    }
                }
        }
    }

    fun onQueryChange(query: String) = _state.update { it.copy(query = query) }

    /** Закрепить доску в начале списка или снять закрепление. */
    fun togglePin(board: Board) {
        viewModelScope.launch {
            if (_state.value.pinnedBoardIds.contains(board.id)) {
                dao.unpinBoard(board.id)
            } else {
                dao.pinBoard(PinnedBoardEntity(board.id, System.currentTimeMillis()))
            }
        }
    }
}
