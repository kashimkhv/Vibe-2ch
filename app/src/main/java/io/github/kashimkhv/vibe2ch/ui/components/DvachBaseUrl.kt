package io.github.kashimkhv.vibe2ch.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.kashimkhv.vibe2ch.domain.DEFAULT_DVACH_HOST
import io.github.kashimkhv.vibe2ch.domain.baseUrlOf

/**
 * Домен движка, выбранный в настройках.
 *
 * Нужен ссылкам внутри постов: движок отдаёт относительные пути (`/b/res/1.html`)
 * без хоста, и открывать их надо на выбранном домене, а не всегда на `2ch.hk`.
 * Провайдер один — в `MainActivity`, поэтому настройка не протаскивается через
 * каждый компонент.
 */
val LocalDvachBaseUrl = staticCompositionLocalOf { baseUrlOf(DEFAULT_DVACH_HOST) }
