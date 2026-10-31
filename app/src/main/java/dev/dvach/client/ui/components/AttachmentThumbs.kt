package dev.dvach.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.dvach.client.domain.model.Attachment

private val Corner = RoundedCornerShape(0.dp)
private val BadgeShape = RoundedCornerShape(2.dp)

/** Затемнение под значками: белый текст должен читаться на любой картинке. */
private val BadgeScrim = Color.Black.copy(alpha = 0.62f)

/** Отступ значка от края превью. */
private val BadgePadding = 3.dp

/**
 * Превью вложения — единственный вид превью в приложении: и в списке тредов,
 * и в самом треде картинка рисуется этим компонентом, поэтому выглядит она
 * одинаково. Углы прямые: в старом Material картинки не скруглялись.
 *
 * Превью — квадратная ячейка размером [side], картинка заполняет её целиком
 * (лишнее по краям подрезается по центру). Квадрат невидимый: ни рамки, ни
 * фона у загруженного превью нет, поэтому в списке и в треде картинки
 * выглядят одинаково и сетка не «прыгает» по высоте.
 *
 * В правом нижнем углу — значок типа файла: круглый «play» у видео, плашка
 * «GIF» у анимированной картинки. Других подписей у превью нет.
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
        modifier = modifier
            .size(side)
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
            when {
                file.isVideo -> PlayBadge(
                    size = badgeSize,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(BadgePadding),
                )
                file.isAnimated -> GifBadge(
                    side = side,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(BadgePadding),
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
                    .padding(BadgePadding)
                    .clip(BadgeShape)
                    .background(BadgeScrim)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
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

/** Кружок с треугольником «play» в углу превью видео. */
@Composable
private fun PlayBadge(size: Dp, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(BadgeScrim),
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
 * Плашка «GIF» в углу анимированной картинки. Форма и затемнение те же, что
 * у значка видео, поэтому тип файла читается одинаково в списке и в треде.
 */
@Composable
private fun GifBadge(side: Dp, modifier: Modifier = Modifier) {
    // На мелком превью список не должен «зарастать» плашкой, поэтому шрифт
    // растёт вместе с ячейкой, но в разумных пределах.
    val fontSize = (side.value * 0.1f).coerceIn(8f, 12f).sp
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(BadgeShape)
            .background(BadgeScrim)
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text(
            text = "GIF",
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
    }
}

/**
 * Ряд превью поста: квадратные ячейки [side], переносятся по строкам.
 *
 * @param maxVisible сколько превью показать; остальные сворачиваются в плашку
 *   «+N» на последнем — так ОП-пост в списке тредов не растягивает карточку.
 * @param gap промежуток между ячейками; в списке тредов он ужимается, чтобы
 *   четыре превью влезли в строку при обычном масштабе (см. `thumbnailRowGap`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttachmentThumbsRow(
    files: List<Attachment>,
    side: Dp,
    onClick: (Attachment) -> Unit,
    modifier: Modifier = Modifier,
    maxVisible: Int = Int.MAX_VALUE,
    gap: Dp = 4.dp,
) {
    if (files.isEmpty()) return
    val visible = files.take(maxVisible)
    val hidden = files.size - visible.size
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        visible.forEachIndexed { index, file ->
            AttachmentThumb(
                file = file,
                side = side,
                onClick = { onClick(file) },
                hiddenCount = if (hidden > 0 && index == visible.lastIndex) hidden else 0,
            )
        }
    }
}
