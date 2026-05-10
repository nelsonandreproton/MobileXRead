package com.mobilexread.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val originalUrl: String,
    val authorHandle: String,
    val rawContent: String,
    val summaryJson: String,
    val tweetDateMillis: Long?,
    val processedDateMillis: Long,
    val language: String
)
