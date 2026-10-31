package dev.dvach.client.ui.thread

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.dvach.client.domain.model.Attachment
import dev.dvach.client.domain.model.Post
import dev.dvach.client.ui.components.AttachmentThumbsRow
import dev.dvach.client.ui.components.HtmlText
import dev.dvach.client.util.THUMBNAIL_SCALE_DEFAULT
import dev.dvach.client.util.formatTimestamp
import dev.dvach.client.util.THUMBNAIL_BASE_SIDE_DP
import dev.dvach.client.util.scaleThumbnail
import dev.dvach.client.util.stripHtml

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostCard(
    post: Post,
    isHighlighted: Boolean,
    onPostClick: (postNum: Int, threadNum: Int?) -> Unit,
    onOpenAttachment: (Attachment) -> Unit,
    bookmarked: Boolean = false,
    onToggleBookmark: () -> Unit = {},
    showThumbnails: Boolean = true,
    thumbnailScalePercent: Int = THUMBNAIL_SCALE_DEFAULT,
    cropThumbnails: Boolean = true,
    autoRevealSpoilers: Boolean = false,
    /** Уровень вложенности в дереве ответов: 0 — корень ветки. */
    depth: Int = 0,
) {
    // Меню привязано к шапке поста: оно открывается под ней, а не в углу экрана.
    var menuOpen by remember { mutableStateOf(false) }
    val plainComment = remember(post.commentHtml) { stripHtml(post.commentHtml) }
    val subject = post.subject.trim()
    val showSubject = subject.isNotBlank() && !plainComment.startsWith(subject, ignoreCase = true)
    // Подсветка перехода по цитате — тёплый жёлтый, как выделение в старом
    // Android. Он не путается с голубым ОП и сразу виден среди белых ответов.
    val highlightColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        Color(0xFF46401C)
    } else {
        Color(0xFFFFF2C0)
    }

    val container = when {
        isHighlighted -> highlightColor
        // ОП — светло-голубая строка, ответы белые: иерархию задаёт не тень,
        // а подложка и разделитель.
        post.isOp -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val guideColor = MaterialTheme.colorScheme.outlineVariant

    // Пост — плоская белая строка на всю ширину, отделённая от следующей
    // тонким разделителем. Так выглядели списки в старом Material: никаких
    // карточек с тенью и скруглением, иерархию задаёт граница.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(container),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Направляющие в отступе: по одной линии на каждый уровень
                // вложенности. Сами строки не рисуют ветки — линии показывают,
                // что пост находится внутри ответа на пост выше.
                .drawBehind {
                    if (depth > 0) {
                        val step = TREE_INDENT_DP.dp.toPx()
                        val stroke = 1.dp.toPx()
                        for (level in 1..depth) {
                            val x = (level - 0.5f) * step
                            drawLine(
                                color = guideColor,
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = stroke,
                            )
                        }
                    }
                }
                .padding(start = (depth * TREE_INDENT_DP).dp)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                    onLongClick = { menuOpen = true },
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Box {
                // Долгое нажатие на пост открывает меню; якорь — шапка, поэтому
                // список появляется под именем и номером нажатого поста.
                // Шапка в одну строку: слева имя и трип, справа дата и номер.
                // Дата ушла к правому краю, чтобы имя получило её место: на /d/
                // ники длинные, и раньше они обрезались на 150 dp.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        Text(
                            text = post.name.ifBlank { "Аноним" },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (post.isOp) FontWeight.Bold else FontWeight.Medium,
                            color = if (post.isOp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        post.trip?.let { trip ->
                            Text(
                                text = " !$trip",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                maxLines = 1,
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Text(
                            text = formatTimestamp(post.timestamp).ifBlank { post.date },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                        // Точка вместо пробела: дата и номер — разные вещи,
                        // слитно они читаются как одно число.
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                        Text(
                            text = post.num.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                BookmarkMenu(
                    expanded = menuOpen,
                    bookmarked = bookmarked,
                    onDismiss = { menuOpen = false },
                    onToggle = onToggleBookmark,
                )
            }

            // Вложения идут перед текстом. Вид превью общий с карточкой в списке
            // тредов — это один и тот же компонент [AttachmentThumbsRow].
            if (showThumbnails && post.files.isNotEmpty()) {
                AttachmentThumbsRow(
                    files = post.files,
                    side = scaleThumbnail(THUMBNAIL_BASE_SIDE_DP, thumbnailScalePercent).dp,
                    onClick = onOpenAttachment,
                    crop = cropThumbnails,
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
                    autoRevealSpoilers = autoRevealSpoilers,
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
        }

        // Разделитель идёт во всю ширину, включая отступ дерева.
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

/**
 * Шаг отступа на уровень вложенности. Пост сдвигается вправо, поэтому
 * дерево читается без линий-коннекторов, а глубина ограничена в [buildPostTree].
 */
private const val TREE_INDENT_DP = 16

