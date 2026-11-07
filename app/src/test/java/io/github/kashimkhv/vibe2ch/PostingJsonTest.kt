package io.github.kashimkhv.vibe2ch

import io.github.kashimkhv.vibe2ch.data.captchaProblem
import io.github.kashimkhv.vibe2ch.data.remote.dto.CaptchaDto
import io.github.kashimkhv.vibe2ch.data.remote.dto.CaptchaShowDto
import io.github.kashimkhv.vibe2ch.data.remote.dto.PostingDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Фикстуры сняты с живого движка в ноябре 2026:
 *  - captcha-id.json  — GET /api/captcha/emoji/id?board=abu&thread=42375
 *  - captcha-show.json — GET /api/captcha/emoji/show (картинки заменены
 *    на PNG 1×1, чтобы фикстура не весила 50 КБ: проверяется форма ответа,
 *    а не содержимое картинок)
 *  - captcha-success.json — POST /api/captcha/emoji/click с верным символом
 *  - posting-error.json — POST /user/posting с невалидным emoji_captcha_id
 *  - posting-captcha-error.json — POST /user/posting с captcha_type=nocaptcha
 *
 * Успешная отправка на живом дваче не проверялась (пост не отправляли),
 * posting-post.json и posting-thread.json собраны по описанию в api.yml.
 */
class PostingJsonTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        explicitNulls = false
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/$name")) { "fixture $name not found" }
            .readBytes()
            .decodeToString()

    @Test
    fun `captcha id decodes`() {
        val captcha = json.decodeFromString<CaptchaDto>(fixture("captcha-id.json"))

        assertEquals(1, captcha.result)
        assertEquals("emoji", captcha.type)
        assertEquals("emoji", captcha.input)
        assertTrue(captcha.id.isNotBlank())
        assertNull(captcha.error)
    }

    @Test
    fun `captcha id brings the proof of work task`() {
        val captcha = json.decodeFromString<CaptchaDto>(fixture("captcha-id.json"))
        val challenge = checkNotNull(captcha.challenge)

        // Шаблон с подстановкой и хеш на 128 символов — форма задачи движка.
        assertTrue(challenge.template.contains("%d"))
        assertEquals(128, challenge.hash.length)
        assertTrue(challenge.limit > 0)
    }

    @Test
    fun `captcha show decodes the picture and the keyboard`() {
        val show = json.decodeFromString<CaptchaShowDto>(fixture("captcha-show.json"))

        assertTrue(show.image.isNotBlank())
        // Клавиатура emoji-капчи всегда 4×2: восемь символов на выбор.
        assertEquals(8, show.keyboard.size)
        assertTrue(show.keyboard.all { it.isNotBlank() })
        assertTrue(show.success.isBlank())
    }

    @Test
    fun `solved captcha comes as success without a picture`() {
        val solved = json.decodeFromString<CaptchaShowDto>(fixture("captcha-success.json"))

        assertTrue(solved.success.isNotBlank())
        assertTrue(solved.image.isBlank())
        assertTrue(solved.keyboard.isEmpty())
    }

    @Test
    fun `posting a reply returns the post number`() {
        val posted = json.decodeFromString<PostingDto>(fixture("posting-post.json"))

        assertEquals(1, posted.result)
        assertEquals(782345671, posted.num)
        // У ответа в тред поля `thread` нет — номер треда клиент уже знает.
        assertEquals(0, posted.thread)
        assertNull(posted.error)
    }

    @Test
    fun `creating a thread returns its number in both fields`() {
        val created = json.decodeFromString<PostingDto>(fixture("posting-thread.json"))

        assertEquals(1, created.result)
        assertEquals(782345670, created.thread)
        assertEquals(782345670, created.num)
    }

    @Test
    fun `posting errors carry the engine code and message`() {
        val invalidCaptcha =
            json.decodeFromString<PostingDto>(fixture("posting-captcha-error.json"))
        assertEquals(0, invalidCaptcha.result)
        assertEquals(-5, invalidCaptcha.error?.code)
        assertEquals("Капча невалидна.", invalidCaptcha.error?.message)

        // Заведомо неверный emoji_captcha_id движок называет внутренней ошибкой
        // с кодом 666, а не «невалидной капчей»: клиент должен просить новую.
        val broken = json.decodeFromString<PostingDto>(fixture("posting-error.json"))
        assertEquals(0, broken.result)
        assertEquals(666, broken.error?.code)
        assertTrue(broken.error?.message.orEmpty().isNotBlank())
    }

    @Test
    fun `captcha problems are explained by engine result codes`() {
        // Серверное сообщение всегда важнее догадок по коду.
        assertEquals("Капча невалидна.", captchaProblem(0, "Капча невалидна."))
        // 2 — залогинен по пасскоду, капчу проходить не нужно.
        assertTrue(captchaProblem(2, null).contains("пасскод"))
        // 3 — капча отключена на доске.
        assertTrue(captchaProblem(3, null).contains("отключена"))
        // 0 — пасскод недействителен.
        assertTrue(captchaProblem(0, null).contains("Пасскод"))
        // Неизвестный код не должен уходить в пустую строку.
        assertTrue(captchaProblem(-99, null).contains("-99"))
    }
}
