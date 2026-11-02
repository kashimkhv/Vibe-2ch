package dev.dvach.client.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Ошибка движка. Приходит и с HTTP 200 — в теле `{"result":0,"error":{…}}`,
 * поэтому её нельзя ловить только по коду ответа.
 */
@Serializable
data class ApiErrorDto(
    val code: Int = 0,
    val message: String = "",
)

/**
 * Ответ `GET /api/captcha/emoji/id`. Для анонима `result = 1` и есть `id`,
 * по которому запрашивается картинка. Остальные `result` — «пасскод», «капча
 * отключена», «слишком часто» и т.п., их разбирает репозиторий.
 *
 * `challenge` — задача «доказательства работы»: её решение уходит в пост
 * отдельным полем `2ch_challenge`, иначе движок пост не принимает.
 */
@Serializable
data class CaptchaDto(
    val result: Int = 0,
    val type: String = "",
    val id: String = "",
    val input: String = "",
    val challenge: ChallengeDto? = null,
    val error: ApiErrorDto? = null,
)

/** Задача для поля `2ch_challenge`: sha512(template с числом вместо `%d`) == hash. */
@Serializable
data class ChallengeDto(
    val template: String = "",
    val hash: String = "",
    val limit: Int = 0,
)

/**
 * `GET /api/captcha/emoji/show` и `POST /api/captcha/emoji/click` отвечают
 * одной из двух форм: либо новая картинка с клавиатурой, либо `success` —
 * готовый `emoji_captcha_id` для отправки поста.
 *
 * Картинки — PNG в base64. `image` — что нужно найти, `keyboard` — из чего
 * выбирать; клик по неверному символу возвращает ту же картинку.
 */
@Serializable
data class CaptchaShowDto(
    val image: String = "",
    val keyboard: List<String> = emptyList(),
    val success: String = "",
    val error: ApiErrorDto? = null,
)

/** Тело клика по эмодзи: `emojiNumber` — индекс в `keyboard`, с нуля. */
@Serializable
data class CaptchaClickDto(
    val captchaTokenID: String,
    val emojiNumber: Int,
)

/**
 * Ответ `POST /user/posting`: `thread` — номер нового треда, `num` — номер
 * нового поста в существующем треде.
 */
@Serializable
data class PostingDto(
    val result: Int = 0,
    val thread: Int = 0,
    val num: Int = 0,
    val error: ApiErrorDto? = null,
)
