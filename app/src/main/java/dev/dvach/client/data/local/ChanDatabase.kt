package dev.dvach.client.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BookmarkEntity::class,
        HistoryEntity::class,
        PinnedBoardEntity::class,
        ThreadScrollEntity::class,
        MyPostEntity::class,
        HiddenThreadEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class ChanDatabase : RoomDatabase() {
    abstract fun chanDao(): ChanDao
}

/**
 * v2: закреплённые доски. Миграция ручная — иначе при обновлении приложения
 * Room либо упадёт, либо сотрёт закладки и историю.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `pinned_boards` (" +
                "`board_id` TEXT NOT NULL, " +
                "`pinned_at` INTEGER NOT NULL, " +
                "PRIMARY KEY(`board_id`))",
        )
    }
}

/**
 * v3: позиция чтения треда. Миграция ручная — иначе Room сотрёт закладки,
 * историю и закреплённые доски.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `thread_scroll` (" +
                "`thread_key` TEXT NOT NULL, " +
                "`post_num` INTEGER NOT NULL, " +
                "`scroll_index` INTEGER NOT NULL, " +
                "`scroll_offset` INTEGER NOT NULL, " +
                "`updated_at` INTEGER NOT NULL, " +
                "PRIMARY KEY(`thread_key`))",
        )
    }
}

/**
 * v4: свои посты для подсветки «мой пост». Миграция ручная — без неё Room
 * сотрёт закладки, историю, позиции чтения и закреплённые доски.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `my_posts` (" +
                "`board_id` TEXT NOT NULL, " +
                "`thread_num` INTEGER NOT NULL, " +
                "`post_num` INTEGER NOT NULL, " +
                "`created_at` INTEGER NOT NULL, " +
                "PRIMARY KEY(`board_id`, `post_num`))",
        )
    }
}

/**
 * v5: скрытые треды. Миграция ручная — без неё Room сотрёт закладки, историю,
 * позиции чтения, свои посты и закреплённые доски.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `hidden_threads` (" +
                "`board_id` TEXT NOT NULL, " +
                "`thread_num` INTEGER NOT NULL, " +
                "`subject` TEXT NOT NULL, " +
                "`hidden_at` INTEGER NOT NULL, " +
                "PRIMARY KEY(`board_id`, `thread_num`))",
        )
    }
}
