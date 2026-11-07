package dev.dvach.client

import dev.dvach.client.domain.model.CaptchaChallenge
import dev.dvach.client.domain.solveChallenge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Задача `2ch_challenge` из ответа `/api/captcha/emoji/id`: число, при котором
 * sha512(шаблон с подставленным числом) совпадает с хешем. Данные — из живой
 * фикстуры captcha-id.json, ответ для неё 2655; пересчитано независимо.
 */
class ProofOfWorkTest {

    private val real = CaptchaChallenge(
        template = "xiEX66SNJcrbl6uJ%d31fSSy3p4phw9XJZ",
        hash = "3232c7676328c648b322b69bcd59d80b685a009bc1d1f179e15a479073400615" +
            "ff87dbbeeded8107a7fb1bfcc4e6da1bfd72dd20d8916bebf67f90436b4d628d",
        limit = 20_000,
    )

    @Test
    fun `solves the real challenge`() {
        assertEquals("2655", solveChallenge(real))
    }

    @Test
    fun `gives up when the number is beyond the limit`() {
        assertNull(solveChallenge(real.copy(limit = 100)))
    }

    @Test
    fun `rejects a broken task`() {
        assertNull(solveChallenge(real.copy(template = "без-подстановки")))
        assertNull(solveChallenge(real.copy(limit = 0)))
        assertNull(solveChallenge(real.copy(hash = "не-хеш")))
    }

    /**
     * Ответ с капчей приходит по сети: испорченный `limit` вроде `Int.MAX_VALUE`
     * не должен заставлять телефон считать sha512 миллиарды раз.
     */
    @Test
    fun `rejects absurd attempt limit`() {
        assertNull(solveChallenge(real.copy(limit = Int.MAX_VALUE)))
        assertNull(solveChallenge(real.copy(limit = 1_000_001)))
    }

    @Test
    fun `rejects oversized template`() {
        assertNull(solveChallenge(real.copy(template = "%d" + "x".repeat(10_000))))
    }
}
