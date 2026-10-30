package dev.dvach.client.util

/** Границы настройки «Размер превью», в процентах от базового размера. */
const val THUMBNAIL_SCALE_MIN = 60
const val THUMBNAIL_SCALE_MAX = 200
const val THUMBNAIL_SCALE_STEP = 10

/**
 * Базовая сторона квадратного превью в треде при 100 %. Список тредов берёт
 * от неё половину ([THUMBNAIL_LIST_BASE_SIDE_DP]), поэтому оба экрана всегда
 * остаются в одной пропорции и не расходятся, если поменять размер в треде.
 */
const val THUMBNAIL_BASE_SIDE_DP = 150

/** Размер превью ОП-поста в списке тредов при 100 % — половина тредового. */
const val THUMBNAIL_LIST_BASE_SIDE_DP = THUMBNAIL_BASE_SIDE_DP / 2

/**
 * Процент по умолчанию. Он общий для обоих ползунков: превью в треде 150 dp,
 * в списке 75 dp, после масштабирования — 180 dp и 90 dp.
 */
const val THUMBNAIL_SCALE_DEFAULT = 120

/**
 * Базовый размер превью в dp, умноженный на процент из настроек.
 *
 * Процент зажимается в [THUMBNAIL_SCALE_MIN]..[THUMBNAIL_SCALE_MAX]: значение
 * приходит из DataStore, а туда могло попасть что угодно (старая версия,
 * ручная правка файла настроек). Результат округляется к ближайшему целому.
 */
fun scaleThumbnail(baseDp: Int, percent: Int): Int {
    val safe = percent.coerceIn(THUMBNAIL_SCALE_MIN, THUMBNAIL_SCALE_MAX)
    return (baseDp * safe + 50) / 100
}

/**
 * Сторона превью с учётом ширины карточки списка тредов.
 *
 * При крупном масштабе превью четыре картинки в строку не влезают (4 × 150 dp
 * при 200 %), и FlowRow переносит лишние на вторую строку — карточка с
 * картинками вырастает вдвое, а список от этого «дёргается». Поэтому при
 * нехватке места сторона ужимается ровно до ширины ряда, чтобы все превью
 * остались в одной строке; при обычном масштабе возвращается [sideDp].
 *
 * @param availableDp ширина, в которую должны влезть [visible] превью.
 * @param visible сколько превью показываем в ряду (до четырёх).
 */
fun fitThumbnailSide(sideDp: Int, availableDp: Int, visible: Int, gapDp: Int = 4): Int {
    if (visible <= 0 || availableDp <= 0) return sideDp
    val fit = (availableDp - gapDp * (visible - 1)) / visible
    return if (fit in 1 until sideDp) fit else sideDp
}
