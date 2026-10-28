package dev.dvach.client.util

/** Границы настройки «Размер превью», в процентах от базового размера. */
const val THUMBNAIL_SCALE_MIN = 60
const val THUMBNAIL_SCALE_MAX = 200
const val THUMBNAIL_SCALE_STEP = 10

/**
 * По умолчанию превью чуть крупнее исходных размеров: на телефоне 76 dp
 * в списке тредов и 260 dp в треде читались мелковато.
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
