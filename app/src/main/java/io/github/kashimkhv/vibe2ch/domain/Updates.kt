package io.github.kashimkhv.vibe2ch.domain

/**
 * Правила сравнения версий для проверки обновлений с GitHub. Вынесены из UI,
 * чтобы их было видно в тестах: от них зависит, предложим ли человеку скачать
 * APK, и ошибка здесь либо спамит ложными обновлениями, либо прячет настоящее.
 */

/** Репозиторий, в котором публикуются релизы. */
const val GITHUB_REPO_URL = "https://github.com/kashimkhv/Vibe-2ch"

/**
 * Числовые части версии: `v3.1.0` → `[3, 1, 0]`, `3.1.0-debug` → `[3, 1, 0]`.
 * Нечисловой хвост после дефиса (суффикс debug-сборки) и префикс `v` при
 * сравнении не учитываются. Пустые и нечисловые куски читаются как ноль.
 */
fun versionNumbers(version: String): List<Int> =
    version.trim()
        .removePrefix("v")
        .removePrefix("V")
        .substringBefore('-')
        .split('.')
        .map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }

/**
 * Сравнение версий по числам, без учёта суффиксов: `3.10.0` новее `3.9.1`.
 * Возвращает положительное число, если [latest] новее [current].
 *
 * Предрелизные суффиксы (`-rc1`) намеренно не различаются: приложение
 * публикует только стабильные релизы, а лишняя сложность здесь дороже пользы.
 */
fun compareVersions(latest: String, current: String): Int {
    val left = versionNumbers(latest)
    val right = versionNumbers(current)
    val size = maxOf(left.size, right.size)
    for (index in 0 until size) {
        val l = left.getOrElse(index) { 0 }
        val r = right.getOrElse(index) { 0 }
        if (l != r) return l.compareTo(r)
    }
    return 0
}

/** Есть ли на GitHub версия новее установленной. */
fun isNewerVersion(latest: String, current: String): Boolean = compareVersions(latest, current) > 0
