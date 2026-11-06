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
import dev.dvach.client.data.local.ChanDao
import dev.dvach.client.data.local.HiddenThreadEntity
import dev.dvach.client.data.settings.SettingsRepository
import dev.dvach.client.domain.ThreadSort
import dev.dvach.client.domain.boardActivity
import dev.dvach.client.domain.sortThreads
import dev.dvach.client.domain.model.Attachment
import dev.dvach.client.domain.model.ThreadSummary
import dev.dvach.client.domain.model.ThreadsPage
import dev.dvach.client.ui.media.MediaPlaylist
import dev.dvach.client.ui.navigation.ThreadsRoute
import dev.dvach.client.util.TEXT_MARGIN_DEFAULT
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
    /** Поля текста карточки — та же настройка чтения, что и в треде. */
    val textMarginsDp: Int = TEXT_MARGIN_DEFAULT,
    /** Порядок тредов: бамп движка или своя сортировка по загруженным страницам. */
    val sort: ThreadSort = ThreadSort.BUMP,
    /** Направление сортировки: по возрастанию — А→Я, старые и маленькие сверху. */
    val sortAscending: Boolean = false,
    /** Время в карточках — словами («5 мин назад») вместо точной даты. */
    val relativeDates: Boolean = false,
    /**
     * Активность доски: постов в час. Число движка (`board_speed`) берётся
     * при загрузке и тихом обновлении, а не на каждый пост, — иначе оно
     * дёргалось бы при прокрутке. `null` — ещё не посчитано.
     */
    val postsPerHour: Double? = null,
    /** `postsPerHour` посчитан клиентом по странице, а не прислан движком. */
    val postsPerHourIsEstimate: Boolean = false,
    /** Номера скрытых тредов этой доски: их карточки в ленту не попадают. */
    val hiddenNums: Set<Int> = emptySet(),
) {
    /**
     * Треды в том порядке, в котором их видит человек. Исходный список
     * (`threads`) всегда в порядке движка: по нему же склеиваются страницы,
     * поэтому сортировку считаем поверх, а не меняем сам список. Скрытые
     * вычитаются до сортировки — иначе они занимали бы место в списке
     * и попадали бы под правило «закреплённые сверху».
     */
    val orderedThreads: List<ThreadSummary>
        get() = sortThreads(threads.withoutHidden(hiddenNums), sort, sortAscending)
}

@HiltViewModel
class ThreadsViewModel @Inject constructor(
    private val repository: ChanRepository,
    private val settings: SettingsRepository,
    private val mediaPlaylist: MediaPlaylist,
    private val dao: ChanDao,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<ThreadsRoute>()

    private val _state = MutableStateFlow(
        ThreadsUiState(boardId = route.boardId, boardName = route.boardName),
    )
    val state: StateFlow<ThreadsUiState> = _state.asStateFlow()

    /**
     * Когда список последний раз запрашивали. Тихий refresh после возврата из
     * треда смотрит на это время, чтобы «туда-обратно» не долбило сервер.
     */
    private var lastLoadAt = 0L

    /** Тихий refresh уже идёт: второй параллельный не нужен. */
    private var quietRefreshRunning = false

    init {
        loadFirstPage()
        // Ссылка из чужого приложения знает только id доски: имя для шапки
        // дочитываем из списка досок. Без сети останется id — экран не пустой.
        if (route.boardName.isBlank()) {
            viewModelScope.launch {
                repository.board(route.boardId)?.let { board ->
                    _state.update { it.copy(boardName = board.name) }
                }
            }
        }
        // Скрытые треды живут в базе: отметил — карточка пропала и после
        // перезапуска, вернул в настройках — появилась снова.
        viewModelScope.launch {
            dao.hiddenThreads(route.boardId).collect { hidden ->
                _state.update { it.copy(hiddenNums = hidden.map { h -> h.threadNum }.toSet()) }
            }
        }
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
                        textMarginsDp = appSettings.textMarginsDp,
                        sort = appSettings.threadSort,
                        sortAscending = appSettings.threadSortAscending,
                        relativeDates = appSettings.relativeDates,
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
                    lastLoadAt = System.currentTimeMillis()
                    val activity = boardActivity(result)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            threads = result.threads,
                            page = result.currentPage,
                            canGoNext = result.canGoNext(),
                            // Число движка — по всей доске; своя оценка — только
                            // если он его не прислал.
                            postsPerHour = activity.postsPerHour,
                            postsPerHourIsEstimate = activity.isEstimate,
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
                    lastLoadAt = System.currentTimeMillis()
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
     * Тихий refresh при возврате из треда: подтягивает первую страницу, чтобы
     * счётчики постов были свежими, тред встал по бампу, а удалённый на движке
     * тред пропал из ленты.
     *
     * В отличие от [refresh] он ничего не показывает: нет ни спиннера в шапке,
     * ни ошибки — при сбое сети список просто остаётся прежним. Догруженные
     * страницы остаются под свежей первой (см. [mergeFirstPage]), а позицию
     * держит сама лента: у карточек ключ — номер треда. Если список только что
     * запрашивали, запрос не повторяется: быстрое «открыл тред — вернулся»
     * ничего не меняет.
     */
    suspend fun refreshQuietly() {
        if (quietRefreshRunning) return
        if (_state.value.isLoading || _state.value.isLoadingMore) return
        if (System.currentTimeMillis() - lastLoadAt < QUIET_REFRESH_INTERVAL_MS) return
        // Время попытки, а не успеха: иначе при недоступной сети каждый возврат
        // снова ходил бы в сеть.
        lastLoadAt = System.currentTimeMillis()
        quietRefreshRunning = true
        try {
            val fresh = runCatching { repository.index(route.boardId, 0) }.getOrNull() ?: return
            // Пустая страница — скорее сбой движка, чем «доска опустела»:
            // гасить ленту тихим обновлением нельзя, для этого есть «Обновить».
            if (fresh.threads.isEmpty()) return
            val activity = boardActivity(fresh)
            _state.update { state ->
                state.copy(
                    threads = mergeFirstPage(
                        loaded = state.threads,
                        fresh = fresh.threads,
                        keepLoadedTail = state.page > 0,
                    ),
                    // Если догруженных страниц не было, свежая первая — это и есть
                    // весь список: «есть ли дальше» берём из неё.
                    canGoNext = if (state.page > 0) state.canGoNext else fresh.canGoNext(),
                    // Активность обновляем вместе с лентой: по ней видно, ожил
                    // тред или нет.
                    postsPerHour = activity.postsPerHour,
                    postsPerHourIsEstimate = activity.isEstimate,
                )
            }
        } finally {
            quietRefreshRunning = false
        }
    }

    /**
     * Выбор порядка тредов. Пишется в настройки: порядок один на всё приложение
     * и переживает перезапуск.
     */
    fun setSort(sort: ThreadSort, ascending: Boolean = sort.defaultAscending) {
        _state.update { it.copy(sort = sort, sortAscending = ascending) }
        viewModelScope.launch { settings.setThreadSort(sort, ascending) }
    }

    /** Переключение направления у текущей сортировки: А→Я или Я→А, новые или старые. */
    fun toggleSortDirection() {
        val current = _state.value
        if (!current.sort.canReverse) return
        setSort(current.sort, !current.sortAscending)
    }

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

    /**
     * Скрыть тред: карточка уходит из ленты доски и не вернётся после
     * «Обновить». Сама запись остаётся в базе — в настройках её видно
     * в списке «Скрытые треды» и можно вернуть.
     */
    fun hide(thread: ThreadSummary) {
        viewModelScope.launch {
            dao.hideThread(
                HiddenThreadEntity(
                    boardId = route.boardId,
                    threadNum = thread.num,
                    subject = thread.subject,
                    hiddenAt = System.currentTimeMillis(),
                ),
            )
        }
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

/**
 * Место свежей первой страницы в уже загруженном списке — тихий refresh после
 * возврата из треда.
 *
 * Догруженные страницы остаются под свежей первой: возврат из треда не должен
 * схлопывать ленту до одной страницы. Совпавшие номера берутся из свежей
 * страницы — у неё новые счётчики и порядок по бампу. Если догруженных страниц
 * не было, старый список заменяется целиком: иначе тред, удалённый на движке,
 * висел бы в ленте до ручного «Обновить».
 *
 * Когда догруженные страницы есть, треды, удалённые не с первой страницы, так
 * и останутся внизу — тихий refresh знает только про первую. Их уберёт полное
 * обновление («Обновить» или повторный вход на доску).
 */
internal fun mergeFirstPage(
    loaded: List<ThreadSummary>,
    fresh: List<ThreadSummary>,
    keepLoadedTail: Boolean,
): List<ThreadSummary> {
    if (!keepLoadedTail) return fresh
    val freshNums = fresh.mapTo(HashSet(fresh.size)) { it.num }
    return fresh + loaded.filterNot { it.num in freshNums }
}

/** Тихий refresh не чаще раза в 5 секунд: быстрый возврат не стоит запроса. */
private const val QUIET_REFRESH_INTERVAL_MS = 5_000L

/**
 * Лента без скрытых тредов. Скрытые — это номера текущей доски, поэтому
 * сравнивать достаточно по номеру: на другой доске он значил бы другой тред,
 * но туда этот набор и не попадает.
 */
internal fun List<ThreadSummary>.withoutHidden(hiddenNums: Set<Int>): List<ThreadSummary> =
    if (hiddenNums.isEmpty()) this else filterNot { it.num in hiddenNums }
