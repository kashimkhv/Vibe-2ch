package io.github.kashimkhv.vibe2ch.ui.hidden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import io.github.kashimkhv.vibe2ch.data.local.ChanDao
import io.github.kashimkhv.vibe2ch.data.local.HiddenThreadEntity
import javax.inject.Inject

/**
 * Список скрытых тредов. Скрытие только прячет карточку из ленты: тред
 * остаётся на доске и в базе, поэтому его видно здесь и можно вернуть.
 */
@HiltViewModel
class HiddenThreadsViewModel @Inject constructor(
    private val dao: ChanDao,
) : ViewModel() {

    val hidden: StateFlow<List<HiddenThreadEntity>> = dao.hiddenThreads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun restore(thread: HiddenThreadEntity) {
        viewModelScope.launch { dao.unhideThread(thread.boardId, thread.threadNum) }
    }

    fun restoreAll() {
        viewModelScope.launch { dao.unhideAllThreads() }
    }
}
