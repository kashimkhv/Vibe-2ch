package dev.dvach.client.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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

/**
 * Тень шапки. В старом Material плашка над списком всегда чуть выступала над
 * ним, и без неё светлая шапка сливается с такими же светлыми строками.
 *
 * 8 dp — как у app bar в старом Material: край плашки читается, но контент
 * под ним не тонет в тени. Меньше 4 dp на светлом фоне почти не видно, а на
 * 10 dp разница уже не считывается — тень просто расползается шире.
 * Цвет холодный, в тон синей палитре, иначе на белом фоне тень выглядит грязной.
 */
internal val TopBarElevation = 8.dp

/**
 * Мягкая тень для плашек, которые лежат поверх контента: шапок и кнопок.
 *
 * Тень рисуется самим элементом, его фон её перекрывает, поэтому виден только
 * край — ровно то, что нужно, чтобы поверхность читалась как отдельный слой.
 */
fun Modifier.dvachShadow(
    elevation: Dp = TopBarElevation,
    shape: Shape = RectangleShape,
): Modifier = shadow(
    elevation = elevation,
    shape = shape,
    clip = false,
    ambientColor = ShadowTint,
    spotColor = ShadowTint,
)

/** Тёмно-синий, а не чёрный: на светлом фоне чёрная тень уходит в грязь. */
private val ShadowTint = Color(0xFF1B2A3A)
