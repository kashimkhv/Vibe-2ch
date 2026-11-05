package dev.dvach.client

import androidx.compose.ui.graphics.Color
import dev.dvach.client.data.settings.DvachTheme
import dev.dvach.client.data.settings.legacyTheme
import dev.dvach.client.ui.theme.DvachPalette
import dev.dvach.client.ui.theme.palette
import dev.dvach.client.ui.theme.palettes
import dev.dvach.client.ui.theme.toColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Темы проверяются не «на глаз», а по контрасту WCAG: палитру легко сделать
 * красивой и нечитаемой, а тёмных тем у нас четырнадцать, и глазами каждую
 * после каждой правки не пересмотришь.
 *
 * Порог 7:1 — основной текст постов, 4.5:1 — ники, даты и ссылки. Разделитель
 * проверяется снизу (1.2:1): он должен быть виден, но не превращаться в рамку.
 */
class ThemeTest {

    // --- WCAG -----------------------------------------------------------------

    private fun channel(component: Float): Double {
        val c = component.toDouble()
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double =
        0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun assertContrast(
        palette: DvachPalette,
        what: String,
        foreground: Color,
        background: Color,
        minimum: Double,
    ) {
        val ratio = contrast(foreground, background)
        assertTrue(
            "${palette.theme.label}: $what — контраст ${"%.2f".format(ratio)}:1, " +
                "нужно не ниже $minimum:1",
            ratio >= minimum,
        )
    }

    // --- Состав списка ---------------------------------------------------------

    @Test
    fun `every theme has an id, a label and a hint`() {
        // Двадцать тем: тринадцать из коллажа и семь из редакторов кода,
        // плюс пункт «Как в системе», который разворачивается в пару Material You.
        assertEquals(20, DvachTheme.entries.count { it != DvachTheme.SYSTEM })
        assertEquals(
            "id тем должны быть уникальны: по ним хранится выбор",
            DvachTheme.entries.size,
            DvachTheme.entries.map { it.id }.toSet().size,
        )
        DvachTheme.entries.forEach { theme ->
            assertTrue("${theme.name}: пустой id", theme.id.isNotBlank())
            assertTrue("${theme.name}: пустая подпись", theme.label.isNotBlank())
            assertTrue("${theme.name}: пустая подсказка", theme.hint.isNotBlank())
        }
    }

    @Test
    fun `palettes cover every theme except the system pair`() {
        val expected = DvachTheme.entries.filter { it != DvachTheme.SYSTEM }.toSet()
        assertEquals(expected, palettes.keys)
        palettes.forEach { (theme, palette) ->
            assertEquals(theme, palette.theme)
        }
    }

    @Test
    fun `theme ids survive a round trip and unknown ids fall back to light`() {
        DvachTheme.entries.forEach { theme ->
            assertEquals(theme, DvachTheme.fromId(theme.id))
        }
        assertEquals(DvachTheme.MATERIAL_LIGHT, DvachTheme.fromId(null))
        assertEquals(DvachTheme.MATERIAL_LIGHT, DvachTheme.fromId("no_such_theme"))
    }

    @Test
    fun `system theme resolves to the material you pair`() {
        assertEquals(
            palettes.getValue(DvachTheme.MATERIAL_LIGHT),
            DvachTheme.SYSTEM.palette(systemDark = false),
        )
        assertEquals(
            palettes.getValue(DvachTheme.MATERIAL_DARK),
            DvachTheme.SYSTEM.palette(systemDark = true),
        )
        // Своя тема от настройки системы не зависит.
        assertEquals(
            palettes.getValue(DvachTheme.YOTSUBA),
            DvachTheme.YOTSUBA.palette(systemDark = true),
        )
    }

    @Test
    fun `old theme mode values migrate to the closest theme`() {
        assertEquals(DvachTheme.SYSTEM, legacyTheme("SYSTEM"))
        assertEquals(DvachTheme.MATERIAL_LIGHT, legacyTheme("LIGHT"))
        assertEquals(DvachTheme.MATERIAL_DARK, legacyTheme("DARK"))
        // Отсутствие ключа и мусор — это прежняя светлая тема по умолчанию.
        assertEquals(DvachTheme.MATERIAL_LIGHT, legacyTheme(null))
        assertEquals(DvachTheme.MATERIAL_LIGHT, legacyTheme("SYSTEM_DEFAULT"))
    }

    // --- Читаемость ------------------------------------------------------------

    @Test
    fun `body text is readable on every surface`() {
        palettes.values.forEach { palette ->
            // 7:1 — как у основного текста в WCAG AAA. Посты читают подолгу.
            assertContrast(palette, "текст/строка", palette.text, palette.surface, 7.0)
            assertContrast(palette, "текст/холст", palette.text, palette.background, 7.0)
            assertContrast(palette, "текст/ОП", palette.text, palette.op, 7.0)
            assertContrast(palette, "текст/подсветка цитаты", palette.text, palette.quoteHighlight, 7.0)
        }
    }

    @Test
    fun `headers and op posts keep their text readable`() {
        palettes.values.forEach { palette ->
            assertContrast(palette, "текст шапки", palette.onHeader, palette.header, 7.0)
            assertContrast(palette, "текст ОП", palette.onOp, palette.op, 7.0)
            assertContrast(palette, "текст шапки на ОП", palette.onHeader, palette.op, 7.0)
            // Текст шапки и ОП должен читаться и на строке: заголовок темы
            // в ленте лежит на обычной поверхности, а не только на плашке.
            assertContrast(palette, "текст шапки на строке", palette.onHeader, palette.surface, 7.0)
        }
    }

    @Test
    fun `meta and links stay above the small text threshold`() {
        palettes.values.forEach { palette ->
            assertContrast(palette, "мета/строка", palette.muted, palette.surface, 4.5)
            assertContrast(palette, "мета/подложка", palette.muted, palette.surfaceAlt, 4.5)
            assertContrast(palette, "мета/ОП", palette.muted, palette.op, 4.5)
            // Ссылки и акцентные подписи живут на строках и подложках, а не
            // прямо на холсте: на холсте бывают только разделители экранов.
            assertContrast(palette, "акцент/строка", palette.accent, palette.surface, 4.5)
            assertContrast(palette, "акцент/подложка", palette.accent, palette.surfaceAlt, 4.5)
            assertContrast(palette, "акцент/ОП", palette.accent, palette.op, 4.5)
            assertContrast(palette, "текст на акценте", palette.onAccent, palette.accent, 4.5)
            assertContrast(palette, "ошибка/строка", palette.error, palette.surface, 4.5)
        }
    }

    @Test
    fun `dividers are visible without turning into frames`() {
        palettes.values.forEach { palette ->
            assertContrast(palette, "разделитель/строка", palette.divider, palette.surface, 1.2)
            // И сверху тоже: иначе на тёмном фоне разделитель исчезает в строке.
            val ratio = contrast(palette.divider, palette.surface)
            assertTrue(
                "${palette.theme.label}: разделитель слишком контрастный " +
                    "(${"%.2f".format(ratio)}:1) — это уже рамка",
                ratio < 3.0,
            )
        }
    }

    @Test
    fun `the header reads as a layer above the rows`() {
        palettes.values.forEach { palette ->
            assertTrue(
                "${palette.theme.label}: шапка совпала со строкой — граница пропадёт",
                palette.header != palette.surface,
            )
            // ОП не должен сливаться ни со строкой, ни с шапкой: иначе при
            // переходе из ленты в тред непонятно, где начало треда.
            assertTrue(
                "${palette.theme.label}: ОП совпал со строкой",
                palette.op != palette.surface,
            )
            assertTrue(
                "${palette.theme.label}: ОП совпал с шапкой",
                palette.op != palette.header,
            )
        }
    }

    // --- Маппинг ролей ---------------------------------------------------------

    @Test
    fun `palette roles land in the color scheme the ui reads`() {
        palettes.values.forEach { palette ->
            val scheme = palette.toColorScheme()
            assertEquals(palette.accent, scheme.primary)
            assertEquals(palette.onAccent, scheme.onPrimary)
            assertEquals(palette.header, scheme.primaryContainer)
            assertEquals(palette.onHeader, scheme.onPrimaryContainer)
            assertEquals(palette.op, scheme.secondaryContainer)
            assertEquals(palette.onOp, scheme.onSecondaryContainer)
            assertEquals(palette.background, scheme.background)
            assertEquals(palette.surface, scheme.surface)
            assertEquals(palette.surfaceAlt, scheme.surfaceVariant)
            assertEquals(palette.muted, scheme.onSurfaceVariant)
            assertEquals(palette.divider, scheme.outlineVariant)
            assertEquals(palette.text, scheme.onSurface)
            assertEquals(palette.error, scheme.error)
            assertEquals(palette.text, scheme.inverseSurface)
        }
    }

    @Test
    fun `dark palettes say they are dark and light ones do not`() {
        palettes.values.forEach { palette ->
            val luminance = luminance(palette.surface)
            if (palette.isDark) {
                assertTrue(
                    "${palette.theme.label}: тёмная тема со светлой строкой",
                    luminance < 0.2,
                )
            } else {
                assertTrue(
                    "${palette.theme.label}: светлая тема с тёмной строкой",
                    luminance > 0.5,
                )
            }
        }
    }
}
