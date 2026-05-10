package com.mobilexread.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class ArticleStatusConverter {
    @TypeConverter
    fun fromStatus(status: ArticleStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): ArticleStatus =
        runCatching { ArticleStatus.valueOf(value) }.getOrDefault(ArticleStatus.COMPLETED)
}

class LogLevelConverter {
    @TypeConverter
    fun fromLevel(level: LogLevel): String = level.name

    @TypeConverter
    fun toLevel(value: String): LogLevel =
        runCatching { LogLevel.valueOf(value) }.getOrDefault(LogLevel.INFO)
}

@Database(
    entities = [ArticleEntity::class, ProcessingLog::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(ArticleStatusConverter::class, LogLevelConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun processingLogDao(): ProcessingLogDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE articles ADD COLUMN status TEXT NOT NULL DEFAULT 'COMPLETED'"
                )
                db.execSQL(
                    "ALTER TABLE articles ADD COLUMN errorMessage TEXT"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS processing_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        articleId INTEGER,
                        url TEXT,
                        timestampMillis INTEGER NOT NULL,
                        level TEXT NOT NULL,
                        message TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
