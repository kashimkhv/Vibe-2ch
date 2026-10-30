package dev.dvach.client.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.floor
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
 * Позиция считается **по индексам, а не в пикселях**. Раньше было наоборот:
 * средняя высота видимых элементов давала оценку всего содержимого, а палец
 * переводился в «пиксели» и округлялся до поста. В треде посты разной высоты —
 * картинка на пол-экрана, простыня текста, «Да.» в одну строку, — поэтому
 * оценка всё время врала: бегунок под пальцем уезжал на другой пост, и
 * протяжка читалась как «привязка к границам». В индексном пространстве
 * позиция и заказ на прокрутку означают одно и то же число, так что бегунок
 * идёт за пальцем один в один.
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
    if (total <= 1 || visible.isEmpty()) return
    if (!state.canScrollBackward && !state.canScrollForward) return

    val viewport = (layout.viewportEndOffset - layout.viewportStartOffset).toFloat()
    // Длина бегунка — доля содержимого, которая влезла на экран. У `LazyColumn`
    // нет высоты всего содержимого, поэтому она оценивается по средней высоте
    // видимых постов: цифра приблизительная и гуляет на десятки процентов от
    // экрана к экрану, поэтому её ещё и сглаживаем (см. ScrollbarRail). Точнее
    // не сделать, а на длинном треде всё равно перекроет MIN_THUMB_HEIGHT.
    val averageSize = visible.sumOf { it.size }.toFloat() / visible.size
    val viewportFraction = if (averageSize > 0f) {
        (viewport / (averageSize * total)).coerceIn(0f, 1f)
    } else {
        1f
    }

    // Для смещения внутри поста берём самый низкий видимый: если взять
    // средний, у коротких постов смещение перескакивает через весь пост и
    // список уезжает на пост-другой дальше, чем просил палец.
    val live = LazyMapping(total, visible.size, visible.minOf { it.size }.toFloat())
    // Шкалу фиксируем на время протяжки: число видимых постов гуляет на
    // единицу, а вместе с ним менялся бы весь пересчёт — бегунок уезжал бы
    // из-под пальца. По той же шкале считаем и позицию, иначе после
    // отпускания она не совпала бы с тем, что просил палец.
    var frozen by remember { mutableStateOf<LazyMapping?>(null) }
    val mapping = frozen ?: live
    val scope = rememberCoroutineScope()

    val progress = scrollProgress(
        index = state.firstVisibleItemIndex,
        offset = state.firstVisibleItemScrollOffset,
        itemSize = visible.first().size.toFloat(),
        count = mapping.count,
        visibleCount = mapping.visibleCount,
    )

    ScrollbarRail(
        progress = progress,
        viewportFraction = viewportFraction,
        modifier = modifier,
        onSeek = { fraction ->
            val seek = scrollSeek(fraction, mapping.count, mapping.visibleCount, mapping.itemSize)
            // requestScrollToItem, а не scrollToItem: он не запускает корутину
            // на каждое движение пальца и не отменяет предыдущую на полпути —
            // от этого протяжка тоже рвалась.
            state.requestScrollToItem(seek.index, seek.offset)
        },
        onDragStart = {
            frozen = live
            // Взяли полосу во время флинга — останавливаем его, иначе список
            // продолжит ехать под бегунком.
            scope.launch { state.scroll { } }
        },
        onDragEnd = { frozen = null },
    )
}

/** Что нужно, чтобы переводить долю дорожки в позицию списка. */
private data class LazyMapping(val count: Int, val visibleCount: Int, val itemSize: Float)

/** Куда прокрутить список: индекс элемента и смещение внутри него. */
internal data class ScrollSeek(val index: Int, val offset: Int)

/**
 * Позиция в списке → доля дорожки (0 — начало, 1 — конец).
 *
 * [index] и [offset] — то, что отдаёт `LazyListState` (первый видимый элемент
 * и прокрутка внутри него), [itemSize] — высота этого элемента. Низ списка —
 * не последний элемент, а тот, с которого начинается последний экран:
 * `count - visibleCount`, иначе бегунок не доезжал бы до края.
 */
internal fun scrollProgress(
    index: Int,
    offset: Int,
    itemSize: Float,
    count: Int,
    visibleCount: Int,
): Float {
    if (count <= 1) return 0f
    val max = maxPosition(count, visibleCount)
    val inside = if (itemSize > 0f) (offset / itemSize).coerceIn(0f, 1f) else 0f
    return ((index + inside) / max).coerceIn(0f, 1f)
}

/**
 * Доля дорожки → позиция в списке.
 *
 * Дробная часть доли переводится в смещение внутри поста, поэтому бегунок
 * едет плавно и внутри одного длинного поста, а не прыгает по постам.
 */
internal fun scrollSeek(
    fraction: Float,
    count: Int,
    visibleCount: Int,
    itemSize: Float,
): ScrollSeek {
    if (count <= 0) return ScrollSeek(0, 0)
    val target = fraction.coerceIn(0f, 1f) * maxPosition(count, visibleCount)
    val index = floor(target).toInt().coerceIn(0, count - 1)
    val inside = target - index
    if (itemSize <= 0f) return ScrollSeek(index, 0)
    // Смещение строго внутри элемента: ровно высота — это уже следующий пост.
    val limit = (itemSize - 1f).toInt().coerceIn(0, Int.MAX_VALUE)
    val offset = (inside * itemSize).roundToInt().coerceIn(0, limit)
    return ScrollSeek(index, offset)
}

/** Наибольшая позиция, с которой список ещё прокручивается. */
private fun maxPosition(count: Int, visibleCount: Int): Float =
    (count - visibleCount).coerceAtLeast(1).toFloat()

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

    ScrollbarRail(
        progress = progress,
        viewportFraction = viewportFraction,
        modifier = modifier,
        onSeek = { fraction ->
            // Тут смещение точное — пиксели известны, — поэтому и промахов нет.
            scope.launch { state.scrollTo((fraction * max).roundToInt()) }
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
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
) {
    val density = LocalDensity.current
    var heightPx by remember { mutableFloatStateOf(0f) }
    // Высота бегунка на время протяжки: она зависит от оценки содержимого,
    // а та меняется по ходу прокрутки — без фиксации бегунок «дышал» бы
    // под пальцем.
    var dragThumbPx by remember { mutableStateOf<Float?>(null) }
    // Куда палец поставил бегунок. Пока его тянут, рисуем по пальцу, а не по
    // позиции списка: список подтягивается на кадр позже, и в треде, где посты
    // разной высоты, «доля дорожки» и «доля содержимого» расходятся на
    // несколько процентов — бегунок отставал бы от пальца и подрагивал на
    // границах постов.
    var dragFraction by remember { mutableStateOf<Float?>(null) }

    val trackColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    val thumbColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)

    val minThumbPx = with(density) { MIN_THUMB_HEIGHT.toPx() }
    // Длину бегунка тоже сглаживаем: она считается по средней высоте видимых
    // постов, а та скачет на каждом новом экране — «дышащий» бегунок выглядит
    // как поломка.
    val shownViewportFraction by animateFloatAsState(
        targetValue = viewportFraction,
        animationSpec = tween(durationMillis = 180),
        label = "scrollbarThumbSize",
    )
    val thumbPx = dragThumbPx ?: (heightPx * shownViewportFraction)
        .coerceIn(minThumbPx.coerceAtMost(heightPx), heightPx.coerceAtLeast(1f))
    val travel = (heightPx - thumbPx).coerceAtLeast(1f)
    val thumbTop = progress * travel
    // Пока тянут — бегунок ровно под пальцем, без сглаживания. После
    // отпускания он за сотню миллисекунд доезжает до настоящей позиции
    // списка: она может отличаться на долю поста (посты разной высоты),
    // и рывок на этом месте выглядел бы поломкой.
    val liveFraction = dragFraction ?: progress
    val shownFraction by animateFloatAsState(
        targetValue = liveFraction,
        animationSpec = if (dragFraction != null) snap() else tween(durationMillis = 120),
        label = "scrollbarThumb",
    )
    val shownTop = shownFraction * travel

    // Жест не пересоздаётся при прокрутке, поэтому положение бегунка, его
    // высоту и колбэки читаем через состояние: иначе в замыкании остался бы
    // старый кадр.
    // Именно нарисованное положение: при касании во время «доезда» палец
    // должен схватить бегунок там, где его видно.
    val currentThumbTop by rememberUpdatedState(shownTop)
    val currentThumb by rememberUpdatedState(thumbPx)
    val currentSeek by rememberUpdatedState(onSeek)
    val currentDragStart by rememberUpdatedState(onDragStart)
    val currentDragEnd by rememberUpdatedState(onDragEnd)

    Box(
        modifier = modifier
            .width(SCROLLBAR_WIDTH)
            .onSizeChanged { heightPx = it.height.toFloat() }
            // Ключ — Unit: раньше здесь были высота бегунка и длина дорожки,
            // они менялись прямо во время протяжки, `pointerInput` перезапускался
            // и жест обрывался — та самая «привязка к границам».
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // Забираем жест себе: иначе список начнёт прокручиваться
                    // под полосой, и бегунок «убежит» от пальца.
                    down.consume()
                    val thumb = currentThumb
                    val railTravel = (size.height - thumb).coerceAtLeast(1f)
                    // Высоту бегунка фиксируем на всё время жеста.
                    dragThumbPx = thumb
                    currentDragStart()
                    // Взяли сам бегунок — тянем его как есть, без рывка центром
                    // под палец. Ткнули в дорожку — бегунок прыгает под палец.
                    val grab = down.position.y - currentThumbTop
                    val offset = if (grab in 0f..thumb) grab - thumb / 2f else 0f
                    try {
                        currentSeek(seekFraction(down.position.y - offset, thumb, railTravel))
                        dragFraction = seekFraction(down.position.y - offset, thumb, railTravel)
                        drag(down.id) { change ->
                            change.consume()
                            val f = seekFraction(change.position.y - offset, thumb, railTravel)
                            dragFraction = f
                            currentSeek(f)
                        }
                    } finally {
                        currentDragEnd()
                        dragThumbPx = null
                        dragFraction = null
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
                topLeft = Offset(size.width - thumbWidth, shownTop),
                size = Size(thumbWidth, thumbPx),
            )
        }
    }
}

/** Позиция пальца → доля прокрутки; центр бегунка идёт за пальцем. */
private fun seekFraction(y: Float, thumbPx: Float, travel: Float): Float =
    ((y - thumbPx / 2f) / travel).coerceIn(0f, 1f)
