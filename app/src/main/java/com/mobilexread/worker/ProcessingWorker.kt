package com.mobilexread.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.mobilexread.data.local.ArticleStatus
import com.mobilexread.data.model.Article
import com.mobilexread.data.repository.ArticleRepository
import com.mobilexread.data.repository.LogRepository
import com.mobilexread.llm.GemmaInference
import com.mobilexread.raindrop.RaindropRepository
import com.mobilexread.scraper.FxTwitterScraper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import java.time.Instant

@HiltWorker
class ProcessingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val scraper: FxTwitterScraper,
    private val gemmaInference: GemmaInference,
    private val repository: ArticleRepository,
    private val logRepository: LogRepository,
    private val raindropRepository: RaindropRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_ARTICLE_ID = "article_id"
        const val KEY_URL = "url"
        private const val NOTIFICATION_CHANNEL_ID = "processing"
        private const val NOTIFICATION_ID = 1001
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = buildForegroundInfo("A processar…")

    override suspend fun doWork(): Result {
        val articleId = inputData.getLong(KEY_ARTICLE_ID, -1L)
        val url = inputData.getString(KEY_URL) ?: run {
            logRepository.error("Worker iniciado sem URL (articleId=$articleId)")
            return Result.failure()
        }

        if (articleId == -1L) {
            logRepository.error("Worker iniciado sem articleId (url=$url)")
            return Result.failure()
        }

        logRepository.info("▶ Início do processamento", articleId = articleId, url = url)
        setForeground(buildForegroundInfo("A processar…"))

        return try {
            repository.markProcessing(articleId)
            logRepository.info("→ Estado: PROCESSING", articleId = articleId, url = url)

            logRepository.info("→ A extrair conteúdo via FxTwitter…", articleId = articleId, url = url)
            val tweetData = scraper.scrape(url)
            logRepository.info(
                "→ Conteúdo extraído: @${tweetData.handle}, ${tweetData.fullText().length} chars",
                articleId = articleId, url = url
            )

            val modelLoaded = gemmaInference.isLoaded()
            logRepository.info(
                if (modelLoaded) "→ Modelo já carregado, a gerar resumo…"
                else "→ Modelo não carregado, a inicializar…",
                articleId = articleId, url = url
            )
            setForeground(buildForegroundInfo(
                if (modelLoaded) "A gerar resumo…" else "A carregar modelo de IA…"
            ))

            val summary = gemmaInference.summarize(tweetData)
            logRepository.info(
                "→ Resumo gerado: \"${summary.title.take(60)}\" (${summary.points.size} pontos, lang=${summary.detectedLanguage})",
                articleId = articleId, url = url
            )

            val article = Article(
                id = articleId,
                title = summary.title,
                originalUrl = url,
                authorHandle = tweetData.handle,
                rawContent = tweetData.fullText(),
                summaryPoints = summary.points,
                tweetDate = tweetData.publishedDateMillis?.let { Instant.ofEpochMilli(it) },
                processedDate = Instant.now(),
                language = summary.detectedLanguage,
                status = ArticleStatus.COMPLETED
            )
            repository.markCompleted(articleId, article)
            logRepository.info("✓ Concluído com sucesso", articleId = articleId, url = url)
            showCompletionNotification(summary.title)

            val excerpt = summary.points.joinToString("\n") { "• $it" }
            val raindropResult = raindropRepository.sendBookmark(url, summary.title, excerpt)
            raindropResult.fold(
                onSuccess = { logRepository.info("→ Guardado no Raindrop.io", articleId = articleId, url = url) },
                onFailure = { e -> logRepository.warn("→ Raindrop ignorado: ${e.message}", articleId = articleId, url = url) }
            )

            Result.success()
        } catch (e: CancellationException) {
            logRepository.warn("⚡ Worker cancelado", articleId = articleId, url = url)
            throw e
        } catch (e: Exception) {
            val msg = e.message ?: "Erro desconhecido"
            logRepository.error("✗ Falha: $msg", cause = e, articleId = articleId, url = url)
            repository.markFailed(articleId, msg)
            showErrorNotification(msg)
            Result.failure()
        }
    }

    private fun buildForegroundInfo(text: String): ForegroundInfo {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("MobileXRead")
            .setContentText(text)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun showCompletionNotification(title: String) {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("Resumo pronto")
            .setContentText(title)
            .setAutoCancel(true)
            .build()
        notificationManager().notify(NOTIFICATION_ID + 1, notification)
    }

    private fun showErrorNotification(error: String) {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("Erro ao processar")
            .setContentText(error)
            .setAutoCancel(true)
            .build()
        notificationManager().notify(NOTIFICATION_ID + 2, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Processamento",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Estado do processamento de tweets"
            }
            notificationManager().createNotificationChannel(channel)
        }
    }

    private fun notificationManager(): NotificationManager =
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}
