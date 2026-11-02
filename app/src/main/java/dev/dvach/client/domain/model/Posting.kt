package dev.dvach.client.domain.model

/**
 * Состояние эмодзи-капчи. Решает её человек: движок показывает картинку
 * с символами и клавиатуру, по которой нужно кликать. Клик по верному символу
 * присылает следующую картинку, последний — `emoji_captcha_id` для постинга.
 */
sealed interface CaptchaStep {
    /** Картинка-задание и клавиатура; всё в base64 PNG. */
    data class Puzzle(
        val id: String,
        val image: String,
        val keyboard: List<String>,
    ) : CaptchaStep

    /**
     * Капча решена: [token] — тот самый `emoji_captcha_id`, который уходит
     * в форму постинга. Живёт ограниченное время (движок даёт около 5 минут).
     */
    data class Solved(val token: String) : CaptchaStep

    /** Капча недоступна: пасскод, отключена на доске, сеть и т.п. */
    data class Failed(val message: String) : CaptchaStep
}

/** Файл, который уйдёт вместе с постом. Держим байты, а не Uri: экран можно закрыть. */
class OutgoingFile(
    val name: String,
    val mimeType: String,
    val bytes: ByteArray,
) {
    val sizeBytes: Int get() = bytes.size
}

/** Всё, что нужно для отправки: поля формы и решённая капча. */
data class PostDraft(
    val boardId: String,
    /** null — создаём новый тред. */
    val threadNum: Int?,
    val name: String,
    val email: String,
    val subject: String,
    val comment: String,
    val captchaToken: String,
    val file: OutgoingFile?,
)

/** Результат отправки. */
sealed interface PostingOutcome {
    data class Posted(val num: Int) : PostingOutcome
    data class ThreadCreated(val num: Int) : PostingOutcome

    /**
     * [code] — код движка (`-5` — невалидная капча, `666` — внутренняя
     * ошибка). По нему экран понимает, надо ли сразу просить новую капчу.
     */
    data class Failed(val message: String, val code: Int = 0) : PostingOutcome
}
