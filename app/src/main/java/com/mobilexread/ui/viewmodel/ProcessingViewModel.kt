package com.mobilexread.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilexread.data.model.Article
import com.mobilexread.data.repository.ArticleRepository
import com.mobilexread.llm.GemmaInference
import com.mobilexread.scraper.NitterScraper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

sealed class ProcessingState {
    data object Idle : ProcessingState()
    data class Loading(val step: String) : ProcessingState()
    data class Success(val articleId: Long) : ProcessingState()
    data class Error(val message: String) : ProcessingState()
}

@HiltViewModel
class ProcessingViewModel @Inject constructor(
    private val scraper: NitterScraper,
    private val gemmaInference: GemmaInference,
    private val repository: ArticleRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val state: StateFlow<ProcessingState> = _state

    fun process(url: String) {
        if (_state.value is ProcessingState.Loading) return
        viewModelScope.launch {
            runCatching {
                _state.value = ProcessingState.Loading("A validar URL…")
                if (!url.contains("x.com") && !url.contains("twitter.com")) {
                    throw IllegalArgumentException("URL não reconhecido. Partilha um link do X/Twitter.")
                }

                _state.value = ProcessingState.Loading("A extrair tweet…")
                val tweetData = scraper.scrape(url)

                val threadCount = tweetData.threadContents.size
                if (threadCount > 0) {
                    _state.value = ProcessingState.Loading("A ler thread ($threadCount respostas)…")
                }
                if (tweetData.embeddedTweetTexts.isNotEmpty()) {
                    _state.value = ProcessingState.Loading("A ler tweets incorporados…")
                }

                _state.value = ProcessingState.Loading("A carregar modelo de IA…")
                gemmaInference.loadModel()

                _state.value = ProcessingState.Loading("A gerar resumo (pode demorar 30-90s)…")
                val summary = gemmaInference.summarize(tweetData)

                _state.value = ProcessingState.Loading("A guardar…")
                val article = Article(
                    id = 0,
                    title = summary.title,
                    originalUrl = url,
                    authorHandle = tweetData.handle,
                    rawContent = tweetData.fullText(),
                    summaryPoints = summary.points,
                    tweetDate = tweetData.publishedDateMillis?.let { Instant.ofEpochMilli(it) },
                    processedDate = Instant.now(),
                    language = summary.detectedLanguage
                )
                val savedId = repository.saveArticle(article)
                _state.value = ProcessingState.Success(savedId)
            }.onFailure { e ->
                _state.value = ProcessingState.Error(
                    e.message ?: "Erro desconhecido. Tenta novamente."
                )
            }
        }
    }

    fun reset() {
        _state.value = ProcessingState.Idle
    }
}
