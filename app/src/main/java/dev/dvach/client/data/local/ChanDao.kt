package dev.dvach.client.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ChanDao {

    @Query("SELECT * FROM bookmarks ORDER BY created_at DESC")
    fun bookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE board_id = :boardId AND thread_num = :threadNum)")
    fun isBookmarked(boardId: String, threadNum: Int): Flow<Boolean>

    @Upsert
    suspend fun addBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE board_id = :boardId AND thread_num = :threadNum")
    suspend fun removeBookmark(boardId: String, threadNum: Int)

    @Query("SELECT * FROM history ORDER BY visited_at DESC LIMIT :limit")
    fun history(limit: Int = 50): Flow<List<HistoryEntity>>

    @Upsert
    suspend fun touchHistory(entry: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clearHistory()

    /** Закреплённые доски: в порядке закрепления — он же порядок в начале списка. */
    @Query("SELECT * FROM pinned_boards ORDER BY pinned_at ASC")
    fun pinnedBoards(): Flow<List<PinnedBoardEntity>>

    @Upsert
    suspend fun pinBoard(board: PinnedBoardEntity)

    @Query("DELETE FROM pinned_boards WHERE board_id = :boardId")
    suspend fun unpinBoard(boardId: String)
}
