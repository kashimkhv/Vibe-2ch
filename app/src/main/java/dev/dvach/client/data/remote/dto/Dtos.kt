package dev.dvach.client.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BoardDto(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val info: String = "",
    @SerialName("info_outer") val infoOuter: String = "",
    @SerialName("threads_per_page") val threadsPerPage: Int = 20,
    @SerialName("max_pages") val maxPages: Int = 10,
    @SerialName("bump_limit") val bumpLimit: Int = 500,
    @SerialName("max_comment") val maxComment: Int = 15000,
    @SerialName("max_files_size") val maxFilesSize: Int = 0,
    @SerialName("enable_posting") val enablePosting: Boolean = false,
    @SerialName("enable_names") val enableNames: Boolean = false,
    @SerialName("enable_subject") val enableSubject: Boolean = false,
    @SerialName("enable_trips") val enableTrips: Boolean = false,
    @SerialName("enable_flags") val enableFlags: Boolean = false,
    @SerialName("enable_icons") val enableIcons: Boolean = false,
    @SerialName("enable_likes") val enableLikes: Boolean = false,
    @SerialName("enable_sage") val enableSage: Boolean = false,
    @SerialName("enable_dices") val enableDices: Boolean = false,
    @SerialName("file_types") val fileTypes: List<String> = emptyList(),
)

@Serializable
data class ThreadsPageDto(
    val board: BoardDto? = null,
    val threads: List<ThreadDto> = emptyList(),
    @SerialName("current_page") val currentPage: Int = 0,
    @SerialName("is_index") val isIndex: Boolean = false,
    @SerialName("is_board") val isBoard: Boolean = false,
    @SerialName("current_thread") val currentThread: Int = 0,
)

@Serializable
data class ThreadDto(
    val posts: List<PostDto> = emptyList(),
    @SerialName("files_count") val filesCount: Int = 0,
    @SerialName("posts_count") val postsCount: Int = 0,
    val views: Int = 0,
    val sticky: Int = 0,
    val closed: Int = 0,
)

@Serializable
data class PostDto(
    val num: Int = 0,
    val number: Int = 0,
    val parent: Int = 0,
    val board: String = "",
    val name: String = "",
    val subject: String = "",
    val comment: String = "",
    val date: String = "",
    val timestamp: Long = 0,
    val files: List<FileDto> = emptyList(),
    val sticky: Int = 0,
    val closed: Int = 0,
    val endless: Int = 0,
    val banned: Int = 0,
    @SerialName("op") val isOp: Int = 0,
    val views: Int = 0,
    val trip: String = "",
    val email: String = "",
    val tags: String = "",
)

@Serializable
data class FileDto(
    val name: String = "",
    val displayname: String = "",
    @SerialName("fullname") val fullName: String = "",
    val path: String = "",
    /** актуальное поле превью в Makaba 2026 */
    val thumbnail: String = "",
    /** legacy-поле, встречается в старых ответах */
    @SerialName("tn_path") val thumbnailPath: String = "",
    val size: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    @SerialName("tn_width") val thumbWidth: Int = 0,
    @SerialName("tn_height") val thumbHeight: Int = 0,
    val type: Int = 0,
    val duration: String = "",
    val audio: Boolean = false,
    val board: String = "",
)
