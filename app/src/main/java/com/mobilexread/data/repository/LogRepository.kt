package com.mobilexread.data.repository

import com.mobilexread.data.local.LogLevel
import com.mobilexread.data.local.ProcessingLog
import com.mobilexread.data.local.ProcessingLogDao
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogRepository @Inject constructor(
    private val dao: ProcessingLogDao
) {
    val logs: Flow<List<ProcessingLog>> = dao.observeAll()

    suspend fun info(message: String, articleId: Long? = null, url: String? = null) =
        dao.insert(ProcessingLog(
            articleId = articleId,
            url = url?.take(80),
            timestampMillis = Instant.now().toEpochMilli(),
            level = LogLevel.INFO,
            message = message
        ))

    suspend fun warn(message: String, articleId: Long? = null, url: String? = null) =
        dao.insert(ProcessingLog(
            articleId = articleId,
            url = url?.take(80),
            timestampMillis = Instant.now().toEpochMilli(),
            level = LogLevel.WARN,
            message = message
        ))

    suspend fun error(message: String, cause: Throwable? = null, articleId: Long? = null, url: String? = null) {
        val full = if (cause != null) {
            val trace = cause.stackTraceToString().take(1200)
            "$message\n$trace"
        } else {
            message
        }
        dao.insert(ProcessingLog(
            articleId = articleId,
            url = url?.take(80),
            timestampMillis = Instant.now().toEpochMilli(),
            level = LogLevel.ERROR,
            message = full
        ))
    }

    suspend fun clearAll() = dao.clearAll()
}
