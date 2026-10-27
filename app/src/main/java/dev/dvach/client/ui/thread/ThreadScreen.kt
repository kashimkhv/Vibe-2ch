package dev.dvach.client.ui.thread

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import dev.dvach.client.domain.model.Post
import dev.dvach.client.ui.components.ErrorBox
import dev.dvach.client.ui.components.LoadingBox
import dev.dvach.client.util.postUrl
import dev.dvach.client.util.postsLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadScreen(
    onBack: () -> Unit,
    viewModel: ThreadViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val bookmarked by viewModel.isBookmarked.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    var highlighted by remember { mutableIntStateOf(-1) }

    val boardId = viewModel.route.boardId
    val threadNum = viewModel.route.threadNum

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "/$boardId/ №$threadNum",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (state.posts.isEmpty()) "Тред" else postsLabel(state.posts.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleBookmark) {
                        Icon(
                            imageVector = if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (bookmarked) "Убрать из избранного" else "В избранное",
                        )
                    }
                    IconButton(onClick = { uriHandler.runCatching { openUri(postUrl(boardId, threadNum)) } }) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = "Открыть в браузере")
                    }
                    IconButton(onClick = viewModel::load) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading && state.posts.isEmpty() -> LoadingBox()
                state.error != null && state.posts.isEmpty() ->
                    ErrorBox(message = state.error.orEmpty(), onRetry = viewModel::load)
                else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(items = state.posts, key = { it.num }) { post ->
                        PostCard(
                            post = post,
                            isHighlighted = post.num == highlighted,
                            showThumbnails = settings.showThumbnails,
                            onPostClick = { ref ->
                                val index = state.posts.indexOfFirst { it.num == ref }
                                if (index >= 0) {
                                    highlighted = ref
                                    scope.launch { listState.animateScrollToItem(index) }
                                }
                            },
                            onOpenAttachment = { url ->
                                uriHandler.runCatching { openUri(url) }
                            },
                            onCopyLink = { p ->
                                copyToClipboard(context, postUrl(boardId, threadNum, p.num))
                            },
                            onOpenInBrowser = { p ->
                                uriHandler.runCatching { openUri(postUrl(boardId, threadNum, p.num)) }
                            },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Ссылка", text))
    Toast.makeText(context, "Ссылка скопирована", Toast.LENGTH_SHORT).show()
}
