package io.github.kashimkhv.vibe2ch.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.kashimkhv.vibe2ch.BuildConfig
import io.github.kashimkhv.vibe2ch.data.cache.CacheUsage
import io.github.kashimkhv.vibe2ch.data.settings.FontChoice
import io.github.kashimkhv.vibe2ch.data.update.UpdateInfo
import io.github.kashimkhv.vibe2ch.domain.GITHUB_REPO_URL
import io.github.kashimkhv.vibe2ch.ui.components.dvachShadow
import io.github.kashimkhv.vibe2ch.ui.components.dvachTopAppBarColors
import io.github.kashimkhv.vibe2ch.ui.components.DvachScrollbar
import io.github.kashimkhv.vibe2ch.ui.components.HtmlText
import io.github.kashimkhv.vibe2ch.ui.theme.fontFamily
import io.github.kashimkhv.vibe2ch.util.FONT_SCALE_MAX
import io.github.kashimkhv.vibe2ch.util.FONT_SCALE_MIN
import io.github.kashimkhv.vibe2ch.util.FONT_SCALE_STEP
import io.github.kashimkhv.vibe2ch.util.LINE_SPACING_MAX
import io.github.kashimkhv.vibe2ch.util.LINE_SPACING_MIN
import io.github.kashimkhv.vibe2ch.util.LINE_SPACING_STEP
import io.github.kashimkhv.vibe2ch.util.TEXT_MARGIN_MAX
import io.github.kashimkhv.vibe2ch.util.TEXT_MARGIN_MIN
import io.github.kashimkhv.vibe2ch.util.TEXT_MARGIN_STEP
import io.github.kashimkhv.vibe2ch.util.THUMBNAIL_SCALE_MAX
import io.github.kashimkhv.vibe2ch.util.THUMBNAIL_SCALE_MIN
import io.github.kashimkhv.vibe2ch.util.TREE_INDENT_MAX
import io.github.kashimkhv.vibe2ch.util.TREE_INDENT_MIN
import io.github.kashimkhv.vibe2ch.util.TREE_INDENT_STEP
import io.github.kashimkhv.vibe2ch.util.canInstallPackages
import io.github.kashimkhv.vibe2ch.util.formatCacheSize
import io.github.kashimkhv.vibe2ch.util.formatFileSize
import io.github.kashimkhv.vibe2ch.util.installApk
import io.github.kashimkhv.vibe2ch.util.openInstallPermissionSettings
import io.github.kashimkhv.vibe2ch.util.plural
import io.github.kashimkhv.vibe2ch.util.releaseNotesText
import io.github.kashimkhv.vibe2ch.util.THUMBNAIL_SCALE_STEP
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenHiddenThreads: () -> Unit = {},
    onOpenThemes: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val hiddenCount by viewModel.hiddenCount.collectAsStateWithLifecycle()
    val cacheUsage by viewModel.cacheUsage.collectAsStateWithLifecycle()
    val cookiesCount by viewModel.cookiesCount.collectAsStateWithLifecycle()
    val update by viewModel.update.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var confirmClear by remember { mutableStateOf(false) }
    var confirmClearCookies by remember { mutableStateOf(false) }
    var fontPicker by remember { mutableStateOf<FontRole?>(null) }
    // Разрешение на установку выдаётся в системных настройках, поэтому его
    // надо перечитывать при возврате в приложение, а не только при открытии.
    var installAllowed by remember { mutableStateOf(canInstallPackages(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        installAllowed = canInstallPackages(context)
    }

    LaunchedEffect(update) {
        when (val state = update) {
            UpdateState.UpToDate -> {
                snackbarHostState.showSnackbar("У вас последняя версия — ${BuildConfig.VERSION_NAME}")
                viewModel.consumeUpdateResult()
            }
            is UpdateState.Failed -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.consumeUpdateResult()
            }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = dvachTopAppBarColors(),
                // Шапка чуть выступает над списком — как app bar в старом Material.
                modifier = Modifier.dvachShadow(),
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
            ) {
                SectionTitle("Оформление")

                LinkRow(
                    title = "Тема",
                    subtitle = settings.theme.label,
                    onClick = onOpenThemes,
                )

                HorizontalDivider()
                SectionTitle("Шрифты")

                FontsPreview()

                FontRow(
                    title = "Заголовки",
                    subtitle = "Шапки экранов, названия досок, темы постов",
                    choice = settings.headingFont,
                    onClick = { fontPicker = FontRole.HEADINGS },
                )

                TextScaleRow(
                    title = "Размер заголовков",
                    subtitle = "Кегль заголовков в процентах от обычного",
                    percent = settings.headingScalePercent,
                    onChange = viewModel::setHeadingScale,
                )

                SwitchRow(
                    title = "Жирные заголовки",
                    subtitle = "Жирное начертание для шапок, названий досок и тем постов",
                    checked = settings.headingBold,
                    onCheckedChange = viewModel::setHeadingBold,
                )

                FontRow(
                    title = "Мета-данные",
                    subtitle = "Ники, даты, номера постов, счётчики",
                    choice = settings.metaFont,
                    onClick = { fontPicker = FontRole.META },
                )

                TextScaleRow(
                    title = "Размер мета-данных",
                    subtitle = "Кегль ников, дат и счётчиков в процентах",
                    percent = settings.metaScalePercent,
                    onChange = viewModel::setMetaScale,
                )

                SwitchRow(
                    title = "Жирные мета-данные",
                    subtitle = "Жирное начертание для ников, дат и счётчиков",
                    checked = settings.metaBold,
                    onCheckedChange = viewModel::setMetaBold,
                )

                FontRow(
                    title = "Текст постов",
                    subtitle = "Сам текст постов и описания в настройках",
                    choice = settings.bodyFont,
                    onClick = { fontPicker = FontRole.BODY },
                )

                TextScaleRow(
                    title = "Размер текста постов",
                    subtitle = "Кегль текста постов в процентах от обычного",
                    percent = settings.bodyScalePercent,
                    onChange = viewModel::setBodyScale,
                )

                SwitchRow(
                    title = "Жирный текст постов",
                    subtitle = "Жирное начертание для самого текста постов",
                    checked = settings.bodyBold,
                    onCheckedChange = viewModel::setBodyBold,
                )

                HorizontalDivider()
                SectionTitle("Чтение")

                ScaleSliderRow(
                    title = "Межстрочный интервал",
                    subtitle = "Расстояние между строками текста постов; кегль не меняется",
                    value = settings.lineSpacingPercent,
                    range = LINE_SPACING_MIN..LINE_SPACING_MAX,
                    step = LINE_SPACING_STEP,
                    onChange = viewModel::setLineSpacing,
                )

                ScaleSliderRow(
                    title = "Поля текста",
                    subtitle = "Отступ слева и справа у текста постов и карточек ленты; " +
                        "превью остаются во всю ширину",
                    value = settings.textMarginsDp,
                    range = TEXT_MARGIN_MIN..TEXT_MARGIN_MAX,
                    step = TEXT_MARGIN_STEP,
                    unit = "dp",
                    onChange = viewModel::setTextMargins,
                )

                ScaleSliderRow(
                    title = "Отступ в дереве ответов",
                    subtitle = "Сдвиг каждого уровня вложенности; 0 — ответы в один " +
                        "столбец, без направляющих линий",
                    value = settings.treeIndentDp,
                    range = TREE_INDENT_MIN..TREE_INDENT_MAX,
                    step = TREE_INDENT_STEP,
                    unit = "dp",
                    onChange = viewModel::setTreeIndent,
                )

                SwitchRow(
                    title = "Относительные даты",
                    subtitle = "«5 мин назад» вместо точного времени в тредах и списке " +
                        "тредов; старше месяца — снова дата",
                    checked = settings.relativeDates,
                    onCheckedChange = viewModel::setRelativeDates,
                )

                HorizontalDivider()
                SectionTitle("Контент")

                SwitchRow(
                    title = "Превью картинок в тредах",
                    subtitle = "Выключите, если экономно расходуете трафик",
                    checked = settings.showThumbnails,
                    onCheckedChange = viewModel::setShowThumbnails,
                )

                ThumbnailScaleRow(
                    title = "Размер превью в тредах",
                    subtitle = "Картинки и видео внутри постов, база 150 dp",
                    percent = settings.thumbnailScalePercent,
                    enabled = settings.showThumbnails,
                    onChange = viewModel::setThumbnailScale,
                )

                ThumbnailScaleRow(
                    title = "Размер превью в списке тредов",
                    subtitle = "Вложения ОП-поста в ленте, база 75 dp — половина тредового",
                    percent = settings.listThumbnailScalePercent,
                    enabled = settings.showThumbnails,
                    onChange = viewModel::setListThumbnailScale,
                )

                SwitchRow(
                    title = "Крупные превью, если вложений мало",
                    subtitle = "Меньше трёх вложений — превью в списке тредов вдвое " +
                        "крупнее: карточка заполняется плотнее, без пустой половины строки",
                    checked = settings.enlargeFewListThumbnails,
                    enabled = settings.showThumbnails,
                    onCheckedChange = viewModel::setEnlargeFewListThumbnails,
                )

                SwitchRow(
                    title = "Обрезать превью до квадрата",
                    subtitle = "Выключите, чтобы картинка вписывалась целиком, — " +
                        "по краям останется фон",
                    checked = settings.cropThumbnails,
                    enabled = settings.showThumbnails,
                    onCheckedChange = viewModel::setCropThumbnails,
                )

                SwitchRow(
                    title = "Фон ячейки превью",
                    subtitle = "Подложка под картинкой; выключите, чтобы поля были " +
                        "цвета поста или карточки",
                    checked = settings.thumbnailBackground,
                    enabled = settings.showThumbnails,
                    onCheckedChange = viewModel::setThumbnailBackground,
                )

                SwitchRow(
                    title = "Листать медиа вертикально",
                    subtitle = "Как в TikTok: свайп вверх-вниз. Выключено — листание " +
                        "вбок, как в галерее",
                    checked = settings.verticalMediaPaging,
                    onCheckedChange = viewModel::setVerticalMediaPaging,
                )

                SwitchRow(
                    title = "Раскрывать спойлеры",
                    subtitle = "Скрытый текст показывается сразу, без тапа по плашке",
                    checked = settings.autoRevealSpoilers,
                    onCheckedChange = viewModel::setAutoRevealSpoilers,
                )

                HorizontalDivider()
                SectionTitle("Лента")

                LinkRow(
                    title = "Скрытые треды",
                    subtitle = if (hiddenCount == 0) {
                        "Ничего не скрыто: долгое нажатие на карточку треда скрывает его"
                    } else {
                        "Скрыто ${hiddenCount} ${plural(hiddenCount, "тред", "треда", "тредов")} — " +
                            "здесь их можно вернуть"
                    },
                    onClick = onOpenHiddenThreads,
                )

                HorizontalDivider()
                SectionTitle("Сеть")

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Куки",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = if (cookiesCount == 0) {
                                "нет"
                            } else {
                                "$cookiesCount ${plural(cookiesCount, "штука", "штуки", "штук")}"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = "Движок выдаёт их после поста и по ним открывает закрытые " +
                            "доски. Сохраняются в файле и переживают перезапуск — без " +
                            "них каждый запрос уходил бы как новый аноним.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { confirmClearCookies = true },
                        enabled = cookiesCount > 0,
                        // Углы задаём явно: кнопки Material3 — «пилюли», а в
                        // остальном интерфейсе углы почти прямые.
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .dvachShadow(2.dp, RoundedCornerShape(2.dp)),
                    ) {
                        Text("Стереть куки")
                    }
                }

                HorizontalDivider()
                SectionTitle("Кэш")

                CacheRow(usage = cacheUsage)

                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    Text(
                        text = "Картинки, превью и ответы API. Закладки, настройки и " +
                            "скачанные файлы очистка не затрагивает.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { confirmClear = true },
                        enabled = !cacheUsage.isEmpty,
                        // Углы задаём явно: кнопки Material3 — «пилюли», а в
                        // остальном интерфейсе углы почти прямые.
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .dvachShadow(2.dp, RoundedCornerShape(2.dp)),
                    ) {
                        Text("Очистить кэш")
                    }
                }

                HorizontalDivider()
                SectionTitle("О приложении")

                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(
                        text = "Клиент для двача · ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    HtmlText(
                        html = "Данные: <a href=\"https://2ch.hk\">2ch.hk</a> (JSON API Makaba). " +
                            "Вдохновлён <a href=\"https://github.com/vortexwolf/2ch-Browser\">2ch-Browser</a>.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(
                        text = "Java 2019 → Kotlin/Compose 2026",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                HorizontalDivider()

                LinkRow(
                    title = "GitHub",
                    subtitle = "Исходный код, релизы и список изменений",
                    onClick = { runCatching { uriHandler.openUri(GITHUB_REPO_URL) } },
                )

                HorizontalDivider()

                LinkRow(
                    title = "Проверить обновления",
                    subtitle = updateSubtitle(update),
                    onClick = viewModel::checkForUpdates,
                )
            }

            // Полоса прокрутки поверх списка: правый край экрана.
            DvachScrollbar(
                state = scrollState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(),
            )
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            shape = RoundedCornerShape(2.dp),
            title = { Text("Очистить кэш?") },
            text = {
                Text(
                    "Картинки, превью и ответы API будут скачаны заново. " +
                        "Закладки, настройки и сохранённые файлы останутся.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        viewModel.clearCache()
                        scope.launch { snackbarHostState.showSnackbar("Кэш очищен") }
                    },
                ) {
                    Text("Очистить")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Отмена") }
            },
        )
    }

    if (confirmClearCookies) {
        AlertDialog(
            onDismissRequest = { confirmClearCookies = false },
            shape = RoundedCornerShape(2.dp),
            title = { Text("Стереть куки?") },
            text = {
                Text(
                    "Движок забудет, что вы уже получали доступ: закрытые доски " +
                        "снова ответят «не найдено», пока доступ не выдадут заново. " +
                        "Закладки, настройки и кэш останутся на месте.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClearCookies = false
                        viewModel.clearCookies()
                        scope.launch { snackbarHostState.showSnackbar("Куки стёрты") }
                    },
                ) {
                    Text("Стереть")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearCookies = false }) { Text("Отмена") }
            },
        )
    }

    fontPicker?.let { role ->
        FontPickerDialog(
            role = role,
            selected = when (role) {
                FontRole.HEADINGS -> settings.headingFont
                FontRole.META -> settings.metaFont
                FontRole.BODY -> settings.bodyFont
            },
            onSelect = { font ->
                when (role) {
                    FontRole.HEADINGS -> viewModel.setHeadingFont(font)
                    FontRole.META -> viewModel.setMetaFont(font)
                    FontRole.BODY -> viewModel.setBodyFont(font)
                }
                fontPicker = null
            },
            onDismiss = { fontPicker = null },
        )
    }

    UpdateDialog(
        state = update,
        installAllowed = installAllowed,
        onDownload = viewModel::downloadUpdate,
        onDismiss = viewModel::dismissUpdate,
        onInstall = { file ->
            if (installAllowed) {
                runCatching { installApk(context, file) }
                    .onSuccess { viewModel.dismissUpdate() }
                    .onFailure {
                        scope.launch { snackbarHostState.showSnackbar("Не удалось открыть установщик") }
                    }
            } else {
                openInstallPermissionSettings(context)
            }
        },
    )
}

/** Подпись пункта «Проверить обновления»: что происходит прямо сейчас. */
private fun updateSubtitle(state: UpdateState): String = when (state) {
    UpdateState.Idle -> "Текущая версия ${BuildConfig.VERSION_NAME}"
    UpdateState.Checking -> "Проверяем GitHub…"
    UpdateState.UpToDate -> "Обновлений нет — ${BuildConfig.VERSION_NAME}"
    is UpdateState.Available -> "Доступна версия ${state.info.version}"
    is UpdateState.Downloading -> state.progress?.let { progress ->
        "Скачиваем ${state.info.version}: ${(progress * 100).roundToInt()}%"
    } ?: "Скачиваем ${state.info.version}…"
    is UpdateState.Downloaded -> "Версия ${state.info.version} скачана"
    is UpdateState.Failed -> "Не удалось — попробуйте ещё раз"
}

/**
 * Диалог обновления: предложение скачать, полоса загрузки и кнопка установки.
 * Показывается только для трёх состояний — проверка и её результат живут
 * в снекбаре и подписи пункта, чтобы не перекрывать экран на пустом месте.
 */
@Composable
private fun UpdateDialog(
    state: UpdateState,
    installAllowed: Boolean,
    onDownload: (UpdateInfo) -> Unit,
    onDismiss: () -> Unit,
    onInstall: (File) -> Unit,
) {
    val info = when (state) {
        is UpdateState.Available -> state.info
        is UpdateState.Downloading -> state.info
        is UpdateState.Downloaded -> state.info
        else -> return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(2.dp),
        title = {
            Text(
                when (state) {
                    is UpdateState.Downloaded -> "Версия ${info.version} скачана"
                    else -> "Доступна версия ${info.version}"
                },
            )
        },
        text = {
            Column {
                Text(
                    text = "У вас ${BuildConfig.VERSION_NAME} · ${formatFileSize(info.sizeBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when (state) {
                    is UpdateState.Downloading -> {
                        Text(
                            text = state.progress?.let { "Скачиваем APK: ${(it * 100).roundToInt()}%" }
                                ?: "Скачиваем APK…",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        // Пока байты не пошли, полоса «бежит»: соединение и
                        // редирект на CDN занимают несколько секунд, и полоска,
                        // застывшая на нуле, выглядела бы как зависание.
                        val progress = state.progress
                        if (progress == null) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                            )
                        } else {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                            )
                        }
                    }
                    is UpdateState.Downloaded -> {
                        Text(
                            text = if (installAllowed) {
                                "Откроется системный установщик. Настройки, закладки " +
                                    "и история сохранятся."
                            } else {
                                "Android не разрешает установку из этого приложения. " +
                                    "Разрешите её в настройках и вернитесь сюда — кнопка " +
                                    "сменится на «Установить»."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                    else -> {
                        val notes = releaseNotesText(info.notes)
                        if (notes.isNotBlank()) {
                            val notesScroll = rememberScrollState()
                            Box(
                                modifier = Modifier
                                    .padding(top = 10.dp)
                                    .heightIn(max = 260.dp),
                            ) {
                                Text(
                                    text = notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .verticalScroll(notesScroll)
                                        .padding(end = 12.dp),
                                )
                                DvachScrollbar(
                                    state = notesScroll,
                                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (state) {
                        is UpdateState.Available -> onDownload(info)
                        is UpdateState.Downloaded -> onInstall(state.file)
                        else -> Unit
                    }
                },
                enabled = state !is UpdateState.Downloading,
            ) {
                Text(
                    when (state) {
                        is UpdateState.Downloaded -> if (installAllowed) "Установить" else "Разрешить"
                        else -> "Скачать"
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (state is UpdateState.Downloading) "Отмена" else "Позже")
            }
        },
    )
}

/**
 * Сколько занято и из какого предела: два числа рядом с кнопкой очистки,
 * чтобы было видно, что именно освободится.
 */
@Composable
private fun CacheRow(usage: CacheUsage) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Занято",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatCacheSize(usage.totalBytes),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = cacheDetails(usage),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** «Картинки: 12.4 МБ из 256.0 МБ · сеть: 3.1 МБ из 64.0 МБ». */
internal fun cacheDetails(usage: CacheUsage): String =
    "Картинки: ${withLimit(usage.imagesBytes, usage.imagesLimitBytes)}" +
        " · сеть: ${withLimit(usage.networkBytes, usage.networkLimitBytes)}"

private fun withLimit(bytes: Long, limitBytes: Long): String =
    if (limitBytes > 0) "${formatCacheSize(bytes)} из ${formatFileSize(limitBytes)}" else formatCacheSize(bytes)

/**
 * Живой пример: три роли рядом, каждая своим шрифтом. Показывать список
 * названий без образца бессмысленно — по названию не понять, как шрифт
 * выглядит в русском тексте.
 */
@Composable
private fun FontsPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            text = "Тред про шрифты",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Аноним · 31.10.26 03:43 · №337842318",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Съешь ещё этих мягких французских булок, да выпей чаю. " +
                "Так видно, как шрифт держит кириллицу: 0123456789.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/**
 * Строка выбора шрифта для одной роли. Текущий вариант подписан своим же
 * шрифтом — сразу видно, что выбрано, не открывая список.
 */
@Composable
private fun FontRow(
    title: String,
    subtitle: String,
    choice: FontChoice,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = choice.title,
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = choice.fontFamily),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/**
 * Строка-переход в другой экран: заголовок, пояснение и шеврон справа.
 * В старом Material так выглядели пункты, которые открывают новый список.
 */
@Composable
private fun LinkRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/** Варианты шрифтов с образцом: при выборе сразу применяются и диалог закрывается. */
@Composable
private fun FontPickerDialog(
    role: FontRole,
    selected: FontChoice,
    onSelect: (FontChoice) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(2.dp),
        title = { Text("Шрифт: ${role.title.lowercase()}") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                FontChoice.entries.forEach { font ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = font == selected,
                                role = Role.RadioButton,
                                onClick = { onSelect(font) },
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = font == selected, onClick = null)
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = font.title,
                                style = MaterialTheme.typography.bodyLarge
                                    .copy(fontFamily = font.fontFamily),
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = FONT_SAMPLE,
                                style = MaterialTheme.typography.bodySmall
                                    .copy(fontFamily = font.fontFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = font.note,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        },
    )
}

/** Роль шрифта: заголовки, мета-данные или текст постов. */
private enum class FontRole(val title: String) {
    HEADINGS("Заголовки"),
    META("Мета-данные"),
    BODY("Текст постов"),
}

/** Образец для списка шрифтов: кириллица, цифры и знаки препинания. */
private const val FONT_SAMPLE = "Съешь ещё этих мягких французских булок — 0123456789"

/**
 * Заголовок раздела — серая полоса во всю ширину, как подзаголовки списков
 * в старом Material. Плашка отделяет блок настроек от предыдущего, не
 * добавляя лишних отступов.
 */
@Composable
private fun SectionTitle(text: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun ThumbnailScaleRow(
    title: String,
    subtitle: String,
    percent: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit,
) {
    ScaleSliderRow(
        title = title,
        subtitle = subtitle,
        value = percent,
        range = THUMBNAIL_SCALE_MIN..THUMBNAIL_SCALE_MAX,
        step = THUMBNAIL_SCALE_STEP,
        enabled = enabled,
        onChange = onChange,
    )
}

/** Размер шрифта одной роли: проценты от обычного кегля. */
@Composable
private fun TextScaleRow(
    title: String,
    subtitle: String,
    percent: Int,
    onChange: (Int) -> Unit,
) {
    ScaleSliderRow(
        title = title,
        subtitle = subtitle,
        value = percent,
        range = FONT_SCALE_MIN..FONT_SCALE_MAX,
        step = FONT_SCALE_STEP,
        onChange = onChange,
    )
}

/**
 * Общий вид ползунка размера: подпись, текущее значение и дорожка. Размер
 * превью и шрифта измеряется в процентах, поля текста — в dp, поэтому единица
 * измерения вынесена в параметр.
 */
@Composable
private fun ScaleSliderRow(
    title: String,
    subtitle: String,
    value: Int,
    range: IntRange,
    step: Int,
    unit: String = "%",
    enabled: Boolean = true,
    onChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$value $unit",
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Text(
            text = "$subtitle. От ${range.first} до ${range.last} $unit",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first) / step - 1,
            enabled = enabled,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
