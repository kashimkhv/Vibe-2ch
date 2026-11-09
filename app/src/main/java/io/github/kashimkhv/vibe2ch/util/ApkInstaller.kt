package io.github.kashimkhv.vibe2ch.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

/**
 * Установка скачанного APK. Android не даёт приложению запустить установщик
 * молча: сначала человек разрешает установку из этого источника в системных
 * настройках, и только потом установщик показывает свой экран.
 */

/** Разрешена ли установка APK из этого приложения (Android 8+). */
fun canInstallPackages(context: Context): Boolean =
    context.packageManager.canRequestPackageInstalls()

/** Системный экран «Установка неизвестных приложений» для нашего пакета. */
fun openInstallPermissionSettings(context: Context) {
    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
        .setData(Uri.parse("package:${context.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

/**
 * Открывает системный установщик для APK из кэша обновлений. Файл отдаётся
 * через FileProvider: `file://` для чужих приложений на Android 7+ запрещён.
 */
fun installApk(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, "application/vnd.android.package-archive")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
