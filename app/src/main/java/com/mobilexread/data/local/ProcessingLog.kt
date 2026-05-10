package com.mobilexread.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LogLevel { INFO, WARN, ERROR }

@Entity(tableName = "processing_logs")
data class ProcessingLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val articleId: Long?,        // null for app-level events
    val url: String?,            // snippet of the URL being processed
    val timestampMillis: Long,
    val level: LogLevel,
    val message: String
)
