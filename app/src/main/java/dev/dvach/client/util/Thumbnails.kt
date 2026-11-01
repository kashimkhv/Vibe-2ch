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
 * «Мало вложений» — один или два файла. Такой ОП-пост занимает один ряд из
 * одного-двух превью, и справа остаётся пустое место.
 */
const val THUMBNAIL_FEW_FILES_LIMIT = 3

/** Во сколько раз крупнее превью, когда вложений меньше [THUMBNAIL_FEW_FILES_LIMIT]. */
const val THUMBNAIL_FEW_FILES_MULTIPLIER = 2

/**
 * Сторона превью ОП-поста в списке тредов.
 *
 * Сначала базовый размер умножается на процент из настроек, и только потом —
 * на множитель для карточек с малым числом вложений: иначе «вдвое крупнее»
 * зависело бы от ползунка и на 200 % давало бы совсем другой шаг.
 *
 * @param fileCount сколько вложений у ОП-поста.
 * @param enlargeFew включена ли настройка «Крупные превью, если вложений мало».
 */
fun listThumbnailSide(
    baseDp: Int,
    percent: Int,
    fileCount: Int,
    enlargeFew: Boolean,
): Int {
    val side = scaleThumbnail(baseDp, percent)
    return if (enlargeFew && fileCount < THUMBNAIL_FEW_FILES_LIMIT) {
        side * THUMBNAIL_FEW_FILES_MULTIPLIER
    } else {
        side
    }
}

/**
 * Промежуток между превью в ряду списка тредов.
 *
 * Сторона ячейки в ленте одна на все карточки — иначе настройка «размер превью
 * в списке» срабатывала бы по-разному: у треда с четырьмя картинками ячейка
 * ужималась бы, а у треда с одной оставалась прежней. Поэтому при нехватке
 * места ужимается не превью, а промежуток: четыре ячейки по 90 dp при 120 %
 * занимают 372 dp, а в карточку влезает 368 — зазор 2 dp вместо 4 dp
 * выравнивает ряд. Если не помогает и это (крупный масштаб), превью
 * переносятся на вторую строку, но остаются одного размера — как задано
 * в настройках.
 *
 * @param availableDp ширина, в которую должны влезть [visible] превью.
 * @param maxGapDp обычный промежуток, если места хватает.
 */
fun thumbnailRowGap(
    sideDp: Int,
    availableDp: Int,
    visible: Int,
    maxGapDp: Int = 4,
): Int {
    if (visible <= 1 || availableDp <= 0) return maxGapDp
    val fit = (availableDp - sideDp * visible) / (visible - 1)
    return if (fit in 0 until maxGapDp) fit else maxGapDp
}
