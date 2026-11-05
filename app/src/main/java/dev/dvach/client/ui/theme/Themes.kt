package dev.dvach.client.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import dev.dvach.client.data.settings.DvachTheme

/**
 * Одна тема целиком. Ролей мало намеренно: всё остальное выводится из них
 * в [toColorScheme], и новая тема — это 16 строк цветов, а не тридцать ролей
 * Material.
 *
 * Порядок групп — от «жёстких» поверхностей к акцентам: шапка, холст, строки,
 * текст, служебные подложки.
 *
 * @property header плашка шапки на всех экранах (`primaryContainer`).
 * @property onHeader текст и иконки на шапке.
 * @property accent ссылки, кнопки, полоса своего поста, бегунок (`primary`).
 * @property onAccent текст на акцентной плашке (кнопки, капча).
 * @property background холст под списками (`background`).
 * @property surface строка списка и тело поста (`surface`).
 * @property surfaceAlt полоса подзаголовка, «мой пост», чипы (`surfaceVariant`).
 * @property text основной текст (`onSurface`).
 * @property muted ники, даты, счётчики (`onSurfaceVariant`).
 * @property divider разделители строк (`outlineVariant`).
 * @property op подложка ОП-поста: тон шапки, но светлее неё, — шапка читается
 *   как слой над строкой, а не сливается с ней (`secondaryContainer`).
 * @property onOp текст на подложке ОП-поста.
 * @property quoteHighlight подсветка поста, к которому перешли по цитате.
 * @property error цвет ошибок и неудачных запросов.
 * @property shadowTint тон тени шапки: на светлом фоне чёрная тень выглядит
 *   грязной, поэтому она окрашена в тон темы.
 */
@Immutable
data class DvachPalette(
    val theme: DvachTheme,
    val isDark: Boolean,
    val header: Color,
    val onHeader: Color,
    val accent: Color,
    val onAccent: Color,
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val text: Color,
    val muted: Color,
    val divider: Color,
    val op: Color,
    val onOp: Color,
    val quoteHighlight: Color,
    val error: Color,
    val onError: Color,
    val shadowTint: Color,
)

/** Тема, выбранная в настройках. По умолчанию — светлая, как раньше. */
val LocalDvachPalette = staticCompositionLocalOf { palettes.getValue(DvachTheme.MATERIAL_LIGHT) }

/** Палитра темы; «как в системе» разворачивается в пару Material You. */
fun DvachTheme.palette(systemDark: Boolean = false): DvachPalette = when (this) {
    DvachTheme.SYSTEM ->
        if (systemDark) palettes.getValue(DvachTheme.MATERIAL_DARK) else
            palettes.getValue(DvachTheme.MATERIAL_LIGHT)
    else -> palettes.getValue(this)
}

/**
 * Роли Material из палитры темы. Всё, что не перечислено, берётся из базовой
 * схемы: приложение эти роли не использует, а полный ColorScheme из 30 полей
 * пришлось бы расписывать в каждой теме.
 */
fun DvachPalette.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = header,
        onPrimaryContainer = onHeader,

        // Вторичная группа — ОП-пост: у него та же гамма, что у шапки, но тише.
        secondary = muted,
        onSecondary = surface,
        secondaryContainer = op,
        onSecondaryContainer = onOp,

        tertiary = muted,
        onTertiary = surface,
        tertiaryContainer = surfaceAlt,
        onTertiaryContainer = text,

        background = background,
        onBackground = text,
        surface = surface,
        onSurface = text,
        surfaceVariant = surfaceAlt,
        onSurfaceVariant = muted,
        surfaceTint = accent,

        // Старый Material строит иерархию на границах, а не на тоне: все
        // «контейнеры» — это либо строка, либо вторичная подложка.
        surfaceContainerLowest = background,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceAlt,
        surfaceContainerHighest = surfaceAlt,

        outline = divider,
        outlineVariant = divider,

        error = error,
        onError = onError,
        // Плашки ошибок не выводятся за пределы гаммы: тонируем поверхность
        // цветом ошибки, а не берём красный из чужой палитры.
        errorContainer = lerp(surface, error, if (isDark) 0.22f else 0.14f),
        onErrorContainer = if (isDark) error else lerp(error, Color.Black, 0.45f),

        inverseSurface = text,
        inverseOnSurface = surface,
    )
}

/**
 * Все палитры. Требования к каждой (их проверяет `ThemeTest`):
 *
 * - текст на строке и на холсте — не ниже 7:1 по WCAG: это основной текст постов;
 * - ники и даты — не ниже 4.5:1: они мелкие, но читаться должны;
 * - текст на шапке и на ОП-посте — не ниже 7:1;
 * - ссылки и акценты — не ниже 4.5:1 к строке, иначе оранжевая ссылка на кремовом
 *   фоне сливается с текстом;
 * - разделитель виден (не ниже 1.2:1), но не превращается в линию-рамку.
 *
 * В тёмных темах текст не абсолютно белый: на чёрном он «звенит» и после
 * длинного треда начинает плыть. В светлых — не абсолютно чёрный по той же
 * причине, но с запасом по контрасту.
 */
val palettes: Map<DvachTheme, DvachPalette> = listOf(
    // Текущее светлое оформление: голубая шапка, белые строки, серый холст.
    DvachPalette(
        theme = DvachTheme.MATERIAL_LIGHT,
        isDark = false,
        header = Color(0xFFD3E4F4),
        onHeader = Color(0xFF0D2A44),
        accent = Color(0xFF1976D2),
        onAccent = Color(0xFFFFFFFF),
        background = Color(0xFFF5F6F7),
        surface = Color(0xFFFFFFFF),
        surfaceAlt = Color(0xFFECEFF1),
        text = Color(0xFF1A1C1E),
        muted = Color(0xFF5F6B75),
        divider = Color(0xFFDDE3E8),
        op = Color(0xFFE7F0F9),
        onOp = Color(0xFF0D2A44),
        quoteHighlight = Color(0xFFFFF2C0),
        error = Color(0xFFC62828),
        onError = Color(0xFFFFFFFF),
        shadowTint = Color(0xFF1B2A3A),
    ),
    // Текущее тёмное оформление: тёмно-синяя шапка, почти чёрные строки.
    DvachPalette(
        theme = DvachTheme.MATERIAL_DARK,
        isDark = true,
        header = Color(0xFF1B3A52),
        onHeader = Color(0xFFD3E4F4),
        accent = Color(0xFF90CAF9),
        onAccent = Color(0xFF0D2A44),
        background = Color(0xFF111315),
        surface = Color(0xFF1A1C1E),
        surfaceAlt = Color(0xFF2A2E31),
        text = Color(0xFFE3E5E8),
        muted = Color(0xFFB6BFC7),
        divider = Color(0xFF34383C),
        op = Color(0xFF17293A),
        onOp = Color(0xFFD3E4F4),
        quoteHighlight = Color(0xFF46401C),
        error = Color(0xFFEF9A9A),
        onError = Color(0xFF601410),
        shadowTint = Color(0xFF000000),
    ),
    // Нейтральный графитовый холст с сиреневым акцентом: без синевы и без
    // коричневого, самый «тихий» из тёмных.
    DvachPalette(
        theme = DvachTheme.NOMIE,
        isDark = true,
        header = Color(0xFF2B2D3A),
        onHeader = Color(0xFFE9E8F2),
        accent = Color(0xFFA5A2E0),
        onAccent = Color(0xFF1A1730),
        background = Color(0xFF191A21),
        surface = Color(0xFF21222B),
        surfaceAlt = Color(0xFF2A2B36),
        text = Color(0xFFE5E4EC),
        muted = Color(0xFFA8A6B8),
        divider = Color(0xFF34353F),
        op = Color(0xFF26283A),
        onOp = Color(0xFFE9E8F2),
        quoteHighlight = Color(0xFF3E3A22),
        error = Color(0xFFF08A8A),
        onError = Color(0xFF3B0A0A),
        shadowTint = Color(0xFF000000),
    ),
    // Классическая Tomorrow: сине-серый фон, приглушённый синий акцент.
    // Текст чуть светлее канонического #C5C8C6: на подсветке цитаты канон
    // давал 6.1:1, а это уже мелкий шрифт «на грани».
    DvachPalette(
        theme = DvachTheme.TOMORROW,
        isDark = true,
        header = Color(0xFF35383D),
        onHeader = Color(0xFFE8EAED),
        accent = Color(0xFF81A2BE),
        onAccent = Color(0xFF14181C),
        background = Color(0xFF1D1F21),
        surface = Color(0xFF282A2E),
        surfaceAlt = Color(0xFF313338),
        text = Color(0xFFD4D7D5),
        muted = Color(0xFF9AA0A6),
        divider = Color(0xFF373B40),
        op = Color(0xFF2B3039),
        onOp = Color(0xFFE8EAED),
        quoteHighlight = Color(0xFF46402A),
        // Канонический #CC6666 давал 3.9:1 — ошибку было трудно прочитать.
        error = Color(0xFFE08080),
        onError = Color(0xFF1A0A0A),
        shadowTint = Color(0xFF000000),
    ),
    // Чистый чёрный фон: строки отличаются от него на пару процентов яркости,
    // иначе карточки «проваливаются» и список читается как сплошное полотно.
    DvachPalette(
        theme = DvachTheme.AMOLED,
        isDark = true,
        header = Color(0xFF0A0A0A),
        onHeader = Color(0xFFF2F2F2),
        accent = Color(0xFF8AB4F8),
        onAccent = Color(0xFF0B1A2A),
        background = Color(0xFF000000),
        surface = Color(0xFF0C0C0C),
        surfaceAlt = Color(0xFF161616),
        text = Color(0xFFE6E6E6),
        muted = Color(0xFF9E9E9E),
        divider = Color(0xFF232323),
        op = Color(0xFF0F0F0F),
        onOp = Color(0xFFF2F2F2),
        quoteHighlight = Color(0xFF3A3418),
        error = Color(0xFFF28B82),
        onError = Color(0xFF3B0A0A),
        shadowTint = Color(0xFF000000),
    ),
    // Тёплая коричневая: как старая лампа над столом, без синевы вообще.
    DvachPalette(
        theme = DvachTheme.BURCHAN,
        isDark = true,
        header = Color(0xFF3A2E23),
        onHeader = Color(0xFFF0E4D6),
        accent = Color(0xFFD9A05B),
        onAccent = Color(0xFF241505),
        background = Color(0xFF1A1512),
        surface = Color(0xFF231D18),
        surfaceAlt = Color(0xFF2C251E),
        text = Color(0xFFE7DDD2),
        muted = Color(0xFFB9A895),
        divider = Color(0xFF3A3129),
        op = Color(0xFF2B2119),
        onOp = Color(0xFFF0E4D6),
        quoteHighlight = Color(0xFF40331B),
        error = Color(0xFFE0888A),
        onError = Color(0xFF33090C),
        shadowTint = Color(0xFF000000),
    ),
    // Тёмно-бордовая шапка и розовый акцент: резкий, но текст остаётся спокойным.
    DvachPalette(
        theme = DvachTheme.HARDON,
        isDark = true,
        header = Color(0xFF3B1C21),
        onHeader = Color(0xFFF6DDE0),
        accent = Color(0xFFE4707F),
        onAccent = Color(0xFF2A0A0F),
        background = Color(0xFF15100F),
        surface = Color(0xFF1E1616),
        surfaceAlt = Color(0xFF271D1D),
        text = Color(0xFFEADBDC),
        muted = Color(0xFFBDA7A9),
        divider = Color(0xFF35282A),
        op = Color(0xFF2A1A1C),
        onOp = Color(0xFFF6DDE0),
        quoteHighlight = Color(0xFF3D2418),
        error = Color(0xFFF08A8A),
        onError = Color(0xFF360A0D),
        shadowTint = Color(0xFF000000),
    ),
    // Тёмно-синяя: шапка заметно синее строк, поэтому граница экрана читается
    // без тени, даже если тень на чёрном не видна.
    DvachPalette(
        theme = DvachTheme.NEUTRON,
        isDark = true,
        header = Color(0xFF1D2E40),
        onHeader = Color(0xFFD8E7F5),
        accent = Color(0xFF6FB6E0),
        onAccent = Color(0xFF06141F),
        background = Color(0xFF0D141C),
        surface = Color(0xFF141E28),
        surfaceAlt = Color(0xFF1B2733),
        text = Color(0xFFD9E2EA),
        muted = Color(0xFF9FB0C0),
        divider = Color(0xFF26333F),
        op = Color(0xFF16222E),
        onOp = Color(0xFFD8E7F5),
        quoteHighlight = Color(0xFF3C3720),
        error = Color(0xFFEF8F8F),
        onError = Color(0xFF350A0A),
        shadowTint = Color(0xFF000000),
    ),
    // Светлая холодная: голубоватый холст и синие строки, тёмно-синие ссылки.
    DvachPalette(
        theme = DvachTheme.PHOTON,
        isDark = false,
        header = Color(0xFFD6E0F5),
        onHeader = Color(0xFF26324F),
        accent = Color(0xFF34345C),
        onAccent = Color(0xFFFFFFFF),
        background = Color(0xFFEEF2FF),
        surface = Color(0xFFFFFFFF),
        surfaceAlt = Color(0xFFE3E9F7),
        text = Color(0xFF1B1B24),
        muted = Color(0xFF5B6178),
        divider = Color(0xFFC9D2E6),
        op = Color(0xFFE7ECFA),
        onOp = Color(0xFF26324F),
        quoteHighlight = Color(0xFFFFF2A8),
        error = Color(0xFFC62828),
        onError = Color(0xFFFFFFFF),
        shadowTint = Color(0xFF202A44),
    ),
    // Классическая Yotsuba: кремовый холст и оранжевые ссылки. Текст почти
    // чёрный с коричневым оттенком — так он не спорит с тёплым фоном;
    // оранжевый чуть темнее канонического #C7511F, иначе ссылка на белой
    // строке не дотягивала до 4.5:1.
    DvachPalette(
        theme = DvachTheme.YOTSUBA,
        isDark = false,
        header = Color(0xFFEFD9B0),
        onHeader = Color(0xFF4A3517),
        accent = Color(0xFFC24C1A),
        onAccent = Color(0xFFFFFFFF),
        background = Color(0xFFFFF6E0),
        surface = Color(0xFFFFFEF5),
        surfaceAlt = Color(0xFFF5E6CC),
        text = Color(0xFF241C12),
        muted = Color(0xFF6E5A44),
        divider = Color(0xFFE4D2B0),
        op = Color(0xFFF8E2BE),
        onOp = Color(0xFF4A3517),
        quoteHighlight = Color(0xFFFFE58A),
        error = Color(0xFFB3261E),
        onError = Color(0xFFFFFFFF),
        shadowTint = Color(0xFF4A3517),
    ),
    // Тёплая светлая: мягкий бежевый холст, коричневый акцент, никакого синего.
    DvachPalette(
        theme = DvachTheme.TEPLO,
        isDark = false,
        header = Color(0xFFE9D9C6),
        onHeader = Color(0xFF3E2C1B),
        accent = Color(0xFFA85A2A),
        onAccent = Color(0xFFFFFFFF),
        background = Color(0xFFF6F0E8),
        surface = Color(0xFFFFFDFA),
        surfaceAlt = Color(0xFFEDE4D8),
        text = Color(0xFF241B12),
        muted = Color(0xFF6B5847),
        divider = Color(0xFFDDD0BF),
        op = Color(0xFFF2E3D0),
        onOp = Color(0xFF3E2C1B),
        quoteHighlight = Color(0xFFFFEFC2),
        error = Color(0xFFB3261E),
        onError = Color(0xFFFFFFFF),
        shadowTint = Color(0xFF3E2C1B),
    ),
    // Monet: тёмный сиреневый Material You. Светлый текст и лавандовый акцент,
    // шапка чуть светлее строк — в стиле тональных поверхностей.
    DvachPalette(
        theme = DvachTheme.MONET_DARK,
        isDark = true,
        header = Color(0xFF2B2830),
        onHeader = Color(0xFFE6E0E9),
        accent = Color(0xFFD0BCFF),
        onAccent = Color(0xFF381E72),
        background = Color(0xFF141218),
        surface = Color(0xFF1D1B20),
        surfaceAlt = Color(0xFF26232B),
        text = Color(0xFFE6E0E9),
        muted = Color(0xFFB0A9BA),
        divider = Color(0xFF322F37),
        op = Color(0xFF242029),
        onOp = Color(0xFFE6E0E9),
        quoteHighlight = Color(0xFF46401C),
        error = Color(0xFFF2B8B5),
        onError = Color(0xFF3B0907),
        shadowTint = Color(0xFF000000),
    ),
    // Осень: тёмно-коричневый холст и янтарный акцент. Ближе к Burchan, но
    // холоднее и с более яркой шапкой.
    DvachPalette(
        theme = DvachTheme.AUTUMN,
        isDark = true,
        header = Color(0xFF3A2A1B),
        onHeader = Color(0xFFF3E2CB),
        accent = Color(0xFFE0A458),
        onAccent = Color(0xFF2A1A08),
        background = Color(0xFF191410),
        surface = Color(0xFF221B15),
        surfaceAlt = Color(0xFF2B2219),
        text = Color(0xFFEADFD2),
        muted = Color(0xFFBBA895),
        divider = Color(0xFF372C20),
        op = Color(0xFF2B2118),
        onOp = Color(0xFFF3E2CB),
        quoteHighlight = Color(0xFF443518),
        error = Color(0xFFEE9A8A),
        onError = Color(0xFF36090A),
        shadowTint = Color(0xFF000000),
    ),
).associateBy { it.theme }
