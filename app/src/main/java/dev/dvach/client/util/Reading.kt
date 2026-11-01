package dev.dvach.client.util

/**
 * Межстрочный интервал текста постов, в процентах от обычного.
 *
 * Это не кегль: размер строки задаёт ползунок размера шрифта, а здесь меняется
 * только расстояние между строками. 80 % — плотная «газетная» вёрстка, 160 % —
 * просторно для длинных постов.
 */
const val LINE_SPACING_DEFAULT = 100
const val LINE_SPACING_MIN = 80
const val LINE_SPACING_MAX = 160
const val LINE_SPACING_STEP = 10

/** В DataStore могло попасть что угодно — зажимаем в рабочий диапазон. */
fun clampLineSpacing(percent: Int): Int =
    percent.coerceIn(LINE_SPACING_MIN, LINE_SPACING_MAX)

/**
 * Поля текста: дополнительный отступ слева и справа от текста постов, в dp.
 * Превью вложений при этом остаются во всю ширину — картинки не должны
 * мельчать из-за настройки чтения.
 */
const val TEXT_MARGIN_DEFAULT = 0
const val TEXT_MARGIN_MIN = 0
const val TEXT_MARGIN_MAX = 32
const val TEXT_MARGIN_STEP = 4

fun clampTextMargin(dp: Int): Int = dp.coerceIn(TEXT_MARGIN_MIN, TEXT_MARGIN_MAX)
