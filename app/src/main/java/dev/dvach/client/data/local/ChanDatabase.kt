package dev.dvach.client.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BookmarkEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ChanDatabase : RoomDatabase() {
    abstract fun chanDao(): ChanDao
}
