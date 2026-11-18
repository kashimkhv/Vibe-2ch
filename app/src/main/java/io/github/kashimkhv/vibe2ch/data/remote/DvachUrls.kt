package io.github.kashimkhv.vibe2ch.data.remote

import okhttp3.HttpUrl

/**
 * Переводит запрос с домена движка на выбранный домен. `null` — запрос чужой
 * (GitHub, внешняя картинка, CDN обновлений) или уже идёт на выбранный домен:
 * такие запросы трогать нельзя.
 *
 * Схема и порт остаются прежними: движок живёт на `https`, а порт в настройках
 * не выбирается. Меняется только хост — путь, параметры и тело запроса
 * остаются как были.
 */
internal fun rewriteDvachUrl(url: HttpUrl, host: String, aliases: Set<String>): HttpUrl? {
    if (url.host == host || url.host !in aliases) return null
    return url.newBuilder().host(host).build()
}
