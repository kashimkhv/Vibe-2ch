package dev.dvach.client.data.settings

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
import dev.dvach.client.util.THUMBNAIL_SCALE_DEFAULT
import dev.dvach.client.util.THUMBNAIL_SCALE_MAX
import dev.dvach.client.util.THUMBNAIL_SCALE_MIN
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val showThumbnails: Boolean = true,
    /** Размер превью вложений в треде, в процентах от базового. */
    val thumbnailScalePercent: Int = THUMBNAIL_SCALE_DEFAULT,
    /** Размер превью ОП-поста в списке тредов — отдельный ползунок. */
    val listThumbnailScalePercent: Int = THUMBNAIL_SCALE_DEFAULT,
    /** Скрытый текст (спойлеры) показывается сразу, без тапа по плашке. */
    val autoRevealSpoilers: Boolean = false,
    val baseUrl: String = "https://2ch.hk",
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val THUMBS = booleanPreferencesKey("show_thumbnails")
        val THUMB_SCALE = intPreferencesKey("thumbnail_scale_percent")

        /** Появился вместе с отдельным ползунком для списка тредов. */
        val LIST_THUMB_SCALE = intPreferencesKey("list_thumbnail_scale_percent")
        val AUTO_REVEAL = booleanPreferencesKey("auto_reveal_spoilers")
        val BASE_URL = stringPreferencesKey("base_url")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppSettings(
                themeMode = prefs[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.LIGHT,
                showThumbnails = prefs[Keys.THUMBS] ?: true,
                thumbnailScalePercent = (prefs[Keys.THUMB_SCALE] ?: THUMBNAIL_SCALE_DEFAULT)
                    .coerceIn(THUMBNAIL_SCALE_MIN, THUMBNAIL_SCALE_MAX),
                listThumbnailScalePercent = (
                    prefs[Keys.LIST_THUMB_SCALE]
                        ?: prefs[Keys.THUMB_SCALE]
                        ?: THUMBNAIL_SCALE_DEFAULT
                    ).coerceIn(THUMBNAIL_SCALE_MIN, THUMBNAIL_SCALE_MAX),
                autoRevealSpoilers = prefs[Keys.AUTO_REVEAL] ?: false,
                baseUrl = prefs[Keys.BASE_URL] ?: "https://2ch.hk",
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit {
        it[Keys.THEME] = mode.name
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

    suspend fun setAutoRevealSpoilers(enabled: Boolean) = context.dataStore.edit {
        it[Keys.AUTO_REVEAL] = enabled
    }
}

private fun emptyPreferences(): Preferences =
    androidx.datastore.preferences.core.emptyPreferences()
