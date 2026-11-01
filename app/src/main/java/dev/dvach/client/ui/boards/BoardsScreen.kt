package dev.dvach.client.ui.boards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dvach.client.domain.model.Board
import dev.dvach.client.ui.components.EmptyBox
import dev.dvach.client.ui.components.ErrorBox
import dev.dvach.client.ui.components.LoadingBox
import dev.dvach.client.ui.components.dvachShadow
import dev.dvach.client.ui.components.dvachTopAppBarColors
import dev.dvach.client.ui.components.DvachScrollbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardsScreen(
    onBoardClick: (Board) -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: BoardsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = dvachTopAppBarColors(),
                // Шапка чуть выступает над списком — как app bar в старом Material.
                modifier = Modifier.dvachShadow(),
                title = { Text("Двач") },
                actions = {
                    IconButton(onClick = onOpenBookmarks) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Избранное")
                    }
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Поиск в старом Material — не «пилюля», а поле с подчёркиванием
            // в цвет акцента: видно, куда вводить текст, и не режет глаз
            // скруглением на квадратном экране.
            TextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Поиск доски: b, tech, музыка…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(0.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                    disabledIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )

            if (state.isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when {
                state.isLoading -> LoadingBox()
                state.error != null && state.boards.isEmpty() ->
                    ErrorBox(message = state.error.orEmpty(), onRetry = viewModel::load)
                state.visibleBoards.isEmpty() -> EmptyBox("Ничего не найдено")
                else -> BoardsList(
                    state = state,
                    onBoardClick = onBoardClick,
                    onTogglePin = viewModel::togglePin,
                )
            }
        }
    }
}

@Composable
private fun BoardsList(
    state: BoardsUiState,
    onBoardClick: (Board) -> Unit,
    onTogglePin: (Board) -> Unit,
) {
    val pinnedIds = state.pinnedBoardIds.toSet()
    val listState = rememberLazyListState()
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            // Закреплённые — всегда первыми: до нужной доски не надо листать категории.
            if (state.pinnedBoards.isNotEmpty()) {
                item(key = "header-pinned") {
                    GroupHeader(text = "Закреплённые", highlighted = true)
                }
                items(count = state.pinnedBoards.size, key = { "pinned-${state.pinnedBoards[it].id}" }) { index ->
                    val board = state.pinnedBoards[index]
                    BoardRow(
                        board = board,
                        pinned = true,
                        onClick = { onBoardClick(board) },
                        onTogglePin = { onTogglePin(board) },
                    )
                }
            }
            state.grouped.forEach { (category, boards) ->
                // Закреплённые уже стоят в начале списка — второй раз в категории
                // они не нужны, иначе одна доска видна дважды.
                val rest = boards.filter { it.id !in pinnedIds }
                if (rest.isEmpty()) return@forEach
                item(key = "header-$category") {
                    GroupHeader(text = category)
                }
                items(count = rest.size, key = { rest[it].id }) { index ->
                    val board = rest[index]
                    BoardRow(
                        board = board,
                        pinned = false,
                        onClick = { onBoardClick(board) },
                        onTogglePin = { onTogglePin(board) },
                    )
                }
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

/**
 * Заголовок группы — серая полоса во всю ширину с подписью в верхнем
 * регистре, как подзаголовки списков в Material Design 1–2.
 */
@Composable
private fun GroupHeader(text: String, highlighted: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (highlighted) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 0.8.sp,
                color = if (highlighted) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun BoardRow(
    board: Board,
    pinned: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Ярлык доски: не «пилюля», а прямоугольная плашка-акцент.
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(2.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.widthIn(min = 52.dp).padding(horizontal = 8.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = "/${board.id}/",
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = board.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (board.info.isNotBlank()) {
                    Text(
                        text = board.info,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onTogglePin) {
                Icon(
                    imageVector = if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = if (pinned) "Открепить доску" else "Закрепить доску",
                    tint = if (pinned) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}
