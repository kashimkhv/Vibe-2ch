package dev.dvach.client.ui.posting

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dvach.client.domain.model.PostingOutcome
import dev.dvach.client.domain.uploadHint
import dev.dvach.client.ui.components.DvachScrollbar
import dev.dvach.client.ui.components.dvachShadow
import dev.dvach.client.ui.components.dvachTopAppBarColors
import dev.dvach.client.util.formatFileSize

/**
 * Форма поста: ответ в тред или создание треда. Экран отдельный, а не панель
 * внизу треда: полей много (имя, почта, тема, файл, капча), и в шторке они
 * перекрывали бы читаемый пост.
 *
 * Стиль — тот же старый Material: квадратные поля с подчёркиванием, серые
 * полосы заголовков, плоские строки и никаких «пилюль».
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ComposerScreen(
    onBack: () -> Unit,
    onPosted: () -> Unit,
    onThreadCreated: (Int) -> Unit,
    viewModel: ComposerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.attach(uri)
    }

    // Успех: ответ — назад в тред (он сам перезагрузится и подсветит пост),
    // новый тред — сразу открыть его.
    LaunchedEffect(state.done) {
        when (val done = state.done) {
            is PostingOutcome.Posted -> onPosted()
            is PostingOutcome.ThreadCreated -> onThreadCreated(done.num)
            else -> Unit
        }
    }

    val title = if (viewModel.isNewThread) "Новый тред" else "Ответ"
    val subtitle = buildString {
        append('/').append(viewModel.route.boardId).append('/')
        if (!viewModel.isNewThread) append(" · №").append(viewModel.route.threadNum)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = dvachTopAppBarColors(),
                modifier = Modifier.dvachShadow(),
                title = {
                    Column {
                        Text(title)
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::send, enabled = state.canSend) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Отправить",
                            tint = if (state.canSend) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                contentPadding = PaddingValues(bottom = 28.dp),
            ) {
                if (state.boardLoading) {
                    item(key = "board-loading") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                        ) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Загружаем настройки доски…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }
                }
                if (state.boardError != null) {
                    item(key = "board-error") {
                        Text(
                            text = state.boardError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }
                }
                if (state.board?.enablePosting == false) {
                    item(key = "posting-disabled") {
                        Text(
                            text = "Постинг на этой доске отключён",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }

                item(key = "section-fields") { SectionTitle("Пост") }

                if (state.board?.enableNames != false) {
                    item(key = "field-name") {
                        ComposerField(
                            value = state.name,
                            onValueChange = viewModel::onNameChange,
                            placeholder = "Имя (необязательно)",
                            singleLine = true,
                        )
                    }
                }
                if (state.board == null || state.board?.enableSage != false) {
                    item(key = "field-email") {
                        ComposerField(
                            value = state.email,
                            onValueChange = viewModel::onEmailChange,
                            placeholder = if (state.board?.enableSage != false) {
                                "Email · sage — без бампа"
                            } else {
                                "Email (необязательно)"
                            },
                            singleLine = true,
                        )
                    }
                }
                if (viewModel.isNewThread && state.board?.enableSubject != false) {
                    item(key = "field-subject") {
                        ComposerField(
                            value = state.subject,
                            onValueChange = viewModel::onSubjectChange,
                            placeholder = "Тема треда (необязательно)",
                            singleLine = true,
                        )
                    }
                }

                item(key = "field-comment") {
                    ComposerField(
                        value = state.comment,
                        onValueChange = viewModel::onCommentChange,
                        placeholder = "Текст поста",
                        singleLine = false,
                    )
                }

                item(key = "section-file") { SectionTitle("Файл") }
                item(key = "field-file") {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        val file = state.file
                        if (file == null) {
                            TextButton(
                                onClick = { picker.launch("*/*") },
                                shape = RoundedCornerShape(2.dp),
                            ) {
                                Icon(
                                    Icons.Default.AttachFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text("Прикрепить файл", modifier = Modifier.padding(start = 8.dp))
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(start = 12.dp),
                            ) {
                                Icon(
                                    Icons.Default.AttachFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                                Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = formatFileSize(file.sizeBytes.toLong()),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = viewModel::removeFile) {
                                    Icon(Icons.Default.Close, contentDescription = "Убрать файл")
                                }
                            }
                        }
                        if (state.fileError != null) {
                            Text(
                                text = state.fileError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                        Text(
                            text = uploadHint(
                                allowedExtensions = state.board?.fileTypes.orEmpty(),
                                maxFilesSizeKb = state.board?.maxFilesSize ?: 0,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }

                item(key = "section-captcha") { SectionTitle("Капча") }
                item(key = "field-captcha") {
                    CaptchaBlock(
                        captcha = state.captcha,
                        busy = state.captchaBusy,
                        onEmojiClick = viewModel::clickCaptcha,
                        onRefresh = viewModel::refreshCaptcha,
                    )
                }

                if (state.error != null) {
                    item(key = "send-error") {
                        Text(
                            text = state.error.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }

                item(key = "send") {
                    Button(
                        onClick = viewModel::send,
                        enabled = state.canSend,
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .dvachShadow(2.dp, RoundedCornerShape(2.dp)),
                    ) {
                        if (state.sending) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text("Отправляем…", modifier = Modifier.padding(start = 10.dp))
                        } else {
                            Text(if (viewModel.isNewThread) "Создать тред" else "Отправить")
                        }
                    }
                }
            }

            // Полоса прокрутки поверх формы — как в остальных списках приложения.
            DvachScrollbar(
                state = listState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun ComposerField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 6,
        shape = RoundedCornerShape(0.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
            disabledIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CaptchaBlock(
    captcha: CaptchaUi,
    busy: Boolean,
    onEmojiClick: (Int) -> Unit,
    onRefresh: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        when (captcha) {
            is CaptchaUi.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Text(
                    text = "Загружаем капчу…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }

            is CaptchaUi.Failed -> Column {
                Text(
                    text = captcha.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = onRefresh, shape = RoundedCornerShape(2.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Обновить капчу", modifier = Modifier.padding(start = 8.dp))
                }
            }

            is CaptchaUi.Solved -> Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Капча решена",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f).padding(start = 10.dp),
                )
                TextButton(onClick = onRefresh, shape = RoundedCornerShape(2.dp)) {
                    Text("Сбросить")
                }
            }

            is CaptchaUi.Puzzle -> Column {
                Text(
                    text = "Выберите все символы, которые видите на картинке. " +
                        "Порядок не важен, часть символов появится на следующих шагах.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val prompt = rememberDecodedImage(captcha.image)
                if (prompt != null) {
                    Image(
                        bitmap = prompt,
                        contentDescription = "Картинка капчи: выберите символы с неё",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .heightIn(min = 90.dp, max = 200.dp)
                            .background(Color.White)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    )
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    maxItemsInEachRow = 4,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    captcha.keyboard.forEachIndexed { index, encoded ->
                        val image = rememberDecodedImage(encoded)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.White)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                .clickable(enabled = !busy) { onEmojiClick(index) }
                                .semantics {
                                    contentDescription = "Символ ${index + 1} на клавиатуре капчи"
                                }
                                .padding(8.dp),
                        ) {
                            if (image != null) {
                                Image(
                                    bitmap = image,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (busy) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.padding(top = 8.dp).size(16.dp),
                        )
                    }
                    TextButton(onClick = onRefresh, enabled = !busy, shape = RoundedCornerShape(2.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Другая капча", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

/** PNG из base64 → bitmap. Декодируем один раз на строку, а не на каждый кадр. */
@Composable
private fun rememberDecodedImage(encoded: String): ImageBitmap? = remember(encoded) {
    runCatching {
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()
}

/** Заголовок секции — серая полоса, как в настройках. */
@Composable
private fun SectionTitle(text: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 0.8.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}
