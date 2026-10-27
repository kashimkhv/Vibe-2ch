package dev.dvach.client.ui.threads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.dvach.client.domain.model.ThreadSummary
import dev.dvach.client.ui.components.EmptyBox
import dev.dvach.client.ui.components.ErrorBox
import dev.dvach.client.ui.components.HtmlText
import dev.dvach.client.ui.components.LoadingBox
import dev.dvach.client.util.filesLabel
import dev.dvach.client.util.formatCount
import dev.dvach.client.util.postsLabel
import dev.dvach.client.util.stripHtml

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ThreadsScreen(
    onBack: () -> Unit,
    onThreadClick: (boardId: String, boardName: String, threadNum: Int) -> Unit,
    viewModel: ThreadsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("/${state.boardId}/", fontFamily = FontFamily.Monospace)
                        Text(
                            text = state.boardName,
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
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                },
            )
        },
        bottomBar = {
            PageBar(
                page = state.page,
                canGoNext = state.canGoNext,
                onPrevious = viewModel::previousPage,
                onNext = viewModel::nextPage,
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isLoading && state.threads.isNotEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            when {
                state.isLoading && state.threads.isEmpty() -> LoadingBox()
                state.error != null && state.threads.isEmpty() ->
                    ErrorBox(message = state.error.orEmpty(), onRetry = viewModel::refresh)
                state.threads.isEmpty() -> EmptyBox("На доске пока нет тредов")
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = state.threads, key = { it.num }) { thread ->
                        ThreadCard(
                            thread = thread,
                            onClick = {
                                onThreadClick(thread.boardId, state.boardName, thread.num)
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadCard(thread: ThreadSummary, onClick: () -> Unit) {
    val plainComment = remember(thread.commentHtml) { stripHtml(thread.commentHtml) }
    val subject = thread.subject.trim()
    val showSubject = subject.isNotBlank() && !plainComment.startsWith(subject, ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Вложения ОП-поста идут перед текстом, как и в самом треде.
        // У ОП-поста бывает несколько файлов — показываем превью каждого.
        val thumbnails = thread.attachments.map { it.thumbnailUrl }.filter { it.isNotBlank() }
        if (thumbnails.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                thumbnails.forEach { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (showSubject) {
                Text(
                    text = thread.subject.ifBlank { "Без темы" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            HtmlText(
                html = thread.commentHtml,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                linksEnabled = false,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = postsLabel(thread.postsCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (thread.filesCount > 0) {
                    Text(
                        text = filesLabel(thread.filesCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (thread.views > 0) {
                    Text(
                        text = "${formatCount(thread.views)} просм.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (thread.isSticky) {
                    Text(
                        text = "закреплён",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (thread.isClosed) {
                    Text(
                        text = "закрыт",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun PageBar(
    page: Int,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(tonalElevation = 3.dp, modifier = Modifier.navigationBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onPrevious, enabled = page > 0) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Предыдущая страница")
            }
            Text(
                text = "Страница ${page + 1}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.width(120.dp),
            )
            IconButton(onClick = onNext, enabled = canGoNext) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Следующая страница")
            }
        }
    }
}
