package io.github.kashimkhv.vibe2ch.data.remote

/**
 * 2ch.hk за Cloudflare режет запросы без внятного User-Agent, поэтому подставляем
 * UA десктопного браузера — как это делал старый клиент. Тот же UA нужен
 * проверке домена в настройках: иначе живое зеркало отвечало бы `403`.
 */
internal const val DVACH_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/131.0.0.0 Mobile Safari/537.36"
