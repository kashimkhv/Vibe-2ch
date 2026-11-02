package dev.dvach.client.ui.posting

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import dev.dvach.client.data.ChanRepository
import dev.dvach.client.data.PostedStore
import dev.dvach.client.data.PostingRepository
import dev.dvach.client.data.local.ChanDao
import dev.dvach.client.data.local.MyPostEntity
import dev.dvach.client.domain.MAX_UPLOAD_BYTES
import dev.dvach.client.domain.model.Board
import dev.dvach.client.domain.model.CaptchaChallenge
import dev.dvach.client.domain.model.CaptchaStep
import dev.dvach.client.domain.model.OutgoingFile
import dev.dvach.client.domain.model.PostDraft
import dev.dvach.client.domain.model.PostingOutcome
import dev.dvach.client.domain.postingCommentError
import dev.dvach.client.domain.quotePrefix
import dev.dvach.client.domain.uploadError
import dev.dvach.client.ui.navigation.ComposerRoute

/** Состояние капчи для экрана формы. */
sealed interface CaptchaUi {
    data object Loading : CaptchaUi

    data class Puzzle(
        val id: String,
        val image: String,
        val keyboard: List<String>,
        val challenge: CaptchaChallenge? = null,
    ) : CaptchaUi

    data class Solved(
        val token: String,
        val challenge: CaptchaChallenge? = null,
    ) : CaptchaUi

    data class Failed(val message: String) : CaptchaUi
}

data class ComposerUiState(
    val board: Board? = null,
    val boardLoading: Boolean = true,
    val boardError: String? = null,
    val name: String = "",
    val email: String = "",
    val subject: String = "",
    val comment: String = "",
    val file: OutgoingFile? = null,
    val fileError: String? = null,
    val captcha: CaptchaUi = CaptchaUi.Loading,
    val captchaBusy: Boolean = false,
    val sending: Boolean = false,
    val error: String? = null,
    /** Успешный результат: экран по нему закрывается и уходит в тред. */
    val done: PostingOutcome? = null,
) {
    val captchaSolved: Boolean get() = captcha is CaptchaUi.Solved

    val canSend: Boolean
        get() = !sending &&
            captchaSolved &&
            board?.enablePosting != false
}

@HiltViewModel
class ComposerViewModel @Inject constructor(
    private val chanRepository: ChanRepository,
    private val postingRepository: PostingRepository,
    private val dao: ChanDao,
    private val postedStore: PostedStore,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val route: ComposerRoute = savedStateHandle.toRoute()
    val isNewThread: Boolean = route.threadNum <= 0

    private val _state = MutableStateFlow(
        ComposerUiState(comment = quotePrefix(route.quoteNum)),
    )
    val state: StateFlow<ComposerUiState> = _state.asStateFlow()

    /** Счётчик запросов капчи: ответ на устаревший запрос не должен перебить новый. */
    private var captchaRequest = 0

    init {
        viewModelScope.launch {
            val board = runCatching { chanRepository.board(route.boardId) }.getOrNull()
            _state.update {
                it.copy(
                    board = board,
                    boardLoading = false,
                    boardError = if (board == null) {
                        "Не удалось получить настройки доски — поля и лимиты неизвестны"
                    } else {
                        null
                    },
                )
            }
            if (board?.enablePosting == false) {
                _state.update { it.copy(captcha = CaptchaUi.Failed("Постинг на этой доске отключён")) }
            } else {
                refreshCaptcha()
            }
        }
    }

    fun onNameChange(value: String) = edit { it.copy(name = value) }

    fun onEmailChange(value: String) = edit { it.copy(email = value) }

    fun onSubjectChange(value: String) = edit { it.copy(subject = value) }

    fun onCommentChange(value: String) = edit { it.copy(comment = value) }

    fun removeFile() = edit { it.copy(file = null, fileError = null) }

    fun dismissError() = edit { it.copy(error = null) }

    private fun edit(transform: (ComposerUiState) -> ComposerUiState) {
        _state.update { transform(it).copy(error = null) }
    }

    /** Новая капча: после таймаута, ошибки отправки или по кнопке «Обновить». */
    fun refreshCaptcha() {
        val request = ++captchaRequest
        _state.update { it.copy(captcha = CaptchaUi.Loading, captchaBusy = false) }
        viewModelScope.launch {
            val step = postingRepository.newCaptcha(
                boardId = route.boardId,
                threadNum = route.threadNum.takeIf { it > 0 },
            )
            if (request == captchaRequest) {
                _state.update { it.copy(captcha = step.toUi()) }
            }
        }
    }

    /**
     * Клик по символу клавиатуры. Верный символ присылает следующую картинку,
     * последний — токен; неверный возвращает ту же картинку, и это нормально.
     */
    fun clickCaptcha(index: Int) {
        val puzzle = _state.value.captcha as? CaptchaUi.Puzzle ?: return
        if (_state.value.captchaBusy || _state.value.sending) return
        _state.update { it.copy(captchaBusy = true) }
        viewModelScope.launch {
            val step = postingRepository.click(puzzle.id, index)
            _state.update {
                it.copy(
                    captchaBusy = false,
                    captcha = step.toUi(fallback = puzzle.challenge),
                )
            }
        }
    }

    /**
     * Вложение читается сразу в память: экран можно закрыть, приложение —
     * свернуть, а файл должен уйти вместе с постом. Пределы проверяем здесь,
     * чтобы не держать в памяти мегабайты, которые движок всё равно отвергнет.
     */
    fun attach(uri: Uri) {
        _state.update { it.copy(fileError = null) }
        viewModelScope.launch {
            val board = _state.value.board
            val picked = withContext(Dispatchers.IO) {
                runCatching { readOutgoingFile(uri) }
            }
            picked.onSuccess { file ->
                val problem = uploadError(
                    fileName = file.name,
                    sizeBytes = file.sizeBytes,
                    allowedExtensions = board?.fileTypes.orEmpty(),
                    maxFilesSizeKb = board?.maxFilesSize ?: 0,
                )
                if (problem == null) {
                    _state.update { it.copy(file = file, fileError = null) }
                } else {
                    _state.update { it.copy(file = null, fileError = problem) }
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(file = null, fileError = error.message ?: "Не удалось прочитать файл")
                }
            }
        }
    }

    fun send() {
        val current = _state.value
        if (current.sending) return
        val board = current.board
        if (board?.enablePosting == false) {
            _state.update { it.copy(error = "Постинг на этой доске отключён") }
            return
        }
        val commentProblem = postingCommentError(
            comment = current.comment,
            hasFile = current.file != null,
            maxComment = board?.maxComment ?: 0,
        )
        if (commentProblem != null) {
            _state.update { it.copy(error = commentProblem) }
            return
        }
        val solved = current.captcha as? CaptchaUi.Solved
        if (solved == null) {
            _state.update { it.copy(error = "Сначала решите капчу") }
            return
        }

        _state.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            val outcome = postingRepository.submit(
                PostDraft(
                    boardId = route.boardId,
                    threadNum = route.threadNum.takeIf { it > 0 },
                    name = current.name,
                    email = current.email,
                    subject = current.subject,
                    comment = current.comment,
                    captchaToken = solved.token,
                    challenge = solved.challenge,
                    file = current.file,
                ),
            )
            when (outcome) {
                is PostingOutcome.Posted -> {
                    rememberMyPost(threadNum = route.threadNum, postNum = outcome.num)
                    _state.update { it.copy(sending = false, done = outcome) }
                }
                is PostingOutcome.ThreadCreated -> {
                    // ОП нового треда — пост с тем же номером, что и тред.
                    rememberMyPost(threadNum = outcome.num, postNum = outcome.num)
                    _state.update { it.copy(sending = false, done = outcome) }
                }
                is PostingOutcome.Failed -> {
                    _state.update {
                        it.copy(
                            sending = false,
                            error = outcome.message,
                            captcha = if (outcome.code == -5 || outcome.code == 666) {
                                CaptchaUi.Loading
                            } else {
                                it.captcha
                            },
                        )
                    }
                    // Капча одноразовая и живёт минуты: после отказа движка
                    // просим новую, чтобы следующая попытка не упиралась в ту же.
                    if (outcome.code == -5 || outcome.code == 666) refreshCaptcha()
                }
            }
        }
    }

    private suspend fun rememberMyPost(threadNum: Int, postNum: Int) {
        dao.addMyPost(
            MyPostEntity(
                boardId = route.boardId,
                threadNum = threadNum,
                postNum = postNum,
                createdAt = System.currentTimeMillis(),
            ),
        )
        dao.trimMyPosts(MAX_REMEMBERED_POSTS)
        postedStore.remember(route.boardId, threadNum, postNum)
    }

    private fun readOutgoingFile(uri: Uri): OutgoingFile {
        var name = uri.lastPathSegment?.substringAfterLast('/').orEmpty().ifBlank { "file" }
        var declaredSize = -1L
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) cursor.getString(nameIndex)?.takeIf { it.isNotBlank() }?.let { name = it }
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) declaredSize = cursor.getLong(sizeIndex)
            }
        }
        if (declaredSize > MAX_UPLOAD_BYTES) throw TooBigFileException()

        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val chunk = ByteArray(64 * 1024)
            var total = 0
            while (true) {
                val read = input.read(chunk)
                if (read <= 0) break
                total += read
                if (total > MAX_UPLOAD_BYTES) throw TooBigFileException()
                output.write(chunk, 0, read)
            }
            output.toByteArray()
        } ?: throw IllegalStateException("Не удалось открыть файл")

        val extension = name.substringAfterLast('.', "").lowercase()
        val mime = context.contentResolver.getType(uri) ?: guessMime(extension)
        return OutgoingFile(name = name, mimeType = mime, bytes = bytes)
    }

    private fun CaptchaStep.toUi(fallback: CaptchaChallenge? = null): CaptchaUi = when (this) {
        is CaptchaStep.Puzzle -> CaptchaUi.Puzzle(
            id = id,
            image = image,
            keyboard = keyboard,
            challenge = challenge ?: fallback,
        )
        is CaptchaStep.Solved -> CaptchaUi.Solved(
            token = token,
            challenge = challenge ?: fallback,
        )
        is CaptchaStep.Failed -> CaptchaUi.Failed(message = message)
    }

    private class TooBigFileException :
        IllegalStateException("Файл больше ${MAX_UPLOAD_BYTES / (1024 * 1024)} МБ")
}

private fun guessMime(extension: String): String = when (extension) {
    "jpg", "jpeg" -> "image/jpeg"
    "png" -> "image/png"
    "gif" -> "image/gif"
    "bmp" -> "image/bmp"
    "webm" -> "video/webm"
    "mp4" -> "video/mp4"
    "mov" -> "video/quicktime"
    "mp3" -> "audio/mpeg"
    "ogg" -> "audio/ogg"
    else -> "application/octet-stream"
}

/** Сколько своих постов помнить: больше всё равно не понадобится. */
private const val MAX_REMEMBERED_POSTS = 500
