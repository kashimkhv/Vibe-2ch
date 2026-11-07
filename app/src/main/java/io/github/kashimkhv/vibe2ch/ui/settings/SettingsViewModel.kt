package io.github.kashimkhv.vibe2ch.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import io.github.kashimkhv.vibe2ch.data.cache.CacheManager
import io.github.kashimkhv.vibe2ch.data.cache.CacheUsage
import io.github.kashimkhv.vibe2ch.data.local.ChanDao
import io.github.kashimkhv.vibe2ch.data.remote.CookieStore
import io.github.kashimkhv.vibe2ch.data.settings.AppSettings
import io.github.kashimkhv.vibe2ch.data.settings.DvachTheme
import io.github.kashimkhv.vibe2ch.data.settings.FontChoice
import io.github.kashimkhv.vibe2ch.data.settings.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val cache: CacheManager,
    private val cookies: CookieStore,
    dao: ChanDao,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    /** Сколько тредов скрыто: подпись пункта в настройках и счётчик для списка. */
    val hiddenCount: StateFlow<Int> = dao.hiddenThreads()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _cacheUsage = MutableStateFlow(CacheUsage())
    val cacheUsage: StateFlow<CacheUsage> = _cacheUsage.asStateFlow()

    /** Сколько кук движка сейчас сохранено: по ним открываются закрытые доски. */
    val cookiesCount: StateFlow<Int> = cookies.count

    init {
        refreshCache()
    }

    /** Стереть куки: доступ к закрытым доскам после этого выдаётся заново. */
    fun clearCookies() = cookies.clear()

    /** Перечитывает размер кэшей: он меняется сам, пока экран открыт. */
    fun refreshCache() = viewModelScope.launch {
        _cacheUsage.value = cache.usage()
    }

    fun clearCache() = viewModelScope.launch {
        cache.clear()
        _cacheUsage.value = cache.usage()
    }

    fun setTheme(theme: DvachTheme) = viewModelScope.launch {
        repository.setTheme(theme)
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

    fun setEnlargeFewListThumbnails(enabled: Boolean) = viewModelScope.launch {
        repository.setEnlargeFewListThumbnails(enabled)
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

    fun setRelativeDates(enabled: Boolean) = viewModelScope.launch {
        repository.setRelativeDates(enabled)
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

    fun setHeadingScale(percent: Int) = viewModelScope.launch {
        repository.setHeadingScale(percent)
    }

    fun setMetaScale(percent: Int) = viewModelScope.launch {
        repository.setMetaScale(percent)
    }

    fun setBodyScale(percent: Int) = viewModelScope.launch {
        repository.setBodyScale(percent)
    }

    fun setHeadingBold(enabled: Boolean) = viewModelScope.launch {
        repository.setHeadingBold(enabled)
    }

    fun setMetaBold(enabled: Boolean) = viewModelScope.launch {
        repository.setMetaBold(enabled)
    }

    fun setBodyBold(enabled: Boolean) = viewModelScope.launch {
        repository.setBodyBold(enabled)
    }

    fun setLineSpacing(percent: Int) = viewModelScope.launch {
        repository.setLineSpacing(percent)
    }

    fun setTextMargins(dp: Int) = viewModelScope.launch {
        repository.setTextMargins(dp)
    }
}
