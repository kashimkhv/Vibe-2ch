package io.github.kashimkhv.vibe2ch.util

/** Размер шрифта по умолчанию — 100 %, то есть обычный размер стиля. */
const val FONT_SCALE_DEFAULT = 100

/** Ниже 75 % подписи уже не читаются, выше 150 % шапки начинают обрезаться. */
const val FONT_SCALE_MIN = 75
const val FONT_SCALE_MAX = 150
const val FONT_SCALE_STEP = 5

/** В DataStore могло попасть что угодно — зажимаем в рабочий диапазон. */
fun clampFontScale(percent: Int): Int = percent.coerceIn(FONT_SCALE_MIN, FONT_SCALE_MAX)
