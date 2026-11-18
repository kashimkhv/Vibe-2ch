package io.github.kashimkhv.vibe2ch.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.kashimkhv.vibe2ch.domain.DVACH_MIRRORS
import io.github.kashimkhv.vibe2ch.domain.DomainCheck
import io.github.kashimkhv.vibe2ch.domain.normalizeDvachHost
import io.github.kashimkhv.vibe2ch.ui.components.DvachScrollbar

/**
 * Выбор домена движка: известные зеркала и свой домен.
 *
 * Домен вводится руками, потому что зеркала появляются и умирают быстрее, чем
 * выходят версии клиента: список в диалоге — подсказка, а не единственный
 * вариант. Проверка ходит на выбранный домен отдельным клиентом, поэтому её
 * можно сделать до сохранения.
 */
@Composable
internal fun DomainDialog(
    currentHost: String,
    check: DomainCheck,
    onCheck: (String) -> Unit,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val preset = DVACH_MIRRORS.any { it.host == currentHost }
    var custom by remember { mutableStateOf(!preset) }
    var customText by remember { mutableStateOf(if (preset) "" else currentHost) }
    var selected by remember { mutableStateOf(currentHost) }
    val scroll = rememberScrollState()

    // В поле можно вставить адрес из браузера целиком — разбор отбросит
    // схему, путь и порт. Непонятный ввод не даёт сохранить.
    val typed = normalizeDvachHost(customText)
    val host = if (custom) typed else selected
    val invalid = custom && customText.isNotBlank() && typed == null

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(2.dp),
        title = { Text("Домен двача", style = MaterialTheme.typography.titleMedium) },
        text = {
            Box(modifier = Modifier.heightIn(max = 440.dp)) {
                Column(modifier = Modifier.verticalScroll(scroll)) {
                    Text(
                        text = "Если домен не открывается, выберите другое зеркало: клиент " +
                            "будет ходить туда за тредами, картинками и отправкой постов.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    DVACH_MIRRORS.forEach { mirror ->
                        DomainRow(
                            title = mirror.host,
                            subtitle = mirror.note,
                            selected = !custom && selected == mirror.host,
                            onClick = {
                                custom = false
                                selected = mirror.host
                            },
                        )
                    }

                    DomainRow(
                        title = "Свой домен",
                        subtitle = "Зеркало с JSON API Makaba",
                        selected = custom,
                        onClick = { custom = true },
                    )

                    if (custom) {
                        TextField(
                            value = customText,
                            onValueChange = { customText = it },
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            placeholder = { Text("например, 2ch.example") },
                            singleLine = true,
                            isError = invalid,
                            shape = RoundedCornerShape(0.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                            ),
                        )
                        if (invalid) {
                            Text(
                                text = "Нужен домен вида 2ch.example — без пути и пробелов",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        TextButton(
                            onClick = { host?.let(onCheck) },
                            enabled = host != null && check !is DomainCheck.Checking,
                        ) {
                            Text(if (check is DomainCheck.Checking) "Проверяем…" else "Проверить")
                        }
                        CheckResult(check = check, host = host)
                    }
                }
                DvachScrollbar(
                    state = scroll,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { host?.let(onSave) }, enabled = host != null) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

/** Строка выбора: домен, подпись и радиокнопка. */
@Composable
private fun DomainRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .background(
                if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
            )
            .padding(vertical = 6.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.padding(start = 6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Ответ проверки — только для того домена, который сейчас выбран в диалоге. */
@Composable
private fun CheckResult(check: DomainCheck, host: String?) {
    val text = when {
        check is DomainCheck.Checking && check.host == host -> "Проверяем ${check.host}…"
        check is DomainCheck.Ok && check.host == host ->
            "${check.host} отвечает за ${check.millis} мс"
        check is DomainCheck.Failed && check.host == host ->
            "${check.host} не отвечает: ${check.reason}"
        else -> null
    } ?: return

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = when (check) {
            is DomainCheck.Ok -> MaterialTheme.colorScheme.primary
            is DomainCheck.Failed -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.padding(start = 4.dp),
    )
}
