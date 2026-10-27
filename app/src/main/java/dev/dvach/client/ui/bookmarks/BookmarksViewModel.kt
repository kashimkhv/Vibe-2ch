package dev.dvach.client.ui.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import dev.dvach.client.data.local.BookmarkEntity
import dev.dvach.client.data.local.ChanDao
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val dao: ChanDao,
) : ViewModel() {

    val bookmarks: StateFlow<List<BookmarkEntity>> = dao.bookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun remove(bookmark: BookmarkEntity) {
        viewModelScope.launch { dao.removeBookmark(bookmark.boardId, bookmark.threadNum) }
    }
}
