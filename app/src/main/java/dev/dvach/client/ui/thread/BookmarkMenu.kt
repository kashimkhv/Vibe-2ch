package dev.dvach.client.ui.thread

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * Контекстное меню треда: открывается долгим нажатием на заголовок или на любой
 * пост. Закладка у треда одна (не у поста), поэтому пункт один — и он же
 * переключает состояние.
 */
@Composable
internal fun BookmarkMenu(
    expanded: Boolean,
    bookmarked: Boolean,
    onDismiss: () -> Unit,
    onToggle: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
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
    }
}
