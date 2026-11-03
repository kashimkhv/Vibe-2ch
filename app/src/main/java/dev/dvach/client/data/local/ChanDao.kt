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

    /** Место, на котором остановились в треде, или null, если тред ещё не читали. */
    @Query("SELECT * FROM thread_scroll WHERE thread_key = :threadKey")
    suspend fun threadScroll(threadKey: String): ThreadScrollEntity?

    @Upsert
    suspend fun saveThreadScroll(entry: ThreadScrollEntity)

    /**
     * Держим в базе позиции не больше [limit] последних тредов: иначе таблица
     * растёт бесконечно, а старые позиции всё равно не нужны.
     */
    @Query(
        "DELETE FROM thread_scroll WHERE thread_key NOT IN " +
            "(SELECT thread_key FROM thread_scroll ORDER BY updated_at DESC LIMIT :limit)"
    )
    suspend fun trimThreadScroll(limit: Int)

    /** Номера своих постов в треде — для подсветки «мой пост». */
    @Query("SELECT post_num FROM my_posts WHERE board_id = :boardId AND thread_num = :threadNum")
    fun myPosts(boardId: String, threadNum: Int): Flow<List<Int>>

    @Upsert
    suspend fun addMyPost(post: MyPostEntity)

    /** Старые свои посты не нужны: храним только последние [limit] штук. */
    @Query(
        "DELETE FROM my_posts WHERE rowid NOT IN " +
            "(SELECT rowid FROM my_posts ORDER BY created_at DESC LIMIT :limit)"
    )
    suspend fun trimMyPosts(limit: Int)

    /** Скрытые треды всех досок: свежие сверху — так они идут в списке настроек. */
    @Query("SELECT * FROM hidden_threads ORDER BY hidden_at DESC")
    fun hiddenThreads(): Flow<List<HiddenThreadEntity>>

    /** Скрытые треды одной доски: по ним фильтруется её лента. */
    @Query("SELECT * FROM hidden_threads WHERE board_id = :boardId ORDER BY hidden_at DESC")
    fun hiddenThreads(boardId: String): Flow<List<HiddenThreadEntity>>

    @Upsert
    suspend fun hideThread(thread: HiddenThreadEntity)

    @Query("DELETE FROM hidden_threads WHERE board_id = :boardId AND thread_num = :threadNum")
    suspend fun unhideThread(boardId: String, threadNum: Int)

    @Query("DELETE FROM hidden_threads")
    suspend fun unhideAllThreads()
}
