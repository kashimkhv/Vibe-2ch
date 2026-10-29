package dev.dvach.client.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

/**
 * Сколько постов вперёд от последнего видимого догружать превью.
 *
 * Сами превью грузятся только когда ячейка попала в композицию, а при быстром
 * флинге Compose не успевает до неё дойти — картинки остаются серыми, пока
 * палец не остановится. Поэтому на каждый сдвиг списка ставим в очередь
 * картинки следующих [PREFETCH_AHEAD_ITEMS] постов: к моменту, когда они
 * доедут до экрана, они уже в кэше Coil.
 */
const val PREFETCH_AHEAD_ITEMS = 12

/** И немного назад — чтобы прокрутка вверх не мигала серым. */
const val PREFETCH_BEHIND_ITEMS = 3

/**
 * Заранее грузит превью вложений вокруг видимой части списка.
 *
 * @param urlsOf превью поста по индексу элемента списка; пустой список значит,
 *   что у поста нет картинок (или превью выключены в настройках).
 */
@Composable
fun PrefetchThumbnails(
    listState: LazyListState,
    urlsOf: (index: Int) -> List<String>,
    ahead: Int = PREFETCH_AHEAD_ITEMS,
    behind: Int = PREFETCH_BEHIND_ITEMS,
) {
    val context = LocalContext.current
    val imageLoader = remember(context) { SingletonImageLoader.get(context) }
    // Лямбда читает состояние экрана, поэтому её нельзя зашивать в ключи эффекта:
    // берём всегда свежую, а эффект перезапускаем только при смене списка.
    val currentUrlsOf by rememberUpdatedState(urlsOf)
    // Обычный HashSet: читаем его только из корутины эффекта, рекомпозиция не нужна.
    val requested = remember { HashSet<String>() }

    LaunchedEffect(listState, ahead, behind) {
        snapshotFlow {
            val info = listState.layoutInfo
            val first = info.visibleItemsInfo.firstOrNull()?.index
            val last = info.visibleItemsInfo.lastOrNull()?.index
            if (first == null || last == null) null else Triple(first, last, info.totalItemsCount)
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { (first, last, total) ->
                // Вниз — весь диапазон [PREFETCH_AHEAD_ITEMS], вверх — только
                // пара постов: обратный скролл обычно медленнее.
                for (index in prefetchRange(first, last, total, ahead, behind)) {
                    currentUrlsOf(index).forEach { url ->
                        if (url.isBlank() || url in requested) return@forEach
                        requested.add(url)
                        imageLoader.enqueue(
                            ImageRequest.Builder(context)
                                .data(url)
                                .build(),
                        )
                    }
                }
                // Длинный тред из тысяч картинок не должен копить ссылки вечно.
                if (requested.size > MAX_REMEMBERED_URLS) requested.clear()
            }
    }
}

/**
 * Индексы элементов, превью которых надо догрузить, если на экране видно
 * [first]..[last] из [total]. Начало и конец списка не выходят за границы.
 */
internal fun prefetchRange(first: Int, last: Int, total: Int, ahead: Int, behind: Int): IntRange {
    if (total <= 0) return IntRange.EMPTY
    val from = (first - behind).coerceAtLeast(0)
    val until = (last + ahead).coerceAtMost(total - 1)
    return if (until < from) IntRange.EMPTY else from..until
}

/** Порог, после которого список уже загруженных ссылок сбрасывается. */
private const val MAX_REMEMBERED_URLS = 2_000
