package dev.dvach.client.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "board_id") val boardId: String,
    @ColumnInfo(name = "thread_num") val threadNum: Int,
    val subject: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(tableName = "pinned_boards")
data class PinnedBoardEntity(
    @PrimaryKey @ColumnInfo(name = "board_id") val boardId: String,
    /** Порядок закрепления: доски показываются в том порядке, в котором их закрепили. */
    @ColumnInfo(name = "pinned_at") val pinnedAt: Long,
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey @ColumnInfo(name = "thread_key") val threadKey: String,
    @ColumnInfo(name = "board_id") val boardId: String,
    @ColumnInfo(name = "thread_num") val threadNum: Int,
    val subject: String,
    @ColumnInfo(name = "visited_at") val visitedAt: Long,
)

/**
 * Место, на котором пользователь остановился в треде.
 *
 * [postNum] — первый видимый пост: при возврате прокрутка идёт к нему, а не
 * к сохранённому индексу, потому что после обновления треда посты могли
 * переехать (новые ответы, перестроенное дерево). [scrollIndex] — резерв,
 * если этого поста в треде уже нет (удалён).
 */
@Entity(tableName = "thread_scroll")
data class ThreadScrollEntity(
    @PrimaryKey @ColumnInfo(name = "thread_key") val threadKey: String,
    @ColumnInfo(name = "post_num") val postNum: Int,
    @ColumnInfo(name = "scroll_index") val scrollIndex: Int,
    @ColumnInfo(name = "scroll_offset") val scrollOffset: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
