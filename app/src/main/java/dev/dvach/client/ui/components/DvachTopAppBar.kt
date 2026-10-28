package dev.dvach.client.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

/**
 * Цвета шапки во всех экранах: светло-голубая плашка [MaterialTheme.colorScheme.primaryContainer]
 * с тёмно-синим текстом. В тёмной теме — та же роль, только тёмно-синяя.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun dvachTopAppBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer,
    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
)
