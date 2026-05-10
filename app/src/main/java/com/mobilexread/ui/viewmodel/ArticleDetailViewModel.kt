package com.mobilexread.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilexread.data.model.Article
import com.mobilexread.data.repository.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ArticleRepository
) : ViewModel() {

    private val articleId: Long = savedStateHandle["articleId"] ?: 0L

    private val _article = MutableStateFlow<Article?>(null)
    val article: StateFlow<Article?> = _article

    init {
        viewModelScope.launch {
            _article.value = repository.getArticleById(articleId)
        }
    }

    fun buildShareText(article: Article): String {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
        val tweetDateStr = article.tweetDate?.let {
            dateFormatter.format(it.atZone(ZoneId.systemDefault()))
        } ?: "desconhecida"
        val processedStr = dateFormatter.format(article.processedDate.atZone(ZoneId.systemDefault()))

        return buildString {
            appendLine(article.title)
            appendLine()
            appendLine(article.originalUrl)
            appendLine()
            article.summaryPoints.forEachIndexed { i, point ->
                appendLine("${i + 1}. $point")
            }
            appendLine()
            appendLine("Publicado: $tweetDateStr | Processado: $processedStr")
        }
    }

    fun deleteArticle(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteArticle(articleId)
            onDeleted()
        }
    }
}
