package com.mobilexread.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.mobilexread.data.repository.ArticleRepository
import com.mobilexread.worker.ProcessingWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ShareState {
    data object Idle : ShareState()
    data object Queued : ShareState()
    data class AlreadyQueued(val status: String) : ShareState()
    data class Error(val message: String) : ShareState()
}

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val repository: ArticleRepository,
    private val workManager: WorkManager
) : ViewModel() {

    private val _state = MutableStateFlow<ShareState>(ShareState.Idle)
    val state: StateFlow<ShareState> = _state

    fun enqueue(url: String) {
        viewModelScope.launch {
            try {
                // Dedup: if this URL is already known (any status), don't re-enqueue
                val existing = repository.findByUrl(url)
                if (existing != null) {
                    _state.value = ShareState.AlreadyQueued(existing.status.name)
                    return@launch
                }

                val articleId = repository.enqueue(url)

                val inputData = Data.Builder()
                    .putLong(ProcessingWorker.KEY_ARTICLE_ID, articleId)
                    .putString(ProcessingWorker.KEY_URL, url)
                    .build()

                val request = OneTimeWorkRequestBuilder<ProcessingWorker>()
                    .setInputData(inputData)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()

                // Unique work prevents two workers racing on the same article
                workManager.enqueueUniqueWork(
                    "process_article_$articleId",
                    ExistingWorkPolicy.KEEP,
                    request
                )
                _state.value = ShareState.Queued
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = ShareState.Error(e.message ?: "Erro ao adicionar à fila")
            }
        }
    }
}
