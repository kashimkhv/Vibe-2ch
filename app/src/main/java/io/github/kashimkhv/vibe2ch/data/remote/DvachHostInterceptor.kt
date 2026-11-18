package io.github.kashimkhv.vibe2ch.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Подменяет домен движка на выбранный в настройках.
 *
 * Стоит первым в цепочке, поэтому куки, кэш и лог уже видят настоящий адрес:
 * `CookieJar` получает запрос к выбранному домену, а не к `2ch.hk`.
 * Запросы к чужим хостам (GitHub, внешние картинки) проходят как есть —
 * это решает [DvachServer.rewrite].
 */
class DvachHostInterceptor(private val server: DvachServer) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val rewritten = server.rewrite(request.url) ?: return chain.proceed(request)
        return chain.proceed(request.newBuilder().url(rewritten).build())
    }
}
