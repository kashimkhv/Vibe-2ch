package dev.dvach.client.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import dev.dvach.client.data.cache.CacheManager
import dev.dvach.client.data.cache.CacheUsage
import dev.dvach.client.data.settings.AppSettings
import dev.dvach.client.data.settings.FontChoice
import dev.dvach.client.data.settings.SettingsRepository
import dev.dvach.client.data.settings.ThemeMode
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val cache: CacheManager,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _cacheUsage = MutableStateFlow(CacheUsage())
    val cacheUsage: StateFlow<CacheUsage> = _cacheUsage.asStateFlow()

    init {
        refreshCache()
    }

    /** Перечитывает размер кэшей: он меняется сам, пока экран открыт. */
    fun refreshCache() = viewModelScope.launch {
        _cacheUsage.value = cache.usage()
    }

    fun clearCache() = viewModelScope.launch {
        cache.clear()
        _cacheUsage.value = cache.usage()
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        repository.setThemeMode(mode)
    }


    fun setShowThumbnails(enabled: Boolean) = viewModelScope.launch {
        repository.setShowThumbnails(enabled)
    }

    fun setThumbnailScale(percent: Int) = viewModelScope.launch {
        repository.setThumbnailScale(percent)
    }

    fun setListThumbnailScale(percent: Int) = viewModelScope.launch {
        repository.setListThumbnailScale(percent)
    }

    fun setCropThumbnails(enabled: Boolean) = viewModelScope.launch {
        repository.setCropThumbnails(enabled)
    }

    fun setThumbnailBackground(enabled: Boolean) = viewModelScope.launch {
        repository.setThumbnailBackground(enabled)
    }

    fun setVerticalMediaPaging(enabled: Boolean) = viewModelScope.launch {
        repository.setVerticalMediaPaging(enabled)
    }

    fun setAutoRevealSpoilers(enabled: Boolean) = viewModelScope.launch {
        repository.setAutoRevealSpoilers(enabled)
    }

    fun setHeadingFont(font: FontChoice) = viewModelScope.launch {
        repository.setHeadingFont(font)
    }

    fun setMetaFont(font: FontChoice) = viewModelScope.launch {
        repository.setMetaFont(font)
    }

    fun setBodyFont(font: FontChoice) = viewModelScope.launch {
        repository.setBodyFont(font)
    }
}
