package com.mobilexread.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.mobilexread.data.model.Article
import com.mobilexread.data.repository.ArticleRepository
import com.mobilexread.worker.ProcessingWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArticleListViewModel @Inject constructor(
    private val repository: ArticleRepository,
    private val workManager: WorkManager
) : ViewModel() {

    /** Completed + failed articles shown in the list */
    val articles: StateFlow<List<Article>> = repository.displayArticles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Number of items still in PENDING or PROCESSING state */
    val queueCount: StateFlow<Int> = repository.queueCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun deleteArticle(id: Long) {
        viewModelScope.launch { repository.deleteArticle(id) }
    }

    fun retryArticle(article: Article) {
        viewModelScope.launch {
            // Reset to PENDING so it re-appears in queue count and re-runs
            repository.markPending(article.id)

            val inputData = Data.Builder()
                .putLong(ProcessingWorker.KEY_ARTICLE_ID, article.id)
                .putString(ProcessingWorker.KEY_URL, article.originalUrl)
                .build()

            val request = OneTimeWorkRequestBuilder<ProcessingWorker>()
                .setInputData(inputData)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            // Unique work prevents two workers racing on the same article
            workManager.enqueueUniqueWork(
                "process_article_${article.id}",
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
