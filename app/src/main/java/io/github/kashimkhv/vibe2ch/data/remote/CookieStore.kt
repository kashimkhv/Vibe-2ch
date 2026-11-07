package io.github.kashimkhv.vibe2ch.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File

/**
 * Куки движка, которые переживают перезапуск приложения.
 *
 * У OkHttp куки по умолчанию не хранятся вовсе (`CookieJar.NO_COOKIES`):
 * `Set-Cookie` из ответа выбрасывается, и следующий запрос уходит как новый
 * аноним. Для 2ch это значило, что доступ, который движок выдаёт кукой, терялся
 * сразу: закрытые доски («Взрослым» — `/fet/`, `/e/`, `/h/`, `/nf/`) отвечают
 * `404`, пока браузер не принесёт нужную куку. Заодно сохраняется
 * `cf_clearance` от Cloudflare.
 *
 * Хранилище — один текстовый файл в `filesDir` (не в `cacheDir`: кэш система
 * чистит, когда ей мало места). На строку — адрес, с которого пришла кука, таб
 * и сама кука в формате `Set-Cookie`; `Cookie.parse` читает эту строку обратно.
 * Формат видно глазами, и он не ломается от новых полей OkHttp.
 *
 * Запись атомарная: сначала временный файл, потом `rename`. Оборванная запись
 * не оставит половину куки — либо старый файл, либо новый.
 *
 * Файл читается в конструкторе: он крошечный (единицы килобайт), а счётчик
 * кук нужен настройкам ещё до первого запроса в сеть.
 *
 * Отдельная забота — зеркала движка. Список тредов приходит с `2ch.hk`, а
 * картинки тот же `2ch.hk` отдаёт редиректом (`301`) на `2ch.su`. Доступ к
 * закрытой доске — это host-only кука, поставленная на `2ch.hk`, и по правилам
 * браузера на другой хост она не уходит: OkHttp честно перезапрашивает картинку
 * уже у `2ch.su`, но без куки, и получает `500` с HTML «Not found» вместо
 * превью. Поэтому при запросе к зеркалу кука второго зеркала тоже считается
 * подходящей — хранится при этом одна, а не две копии.
 */
class CookieStore(private val file: File) : CookieJar {

    /** Кука и адрес, с которого она пришла: по нему [Cookie.parse] восстановит домен. */
    private class Entry(val url: String, val cookie: Cookie)

    private val lock = Any()

    /** Порядок вставки: при переполнении вытесняются самые старые куки. */
    private val entries = LinkedHashMap<String, Entry>()

    private val _count = MutableStateFlow(0)

    /** Сколько кук сохранено: настройки показывают это число и дают их стереть. */
    val count: StateFlow<Int> = _count.asStateFlow()

    init {
        synchronized(lock) { load() }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        // Кука, поставленная на 2ch.hk, должна уйти и на 2ch.su: картинки
        // приходят оттуда редиректом. Для остальных хостов зеркала нет.
        val mirror = mirrorUrl(url)
        synchronized(lock) {
            val expired = mutableListOf<String>()
            val result = ArrayList<Cookie>(entries.size)
            for ((key, entry) in entries) {
                val cookie = entry.cookie
                if (cookie.expiresAt <= now) {
                    expired += key
                } else if (cookie.matches(url) || (mirror != null && cookie.matches(mirror))) {
                    result += cookie
                }
            }
            // Досрочно протухшие убираем сразу: иначе они висели бы в файле вечно.
            if (expired.isNotEmpty()) {
                expired.forEach(entries::remove)
                save()
            }
            return result
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        val now = System.currentTimeMillis()
        synchronized(lock) {
            var changed = false
            for (cookie in cookies) {
                val key = keyOf(cookie)
                // Кука с истёкшим сроком — это требование движка её удалить.
                if (cookie.expiresAt <= now) {
                    if (entries.remove(key) != null) changed = true
                    continue
                }
                val header = cookie.toString()
                if (header.length > MAX_COOKIE_SIZE) continue
                val old = entries.put(key, Entry(url.toString(), cookie))
                if (old == null || old.cookie.toString() != header) changed = true
            }
            // Защита от разрастания: движок не должен уметь забить хранилище.
            var trimmed = false
            while (entries.size > MAX_COOKIES) {
                val iterator = entries.keys.iterator()
                iterator.next()
                iterator.remove()
                trimmed = true
            }
            if (changed || trimmed) save()
        }
    }

    /** Стереть все куки: доступ к закрытым доскам после этого надо получать заново. */
    fun clear() {
        synchronized(lock) {
            entries.clear()
            file.delete()
            _count.value = 0
        }
    }

    private fun keyOf(cookie: Cookie): String =
        "${cookie.domain}|${cookie.path}|${cookie.name}"

    /** Адрес того же пути на зеркале — или `null`, если хост не зеркало. */
    private fun mirrorUrl(url: HttpUrl): HttpUrl? {
        val mirror = MIRRORS[url.host] ?: return null
        return url.newBuilder().host(mirror).build()
    }

    private fun load() {
        entries.clear()
        if (!file.isFile) {
            _count.value = 0
            return
        }
        val now = System.currentTimeMillis()
        var stale = false
        runCatching {
            file.useLines { lines ->
                lines.forEach { line ->
                    val tab = line.indexOf('\t')
                    if (tab <= 0) return@forEach
                    val url = line.substring(0, tab).toHttpUrlOrNull() ?: return@forEach
                    val cookie = Cookie.parse(url, line.substring(tab + 1)) ?: return@forEach
                    if (cookie.expiresAt <= now) {
                        stale = true
                        return@forEach
                    }
                    entries[keyOf(cookie)] = Entry(line.substring(0, tab), cookie)
                }
            }
        }
        _count.value = entries.size
        // Просроченные строки (например, доступ, который движок уже отозвал)
        // сразу выкидываем из файла, а не держим до следующей записи.
        if (stale) save()
    }

    /** Вызывается только под [lock]: пишем то, что сейчас в памяти. */
    private fun save() {
        // Кук не осталось — и файла быть не должно: иначе после «стереть»
        // на диске лежал бы пустой след.
        if (entries.isEmpty()) {
            file.delete()
            _count.value = 0
            return
        }
        val tmp = File(file.parentFile, file.name + ".tmp")
        runCatching {
            file.parentFile?.mkdirs()
            tmp.writeText(
                entries.values.joinToString(separator = "\n", postfix = "\n") {
                    "${it.url}\t${it.cookie}"
                },
                Charsets.UTF_8,
            )
            // На Windows rename поверх существующего файла не работает —
            // на всякий случай (тесты гоняются на JVM) сначала удаляем цель.
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        }
        _count.value = entries.size
    }

    private companion object {
        /** Сколько кук держим: у 2ch их единицы, предел нужен только от мусора. */
        const val MAX_COOKIES = 512

        /** Кука длиннее этого — уже не кука, а попытка забить файл. */
        const val MAX_COOKIE_SIZE = 4096

        /**
         * Зеркала движка, между которыми ходят запросы. `2ch.hk` отвечает на
         * картинки `301` на `2ch.su`, а доступ выдаёт host-only кукой на
         * `2ch.hk`: без неё на зеркале превью закрытой доски приходит `500`.
         */
        val MIRRORS = mapOf("2ch.hk" to "2ch.su", "2ch.su" to "2ch.hk")
    }
}
