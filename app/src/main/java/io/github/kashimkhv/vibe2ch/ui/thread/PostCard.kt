package io.github.kashimkhv.vibe2ch.ui.thread

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.kashimkhv.vibe2ch.domain.model.Attachment
import io.github.kashimkhv.vibe2ch.domain.model.Post
import io.github.kashimkhv.vibe2ch.ui.components.AttachmentThumbsRow
import io.github.kashimkhv.vibe2ch.ui.components.HtmlText
import io.github.kashimkhv.vibe2ch.ui.theme.LocalDvachPalette
import io.github.kashimkhv.vibe2ch.util.THUMBNAIL_SCALE_DEFAULT
import io.github.kashimkhv.vibe2ch.util.formatPostTime
import io.github.kashimkhv.vibe2ch.util.THUMBNAIL_BASE_SIDE_DP
import io.github.kashimkhv.vibe2ch.util.scaleThumbnail
import io.github.kashimkhv.vibe2ch.util.stripHtml

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostCard(
    post: Post,
    isHighlighted: Boolean,
    onPostClick: (postNum: Int, threadNum: Int?) -> Unit,
    onOpenAttachment: (Attachment) -> Unit,
    /** Свой пост: подсвечивается фоном, синей полосой слева и меткой «мой». */
    isMine: Boolean = false,
    /** Начать ответ на этот пост: форма откроется с цитатой `>>num`. */
    onReply: () -> Unit = {},
    /** Начать ответ с цитатой текста этого поста. */
    onReplyWithQuote: () -> Unit = {},
    bookmarked: Boolean = false,
    onToggleBookmark: () -> Unit = {},
    /** Ссылка на тред для пункта «Поделиться» в контекстном меню. */
    shareUrl: String = "",
    /** Заголовок треда — уходит в тему письма при отправке ссылки. */
    shareTitle: String = "",
    showThumbnails: Boolean = true,
    thumbnailScalePercent: Int = THUMBNAIL_SCALE_DEFAULT,
    cropThumbnails: Boolean = true,
    thumbnailBackground: Boolean = true,
    autoRevealSpoilers: Boolean = false,
    /** Поля текста: дополнительный отступ слева и справа от темы и текста поста. */
    textMargins: Dp = 0.dp,
    /** Время в шапке — словами («5 мин назад») вместо точной даты. */
    relativeDates: Boolean = false,
    /** Текущее время для относительных подписей; тикер общий на весь тред. */
    nowMillis: Long = System.currentTimeMillis(),
    /** Порядковый номер поста в треде: 1 — ОП. null — не показывать. */
    ordinal: Int? = null,
    /** Уровень вложенности в дереве ответов: 0 — корень ветки. */
    depth: Int = 0,
) {
    // Меню привязано к шапке поста: оно открывается под ней, а не в углу экрана.
    var menuOpen by remember { mutableStateOf(false) }
    // Режим выделения текста: включается пунктом меню, выключается «Готово».
    var selectingText by remember { mutableStateOf(false) }
    val plainComment = remember(post.commentHtml) { stripHtml(post.commentHtml) }
    val subject = post.subject.trim()
    val showSubject = subject.isNotBlank() && !plainComment.startsWith(subject, ignoreCase = true)
    // Подсветка перехода по цитате — тёплый жёлтый, как выделение в старом
    // Android. Он не путается с ОП-строкой и сразу виден среди ответов.
    // В каждой теме он свой: на кремовом фоне жёлтый прозрачнее, на чёрном —
    // приглушённый, иначе на OLED бьёт по глазам.
    val highlightColor = LocalDvachPalette.current.quoteHighlight

    val container = when {
        isHighlighted -> highlightColor
        // ОП — подложка в тон шапки, но светлее неё: шапка читается как слой
        // над строкой, а не сливается с ней.
        post.isOp -> MaterialTheme.colorScheme.secondaryContainer
        // Свой пост — приглушённая серая строка: видно, что он твой, но
        // внимание он не оттягивает у остальных, как подсветка перехода.
        isMine -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    val mineStripe = MaterialTheme.colorScheme.primary

    val guideColor = MaterialTheme.colorScheme.outlineVariant

    val interactionSource = remember { MutableInteractionSource() }
    // Пока идёт выделение, долгое нажатие принадлежит SelectionContainer:
    // иначе меню перехватывало бы его и до текста было бы не добраться.
    val postGestures = if (selectingText) {
        Modifier
    } else {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {},
            onLongClick = { menuOpen = true },
        )
    }

    // Пост — плоская белая строка на всю ширину, отделённая от следующей
    // тонким разделителем. Так выглядели списки в старом Material: никаких
    // карточек с тенью и скруглением, иерархию задаёт граница.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(container)
            .drawBehind {
                // Синяя полоса у своего поста — по ней он находится глазом
                // даже в длинном треде, где прокрутили мимо.
                if (isMine) {
                    drawRect(
                        color = mineStripe,
                        size = Size(MINE_STRIPE_DP.dp.toPx(), size.height),
                    )
                }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Направляющая в отступе — одна, вплотную к посту. Раньше линия
                // рисовалась на каждый уровень вложенности, и на глубоких ветках
                // отступ превращался в частокол из параллельных полос: чем
                // глубже ответ, тем больше линий. Глубину и так видно по самому
                // отступу, а одна линия показывает то, что нужно, — что пост
                // стоит внутри ответа на пост выше.
                .drawBehind {
                    if (depth > 0) {
                        val step = TREE_INDENT_DP.dp.toPx()
                        val x = (depth - 0.5f) * step
                        drawLine(
                            color = guideColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                }
                .padding(start = (depth * TREE_INDENT_DP).dp)
                .then(postGestures)
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
                        // Порядковый номер поста в треде — по хронологии, а не по
                        // номеру: номер поста — это его id на сервере (справа),
                        // а здесь место в треде. Приглушённый и мелкий, чтобы
                        // шапка не спорила с ником.
                        ordinal?.let { number ->
                            Text(
                                text = "#$number",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.padding(end = 6.dp),
                            )
                        }
                        if (isMine) {
                            Text(
                                text = "мой",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    )
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                            )
                        }
                        Text(
                            text = post.name.ifBlank { "Аноним" },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (post.isOp) {
                                FontWeight.Bold
                            } else {
                                MaterialTheme.typography.labelLarge.fontWeight
                            },
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
                            text = formatPostTime(post.timestamp, post.date, relativeDates, nowMillis),
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                ThreadMenu(
                    expanded = menuOpen,
                    bookmarked = bookmarked,
                    shareUrl = shareUrl,
                    shareTitle = shareTitle,
                    onDismiss = { menuOpen = false },
                    onToggle = onToggleBookmark,
                    onReply = onReply,
                    onReplyWithQuote = if (plainComment.isNotBlank()) {
                        onReplyWithQuote
                    } else {
                        null
                    },
                    onSelectText = if (plainComment.isNotBlank()) {
                        { selectingText = true }
                    } else {
                        null
                    },
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
                    background = thumbnailBackground,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            if (selectingText) {
                SelectionHint(onDone = { selectingText = false })
            }

            // В обычном режиме долгое нажатие открывает меню, поэтому
            // SelectionContainer включается только пунктом «Выделить текст».
            val message: @Composable () -> Unit = {
                if (showSubject) {
                    Text(
                        text = subject,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(
                            top = 4.dp,
                            start = textMargins,
                            end = textMargins,
                        ),
                    )
                }

                if (plainComment.isNotBlank()) {
                    HtmlText(
                        html = post.commentHtml,
                        onPostClick = onPostClick,
                        autoRevealSpoilers = autoRevealSpoilers,
                        // В режиме выделения ссылки и спойлеры не перехватывают
                        // долгое нажатие: иначе вместо слова открывался пост.
                        interactive = !selectingText,
                        modifier = Modifier.padding(
                            top = 4.dp,
                            start = textMargins,
                            end = textMargins,
                        ),
                    )
                }
            }
            if (selectingText) {
                SelectionContainer { message() }
            } else {
                message()
            }

            if (post.isBanned) {
                Text(
                    text = "Пост забанен",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(
                        top = 4.dp,
                        start = textMargins,
                        end = textMargins,
                    ),
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

/** Ширина синей полосы у своего поста. */
private const val MINE_STRIPE_DP = 3

/**
 * Подсказка режима выделения: объясняет, что делать дальше, и даёт выход —
 * системное меню выделения умеет только копировать, но не закрывать режим.
 */
@Composable
private fun SelectionHint(onDone: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
    ) {
        Text(
            text = "Выделите текст",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = onDone,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) {
            Text("Готово", style = MaterialTheme.typography.labelSmall)
        }
    }
}

