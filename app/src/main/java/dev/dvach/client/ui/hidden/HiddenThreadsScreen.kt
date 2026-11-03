package dev.dvach.client.ui.hidden

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dvach.client.data.local.HiddenThreadEntity
import dev.dvach.client.ui.components.dvachShadow
import dev.dvach.client.ui.components.dvachTopAppBarColors
import dev.dvach.client.ui.components.DvachScrollbar
import dev.dvach.client.ui.components.EmptyBox
import dev.dvach.client.util.formatRelativeTime

/**
 * Скрытые треды: сюда попадает всё, что спрятали долгим нажатием в ленте.
 * Отсюда же тред возвращается — по одному или все сразу.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenThreadsScreen(
    onBack: () -> Unit,
    viewModel: HiddenThreadsViewModel = hiltViewModel(),
) {
    val hidden by viewModel.hidden.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var confirmRestoreAll by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = dvachTopAppBarColors(),
                // Шапка чуть выступает над списком — как app bar в старом Material.
                modifier = Modifier.dvachShadow(),
                title = { Text("Скрытые треды") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    // Действие появляется только когда есть что возвращать.
                    if (hidden.isNotEmpty()) {
                        TextButton(onClick = { confirmRestoreAll = true }) {
                            Text("Вернуть все")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (hidden.isEmpty()) {
            EmptyBox(
                "Ничего не скрыто. Долгое нажатие на карточку треда в ленте скрывает " +
                    "его, а вернуть можно здесь.",
                modifier = Modifier.padding(padding),
            )
        } else {
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(items = hidden, key = { "${it.boardId}/${it.threadNum}" }) { thread ->
                        HiddenThreadRow(
                            thread = thread,
                            onRestore = { viewModel.restore(thread) },
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

    if (confirmRestoreAll) {
        AlertDialog(
            onDismissRequest = { confirmRestoreAll = false },
            shape = RoundedCornerShape(2.dp),
            title = { Text("Вернуть все треды?") },
            text = {
                Text(
                    "Все скрытые треды снова появятся в лентах своих досок. " +
                        "Сами треды от этого не меняются.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRestoreAll = false
                        viewModel.restoreAll()
                    },
                ) {
                    Text("Вернуть все")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestoreAll = false }) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun HiddenThreadRow(
    thread: HiddenThreadEntity,
    onRestore: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = thread.subject.ifBlank { "№${thread.threadNum}" },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "/${thread.boardId}/ №${thread.threadNum} · скрыт " +
                        formatRelativeTime(thread.hiddenAt / 1000),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRestore) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Вернуть тред",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}
