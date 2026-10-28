package dev.dvach.client.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dvach.client.BuildConfig
import dev.dvach.client.data.settings.ThemeMode
import dev.dvach.client.ui.components.dvachTopAppBarColors
import dev.dvach.client.ui.components.HtmlText
import dev.dvach.client.util.THUMBNAIL_SCALE_MAX
import dev.dvach.client.util.THUMBNAIL_SCALE_MIN
import dev.dvach.client.util.THUMBNAIL_SCALE_STEP
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = dvachTopAppBarColors(),
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
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
                percent = settings.thumbnailScalePercent,
                enabled = settings.showThumbnails,
                onChange = viewModel::setThumbnailScale,
            )

            SwitchRow(
                title = "Раскрывать спойлеры",
                subtitle = "Скрытый текст показывается сразу, без тапа по плашке",
                checked = settings.autoRevealSpoilers,
                onCheckedChange = viewModel::setAutoRevealSpoilers,
            )

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
    }
}

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
    percent: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Размер превью",
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
            text = "От $THUMBNAIL_SCALE_MIN до $THUMBNAIL_SCALE_MAX %, " +
                "действует в списках тредов и внутри тредов",
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
