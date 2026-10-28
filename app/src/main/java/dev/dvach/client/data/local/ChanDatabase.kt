package dev.dvach.client.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [BookmarkEntity::class, HistoryEntity::class, PinnedBoardEntity::class],
    version = 2,
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
