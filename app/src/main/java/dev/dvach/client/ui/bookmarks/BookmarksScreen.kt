package dev.dvach.client.ui.bookmarks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dvach.client.data.local.BookmarkEntity
import dev.dvach.client.ui.components.EmptyBox
import dev.dvach.client.util.formatRelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    onBack: () -> Unit,
    onOpenThread: (boardId: String, threadNum: Int) -> Unit,
    viewModel: BookmarksViewModel = hiltViewModel(),
) {
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Избранное") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        if (bookmarks.isEmpty()) {
            EmptyBox("Пока пусто. Открывайте треды и добавляйте их в избранное.", modifier = Modifier.padding(padding))
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(items = bookmarks, key = { it.id }) { bookmark ->
                    BookmarkRow(
                        bookmark = bookmark,
                        onClick = { onOpenThread(bookmark.boardId, bookmark.threadNum) },
                        onRemove = { viewModel.remove(bookmark) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun BookmarkRow(
    bookmark: BookmarkEntity,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = bookmark.subject.ifBlank { "№${bookmark.threadNum}" },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "/${bookmark.boardId}/ №${bookmark.threadNum} · ${formatRelativeTime(bookmark.createdAt / 1000)}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
        }
    }
}
