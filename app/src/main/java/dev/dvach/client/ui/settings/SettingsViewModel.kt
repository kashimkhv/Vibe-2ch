package dev.dvach.client.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import dev.dvach.client.data.settings.AppSettings
import dev.dvach.client.data.settings.SettingsRepository
import dev.dvach.client.data.settings.ThemeMode
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        repository.setThemeMode(mode)
    }

    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch {
        repository.setDynamicColor(enabled)
    }

    fun setShowThumbnails(enabled: Boolean) = viewModelScope.launch {
        repository.setShowThumbnails(enabled)
    }

    fun setThumbnailScale(percent: Int) = viewModelScope.launch {
        repository.setThumbnailScale(percent)
    }

    fun setAutoRevealSpoilers(enabled: Boolean) = viewModelScope.launch {
        repository.setAutoRevealSpoilers(enabled)
    }
}
