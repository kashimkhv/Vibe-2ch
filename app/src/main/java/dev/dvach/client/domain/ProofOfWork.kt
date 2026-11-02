package dev.dvach.client.domain

import dev.dvach.client.domain.model.CaptchaChallenge
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
    if (challenge.limit <= 0 || challenge.template.isEmpty()) return null
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
