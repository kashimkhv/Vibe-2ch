package io.github.kashimkhv.vibe2ch.data.update

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.kashimkhv.vibe2ch.di.MediaClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Свежий релиз с GitHub: что за версия, что в ней и какой файл качать. */
data class UpdateInfo(
    /** Версия без префикса `v`: `3.2.0`. */
    val version: String,
    /** Тело релиза в Markdown — то, что автор написал в release notes. */
    val notes: String,
    val apkUrl: String,
    val apkName: String,
    val sizeBytes: Long,
)

/**
 * Проверка обновлений и загрузка APK с GitHub Releases.
 *
 * GitHub отдаёт последний релиз обычным JSON, поэтому Retrofit с базовым URL
 * двача здесь не нужен: запрос и разбор — вручную. Качаем в кэш приложения,
 * а не в «Загрузки»: файл нужен один раз установщику, и после установки его
 * удалит система.
 */
@Singleton
class UpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @MediaClient private val client: OkHttpClient,
    private val json: Json,
) {

    /** Последний стабильный релиз; `null` — релиза или APK в нём нет. */
    suspend fun latest(): UpdateInfo? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(LATEST_RELEASE_URL)
            .header("Accept", "application/vnd.github+json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("GitHub ответил ${response.code}")
            val body = response.body.string()
            val release = json.decodeFromString<ReleaseDto>(body)
            if (release.draft || release.prerelease) return@withContext null
            val apk = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                ?: return@withContext null
            UpdateInfo(
                version = release.tagName.trim().removePrefix("v").removePrefix("V"),
                notes = release.body,
                apkUrl = apk.downloadUrl,
                apkName = apk.name,
                sizeBytes = apk.size,
            )
        }
    }

    /**
     * Скачивает APK и возвращает файл. [onProgress] вызывается по мере чтения
     * (0..1) и приходит из фонового потока — обновлять состояние можно сразу.
     */
    suspend fun download(info: UpdateInfo, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, UPDATE_DIR).apply { mkdirs() }
            // Прошлые попытки не нужны: имя файла содержит версию, и кэш иначе
            // копил бы по APK на каждый релиз.
            dir.listFiles()?.forEach { it.delete() }
            val target = File(dir, safeApkName(info))
            val request = Request.Builder().url(info.apkUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Скачивание не удалось: ${response.code}")
                val body = response.body
                val total = body.contentLength().takeIf { it > 0 } ?: info.sizeBytes
                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(BUFFER_BYTES)
                        var read = 0L
                        while (true) {
                            // Отмена из диалога должна останавливать чтение,
                            // а не просто переставать обновлять прогресс.
                            ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            read += count
                            if (total > 0) {
                                onProgress((read.toFloat() / total).coerceIn(0f, 1f))
                            }
                        }
                    }
                }
            }
            if (target.length() <= 0) error("Файл обновления пустой")
            target
        }

    /** Имя из ответа GitHub, но без сюрпризов вроде `../`: только буквы, цифры и точки. */
    private fun safeApkName(info: UpdateInfo): String {
        val name = info.apkName.ifBlank { "Vibe-2ch-${info.version}.apk" }
        val cleaned = name.map { char ->
            if (char.isLetterOrDigit() || char in "._-") char else '_'
        }.joinToString("")
        return if (cleaned.endsWith(".apk", ignoreCase = true)) cleaned else "$cleaned.apk"
    }
}

@Serializable
private data class ReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    val body: String = "",
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<AssetDto> = emptyList(),
)

@Serializable
private data class AssetDto(
    val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = "",
    val size: Long = 0,
)

private const val LATEST_RELEASE_URL =
    "https://api.github.com/repos/kashimkhv/Vibe-2ch/releases/latest"

/** Подкаталог кэша: установщик читает APK через FileProvider. */
private const val UPDATE_DIR = "updates"

private const val BUFFER_BYTES = 64 * 1024
