package dev.dvach.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.dvach.client.domain.model.Attachment

/** Пропорции, которые подставляем, если API не отдал размеры файла. */
private const val FALLBACK_ASPECT = 4f / 3f
private const val MIN_ASPECT = 0.2f
private const val MAX_ASPECT = 5f

private val Corner = RoundedCornerShape(10.dp)

/**
 * Превью вложения — единственный вид превью в приложении: и в списке тредов,
 * и в самом треде картинка рисуется этим компонентом, поэтому они выглядят
 * одинаково (скругление, обрезка, пропорции, заглушка вместо пустого превью).
 *
 * @param width ширина превью; процент из настроек уже применён вызывающим.
 * @param maxHeight предел высоты: в списке тредов высокие картинки обрезаются,
 *   чтобы карточка не занимала весь экран. [Dp.Unspecified] — без предела.
 */
@Composable
fun AttachmentThumb(
    file: Attachment,
    width: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    maxHeight: Dp = Dp.Unspecified,
) {
    val height = thumbHeight(file = file, width = width, maxHeight = maxHeight)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(width = width, height = height)
            .clip(Corner)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        if (file.thumbnailUrl.isNotBlank()) {
            AsyncImage(
                model = file.thumbnailUrl,
                contentDescription = file.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = if (file.isVideo) "▶ ВИДЕО" else "ФАЙЛ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Ряд превью поста: переносится по строкам, как обычный текст.
 *
 * @param maxVisible сколько превью показать; остальные сворачиваются в плашку
 *   «+N» на последнем — так ОП-пост в списке тредов не растягивает карточку.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttachmentThumbsRow(
    files: List<Attachment>,
    width: Dp,
    onClick: (Attachment) -> Unit,
    modifier: Modifier = Modifier,
    maxHeight: Dp = Dp.Unspecified,
    maxVisible: Int = Int.MAX_VALUE,
    /** Подпись под превью (в треде — имя, размер, разрешение); в списке её нет. */
    label: ((Attachment) -> String?)? = null,
) {
    if (files.isEmpty()) return
    val visible = files.take(maxVisible)
    val hidden = files.size - visible.size
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        visible.forEachIndexed { index, file ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Box {
                    AttachmentThumb(
                        file = file,
                        width = width,
                        maxHeight = maxHeight,
                        onClick = { onClick(file) },
                    )
                    if (hidden > 0 && index == visible.lastIndex) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .matchParentSize()
                                .clip(Corner)
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f))
                                .clickable { onClick(file) },
                        ) {
                            Text(
                                text = "+$hidden",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                            )
                        }
                    }
                }
                label?.invoke(file)?.let { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(width),
                    )
                }
            }
        }
    }
}

private fun thumbHeight(file: Attachment, width: Dp, maxHeight: Dp): Dp {
    val aspect = if (file.width > 0 && file.height > 0) {
        (file.width.toFloat() / file.height.toFloat()).coerceIn(MIN_ASPECT, MAX_ASPECT)
    } else {
        FALLBACK_ASPECT
    }
    val natural = (width.value / aspect).dp
    return if (maxHeight != Dp.Unspecified && natural > maxHeight) maxHeight else natural
}
