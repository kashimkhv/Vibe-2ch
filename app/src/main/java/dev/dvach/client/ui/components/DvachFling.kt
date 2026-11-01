package dev.dvach.client.ui.components

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.math.abs

/**
 * Добавка к скорости флинга в самом быстром свайпе: 0.6 значит «плюс 60 %».
 *
 * Почему вообще добавка. Стандартный флинг Compose (`splineBasedDecay`) тормозит
 * быстро: при свайпе «на 700 px за 250 мс» список после отпускания проезжает
 * чуть больше 500 px, и короткий резкий жест заканчивается почти сразу. Список
 * с постами это переживает, а тред — нет: чтобы доехать до нужного места, рука
 * делает рывок за рывком. Усиление скорости растягивает тот же жест, но не
 * меняет его физику: торможение всё равно плавное, просто запас хода больше.
 *
 * Значение — компромисс. Сначала стояло 1.2 (+120 %), но тогда один рывок
 * проезжал слишком много: докатка вырастала с 510 до 1200 px. Теперь усиление
 * вдвое слабее, и ход растёт примерно в 1,7 раза: короткий резкий свайп
 * заметно докатывается, но не улетает через полтреда.
 */
internal const val FLING_BOOST = 0.6f

/**
 * Ниже этой скорости усиления нет совсем (px/s).
 *
 * Медленное движение и отпускание — это не бросок, а «дотянул и положил
 * палец». Если усиливать и его, список начнёт уезжать от лёгкого толчка.
 * 350 px/s — это около 130 dp/s: медленнее обычной прокрутки пальцем.
 */
internal const val FLING_BOOST_MIN_SPEED = 350f

/**
 * На этой скорости усиление уже полное (px/s).
 *
 * Ограничение сверху нужно, чтобы усиление не росло бесконечно: чем резче
 * жест, тем короче сам путь пальца, и без потолка «чирк» по экрану улетал бы
 * на весь тред. 4500 px/s при плотности 2.75 — это ~1600 dp/s, быстрее так
 * уже не махнуть.
 */
internal const val FLING_BOOST_FULL_SPEED = 4500f

/**
 * Потолок итоговой скорости (px/s). Страховка на случай очень резкого рывка:
 * даже с усилением список не должен улетать на несколько экранов в один кадр.
 */
internal const val FLING_MAX_SPEED = 18000f

/**
 * Скорость флинга с поправкой на резкость жеста.
 *
 * Направление сохраняется, ноль остаётся нулём, медленное движение не
 * трогаем. Дальше усиление растёт линейно от [FLING_BOOST_MIN_SPEED] до
 * [FLING_BOOST_FULL_SPEED] и упирается в [FLING_MAX_SPEED] — то есть «сила
 * инерции» зависит от скорости свайпа, но предсказуемо и с потолком.
 */
internal fun boostedFlingVelocity(velocity: Float): Float {
    val speed = abs(velocity)
    if (speed <= FLING_BOOST_MIN_SPEED) return velocity
    val ramp = ((speed - FLING_BOOST_MIN_SPEED) /
        (FLING_BOOST_FULL_SPEED - FLING_BOOST_MIN_SPEED)).coerceIn(0f, 1f)
    val boosted = velocity * (1f + FLING_BOOST * ramp)
    return boosted.coerceIn(-FLING_MAX_SPEED, FLING_MAX_SPEED)
}

/**
 * Флинг с усилением: оборачивает платформенный `splineBasedDecay`, подменяя
 * только начальную скорость. Всё остальное — кривая торможения, порог
 * остановки, работа с краями списка — остаётся штатным.
 *
 * Подключается к спискам и `verticalScroll` (треды, посты, доски, закладки,
 * настройки). В галерее не используется: там листают по одному изображению,
 * и «доезд» после свайпа только мешает.
 */
@Composable
fun rememberDvachFlingBehavior(): FlingBehavior {
    val base = ScrollableDefaults.flingBehavior()
    return remember(base) { BoostedFlingBehavior(base) }
}

/**
 * Делегат: [performFling] у [FlingBehavior] — член-расширение на `ScrollScope`,
 * поэтому вызывать базовый нужно, передав ему и приёмник-`ScrollScope`.
 */
private class BoostedFlingBehavior(private val base: FlingBehavior) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        val scroll: ScrollScope = this
        return with(base) { scroll.performFling(boostedFlingVelocity(initialVelocity)) }
    }
}
