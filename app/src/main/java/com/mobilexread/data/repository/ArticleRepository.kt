package com.mobilexread.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mobilexread.data.local.ArticleDao
import com.mobilexread.data.local.ArticleEntity
import com.mobilexread.data.local.ArticleStatus
import com.mobilexread.data.model.Article
import com.mobilexread.data.model.toArticle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(
    private val dao: ArticleDao,
    private val gson: Gson
) {
    /** Completed articles, most recent first */
    val completedArticles: Flow<List<Article>> = dao.getCompletedArticles().map { entities ->
        entities.map { it.toArticle(parseSummaryJson(it.summaryJson)) }
    }

    /** Failed articles */
    val failedArticles: Flow<List<Article>> = dao.getFailedArticles().map { entities ->
        entities.map { it.toArticle(parseSummaryJson(it.summaryJson)) }
    }

    /** Combined list: completed + failed, for the main screen */
    val displayArticles: Flow<List<Article>> = combine(completedArticles, failedArticles) { completed, failed ->
        // failed items appear at the top so they're easy to see and retry
        failed + completed
    }

    /** How many items are still being processed (PENDING + PROCESSING) */
    val queueCount: Flow<Int> = dao.observeQueueCount()

    suspend fun getArticleById(id: Long): Article? =
        dao.getArticleById(id)?.let { it.toArticle(parseSummaryJson(it.summaryJson)) }

    /**
     * Enqueue a URL for processing. Creates a PENDING skeleton record and returns its ID.
     * The Worker will update it to COMPLETED or FAILED.
     */
    suspend fun enqueue(url: String): Long {
        val entity = ArticleEntity(
            id = 0,
            title = url,  // placeholder until processing fills it in
            originalUrl = url,
            authorHandle = "",
            rawContent = "",
            summaryJson = "[]",
            tweetDateMillis = null,
            processedDateMillis = Instant.now().toEpochMilli(),
            language = "",
            status = ArticleStatus.PENDING
        )
        return dao.insertArticle(entity)
    }

    suspend fun markPending(id: Long) = dao.updateStatus(id, ArticleStatus.PENDING)

    suspend fun markProcessing(id: Long) = dao.updateStatus(id, ArticleStatus.PROCESSING)

    suspend fun markCompleted(id: Long, article: Article) {
        val entity = ArticleEntity(
            id = id,
            title = article.title,
            originalUrl = article.originalUrl,
            authorHandle = article.authorHandle,
            rawContent = article.rawContent,
            summaryJson = gson.toJson(article.summaryPoints),
            tweetDateMillis = article.tweetDate?.toEpochMilli(),
            processedDateMillis = Instant.now().toEpochMilli(),
            language = article.language,
            status = ArticleStatus.COMPLETED,
            errorMessage = null
        )
        dao.updateArticle(entity)
    }

    suspend fun markFailed(id: Long, error: String) =
        dao.updateStatusWithError(id, ArticleStatus.FAILED, error)

    /** Call once on app start to recover items left in PENDING/PROCESSING by a previous crash */
    suspend fun recoverOrphanedItems() =
        dao.recoverOrphanedItems("Interrompido — app reiniciada")

    suspend fun saveArticle(article: Article): Long {
        val entity = ArticleEntity(
            id = article.id,
            title = article.title,
            originalUrl = article.originalUrl,
            authorHandle = article.authorHandle,
            rawContent = article.rawContent,
            summaryJson = gson.toJson(article.summaryPoints),
            tweetDateMillis = article.tweetDate?.toEpochMilli(),
            processedDateMillis = article.processedDate.toEpochMilli(),
            language = article.language,
            status = article.status,
            errorMessage = article.errorMessage
        )
        return dao.insertArticle(entity)
    }

    /** Finds any article with this URL, regardless of status (used for dedup check) */
    suspend fun findByUrl(url: String): Article? =
        dao.getArticleByUrl(url)?.let { it.toArticle(parseSummaryJson(it.summaryJson)) }

    suspend fun deleteArticle(id: Long) = dao.deleteArticleById(id)

    private fun parseSummaryJson(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(json, type) ?: emptyList()
        }.getOrElse { emptyList() }
    }
}
