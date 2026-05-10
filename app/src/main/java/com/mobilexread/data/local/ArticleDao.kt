package com.mobilexread.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    /** All completed articles, newest first */
    @Query("SELECT * FROM articles WHERE status = 'COMPLETED' ORDER BY processedDateMillis DESC")
    fun getCompletedArticles(): Flow<List<ArticleEntity>>

    /** All failed articles */
    @Query("SELECT * FROM articles WHERE status = 'FAILED' ORDER BY processedDateMillis DESC")
    fun getFailedArticles(): Flow<List<ArticleEntity>>

    /** Queue count: items still in flight (PENDING or PROCESSING) */
    @Query("SELECT COUNT(*) FROM articles WHERE status IN ('PENDING', 'PROCESSING')")
    fun observeQueueCount(): Flow<Int>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getArticleById(id: Long): ArticleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: ArticleEntity): Long

    @Update
    suspend fun updateArticle(article: ArticleEntity)

    @Delete
    suspend fun deleteArticle(article: ArticleEntity)

    @Query("DELETE FROM articles WHERE id = :id")
    suspend fun deleteArticleById(id: Long)

    /** Check for any existing entry by URL regardless of status (dedup) */
    @Query("SELECT * FROM articles WHERE originalUrl = :url LIMIT 1")
    suspend fun getArticleByUrl(url: String): ArticleEntity?

    @Query("UPDATE articles SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: ArticleStatus)

    @Query("UPDATE articles SET status = :status, errorMessage = :error WHERE id = :id")
    suspend fun updateStatusWithError(id: Long, status: ArticleStatus, error: String)

    /** On app start: any PENDING/PROCESSING item has no running worker — mark as FAILED so the user can retry */
    @Query("UPDATE articles SET status = 'FAILED', errorMessage = :error WHERE status IN ('PENDING', 'PROCESSING')")
    suspend fun recoverOrphanedItems(error: String)
}
