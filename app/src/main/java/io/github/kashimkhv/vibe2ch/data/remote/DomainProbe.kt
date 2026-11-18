package io.github.kashimkhv.vibe2ch.data.remote

import io.github.kashimkhv.vibe2ch.domain.DomainCheck
import io.github.kashimkhv.vibe2ch.domain.baseUrlOf
import io.github.kashimkhv.vibe2ch.domain.normalizeDvachHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Проверка домена из настроек: отвечает ли он как движок.
 *
 * Свой клиент, без [DvachHostInterceptor]: запрос должен уйти именно на
 * проверяемый домен, а не на выбранный сейчас. Куки при этом общие — иначе
 * живое зеркало могло бы ответить `403` от Cloudflare, хотя в приложении
 * с сохранённым `cf_clearance` оно работает.
 *
 * Спрашиваем `/b/index.json`: это тот же запрос, с которого начинается лента,
 * и по нему сразу видно, что домен отдаёт JSON API, а не заглушку.
 */
@Singleton
class DomainProbe @Inject constructor(cookies: CookieStore) {

    private val client = OkHttpClient.Builder()
        .cookieJar(cookies)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", DVACH_USER_AGENT)
                    .header("Accept", "application/json")
                    .build(),
            )
        }
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun probe(host: String): DomainCheck = withContext(Dispatchers.IO) {
        val normalized = normalizeDvachHost(host)
            ?: return@withContext DomainCheck.Failed(host, "домен выглядит неправильно")
        // Параметр отсекает ответ из кэша: проверяем сеть, а не прошлый успех.
        val url = "${baseUrlOf(normalized)}/b/index.json?probe=${System.currentTimeMillis()}"
        val started = System.nanoTime()
        try {
            val request = Request.Builder()
                .url(url)
                .header("Cache-Control", "no-store")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val head = runCatching {
                    response.peekBody(HEAD_BYTES).string().trimStart()
                }.getOrDefault("")
                when {
                    !response.isSuccessful ->
                        DomainCheck.Failed(normalized, "ответ ${response.code}")
                    !head.startsWith("{") && !head.startsWith("[") ->
                        DomainCheck.Failed(normalized, "ответ не JSON")
                    else -> DomainCheck.Ok(
                        host = normalized,
                        millis = (System.nanoTime() - started) / 1_000_000,
                    )
                }
            }
        } catch (e: IOException) {
            DomainCheck.Failed(normalized, probeError(e))
        }
    }

    private companion object {
        /** Хватает на первый символ JSON и пробелы перед ним. */
        const val HEAD_BYTES = 64L
    }
}

/** Понятная причина вместо «Failed to connect to /…». */
internal fun probeError(e: IOException): String = when (e) {
    is SocketTimeoutException -> "таймаут"
    is UnknownHostException -> "домен не найден"
    is ConnectException -> "нет соединения"
    else -> e.message?.takeIf { it.isNotBlank() } ?: "ошибка сети"
}
