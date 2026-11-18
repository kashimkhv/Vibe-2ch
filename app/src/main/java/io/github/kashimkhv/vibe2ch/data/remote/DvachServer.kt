package io.github.kashimkhv.vibe2ch.data.remote

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.kashimkhv.vibe2ch.data.settings.BASE_URL_KEY
import io.github.kashimkhv.vibe2ch.data.settings.settingsDataStore
import io.github.kashimkhv.vibe2ch.domain.DEFAULT_DVACH_HOST
import io.github.kashimkhv.vibe2ch.domain.DVACH_HOSTS
import io.github.kashimkhv.vibe2ch.domain.normalizeDvachHost
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Домен движка, на который ходят все запросы: API, картинки, видео, постинг.
 *
 * Retrofit собирается один раз с адресом `2ch.hk`, поэтому домен меняется
 * не в нём, а в перехватчике ([DvachHostInterceptor]): запрос к домену движка
 * переписывается на выбранный. Так одна настройка покрывает и JSON, и медиа,
 * и отправку постов, и её не надо протаскивать через все репозитории.
 *
 * Хост кэшируется в памяти: перехватчик спрашивает его на каждом запросе,
 * а DataStore читается один раз. Первое чтение идёт синхронно — но только
 * на сетевом потоке OkHttp, не на main: иначе холодный старт успел бы сходить
 * на прежний домен, пока настройки ещё грузятся.
 */
@Singleton
class DvachServer @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    @Volatile
    private var cached: String? = null

    /** Домен, на который надо ходить прямо сейчас. */
    fun host(): String = cached ?: readSavedHost()

    /** Новый домен из настроек: действует со следующего же запроса. */
    fun set(host: String) {
        cached = normalizeDvachHost(host) ?: DEFAULT_DVACH_HOST
    }

    /**
     * Домены, которые считаются одним движком: известные зеркала и выбранный
     * домен. Между ними общие куки — доступ к закрытой доске не теряется при
     * переключении, — и с любого из них запрос переводится на выбранный.
     */
    fun aliases(): Set<String> = DVACH_HOSTS + host()

    /** Новый адрес запроса или `null`, если запрос не к движку. */
    fun rewrite(url: HttpUrl): HttpUrl? = rewriteDvachUrl(url, host(), aliases())

    private fun readSavedHost(): String {
        val saved = runCatching {
            runBlocking {
                withTimeoutOrNull(READ_TIMEOUT_MS) {
                    context.settingsDataStore.data.first()[BASE_URL_KEY]
                }
            }
        }.getOrNull()
        return (normalizeDvachHost(saved) ?: DEFAULT_DVACH_HOST).also { cached = it }
    }

    private companion object {
        /**
         * Сколько ждём DataStore на первом запросе. Файл настроек крошечный
         * и читается за миллисекунды; предел нужен только чтобы зависшее
         * хранилище не повесило сеть навсегда.
         */
        const val READ_TIMEOUT_MS = 2_000L
    }
}
