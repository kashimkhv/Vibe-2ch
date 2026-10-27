package dev.dvach.client.ui.boards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dev.dvach.client.data.ChanRepository
import dev.dvach.client.domain.model.Board
import javax.inject.Inject

data class BoardsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val boards: List<Board> = emptyList(),
    val query: String = "",
    val error: String? = null,
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

    val grouped: List<Pair<String, List<Board>>>
        get() = visibleBoards.groupBy { it.category.ifBlank { "Разное" } }.toList()
}

@HiltViewModel
class BoardsViewModel @Inject constructor(
    private val repository: ChanRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BoardsUiState())
    val state: StateFlow<BoardsUiState> = _state.asStateFlow()

    init {
        load()
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
}
