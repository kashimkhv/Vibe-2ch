package dev.dvach.client.ui.thread

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.dvach.client.domain.model.Attachment
import dev.dvach.client.domain.model.Post
import dev.dvach.client.ui.components.HtmlText
import dev.dvach.client.util.formatFileSize
import dev.dvach.client.util.stripHtml
import dev.dvach.client.util.formatTimestamp

@Composable
fun PostCard(
    post: Post,
    isHighlighted: Boolean,
    onPostClick: (postNum: Int, threadNum: Int?) -> Unit,
    onOpenAttachment: (Attachment) -> Unit,
    showThumbnails: Boolean = true,
) {
    val plainComment = remember(post.commentHtml) { stripHtml(post.commentHtml) }
    val subject = post.subject.trim()
    val showSubject = subject.isNotBlank() && !plainComment.startsWith(subject, ignoreCase = true)
    val container = when {
        isHighlighted -> MaterialTheme.colorScheme.secondaryContainer
        post.isOp -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surface
    }

    Surface(color = container, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = post.name.ifBlank { "Аноним" },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (post.isOp) FontWeight.Bold else FontWeight.Medium,
                    color = if (post.isOp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 180.dp),
                )
                post.trip?.let { trip ->
                    Text(
                        text = " !$trip",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                    )
                }
                Box(modifier = Modifier.weight(1f))
                Text(
                    text = post.num.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = formatTimestamp(post.timestamp).ifBlank { post.date },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (showThumbnails && post.files.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    post.files.forEach { file ->
                        AttachmentView(file = file, onOpen = { onOpenAttachment(file) })
                    }
                }
            }

            if (showSubject) {
                Text(
                    text = subject,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (plainComment.isNotBlank()) {
                HtmlText(
                    html = post.commentHtml,
                    onPostClick = onPostClick,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (post.isBanned) {
                Text(
                    text = "Пост забанен",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (!post.isOp) {
                HorizontalDivider(modifier = Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun AttachmentView(file: Attachment, onOpen: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.clickable(onClick = onOpen),
    ) {
        if (file.thumbnailUrl.isNotBlank()) {
            AsyncImage(
                model = file.thumbnailUrl,
                contentDescription = file.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .height(96.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Text(
                    text = if (file.isVideo) "▶ ВИДЕО" else "ФАЙЛ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = buildString {
                append(file.name)
                if (file.sizeBytes > 0) append("  ${formatFileSize(file.sizeBytes)}")
                file.duration?.let { append("  $it") }
                if (file.width > 0 && file.height > 0) append("  ${file.width}×${file.height}")
                if (file.isVideo) {
                    append("  ")
                    append(file.extension.ifBlank { "webm" }.uppercase())
                }
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
