package dev.dvach.client.ui.thread

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.dvach.client.domain.model.Attachment
import dev.dvach.client.domain.model.Post
import dev.dvach.client.ui.components.AttachmentThumbsRow
import dev.dvach.client.ui.components.HtmlText
import dev.dvach.client.util.THUMBNAIL_SCALE_DEFAULT
import dev.dvach.client.util.formatFileSize
import dev.dvach.client.util.formatTimestamp
import dev.dvach.client.util.scaleThumbnail
import dev.dvach.client.util.stripHtml

@Composable
fun PostCard(
    post: Post,
    isHighlighted: Boolean,
    onPostClick: (postNum: Int, threadNum: Int?) -> Unit,
    onOpenAttachment: (Attachment) -> Unit,
    showThumbnails: Boolean = true,
    thumbnailScalePercent: Int = THUMBNAIL_SCALE_DEFAULT,
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

            // Вложения идут перед текстом. Вид превью общий с карточкой в списке
            // тредов — это один и тот же компонент [AttachmentThumbsRow].
            if (showThumbnails && post.files.isNotEmpty()) {
                AttachmentThumbsRow(
                    files = post.files,
                    side = scaleThumbnail(THUMBNAIL_BASE_SIDE_DP, thumbnailScalePercent).dp,
                    onClick = onOpenAttachment,
                    label = ::attachmentLabel,
                    modifier = Modifier.padding(top = 6.dp),
                )
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

/**
 * Сторона квадратной ячейки превью в треде при 100 %. Серверные превью —
 * около 250 px, и если рисовать их «в натуральную величину» (как было), на
 * 3x-экране это ~83 dp: картинка выглядела мелкой. Поэтому размер задаётся
 * явно, а процент из настроек его масштабирует.
 */
private const val THUMBNAIL_BASE_SIDE_DP = 220

/** Подпись под превью: имя файла, размер, длительность, разрешение. */
private fun attachmentLabel(file: Attachment): String = buildString {
    append(file.name)
    if (file.sizeBytes > 0) append("  ${formatFileSize(file.sizeBytes)}")
    file.duration?.let { append("  $it") }
    if (file.width > 0 && file.height > 0) append("  ${file.width}×${file.height}")
    if (file.isVideo) {
        append("  ")
        append(file.extension.ifBlank { "webm" }.uppercase())
    }
}
