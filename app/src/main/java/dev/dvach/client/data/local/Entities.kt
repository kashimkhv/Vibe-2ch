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
