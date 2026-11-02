package dev.dvach.client.data

import dev.dvach.client.data.remote.DvachApi
import dev.dvach.client.data.remote.dto.CaptchaClickDto
import dev.dvach.client.data.remote.dto.CaptchaShowDto
import dev.dvach.client.data.remote.dto.ChallengeDto
import dev.dvach.client.domain.model.CaptchaChallenge
import dev.dvach.client.domain.model.CaptchaStep
import dev.dvach.client.domain.model.PostDraft
import dev.dvach.client.domain.model.PostingOutcome
import dev.dvach.client.domain.solveChallenge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Постинг и эмодзи-капча: всё, чего не хватало читающему клиенту. */
interface PostingRepository {

    /** Новая капча для ответа в тред ([threadNum] != null) или нового треда. */
    suspend fun newCaptcha(boardId: String, threadNum: Int?): CaptchaStep

    /** Клик по символу на клавиатуре: следующая картинка или готовый токен. */
    suspend fun click(captchaId: String, emojiNumber: Int): CaptchaStep

    suspend fun submit(draft: PostDraft): PostingOutcome
}

@Singleton
class NetworkPostingRepository @Inject constructor(
    private val api: DvachApi,
) : PostingRepository {

    override suspend fun newCaptcha(boardId: String, threadNum: Int?): CaptchaStep =
        try {
            val id = api.captchaId(boardId, threadNum)
            if (id.result != 1 || id.id.isBlank()) {
                CaptchaStep.Failed(captchaProblem(id.result, id.error?.message))
            } else {
                api.captchaShow(id.id).toStep(id.id, id.challenge?.toModel())
            }
        } catch (error: IOException) {
            CaptchaStep.Failed("Нет связи с сервером: ${error.message ?: "проверьте сеть"}")
        } catch (error: Exception) {
            CaptchaStep.Failed("Капча не загрузилась: ${error.message ?: "неизвестная ошибка"}")
        }

    override suspend fun click(captchaId: String, emojiNumber: Int): CaptchaStep =
        try {
            api.captchaClick(
                CaptchaClickDto(captchaTokenID = captchaId, emojiNumber = emojiNumber),
            ).toStep(captchaId)
        } catch (error: IOException) {
            CaptchaStep.Failed("Нет связи с сервером: ${error.message ?: "проверьте сеть"}")
        } catch (error: Exception) {
            CaptchaStep.Failed("Капча не отвечает: ${error.message ?: "неизвестная ошибка"}")
        }

    override suspend fun submit(draft: PostDraft): PostingOutcome =
        try {
            // Задачу «доказательства работы» решаем перебором: движок в ответе
            // на /api/captcha/emoji/id даёт хеш и шаблон, а в пост ждёт число.
            val proof = draft.challenge?.let { challenge ->
                withContext(Dispatchers.Default) { solveChallenge(challenge) }
            }
            if (draft.challenge != null && proof == null) {
                return PostingOutcome.Failed(
                    "Не удалось решить задачу движка — обновите капчу и попробуйте снова",
                )
            }
            val response = api.posting(
                nc = "1",
                task = "post".toPart(),
                captchaType = "emoji_captcha".toPart(),
                board = draft.boardId.toPart(),
                thread = draft.threadNum?.toString()?.toPart(),
                name = draft.name.trim().toPart(),
                email = draft.email.trim().toPart(),
                subject = draft.subject.trim().toPart(),
                tags = "".toPart(),
                usercode = "".toPart(),
                comment = draft.comment.toPart(),
                captchaId = draft.captchaToken.toPart(),
                challenge = proof?.toPart(),
                file = draft.file?.toPart(),
            )
            when {
                response.result != 1 ->
                    PostingOutcome.Failed(
                        message = response.error?.message ?: "Сервер не принял пост",
                        code = response.error?.code ?: 0,
                    )
                draft.threadNum == null && response.thread > 0 ->
                    PostingOutcome.ThreadCreated(response.thread)
                draft.threadNum != null && response.num > 0 ->
                    PostingOutcome.Posted(response.num)
                else -> PostingOutcome.Failed("Сервер не вернул номер поста")
            }
        } catch (error: IOException) {
            PostingOutcome.Failed("Пост не отправился: ${error.message ?: "проверьте сеть"}")
        } catch (error: Exception) {
            PostingOutcome.Failed("Пост не отправился: ${error.message ?: "неизвестная ошибка"}")
        }
}

/**
 * Ответ `/show` и `/click` в один тип: `success` — капча решена, иначе
 * следующая картинка. Пустая картинка означает, что движок ответил ошибкой.
 *
 * [challenge] приходит только вместе с ID капчи, поэтому едет в обоих
 * состояниях: пока капча не отправлена, задача нужна для поля `2ch_challenge`.
 */
private fun CaptchaShowDto.toStep(
    captchaId: String,
    challenge: CaptchaChallenge? = null,
): CaptchaStep = when {
    success.isNotBlank() -> CaptchaStep.Solved(success, challenge)
    image.isNotBlank() && keyboard.isNotEmpty() ->
        CaptchaStep.Puzzle(id = captchaId, image = image, keyboard = keyboard, challenge = challenge)
    else -> CaptchaStep.Failed(
        error?.message ?: "Капча не загрузилась, попробуйте ещё раз",
    )
}

private fun ChallengeDto.toModel(): CaptchaChallenge =
    CaptchaChallenge(template = template, hash = hash, limit = limit)

private fun String.toPart() = toRequestBody("text/plain; charset=utf-8".toMediaTypeOrNull())

private fun dev.dvach.client.domain.model.OutgoingFile.toPart(): MultipartBody.Part {
    val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
    return MultipartBody.Part.createFormData("file[]", name, body)
}

/**
 * Человеческое объяснение `result` из `/api/captcha/emoji/id`.
 * Коды описаны в EmojiCaptcha.js самого движка.
 */
internal fun captchaProblem(result: Int, serverMessage: String?): String = when {
    !serverMessage.isNullOrBlank() -> serverMessage
    result == 2 -> "Вы вошли по пасскоду — капча не нужна, но клиент пока не умеет постить с пасскодом"
    result == 3 -> "На этой доске капча отключена"
    result == 0 -> "Пасскод недействителен — капча недоступна"
    result == -1 || result == 4 -> "Капча истекла, попробуйте ещё раз"
    else -> "Капча недоступна (код $result)"
}
