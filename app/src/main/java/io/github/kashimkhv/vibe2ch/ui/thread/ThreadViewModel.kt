package io.github.kashimkhv.vibe2ch.ui.thread

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.github.kashimkhv.vibe2ch.data.ChanRepository
import io.github.kashimkhv.vibe2ch.data.PostedStore
import io.github.kashimkhv.vibe2ch.data.local.BookmarkEntity
import io.github.kashimkhv.vibe2ch.data.local.ChanDao
import io.github.kashimkhv.vibe2ch.data.local.HistoryEntity
import io.github.kashimkhv.vibe2ch.data.local.ThreadScrollRepository
import io.github.kashimkhv.vibe2ch.data.settings.AppSettings
import io.github.kashimkhv.vibe2ch.data.settings.SettingsRepository
import io.github.kashimkhv.vibe2ch.domain.model.Attachment
import io.github.kashimkhv.vibe2ch.domain.model.Post
import io.github.kashimkhv.vibe2ch.ui.media.MediaPlaylist
import io.github.kashimkhv.vibe2ch.ui.navigation.ThreadRoute
import io.github.kashimkhv.vibe2ch.util.stripHtml
import javax.inject.Inject

/** Место, на котором пользователь остановился в треде. */
data class SavedScroll(
    val postNum: Int,
    val index: Int,
    val offset: Int,
)

data class ThreadUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val posts: List<Post> = emptyList(),
    /** Сохранённая позиция чтения; null — тред ещё не открывали. */
    val savedScroll: SavedScroll? = null,
    /** Позиция прочитана из базы: только после этого можно решать, прокручивать ли. */
    val scrollLoaded: Boolean = false,
    /** Строка для снекбара: экран покажет её один раз и погасит. */
    val notice: String? = null,
)

/**
 * Сколько раз перезапрашивать тред после отправки поста и с какой паузой.
 *
 * Восемь попыток по 0,6 с — около пяти секунд ожидания плюс время самих
 * запросов. Обычно пост виден уже в первом ответе, но если запрос попал в
 * кэш Cloudflare или движок ещё не пересобрал JSON, остальные попытки
 * закрывают эту задержку, не заставляя человека обновлять тред руками.
 */
private const val POST_RELOAD_ATTEMPTS = 8
private const val POST_RELOAD_DELAY_MS = 600L

@HiltViewModel
class ThreadViewModel @Inject constructor(
    private val repository: ChanRepository,
    private val dao: ChanDao,
    private val scrollRepository: ThreadScrollRepository,
    private val mediaPlaylist: MediaPlaylist,
    private val postedStore: PostedStore,
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

    /** Номера своих постов в треде: по ним пост подсвечивается как «мой». */
    val myPosts: StateFlow<Set<Int>> = dao.myPosts(route.boardId, route.threadNum)
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Ключ треда в базе: позиция чтения хранится по доске и номеру. */
    private val threadKey: String get() = "${route.boardId}/${route.threadNum}"

    init {
        viewModelScope.launch {
            val saved = scrollRepository.load(threadKey)
            _state.update {
                it.copy(
                    savedScroll = saved?.let { entry ->
                        SavedScroll(entry.postNum, entry.scrollIndex, entry.scrollOffset)
                    },
                    scrollLoaded = true,
                )
            }
        }
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
     * Возврат из формы поста. Если только что написали в этот тред, отдаёт
     * номер нового поста и перезагружает тред — экран прокрутит к нему.
     * Номер забирается один раз: повторный возврат ничего не двигает.
     */
    fun consumePosted(): Int? {
        val posted = postedStore.consume(route.boardId, route.threadNum) ?: return null
        reloadUntilPostAppears(posted)
        return posted
    }

    /** Забирает сообщение для снекбара: показывается один раз. */
    fun consumeNotice(): String? {
        val notice = _state.value.notice ?: return null
        _state.update { it.copy(notice = null) }
        return notice
    }

    /**
     * Перезагрузка сразу после отправки: движок принимает пост постингом
     * раньше, чем показывает его в JSON треда. Одного запроса поэтому мало —
     * повторяем, пока номер не появится в ответе.
     *
     * `isLoading` держим включённым до конца: по нему экран понимает, что
     * поста ещё можно ждать, и не сбрасывает прокрутку к нему (см.
     * `ThreadScreen`, эффект ожидания `pendingScroll`).
     */
    private fun reloadUntilPostAppears(postNum: Int) {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            var lastError: String? = null
            repeat(POST_RELOAD_ATTEMPTS) { attempt ->
                val posts = runCatching { repository.thread(route.boardId, route.threadNum) }
                    .onFailure { lastError = it.message ?: "Ошибка сети" }
                    .getOrNull()
                if (posts != null) {
                    lastError = null
                    _state.update { it.copy(posts = posts) }
                    rememberVisit(posts)
                    if (posts.any { it.num == postNum }) {
                        _state.update { it.copy(isLoading = false, error = null) }
                        return@launch
                    }
                }
                if (attempt < POST_RELOAD_ATTEMPTS - 1) delay(POST_RELOAD_DELAY_MS)
            }
            // Пост ушёл, а тред его так и не показал: бывает, что движок
            // отдаёт старый JSON из своего кэша, бывает — пост не прошёл.
            // Молчать нельзя, человек ждёт свой пост на экране.
            _state.update {
                it.copy(
                    isLoading = false,
                    error = lastError,
                    notice = "Пост $postNum отправлен, но в треде его нет. Потяните вниз, чтобы обновить",
                )
            }
        }
    }

    /**
     * Кладём в просмотрщик все вложения треда, чтобы их можно было листать
     * свайпом, и запоминаем, какое открыли.
     */
    /**
     * Пост, чьё вложение последним листали в просмотрщике, — чтобы после
     * возврата тред встал на него, а не на то место, где его оставили.
     *
     * `null`, если в просмотрщике не листали или долистали только до файлов
     * того же поста: тогда позицию менять незачем.
     */
    fun consumeMediaPost(): Int? {
        val viewed = mediaPlaylist.consumeSwiped() ?: return null
        return _state.value.posts.firstOrNull { post ->
            post.files.any { it.url == viewed.url }
        }?.num
    }

    fun prepareMedia(file: Attachment) {
        val files = _state.value.posts.flatMap { it.files }
        val index = files.indexOfFirst { it.url == file.url && it.name == file.name }
        if (index >= 0) {
            mediaPlaylist.set(files, index)
        } else {
            mediaPlaylist.set(listOf(file), 0)
        }
    }

    /**
     * Запоминает место, на котором закрыли тред. Вызывается при уходе с экрана,
     * поэтому запись идёт через репозиторий со своим scope: `viewModelScope`
     * к этому моменту уже отменён.
     *
     * Позицию держим и в состоянии: при возврате из просмотрщика экран треда
     * собирается заново, и восстанавливать место надо по последнему кадру, а не
     * по записи из базы, прочитанной при открытии треда.
     */
    fun saveScroll(postNum: Int, index: Int, offset: Int) {
        _state.update { it.copy(savedScroll = SavedScroll(postNum, index, offset)) }
        scrollRepository.save(threadKey, postNum, index, offset)
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
