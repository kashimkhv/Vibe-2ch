package dev.dvach.client.di

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.gif.AnimatedImageDecoder
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import okio.Buffer
import retrofit2.Retrofit
import okio.Path.Companion.toOkioPath
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import dev.dvach.client.BuildConfig
import dev.dvach.client.data.remote.CookieStore
import dev.dvach.client.data.remote.DvachApi
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

const val BASE_URL = "https://2ch.hk/"

/**
 * 2ch.hk за Cloudflare режет запросы без внятного User-Agent, поэтому подставляем
 * UA десктопного браузера — как это делал старый клиент.
 */
private const val USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/131.0.0.0 Mobile Safari/537.36"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideCookieStore(@ApplicationContext context: Context): CookieStore =
        CookieStore(File(context.filesDir, "cookies.txt"))

    @Provides
    @Singleton
    fun provideOkHttp(
        @ApplicationContext context: Context,
        cookies: CookieStore,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .cache(Cache(File(context.cacheDir, "http_cache"), 64L * 1024 * 1024))
            // Куки движка переживают перезапуск: по ним открываются закрытые
            // доски, а без них каждый запрос был бы новым анонимом.
            .cookieJar(cookies)
            // Превью грузятся пачкой по всему экрану: пять соединений на хост
            // (умолчание OkHttp) — узкое место при быстром скролле.
            .dispatcher(
                Dispatcher().apply {
                    maxRequests = 32
                    maxRequestsPerHost = 16
                },
            )
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "application/json, text/plain, */*")
                        .build(),
                )
            }
            .addInterceptor(logging)
            .addInterceptor(PostingLoggingInterceptor())
            .addInterceptor(CookieLoggingInterceptor())
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Тот же UA (Cloudflare), но без общего callTimeout: видео и скачивания
     * идут потоком, а не целиком в память. Дисковый кэш видео живёт не в
     * OkHttp, а в ExoPlayer — см. [provideVideoCache].
     */
    @Provides
    @Singleton
    @MediaClient
    fun provideMediaOkHttp(cookies: CookieStore): OkHttpClient = OkHttpClient.Builder()
        // Картинки и видео закрытых досок тоже требуют ту же куку, поэтому
        // у медиа-клиента своё хранилище не заводим — оно общее.
        .cookieJar(cookies)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "*/*")
                    .build(),
            )
        }
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            },
        )
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideApi(retrofit: Retrofit): DvachApi = retrofit.create(DvachApi::class.java)

    /**
     * Дисковый кэш видеофайлов. Без него ExoPlayer каждый раз тянет файл
     * заново: свайпнул на соседнее видео и вернулся — недавно просмотренное
     * грузится ещё секунду-две. Кэш лежит в `cacheDir`, поэтому система может
     * вычистить его сама, и ограничен LRU — видео не растут бесконечно.
     */
    @Provides
    @Singleton
    fun provideVideoCache(@ApplicationContext context: Context): SimpleCache =
        SimpleCache(
            File(context.cacheDir, "video-cache"),
            LeastRecentlyUsedCacheEvictor(VIDEO_CACHE_BYTES),
            StandaloneDatabaseProvider(context),
        )
}

/**
 * Предел дискового кэша видео. Тот же порядок, что у картинок: ролики с /b/
 * весят единицы мегабайт, этого хватает на несколько десятков просмотренных.
 */
private const val VIDEO_CACHE_BYTES = 256L * 1024 * 1024

/**
 * Предел дискового кэша картинок. Задаём явно, а не «сколько даст система»:
 * размер видно в настройках, и он не скачет от свободного места на диске.
 */
private const val IMAGE_DISK_CACHE_BYTES = 256L * 1024 * 1024

/**
 * Ответ `/user/posting` в лог. Body-логирование целиком не включаем: оно
 * печатает base64 капчи и картинки постов (десятки килобайт), а на отказе
 * движка (`result: 0`) тело — единственное, что объясняет причину.
 */
private class PostingLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (BuildConfig.DEBUG && request.url.encodedPath.endsWith("/user/posting")) {
            Log.i("Posting", "→ ${request.url} ${(request.body as? MultipartBody)?.describe().orEmpty()}")
        }
        val response = chain.proceed(request)
        if (BuildConfig.DEBUG && request.url.encodedPath.endsWith("/user/posting")) {
            val body = runCatching { response.peekBody(16L * 1024).string() }.getOrDefault("<нет тела>")
            Log.i("Posting", "← ${response.code}: $body")
        }
        return response
    }
}

/**
 * Движок ставит куки редко (доступ к закрытой доске, `cf_clearance`), поэтому
 * пишем только когда он это сделал — и только имена: значения это по сути
 * пароли, им не место в логе.
 */
private class CookieLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (BuildConfig.DEBUG) {
            val setCookies = response.headers("Set-Cookie")
            if (setCookies.isNotEmpty()) {
                Log.i(
                    "Cookies",
                    "← ${chain.request().url.encodedPath} поставил ${setCookies.size}: " +
                        setCookies.joinToString { it.substringBefore('=') },
                )
            }
        }
        return response
    }
}

/** Поля формы без содержимого: короткие значения печатаем, длинные — только размер. */
private fun MultipartBody.describe(): String = parts.joinToString(", ") { part ->
    val name = part.headers
        ?.get("Content-Disposition")
        ?.substringAfter("name=\"", "")
        ?.substringBefore("\"")
        .orEmpty()
    val length = part.body.contentLength()
    val value = if (length in 0..160) {
        val buffer = Buffer()
        part.body.writeTo(buffer)
        "=" + buffer.readUtf8()
    } else {
        ""
    }
    "$name$value($length Б)"
}

/**
 * Общая сборка Coil-загрузчика: картинки ходят через тот же OkHttp (общий кэш и UA),
 * анимированные гифки декодируются нативно.
 */
fun buildImageLoader(context: PlatformContext, client: OkHttpClient): ImageLoader =
    ImageLoader.Builder(context)
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                .maxSizeBytes(IMAGE_DISK_CACHE_BYTES)
                .build()
        }
        .components {
            add(OkHttpNetworkFetcherFactory(callFactory = { client }))
            // ImageDecoder появился в API 28, ниже анимированные GIF просто не декодируем
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                add(AnimatedImageDecoder.Factory())
            }
        }
        .crossfade(true)
        .build()
