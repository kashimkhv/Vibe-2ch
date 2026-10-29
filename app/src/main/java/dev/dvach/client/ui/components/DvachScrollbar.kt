package dev.dvach.client.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Ширина области захвата: сама полоса тоньше, но палец должен попадать.
 *
 * 20 dp — это ~5 мм на телефоне: по бегунку шириной 12 dp пальцем уже можно
 * попасть, не глядя, а зона захвата ещё и прощает промах на пару миллиметров.
 * Растёт она вправо, поверх содержимого списка, поэтому больше делать не стоит:
 * шире — и полоса начнёт закрывать текст постов.
 */
internal val SCROLLBAR_WIDTH = 20.dp

/** Полоса-дорожка: тонкая линия у самого края экрана. */
private val TRACK_WIDTH = 4.dp

/** Бегунок — заметно толще дорожки, чтобы его было видно и удобно тянуть. */
private val THUMB_WIDTH = 12.dp

/**
 * Минимальная высота бегунка: на длинном треде он иначе стал бы точкой,
 * а по точке в 44 dp пальцем не попасть — отсюда 56 dp.
 */
private val MIN_THUMB_HEIGHT = 56.dp

/**
 * Полоса прокрутки справа: показывает, где пользователь находится в списке,
 * и позволяет тянуть себя пальцем — как в старых приложениях.
 *
 * В Compose готового скроллбара нет, поэтому он нарисован сам: `Canvas` рисует
 * дорожку и бегунок, а жест ловит `pointerInput`.
 *
 * Позиция считается в пикселях, а не по индексу поста: у `LazyColumn` нет
 * общего размера содержимого, зато средняя высота видимых элементов даёт
 * достаточную оценку. Иначе бегунок прыгал бы по постам и не совпадал с тем,
 * насколько список реально прокручен.
 */
@Composable
fun DvachScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
) {
    val layout = state.layoutInfo
    val total = layout.totalItemsCount
    val visible = layout.visibleItemsInfo
    // Список короче экрана — прокручивать нечего, полоса только мешала бы.
    if (total == 0 || visible.isEmpty()) return

    val averageSize = visible.sumOf { it.size }.toFloat() / visible.size
    val viewport = (layout.viewportEndOffset - layout.viewportStartOffset).toFloat()
    val content = averageSize * total
    val travel = content - viewport
    if (averageSize <= 0f || travel <= 0f) return

    val scrolled = (state.firstVisibleItemIndex * averageSize + state.firstVisibleItemScrollOffset)
        .coerceIn(0f, travel)
    val scope = rememberCoroutineScope()
    var seekJob by remember { mutableStateOf<Job?>(null) }

    ScrollbarRail(
        progress = scrolled / travel,
        viewportFraction = viewport / content,
        modifier = modifier,
        onSeek = { fraction ->
            val seek = scrollSeek(fraction, travel, averageSize, total)
            seekJob?.cancel()
            seekJob = scope.launch {
                state.scrollToItem(seek.index, seek.offset)
            }
        },
    )
}

/** Куда прокрутить список: индекс элемента и смещение внутри него. */
internal data class ScrollSeek(val index: Int, val offset: Int)

/**
 * Доля прокрутки (0 — начало, 1 — конец) → позиция в списке.
 *
 * [travel] — насколько прокручивается содержимое, [averageSize] — средняя
 * высота элемента, [count] — сколько элементов всего. Смещение считается от
 * неокруглённого индекса, поэтому бегунок едет плавно, а не прыгает по постам.
 */
internal fun scrollSeek(fraction: Float, travel: Float, averageSize: Float, count: Int): ScrollSeek {
    if (count <= 0 || averageSize <= 0f || travel <= 0f) return ScrollSeek(0, 0)
    val target = (fraction.coerceIn(0f, 1f) * travel).coerceAtLeast(0f)
    val index = (target / averageSize).roundToInt().coerceIn(0, count - 1)
    val offset = (target - index * averageSize).roundToInt().coerceIn(0, averageSize.toInt())
    return ScrollSeek(index, offset)
}

/**
 * Вариант для обычной прокрутки (`Column` + `verticalScroll`), например
 * в настройках: там нет списка постов, но полоса нужна такая же.
 */
@Composable
fun DvachScrollbar(
    state: ScrollState,
    modifier: Modifier = Modifier,
) {
    val max = state.maxValue
    val viewport = state.viewportSize
    if (max <= 0 || viewport <= 0) return

    val progress = (state.value.toFloat() / max).coerceIn(0f, 1f)
    val viewportFraction = viewport.toFloat() / (viewport + max)
    val scope = rememberCoroutineScope()
    var seekJob by remember { mutableStateOf<Job?>(null) }

    ScrollbarRail(
        progress = progress,
        viewportFraction = viewportFraction,
        modifier = modifier,
        onSeek = { fraction ->
            seekJob?.cancel()
            seekJob = scope.launch { state.scrollTo((fraction * max).roundToInt()) }
        },
    )
}

/**
 * Дорожка и бегунок. Тап и перетаскивание считаются одинаково: позиция пальца
 * переводится в долю прокрутки, а что с ней делать, решает [onSeek].
 */
@Composable
private fun ScrollbarRail(
    progress: Float,
    viewportFraction: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier,
) {
    val density = LocalDensity.current
    var heightPx by remember { mutableFloatStateOf(0f) }

    val trackColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    val thumbColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)

    val minThumbPx = with(density) { MIN_THUMB_HEIGHT.toPx() }
    val thumbPx = (heightPx * viewportFraction)
        .coerceIn(minThumbPx.coerceAtMost(heightPx), heightPx.coerceAtLeast(1f))
    val travel = (heightPx - thumbPx).coerceAtLeast(1f)
    val thumbTop = progress * travel

    // Жест не пересоздаётся при прокрутке, поэтому положение бегунка читаем
    // через состояние: иначе в замыкании остался бы старый кадр.
    val currentThumbTop by rememberUpdatedState(thumbTop)

    Box(
        modifier = modifier
            .width(SCROLLBAR_WIDTH)
            .onSizeChanged { heightPx = it.height.toFloat() }
            .pointerInput(travel, thumbPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // Забираем жест себе: иначе список начнёт прокручиваться
                    // под полосой, и бегунок «убежит» от пальца.
                    down.consume()
                    // Взяли сам бегунок — тянем его как есть, без рывка центром
                    // под палец. Ткнули в дорожку — бегунок прыгает под палец.
                    val grab = down.position.y - currentThumbTop
                    val offset = if (grab in 0f..thumbPx) grab - thumbPx / 2f else 0f
                    onSeek(seekFraction(down.position.y - offset, thumbPx, travel))
                    drag(down.id) { change ->
                        change.consume()
                        onSeek(seekFraction(change.position.y - offset, thumbPx, travel))
                    }
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxHeight().width(SCROLLBAR_WIDTH)) {
            val trackWidth = TRACK_WIDTH.toPx()
            val thumbWidth = THUMB_WIDTH.toPx()
            // Дорожка прижата к правому краю, бегунок — тоже, но толще.
            drawRect(
                color = trackColor,
                topLeft = Offset(size.width - trackWidth, 0f),
                size = Size(trackWidth, size.height),
            )
            drawRect(
                color = thumbColor,
                topLeft = Offset(size.width - thumbWidth, thumbTop),
                size = Size(thumbWidth, thumbPx),
            )
        }
    }
}

/** Позиция пальца → доля прокрутки; центр бегунка идёт за пальцем. */
private fun seekFraction(y: Float, thumbPx: Float, travel: Float): Float =
    ((y - thumbPx / 2f) / travel).coerceIn(0f, 1f)
