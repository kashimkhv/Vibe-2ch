package io.github.kashimkhv.vibe2ch.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * Одно хранилище настроек на процесс. Вынесено из [SettingsRepository],
 * потому что выбранный домен нужен сетевому слою ещё до первого чтения
 * настроек: холодный старт не должен успеть сходить на прежний домен.
 */
internal val Context.settingsDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "settings")

/** Домен движка: полный адрес (`https://2ch.hk`). */
internal val BASE_URL_KEY = stringPreferencesKey("base_url")
