package io.github.kashimkhv.vibe2ch

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.kashimkhv.vibe2ch.data.settings.AppSettings
import io.github.kashimkhv.vibe2ch.data.settings.SettingsRepository
import io.github.kashimkhv.vibe2ch.ui.navigation.DvachLink
import io.github.kashimkhv.vibe2ch.ui.navigation.DvachLinks
import io.github.kashimkhv.vibe2ch.ui.navigation.DvachNavHost
import io.github.kashimkhv.vibe2ch.ui.theme.DvachTheme
import io.github.kashimkhv.vibe2ch.ui.theme.rememberDarkTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    /**
     * Ссылка из чужого приложения, которую ещё надо открыть. Живёт в состоянии
     * Activity: холодный старт приходит в `onCreate`, а повторный тап по ссылке
     * при уже открытом приложении — в `onNewIntent` (`singleTop` в манифесте).
     */
    private var pendingLink by mutableStateOf<DvachLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // При повороте экрана Activity пересоздаётся с тем же intent: ссылку
        // уже открыли, второй раз навигировать не нужно. Восстановленное
        // состояние навигации вернёт тот же экран.
        if (savedInstanceState == null) {
            pendingLink = linkFrom(intent)
        }
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            val dark = rememberDarkTheme(settings.theme)

            // enableEdgeToEdge по умолчанию смотрит на ночной режим системы.
            // Если тема выбрана вручную, иконки баров нужно перекрашивать сами.
            LaunchedEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        lightScrim = Color.TRANSPARENT,
                        darkScrim = Color.TRANSPARENT,
                        detectDarkMode = { dark },
                    ),
                    navigationBarStyle = SystemBarStyle.auto(
                        lightScrim = LIGHT_NAV_SCRIM,
                        darkScrim = DARK_NAV_SCRIM,
                        detectDarkMode = { dark },
                    ),
                )
            }

            DvachTheme(
                theme = settings.theme,
                headingFont = settings.headingFont,
                metaFont = settings.metaFont,
                bodyFont = settings.bodyFont,
                headingScalePercent = settings.headingScalePercent,
                metaScalePercent = settings.metaScalePercent,
                bodyScalePercent = settings.bodyScalePercent,
                headingBold = settings.headingBold,
                metaBold = settings.metaBold,
                bodyBold = settings.bodyBold,
                bodyLineSpacingPercent = settings.lineSpacingPercent,
            ) {
                DvachNavHost(
                    deepLink = pendingLink,
                    onDeepLinkHandled = { pendingLink = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        linkFrom(intent)?.let { pendingLink = it }
    }

    /**
     * Ссылка из intent: `VIEW` приходит с `data`, «Поделиться» — текстом
     * в `EXTRA_TEXT` (и заголовком рядом с ним). Разбор одинаковый.
     */
    private fun linkFrom(intent: Intent?): DvachLink? {
        if (intent == null) return null
        DvachLinks.parse(intent.dataString)?.let { return it }
        if (intent.action == Intent.ACTION_SEND) {
            return DvachLinks.parseFromText(intent.getStringExtra(Intent.EXTRA_TEXT))
        }
        return null
    }
}

/** Полупрозрачный светлый скрим для трёхкнопочной навигации — как в androidx.activity. */
private val LIGHT_NAV_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DARK_NAV_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
