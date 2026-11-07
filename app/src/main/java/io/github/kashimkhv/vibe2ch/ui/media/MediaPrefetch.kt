package io.github.kashimkhv.vibe2ch.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import io.github.kashimkhv.vibe2ch.domain.model.Attachment
import kotlin.math.abs

/**
 * Сколько вложений прогревать вперёд от текущего.
 *
 * Пейджер показывает только одну страницу, соседние не компонуются, поэтому
 * картинка на них начинает грузиться лишь в момент, когда страница доехала до
 * экрана: при быстром листании это видно как «крутилка» на каждой.
 *
 * Два, а не больше: файлы в галерее бывают по несколько мегабайт, и запас
 * аккуратный — трафик важнее, чем идеальная плавность на длинном флингe.
 */
const val MEDIA_PREFETCH_AHEAD = 2

/** И столько же назад: к предыдущей картинке возвращаются так же часто. */
const val MEDIA_PREFETCH_BEHIND = 2

/**
 * Индексы вложений, которые надо прогреть, в порядке удалённости от текущего.
 *
 * Ближайшие соседи нужны раньше всех, поэтому и запрашиваются первыми: очередь
 * Coil не бесконечна, и при быстром листании важно, чтобы впереди стоял тот, до
 * которого пользователь доедет следующим.
 */
internal fun mediaPrefetchOrder(page: Int, total: Int, ahead: Int, behind: Int): List<Int> {
    if (total <= 0) return emptyList()
    val from = (page - behind).coerceAtLeast(0)
    val until = (page + ahead).coerceAtMost(total - 1)
    if (until < from) return emptyList()
    return (from..until)
        .filter { it != page }
        .sortedWith(compareBy({ abs(it - page) }, { it }))
}

/**
 * Прогревает картинки соседних страниц просмотрщика — в память и на диск Coil.
 *
 * Размер запроса совпадает с тем, что попросит страница пейджера (весь экран):
 * ключ памяти у Coil — это URL, а размер проверяется при выдаче, поэтому
 * прогрев «не тем» размером дал бы только декодирование заново.
 */
@Composable
fun PrefetchMedia(
    items: List<Attachment>,
    page: Int,
    sizePx: IntSize,
    ahead: Int = MEDIA_PREFETCH_AHEAD,
    behind: Int = MEDIA_PREFETCH_BEHIND,
) {
    if (sizePx.width <= 0 || sizePx.height <= 0) return
    val context = LocalContext.current
    val imageLoader = remember(context) { SingletonImageLoader.get(context) }
    val currentItems by rememberUpdatedState(items)
    // Ссылки, уже поставленные в очередь: при листании туда-сюда не дёргаем
    // загрузчик повторно, он и так отдаст готовое из памяти.
    val requested = remember(items) { HashSet<String>() }

    LaunchedEffect(items, page, sizePx, ahead, behind) {
        for (index in mediaPrefetchOrder(page, currentItems.size, ahead, behind)) {
            val item = currentItems[index]
            // У видео грузим только превью: сам файл отдаёт плеер, и тянуть его
            // целиком в память картинок нельзя.
            val url = if (item.isVideo) item.thumbnailUrl else item.url
            if (url.isBlank() || !requested.add(url)) continue
            imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(url)
                    .size(sizePx.width, sizePx.height)
                    .build(),
            )
        }
        if (requested.size > MAX_REMEMBERED_URLS) requested.clear()
    }
}

/** Порог, после которого набор прогретых ссылок сбрасывается. */
private const val MAX_REMEMBERED_URLS = 500
