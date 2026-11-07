package io.github.kashimkhv.vibe2ch.domain

import io.github.kashimkhv.vibe2ch.domain.model.CaptchaChallenge
import java.security.MessageDigest

/**
 * Задача «доказательства работы», которую движок отдаёт вместе с ID капчи.
 *
 * В ответе `/api/captcha/emoji/id` приходит `challenge: { hash, limit, template }`,
 * а вместе с постом движок ждёт поле `2ch_challenge` — число, для которого
 * `sha512(template)` с подставленным вместо `%d` числом совпадает с `hash`.
 * Тот же перебор делает сам сайт (board-worker.min.js), это часть протокола
 * постинга, а не обход капчи: эмодзи всё равно решает человек.
 *
 * @return число в виде строки или null, если задача не решается в пределах
 *   [CaptchaChallenge.limit] — тогда пост отправлять бессмысленно.
 */
fun solveChallenge(challenge: CaptchaChallenge): String? {
    if (challenge.limit !in 1..MAX_ATTEMPTS) return null
    if (challenge.template.isEmpty() || challenge.template.length > MAX_TEMPLATE_LENGTH) return null
    if (!challenge.template.contains("%d")) return null
    val target = challenge.hash.lowercase()
    val digest = MessageDigest.getInstance("SHA-512")
    for (nonce in 0 until challenge.limit) {
        val candidate = challenge.template.replaceFirst("%d", nonce.toString())
        val hash = digest.digest(candidate.toByteArray(Charsets.UTF_8)).toHex()
        if (hash == target) return nonce.toString()
    }
    return null
}

/**
 * Пределы задачи из ответа движка: `limit` — число попыток, `template` —
 * шаблон. Обычно это 20 000 и сорок символов, но ответ приходит по сети,
 * поэтому без потолка испорченный или подменённый ответ заставил бы телефон
 * считать sha512 миллиарды раз. Задача всё равно решается на устройстве,
 * так что предел должен быть разумным, а не «сколько попросит сервер».
 */
private const val MAX_ATTEMPTS = 1_000_000
private const val MAX_TEMPLATE_LENGTH = 8_192

private fun ByteArray.toHex(): String {
    val digits = "0123456789abcdef"
    val out = CharArray(size * 2)
    for (index in indices) {
        val value = this[index].toInt() and 0xFF
        out[index * 2] = digits[value ushr 4]
        out[index * 2 + 1] = digits[value and 0x0F]
    }
    return String(out)
}
