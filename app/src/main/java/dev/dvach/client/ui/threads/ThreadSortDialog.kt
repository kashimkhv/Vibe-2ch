package dev.dvach.client.ui.threads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.dvach.client.domain.ThreadSort
import dev.dvach.client.ui.components.DvachScrollbar
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Меню порядка тредов: те же строки, что и у движка, но по-русски.
 *
 * Устроено как список в старом Material: иконка слева, подпись справа,
 * у выбранного порядка — стрелка направления. Нажатие на строку выбирает
 * порядок, стрелка разворачивает его (А→Я или Я→А, новые или старые).
 * Направление есть у всех порядков, кроме бампа: там решает движок.
 */
@Composable
internal fun ThreadSortDialog(
    sort: ThreadSort,
    ascending: Boolean,
    onSelect: (ThreadSort, Boolean) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scroll = rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(2.dp),
        title = {
            Text("Сортировка тредов", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Box(modifier = Modifier.heightIn(max = 420.dp)) {
                Column(modifier = Modifier.verticalScroll(scroll)) {
                    ThreadSort.entries.forEach { option ->
                        SortRow(
                            option = option,
                            selected = option == sort,
                            ascending = ascending,
                            onClick = { onSelect(option, if (option == sort) ascending else option.defaultAscending) },
                            onFlipDirection = {
                                onSelect(option, !ascending)
                            },
                        )
                    }
                    Text(
                        text = "Закреплённые треды остаются сверху. Порядок считается " +
                            "по загруженным страницам — новые подгружаются в конец списка.",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                    )
                }
                DvachScrollbar(
                    state = scroll,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        },
        dismissButton = {
            TextButton(onClick = onReset) { Text("Сбросить") }
        },
    )
}

/** Строка меню: иконка, подпись и — у выбранного порядка — стрелка направления. */
@Composable
private fun SortRow(
    option: ThreadSort,
    selected: Boolean,
    ascending: Boolean,
    onClick: () -> Unit,
    onFlipDirection: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surface,
            )
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
    ) {
        Icon(
            imageVector = option.icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = option.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
        )
        if (selected && option.canReverse) {
            IconButton(onClick = onFlipDirection, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = if (ascending) Icons.Default.ArrowUpward
                    else Icons.Default.ArrowDownward,
                    contentDescription = if (ascending) {
                        "По возрастанию — нажмите для обратного порядка"
                    } else {
                        "По убыванию — нажмите для обратного порядка"
                    },
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            // Пустое место того же размера: строки не «пляшут» по ширине,
            // когда выбран другой порядок.
            Box(modifier = Modifier.size(40.dp))
        }
    }
}

/** Иконки повторяют смысл пунктов у движка: бамп, ответы, часы, скорость, картинки, «Аа». */
private val ThreadSort.icon: ImageVector
    get() = when (this) {
        ThreadSort.BUMP -> Icons.AutoMirrored.Filled.Sort
        ThreadSort.REPLIES -> Icons.AutoMirrored.Filled.Reply
        ThreadSort.CREATED -> Icons.Default.Schedule
        ThreadSort.RATE -> Icons.Default.Speed
        ThreadSort.IMAGES -> Icons.Default.Image
        ThreadSort.ALPHABET -> Icons.Default.SortByAlpha
    }
