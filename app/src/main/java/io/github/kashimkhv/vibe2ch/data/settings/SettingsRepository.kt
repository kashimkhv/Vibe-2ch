package io.github.kashimkhv.vibe2ch.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import io.github.kashimkhv.vibe2ch.domain.BoardSort
import io.github.kashimkhv.vibe2ch.domain.ThreadSort
import io.github.kashimkhv.vibe2ch.util.FONT_SCALE_DEFAULT
import io.github.kashimkhv.vibe2ch.util.LINE_SPACING_DEFAULT
import io.github.kashimkhv.vibe2ch.util.TEXT_MARGIN_DEFAULT
import io.github.kashimkhv.vibe2ch.util.TREE_INDENT_DEFAULT
import io.github.kashimkhv.vibe2ch.util.THUMBNAIL_SCALE_DEFAULT
import io.github.kashimkhv.vibe2ch.util.THUMBNAIL_SCALE_MAX
import io.github.kashimkhv.vibe2ch.util.THUMBNAIL_SCALE_MIN
import io.github.kashimkhv.vibe2ch.util.clampFontScale
import io.github.kashimkhv.vibe2ch.util.clampLineSpacing
import io.github.kashimkhv.vibe2ch.util.clampTextMargin
import io.github.kashimkhv.vibe2ch.util.clampTreeIndent
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

data class AppSettings(
    val theme: DvachTheme = DvachTheme.MATERIAL_LIGHT,
    val showThumbnails: Boolean = true,
    /** Размер превью вложений в треде, в процентах от базового. */
    val thumbnailScalePercent: Int = THUMBNAIL_SCALE_DEFAULT,
    /** Размер превью ОП-поста в списке тредов — отдельный ползунок. */
    val listThumbnailScalePercent: Int = THUMBNAIL_SCALE_DEFAULT,
    /**
     * Превью ОП-поста в списке вдвое крупнее, если вложений меньше трёх:
     * один-два файла иначе занимают только часть строки.
     */
    val enlargeFewListThumbnails: Boolean = true,
    /**
     * Превью заполняет квадрат с обрезкой краёв. Выключено — картинка
     * вписывается в квадрат целиком, а по краям остаётся фон.
     */
    val cropThumbnails: Boolean = true,
    /**
     * Подложка под превью. В режиме вписывания она работает полями вокруг
     * картинки; выключена — ячейка прозрачная, виден фон поста или карточки.
     */
    val thumbnailBackground: Boolean = true,
    /**
     * Листать вложения в просмотрщике вертикально, как в TikTok. Выключено —
     * привычное листание вбок.
     */
    val verticalMediaPaging: Boolean = false,
    /** Скрытый текст (спойлеры) показывается сразу, без тапа по плашке. */
    val autoRevealSpoilers: Boolean = false,
    /**
     * Время постов и тредов — словами («5 мин назад»), а не точной датой.
     * Старше месяца остаётся дата: «42 дн назад» уже не помогает.
     */
    val relativeDates: Boolean = false,
    /** Шрифт заголовков: шапки экранов, названия досок, темы постов. */
    val headingFont: FontChoice = FontChoice.SYSTEM,
    /** Шрифт мета-данных: ники, даты, номера постов, счётчики, подписи. */
    val metaFont: FontChoice = FontChoice.SYSTEM,
    /** Шрифт самого текста постов и описаний. */
    val bodyFont: FontChoice = FontChoice.SYSTEM,
    /** Размер заголовков в процентах от обычного. */
    val headingScalePercent: Int = FONT_SCALE_DEFAULT,
    /** Размер мета-данных в процентах от обычного. */
    val metaScalePercent: Int = FONT_SCALE_DEFAULT,
    /** Размер текста постов в процентах от обычного. */
    val bodyScalePercent: Int = FONT_SCALE_DEFAULT,
    /** Жирное начертание для каждой роли независимо. */
    val headingBold: Boolean = false,
    val metaBold: Boolean = false,
    val bodyBold: Boolean = false,
    /** Межстрочный интервал текста постов, в процентах от обычного. */
    val lineSpacingPercent: Int = LINE_SPACING_DEFAULT,
    /** Дополнительный отступ слева и справа у текста постов, в dp. */
    val textMarginsDp: Int = TEXT_MARGIN_DEFAULT,
    /** Отступ каждого уровня вложенности в дереве ответов, в dp. */
    val treeIndentDp: Int = TREE_INDENT_DEFAULT,
    /** Порядок тредов в списке доски: движковый бамп или своя сортировка. */
    val threadSort: ThreadSort = ThreadSort.BUMP,
    /** Направление сортировки: включено — по возрастанию (А→Я, старые сверху). */
    val threadSortAscending: Boolean = false,
    /**
     * Порядок тредов по доскам: ключ — id доски в нижнем регистре. Сюда
     * попадают только доски, где порядок выбирали вручную.
     */
    val threadSortByBoard: Map<String, BoardSort> = emptyMap(),
    val baseUrl: String = "https://2ch.hk",
) {
    /**
     * Порядок для конкретной доски: свой, если его выбирали, иначе общий.
     * Общий остаётся запасным вариантом — так доска, куда зашли впервые,
     * выглядит как раньше, а не как последняя правка на другой доске.
     */
    fun sortFor(boardId: String): BoardSort =
        threadSortByBoard[boardId.lowercase()] ?: BoardSort(threadSort, threadSortAscending)
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        /** Id выбранной темы (`DvachTheme.id`), а не имя enum: так выбор не сломается при переименовании. */
        val THEME = stringPreferencesKey("theme_id")

        /** Старый ключ: хранил SYSTEM/LIGHT/DARK, читается только для миграции. */
        val LEGACY_THEME = stringPreferencesKey("theme_mode")

        val THUMBS = booleanPreferencesKey("show_thumbnails")
        val THUMB_SCALE = intPreferencesKey("thumbnail_scale_percent")

        /** Появился вместе с отдельным ползунком для списка тредов. */
        val LIST_THUMB_SCALE = intPreferencesKey("list_thumbnail_scale_percent")

        /** Крупные превью в списке, если у ОП-поста меньше трёх вложений. */
        val LIST_THUMB_FEW_LARGE = booleanPreferencesKey("list_thumbnail_few_large")

        /** Превью обрезается до квадрата; выключено — вписывается целиком. */
        val CROP_THUMBS = booleanPreferencesKey("crop_thumbnails")

        /** Подложка под превью: фон ячейки или прозрачность. */
        val THUMB_BACKGROUND = booleanPreferencesKey("thumbnail_background")

        /** Галерея листает вложения вертикально (как в TikTok). */
        val VERTICAL_MEDIA = booleanPreferencesKey("vertical_media_paging")
        val AUTO_REVEAL = booleanPreferencesKey("auto_reveal_spoilers")

        /** Время словами: «5 мин назад» вместо даты. */
        val RELATIVE_DATES = booleanPreferencesKey("relative_dates")

        /** Выбранные шрифты: три независимые роли. */
        val FONT_HEADINGS = stringPreferencesKey("font_headings")
        val FONT_META = stringPreferencesKey("font_meta")
        val FONT_BODY = stringPreferencesKey("font_body")

        /** Размеры шрифтов в процентах — по одному на каждую роль. */
        val FONT_SCALE_HEADINGS = intPreferencesKey("font_scale_headings")
        val FONT_SCALE_META = intPreferencesKey("font_scale_meta")
        val FONT_SCALE_BODY = intPreferencesKey("font_scale_body")

        /** Жирное начертание — тоже по одному на роль. */
        val FONT_BOLD_HEADINGS = booleanPreferencesKey("font_bold_headings")
        val FONT_BOLD_META = booleanPreferencesKey("font_bold_meta")
        val FONT_BOLD_BODY = booleanPreferencesKey("font_bold_body")

        /** Чтение: плотность строк и боковые поля текста. */
        val FONT_LINE_SPACING = intPreferencesKey("font_line_spacing")
        val TEXT_MARGINS = intPreferencesKey("text_margins")

        /** Отступ дерева ответов; 0 — ответы в один столбец, без отступа. */
        val TREE_INDENT = intPreferencesKey("tree_indent")

        /** Порядок тредов в списке доски — один на всё приложение. */
        val THREAD_SORT = stringPreferencesKey("thread_sort")
        val THREAD_SORT_ASCENDING = booleanPreferencesKey("thread_sort_ascending")

        /** Свой порядок у отдельных досок: `b=REPLIES:0;po=BUMP:0`. */
        val THREAD_SORT_BOARDS = stringPreferencesKey("thread_sort_boards")
        val BASE_URL = stringPreferencesKey("base_url")
    }

    /** Миграция запускается один раз на процесс, сколько бы экранов ни читали настройки. */
    private val listScaleMigrated = AtomicBoolean(false)

    /**
     * Старые версии хранили один общий размер превью. Переносим его в отдельный
     * ключ списка ровно один раз: раньше список читал общий ключ как запасной,
     * и из-за этого его ползунок двигался вместе с тредовым.
     */
    private suspend fun migrateListThumbnailScale() {
        if (!listScaleMigrated.compareAndSet(false, true)) return
        // Ошибка миграции не должна ронять поток настроек: останется значение
        // по умолчанию, как будто ключа ещё нет.
        runCatching {
            context.dataStore.edit { prefs ->
                if (Keys.LIST_THUMB_SCALE !in prefs) {
                    prefs[Keys.LIST_THUMB_SCALE] =
                        prefs[Keys.THUMB_SCALE] ?: THUMBNAIL_SCALE_DEFAULT
                }
            }
        }
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .onStart { migrateListThumbnailScale() }
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppSettings(
                theme = prefs[Keys.THEME]?.let(DvachTheme::fromId)
                    ?: legacyTheme(prefs[Keys.LEGACY_THEME]),
                showThumbnails = prefs[Keys.THUMBS] ?: true,
                thumbnailScalePercent = (prefs[Keys.THUMB_SCALE] ?: THUMBNAIL_SCALE_DEFAULT)
                    .coerceIn(THUMBNAIL_SCALE_MIN, THUMBNAIL_SCALE_MAX),
                listThumbnailScalePercent = (
                    prefs[Keys.LIST_THUMB_SCALE] ?: THUMBNAIL_SCALE_DEFAULT
                    ).coerceIn(THUMBNAIL_SCALE_MIN, THUMBNAIL_SCALE_MAX),
                enlargeFewListThumbnails = prefs[Keys.LIST_THUMB_FEW_LARGE] ?: true,
                cropThumbnails = prefs[Keys.CROP_THUMBS] ?: true,
                thumbnailBackground = prefs[Keys.THUMB_BACKGROUND] ?: true,
                verticalMediaPaging = prefs[Keys.VERTICAL_MEDIA] ?: false,
                autoRevealSpoilers = prefs[Keys.AUTO_REVEAL] ?: false,
                relativeDates = prefs[Keys.RELATIVE_DATES] ?: false,
                headingFont = prefs.fontChoice(Keys.FONT_HEADINGS),
                metaFont = prefs.fontChoice(Keys.FONT_META),
                bodyFont = prefs.fontChoice(Keys.FONT_BODY),
                headingScalePercent = clampFontScale(
                    prefs[Keys.FONT_SCALE_HEADINGS] ?: FONT_SCALE_DEFAULT,
                ),
                metaScalePercent = clampFontScale(
                    prefs[Keys.FONT_SCALE_META] ?: FONT_SCALE_DEFAULT,
                ),
                bodyScalePercent = clampFontScale(
                    prefs[Keys.FONT_SCALE_BODY] ?: FONT_SCALE_DEFAULT,
                ),
                headingBold = prefs[Keys.FONT_BOLD_HEADINGS] ?: false,
                metaBold = prefs[Keys.FONT_BOLD_META] ?: false,
                bodyBold = prefs[Keys.FONT_BOLD_BODY] ?: false,
                lineSpacingPercent = clampLineSpacing(
                    prefs[Keys.FONT_LINE_SPACING] ?: LINE_SPACING_DEFAULT,
                ),
                textMarginsDp = clampTextMargin(
                    prefs[Keys.TEXT_MARGINS] ?: TEXT_MARGIN_DEFAULT,
                ),
                treeIndentDp = clampTreeIndent(
                    prefs[Keys.TREE_INDENT] ?: TREE_INDENT_DEFAULT,
                ),
                threadSort = prefs[Keys.THREAD_SORT]
                    ?.let { saved -> runCatching { ThreadSort.valueOf(saved) }.getOrNull() }
                    ?: ThreadSort.BUMP,
                threadSortAscending = prefs[Keys.THREAD_SORT_ASCENDING] ?: false,
                threadSortByBoard = decodeBoardSorts(prefs[Keys.THREAD_SORT_BOARDS]),
                baseUrl = prefs[Keys.BASE_URL] ?: "https://2ch.hk",
            )
        }

    suspend fun setTheme(theme: DvachTheme) = context.dataStore.edit {
        it[Keys.THEME] = theme.id
        // Старый ключ больше не нужен: при откате на прежнюю версию тема
        // прочитается как LIGHT/DARK по новому значению, а не потеряется.
        it.remove(Keys.LEGACY_THEME)
    }

    suspend fun setShowThumbnails(enabled: Boolean) = context.dataStore.edit {
        it[Keys.THUMBS] = enabled
    }

    suspend fun setThumbnailScale(percent: Int) = context.dataStore.edit {
        it[Keys.THUMB_SCALE] = percent.coerceIn(THUMBNAIL_SCALE_MIN, THUMBNAIL_SCALE_MAX)
    }

    suspend fun setListThumbnailScale(percent: Int) = context.dataStore.edit {
        it[Keys.LIST_THUMB_SCALE] = percent.coerceIn(THUMBNAIL_SCALE_MIN, THUMBNAIL_SCALE_MAX)
    }

    suspend fun setEnlargeFewListThumbnails(enabled: Boolean) = context.dataStore.edit {
        it[Keys.LIST_THUMB_FEW_LARGE] = enabled
    }

    suspend fun setCropThumbnails(enabled: Boolean) = context.dataStore.edit {
        it[Keys.CROP_THUMBS] = enabled
    }

    suspend fun setThumbnailBackground(enabled: Boolean) = context.dataStore.edit {
        it[Keys.THUMB_BACKGROUND] = enabled
    }

    suspend fun setVerticalMediaPaging(enabled: Boolean) = context.dataStore.edit {
        it[Keys.VERTICAL_MEDIA] = enabled
    }

    suspend fun setAutoRevealSpoilers(enabled: Boolean) = context.dataStore.edit {
        it[Keys.AUTO_REVEAL] = enabled
    }

    suspend fun setRelativeDates(enabled: Boolean) = context.dataStore.edit {
        it[Keys.RELATIVE_DATES] = enabled
    }

    /**
     * Порядок тредов для одной доски. Общий ключ (`thread_sort`) не трогаем:
     * он остаётся запасным вариантом для досок, где свой порядок не выбирали,
     * и благодаря этому доска, куда зашли впервые, открывается как раньше.
     */
    suspend fun setThreadSort(
        boardId: String,
        sort: ThreadSort,
        ascending: Boolean,
    ) = context.dataStore.edit { prefs ->
        val saved = decodeBoardSorts(prefs[Keys.THREAD_SORT_BOARDS])
        prefs[Keys.THREAD_SORT_BOARDS] = encodeBoardSorts(
            saved + (boardId.lowercase() to BoardSort(sort, ascending)),
        )
    }

    suspend fun setHeadingFont(font: FontChoice) = context.dataStore.edit {
        it[Keys.FONT_HEADINGS] = font.name
    }

    suspend fun setMetaFont(font: FontChoice) = context.dataStore.edit {
        it[Keys.FONT_META] = font.name
    }

    suspend fun setBodyFont(font: FontChoice) = context.dataStore.edit {
        it[Keys.FONT_BODY] = font.name
    }

    suspend fun setHeadingScale(percent: Int) = context.dataStore.edit {
        it[Keys.FONT_SCALE_HEADINGS] = clampFontScale(percent)
    }

    suspend fun setMetaScale(percent: Int) = context.dataStore.edit {
        it[Keys.FONT_SCALE_META] = clampFontScale(percent)
    }

    suspend fun setBodyScale(percent: Int) = context.dataStore.edit {
        it[Keys.FONT_SCALE_BODY] = clampFontScale(percent)
    }

    suspend fun setHeadingBold(enabled: Boolean) = context.dataStore.edit {
        it[Keys.FONT_BOLD_HEADINGS] = enabled
    }

    suspend fun setMetaBold(enabled: Boolean) = context.dataStore.edit {
        it[Keys.FONT_BOLD_META] = enabled
    }

    suspend fun setBodyBold(enabled: Boolean) = context.dataStore.edit {
        it[Keys.FONT_BOLD_BODY] = enabled
    }

    suspend fun setLineSpacing(percent: Int) = context.dataStore.edit {
        it[Keys.FONT_LINE_SPACING] = clampLineSpacing(percent)
    }

    suspend fun setTextMargins(dp: Int) = context.dataStore.edit {
        it[Keys.TEXT_MARGINS] = clampTextMargin(dp)
    }

    suspend fun setTreeIndent(dp: Int) = context.dataStore.edit {
        it[Keys.TREE_INDENT] = clampTreeIndent(dp)
    }
}

/**
 * Старый ключ `theme_mode` хранил три значения: SYSTEM/LIGHT/DARK. Переносим
 * их в новую тему: SYSTEM остаётся «как в системе», LIGHT и DARK — это пара
 * Material You. Неизвестное значение (или его отсутствие) — светлая тема,
 * как было по умолчанию.
 */
internal fun legacyTheme(value: String?): DvachTheme = when (value) {
    "SYSTEM" -> DvachTheme.SYSTEM
    "DARK" -> DvachTheme.MATERIAL_DARK
    else -> DvachTheme.MATERIAL_LIGHT
}

/**
 * Неизвестное имя шрифта (например, из старой версии приложения) не должно
 * ронять чтение настроек: неизвестное значение — это системный шрифт.
 */
internal fun Preferences.fontChoice(key: Preferences.Key<String>): FontChoice =
    this[key]?.let { runCatching { FontChoice.valueOf(it) }.getOrNull() } ?: FontChoice.SYSTEM

/**
 * Порядок тредов по доскам одной строкой: `b=REPLIES:0;po=BUMP:0`. DataStore
 * хранит только простые типы, а отдельный ключ на каждую доску заставил бы
 * перебирать все настройки при каждом чтении.
 */
internal fun encodeBoardSorts(sorts: Map<String, BoardSort>): String =
    sorts.entries.joinToString(";") { (board, choice) ->
        "$board=${choice.sort.name}:${if (choice.ascending) 1 else 0}"
    }

/**
 * Разбор [encodeBoardSorts]. Терпимый: битая запись (обрезанная, с неизвестным
 * именем порядка) пропускается, а не роняет чтение настроек — иначе одна
 * испорченная доска оставила бы приложение без всех настроек сразу.
 */
internal fun decodeBoardSorts(raw: String?): Map<String, BoardSort> {
    if (raw.isNullOrBlank()) return emptyMap()
    return raw.split(';').mapNotNull { entry ->
        val board = entry.substringBefore('=', "").trim().lowercase()
        val value = entry.substringAfter('=', "")
        // Запись без направления (старый формат) — это порядок по умолчанию.
        val sort = runCatching {
            ThreadSort.valueOf(value.substringBeforeLast(':', value))
        }.getOrNull()
        if (board.isEmpty() || sort == null) return@mapNotNull null
        board to BoardSort(sort, value.substringAfterLast(':', "0") == "1")
    }.toMap()
}

private fun emptyPreferences(): Preferences =
    androidx.datastore.preferences.core.emptyPreferences()
