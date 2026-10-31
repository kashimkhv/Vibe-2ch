package dev.dvach.client

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.dvach.client.data.settings.AppSettings
import dev.dvach.client.data.settings.SettingsRepository
import dev.dvach.client.ui.navigation.DvachNavHost
import dev.dvach.client.ui.theme.DvachTheme
import dev.dvach.client.ui.theme.rememberDarkTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            val dark = rememberDarkTheme(settings.themeMode)

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
                themeMode = settings.themeMode,
                headingFont = settings.headingFont,
                metaFont = settings.metaFont,
                bodyFont = settings.bodyFont,
            ) {
                DvachNavHost()
            }
        }
    }
}

/** Полупрозрачный светлый скрим для трёхкнопочной навигации — как в androidx.activity. */
private val LIGHT_NAV_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DARK_NAV_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
