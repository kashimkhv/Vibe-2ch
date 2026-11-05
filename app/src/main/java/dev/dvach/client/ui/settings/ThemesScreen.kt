package dev.dvach.client.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dvach.client.data.settings.DvachTheme
import dev.dvach.client.ui.components.DvachScrollbar
import dev.dvach.client.ui.components.dvachShadow
import dev.dvach.client.ui.components.dvachTopAppBarColors
import dev.dvach.client.ui.theme.DvachPalette
import dev.dvach.client.ui.theme.palette

/**
 * Выбор темы. Список устроен как меню в старом Material: слева цветной
 * образец «шапка + строка + ОП», справа название и одна строка о том, зачем
 * эта тема нужна. Подпись важнее образца: по четырём квадратикам трудно
 * понять, чем Amoled отличается от Monet, а по описанию — сразу.
 *
 * Тема применяется сразу по нажатию: экран перекрашивается вместе со всем
 * приложением, поэтому результат виден без «Сохранить».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemesScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val systemDark = isSystemInDarkTheme()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = dvachTopAppBarColors(),
                // Шапка чуть выступает над списком — как app bar в старом Material.
                modifier = Modifier.dvachShadow(),
                title = { Text("Темы") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        text = "Тема меняет цвета шапки, строк, ссылок и ОП-поста. " +
                            "Вёрстка, шрифты и размеры остаются прежними.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }

                items(items = DvachTheme.entries, key = { it.id }) { theme ->
                    // Образец рисуется цветами самой темы, а не текущей:
                    // иначе все строки выглядели бы одинаково.
                    val palette = remember(theme, systemDark) { theme.palette(systemDark) }
                    ThemeRow(
                        theme = theme,
                        palette = palette,
                        selected = settings.theme == theme,
                        onClick = { viewModel.setTheme(theme) },
                    )
                }
            }

            // Полоса прокрутки поверх списка: правый край экрана.
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
private fun ThemeRow(
    theme: DvachTheme,
    palette: DvachPalette,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Выбранная тема подсвечена тоном акцента, а не серой плашкой:
                // на тёмных темах серая плашка почти не отличается от строки.
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                )
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThemeSwatch(palette)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = theme.label,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = theme.hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Выбрана",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 12.dp),
                )
            } else {
                // Пустое место под галочку: названия не прыгают влево-вправо
                // при смене выбора.
                Spacer(modifier = Modifier.width(36.dp))
            }
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

/**
 * Образец темы размером с иконку списка: полоса шапки, строка поста с
 * акцентом и подложка ОП. Четыре цвета из палитры — ровно те, по которым
 * темы и различаются, поэтому образец не занимает пол-экрана.
 */
@Composable
private fun ThemeSwatch(palette: DvachPalette) {
    Column(
        modifier = Modifier
            .size(44.dp)
            .background(palette.surface)
            .border(1.dp, palette.divider),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(palette.header),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 5.dp, vertical = 4.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(palette.text),
            )
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(2.dp)
                    .background(palette.muted),
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(palette.accent),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .background(palette.op),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .background(palette.op),
        )
    }
}
