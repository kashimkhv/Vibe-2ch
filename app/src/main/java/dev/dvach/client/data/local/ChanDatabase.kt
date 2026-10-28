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
    ],
    version = 3,
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
