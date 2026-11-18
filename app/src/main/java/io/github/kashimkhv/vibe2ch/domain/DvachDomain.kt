package io.github.kashimkhv.vibe2ch.domain

import java.net.IDN

/** Зеркало движка: домен и короткая подпись для списка в настройках. */
data class DvachMirror(val host: String, val note: String)

/** Домен, к которому клиент ходит, пока в настройках не выбрано другое. */
const val DEFAULT_DVACH_HOST = "2ch.hk"

/**
 * Зеркала, проверенные на живом движке: у всех трёх одинаковые
 * `/b/index.json`, `/api/mobile/v2/boards` и файлы. `2ch.su` вдобавок
 * отдаёт картинки, на него `2ch.hk` отвечает редиректом `301`.
 *
 * Список — только подсказка: в настройках можно ввести любой домен, который
 * отдаёт JSON API Makaba. Проверено: `2ch.pm` отвечает `403`, `2ch.rip` —
 * другой сайт, а не зеркало.
 */
val DVACH_MIRRORS = listOf(
    DvachMirror("2ch.hk", "Основной"),
    DvachMirror("2ch.life", "Зеркало"),
    DvachMirror("2ch.su", "Зеркало и картинки"),
)

/** Домены движка: запросы к ним можно перевести на выбранный домен. */
val DVACH_HOSTS: Set<String> = DVACH_MIRRORS.mapTo(LinkedHashSet()) { it.host }

/**
 * Приводит ввод к домену: `https://2CH.life/b/` → `2ch.life`.
 * `null` — это не похоже на домен.
 *
 * В поле настроек обычно вставляют адрес из браузера целиком, поэтому схема,
 * путь, порт и `www.` отбрасываются. Порт не поддерживается: движок на
 * зеркалах живёт на обычном `443`, а лишний параметр только путал бы.
 */
fun normalizeDvachHost(raw: String?): String? {
    var text = raw?.trim().orEmpty()
    if (text.isEmpty()) return null
    text = text.substringAfter("://", text)
    text = text.substringBefore('/')
    text = text.substringBefore('?')
    text = text.substringBefore('#')
    // Учётные данные в адресе (`user:pass@host`) — не часть домена.
    text = text.substringAfterLast('@')
    text = text.substringBefore(':')
    text = text.trim().trimEnd('.').lowercase().removePrefix("www.")
    if (text.isEmpty() || text.length > MAX_HOST_LENGTH) return null

    // Кириллический домен браузер показывает как есть, а запросу нужен punycode.
    val ascii = runCatching { IDN.toASCII(text) }.getOrNull()?.lowercase() ?: return null
    if (!HOST_REGEX.matches(ascii)) return null
    // Последняя часть — домен верхнего уровня: `1.2.3.4` и `localhost` не годятся.
    if (!ascii.substringAfterLast('.').any(Char::isLetter)) return null
    return ascii
}

/** Полный адрес движка для Retrofit и ссылок: `https://2ch.hk`. */
fun baseUrlOf(host: String): String = "https://$host"

/** Домен из сохранённого адреса; битое значение — основной домен. */
fun hostOfBaseUrl(baseUrl: String?): String = normalizeDvachHost(baseUrl) ?: DEFAULT_DVACH_HOST

/**
 * Что ответила проверка домена в настройках. Проверка идёт отдельным клиентом:
 * запрос должен уйти именно на проверяемый домен, а не на выбранный.
 */
sealed interface DomainCheck {
    data object Idle : DomainCheck
    data class Checking(val host: String) : DomainCheck
    data class Ok(val host: String, val millis: Long) : DomainCheck
    data class Failed(val host: String, val reason: String) : DomainCheck
}

private const val MAX_HOST_LENGTH = 253

/**
 * Метка домена: буквы, цифры и дефис внутри, от одной до 63 символов.
 * Дефис не может стоять в начале или в конце метки — так же, как в DNS.
 */
private val HOST_REGEX =
    Regex("^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)+$")
