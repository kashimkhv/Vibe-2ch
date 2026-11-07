package io.github.kashimkhv.vibe2ch.di

import javax.inject.Qualifier

/**
 * Клиент для тяжёлых ответов: видео и скачивания файлов. Отличается от основного
 * тем, что не имеет общего `callTimeout` (иначе загрузка длинного webm рвётся на
 * 45-й секунде) и не пишет медиа в HTTP-кэш.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MediaClient
