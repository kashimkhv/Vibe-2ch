package io.github.kashimkhv.vibe2ch.data.cache

import android.content.Context
import coil3.SingletonImageLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Сколько места занимают кэши приложения и как их очистить.
 *
 * Кэшей два, и оба растут сами: картинки (Coil — превью в тредах и полные
 * файлы в просмотрщике) и сеть (дисковый кэш OkHttp — JSON-ответы API и
 * миниатюры, которые Coil взял не из своего кэша). Настройки показывают оба
 * числа, потому что «очистить кэш» без них — кнопка без результата.
 */
@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient,
) {

    /** Текущее состояние кэшей; лимиты — те, что заданы в `NetworkModule`/Coil. */
    suspend fun usage(): CacheUsage = withContext(Dispatchers.IO) {
        val imageLoader = SingletonImageLoader.get(context)
        CacheUsage(
            imagesBytes = imageLoader.diskCache?.size ?: 0L,
            imagesLimitBytes = imageLoader.diskCache?.maxSize ?: 0L,
            networkBytes = httpClient.cache?.size() ?: 0L,
            networkLimitBytes = httpClient.cache?.maxSize() ?: 0L,
        )
    }

    /**
     * Сбрасывает оба кэша и память картинок.
     *
     * Дисковый кэш Coil чистится целиком, а не по файлам: в нём лежат только
     * картинки, заново их скачать — секунды. Память чистим заодно, иначе
     * «очищено» не совпало бы с тем, что видно на экране: картинки остались бы
     * в готовых битмапах.
     */
    suspend fun clear() = withContext(Dispatchers.IO) {
        val imageLoader = SingletonImageLoader.get(context)
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
        httpClient.cache?.evictAll()
    }
}

/** Занято и предел для каждого кэша, в байтах. */
data class CacheUsage(
    val imagesBytes: Long = 0L,
    val imagesLimitBytes: Long = 0L,
    val networkBytes: Long = 0L,
    val networkLimitBytes: Long = 0L,
) {
    val totalBytes: Long get() = imagesBytes + networkBytes

    val isEmpty: Boolean get() = totalBytes <= 0L
}
