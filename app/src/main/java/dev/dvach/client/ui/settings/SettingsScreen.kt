package dev.dvach.client.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dvach.client.BuildConfig
import dev.dvach.client.data.cache.CacheUsage
import dev.dvach.client.data.settings.ThemeMode
import dev.dvach.client.ui.components.dvachShadow
import dev.dvach.client.ui.components.dvachTopAppBarColors
import dev.dvach.client.ui.components.DvachScrollbar
import dev.dvach.client.ui.components.HtmlText
import dev.dvach.client.util.THUMBNAIL_SCALE_MAX
import dev.dvach.client.util.THUMBNAIL_SCALE_MIN
import dev.dvach.client.util.formatCacheSize
import dev.dvach.client.util.formatFileSize
import dev.dvach.client.util.THUMBNAIL_SCALE_STEP
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val cacheUsage by viewModel.cacheUsage.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmClear by remember { mutableStateOf(false) }

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

                Column(modifier = Modifier.selectableGroup()) {
                    ThemeOption(
                        label = "Как в системе",
                        selected = settings.themeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                    )
                    ThemeOption(
                        label = "Светлая",
                        selected = settings.themeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                    )
                    ThemeOption(
                        label = "Тёмная",
                        selected = settings.themeMode == ThemeMode.DARK,
                        onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                    )
                }

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
                    title = "Обрезать превью до квадрата",
                    subtitle = "Выключите, чтобы картинка вписывалась целиком, — " +
                        "по краям останется фон",
                    checked = settings.cropThumbnails,
                    enabled = settings.showThumbnails,
                    onCheckedChange = viewModel::setCropThumbnails,
                )

                SwitchRow(
                    title = "Раскрывать спойлеры",
                    subtitle = "Скрытый текст показывается сразу, без тапа по плашке",
                    checked = settings.autoRevealSpoilers,
                    onCheckedChange = viewModel::setAutoRevealSpoilers,
                )

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
                            "Код открыт: <a href=\"https://github.com/vortexwolf/2ch-Browser\">2ch-Browser</a>.",
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
                fontWeight = FontWeight.Medium,
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
private fun ThemeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp),
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
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$percent %",
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Text(
            text = "$subtitle. От $THUMBNAIL_SCALE_MIN до $THUMBNAIL_SCALE_MAX %",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = percent.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = THUMBNAIL_SCALE_MIN.toFloat()..THUMBNAIL_SCALE_MAX.toFloat(),
            steps = (THUMBNAIL_SCALE_MAX - THUMBNAIL_SCALE_MIN) / THUMBNAIL_SCALE_STEP - 1,
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
