package dev.dvach.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.dvach.client.domain.model.Attachment

private val Corner = RoundedCornerShape(10.dp)
private val BadgeShape = RoundedCornerShape(8.dp)

/**
 * Превью вложения — единственный вид превью в приложении: и в списке тредов,
 * и в самом треде картинка рисуется этим компонентом.
 *
 * Превью — квадратная ячейка размером [side], картинка заполняет её целиком
 * (лишнее по краям подрезается по центру). Квадрат невидимый: ни рамки, ни
 * фона у загруженного превью нет, поэтому в списке и в треде картинки
 * выглядят одинаково и сетка не «прыгает» по высоте.
 * У видео в правом нижнем углу — маленький значок «play».
 *
 * @param hiddenCount сколько файлов не показано: на превью появляется «+N».
 */
@Composable
fun AttachmentThumb(
    file: Attachment,
    side: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hiddenCount: Int = 0,
) {
    val badgeSize = (side.value * 0.24f).coerceIn(18f, 30f).dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(side),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                if (file.isVideo) {
                    PlayBadge(
                        size = badgeSize,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp),
                    )
                }
            } else {
                // Превью не пришло — показываем заглушку того же размера.
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = if (file.isVideo) "▶ ВИДЕО" else "ФАЙЛ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (hiddenCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(BadgeShape)
                        .background(Color.Black.copy(alpha = 0.62f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "+$hiddenCount",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

/** Кружок с треугольником «play» в углу превью видео. */
@Composable
private fun PlayBadge(size: Dp, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f)),
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "Видео",
            tint = Color.White,
            modifier = Modifier.size(size * 0.7f),
        )
    }
}

/**
 * Ряд превью поста: квадратные ячейки [side], переносятся по строкам.
 *
 * @param maxVisible сколько превью показать; остальные сворачиваются в плашку
 *   «+N» на последнем — так ОП-пост в списке тредов не растягивает карточку.
 * @param label подпись под превью (в треде — имя, размер, разрешение); в списке её нет.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttachmentThumbsRow(
    files: List<Attachment>,
    side: Dp,
    onClick: (Attachment) -> Unit,
    modifier: Modifier = Modifier,
    maxVisible: Int = Int.MAX_VALUE,
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
                AttachmentThumb(
                    file = file,
                    side = side,
                    onClick = { onClick(file) },
                    hiddenCount = if (hidden > 0 && index == visible.lastIndex) hidden else 0,
                )
                label?.invoke(file)?.let { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(side),
                    )
                }
            }
        }
    }
}
