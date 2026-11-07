package io.github.kashimkhv.vibe2ch.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay

/** Минута — шаг относительных подписей: точнее показывать нечего. */
private const val TICK_MILLIS = 60_000L

/**
 * Текущее время для относительных дат («5 мин назад»). Обновляется раз
 * в минуту, иначе подпись застывала бы на моменте открытия экрана.
 *
 * Тикер один на экран: карточки списка и посты получают готовое значение
 * параметром и свой таймер не заводят — иначе на каждую видимую карточку
 * приходилась бы своя корутина. При выключенных относительных датах тикера
 * нет вовсе: возвращается 0, а форматтер это значение не использует.
 */
@Composable
fun rememberWallClock(enabled: Boolean): Long {
    if (!enabled) return 0L
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(TICK_MILLIS)
            // Считаем от системных часов каждый раз, а не прибавляем минуту:
            // телефон мог уснуть, и накопился бы отставание.
            value = System.currentTimeMillis()
        }
    }
    return now
}
