package com.mobilexread

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.mobilexread.data.repository.ArticleRepository
import com.mobilexread.data.repository.LogRepository
import com.mobilexread.llm.GemmaInference
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MobileXReadApp : Application(), Configuration.Provider {

    @Inject lateinit var gemmaInference: GemmaInference
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var logRepository: LogRepository
    @Inject lateinit var articleRepository: ArticleRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        installCrashLogger()

        appScope.launch {
            // Recover any items left in PENDING/PROCESSING by a previous crash
            runCatching { articleRepository.recoverOrphanedItems() }
                .onSuccess { logRepository.info("Recuperação de itens interrompidos concluída") }
                .onFailure { e -> logRepository.error("Falha na recuperação de itens: ${e.message}", cause = e) }

            // Pre-warm the model in background so it's ready when the first job runs
            runCatching { gemmaInference.loadModel() }
                .onFailure { e ->
                    logRepository.error("Falha no pré-carregamento do modelo: ${e.message}", cause = e)
                }
        }
    }

    private fun installCrashLogger() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Write to logcat (always available even if DB fails)
            Log.e("MobileXRead", "CRASH no thread '${thread.name}'", throwable)
            // Best-effort write to DB — may fail if Hilt not yet initialized
            runCatching {
                appScope.launch {
                    logRepository.error(
                        "💥 CRASH no thread '${thread.name}': ${throwable.message}",
                        cause = throwable
                    )
                }
            }
            // Let the default handler terminate the process
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
