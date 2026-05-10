package com.mobilexread.data.model

import com.mobilexread.data.local.ArticleEntity
import com.mobilexread.data.local.ArticleStatus
import java.time.Instant

data class Article(
    val id: Long,
    val title: String,
    val originalUrl: String,
    val authorHandle: String,
    val rawContent: String,
    val summaryPoints: List<String>,
    val tweetDate: Instant?,
    val processedDate: Instant,
    val language: String,
    val status: ArticleStatus = ArticleStatus.COMPLETED,
    val errorMessage: String? = null
)

fun ArticleEntity.toArticle(summaryPoints: List<String>): Article = Article(
    id = id,
    title = title,
    originalUrl = originalUrl,
    authorHandle = authorHandle,
    rawContent = rawContent,
    summaryPoints = summaryPoints,
    tweetDate = tweetDateMillis?.let { Instant.ofEpochMilli(it) },
    processedDate = Instant.ofEpochMilli(processedDateMillis),
    language = language,
    status = status,
    errorMessage = errorMessage
)
