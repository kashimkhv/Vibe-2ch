package io.github.kashimkhv.vibe2ch.ui.thread

import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Контекстное меню треда: открывается долгим нажатием на заголовок или на любой
 * пост. «Ответить» начинает ответ в форме и цитирует пост, на котором открыли
 * меню. Закладка у треда одна (не у поста), поэтому её пункт один — и он же
 * переключает состояние. «Поделиться» отдаёт ссылку на тред в системное меню
 * «Отправить»: оттуда её можно кинуть в мессенджер или скопировать.
 *
 * Все пункты оформлены одинаково — иконка слева, текст справа: так меню читается
 * как список действий, а не как набор случайных строк.
 */
@Composable
internal fun ThreadMenu(
    expanded: Boolean,
    bookmarked: Boolean,
    shareUrl: String,
    shareTitle: String,
    onDismiss: () -> Unit,
    onToggle: () -> Unit,
    onReply: () -> Unit,
) {
    val context = LocalContext.current
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Ответить") },
            leadingIcon = {
                Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null)
            },
            onClick = {
                onDismiss()
                onReply()
            },
        )
        DropdownMenuItem(
            text = { Text(if (bookmarked) "Убрать из закладок" else "Сохранить в закладки") },
            leadingIcon = {
                Icon(
                    imageVector = if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = null,
                )
            },
            onClick = {
                onDismiss()
                onToggle()
            },
        )
        DropdownMenuItem(
            text = { Text("Поделиться") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Share, contentDescription = null)
            },
            onClick = {
                onDismiss()
                shareThread(context, shareUrl, shareTitle)
            },
        )
    }
}

/**
 * Системное «Поделиться»: только текст-ссылка. Заголовок треда уходит в
 * `EXTRA_SUBJECT` — почтовые клиенты покажут его темой письма, а мессенджеры
 * просто проигнорируют.
 */
private fun shareThread(context: Context, url: String, title: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
        if (title.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, title)
    }
    context.startActivity(Intent.createChooser(send, "Поделиться"))
}
