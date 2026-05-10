package com.mobilexread.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mobilexread.data.local.ArticleDao
import com.mobilexread.data.local.ArticleEntity
import com.mobilexread.data.model.Article
import com.mobilexread.data.model.toArticle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(
    private val dao: ArticleDao,
    private val gson: Gson
) {
    val articles: Flow<List<Article>> = dao.getAllArticles().map { entities ->
        entities.map { it.toArticle(parseSummaryJson(it.summaryJson)) }
    }

    suspend fun getArticleById(id: Long): Article? =
        dao.getArticleById(id)?.let { it.toArticle(parseSummaryJson(it.summaryJson)) }

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
            language = article.language
        )
        return dao.insertArticle(entity)
    }

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
