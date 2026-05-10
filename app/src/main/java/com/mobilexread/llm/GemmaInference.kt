package com.mobilexread.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.mobilexread.scraper.TweetData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class SummaryResult(
    val title: String,
    val points: List<String>,
    val detectedLanguage: String
)

@Singleton
class GemmaInference @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelManager: ModelManager
) {
    private var llmInference: LlmInference? = null
    private val loadMutex = Mutex()

    fun isLoaded(): Boolean = llmInference != null

    suspend fun loadModel(): Unit = withContext(Dispatchers.Default) {
        loadMutex.withLock {
            if (llmInference != null) return@withLock
            val path = modelManager.getModelPathSync()
                ?: throw IllegalStateException("Modelo não encontrado. Configura o modelo nas definições.")
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(path)
                .setMaxTokens(2048)
                .setTemperature(0.7f)
                .setTopK(40)
                .build()
            llmInference = LlmInference.createFromOptions(context, options)
        }
    }

    suspend fun summarize(
        tweetData: TweetData,
        onPartialResult: (String) -> Unit = {}
    ): SummaryResult = withContext(Dispatchers.Default) {
        val engine = loadMutex.withLock {
            llmInference ?: run {
                loadModel()
                llmInference!!
            }
        }
        val prompt = buildPrompt(tweetData.handle, tweetData.fullText().take(6000))
        val raw = generateStreaming(engine, prompt, onPartialResult)
        parseResponse(raw)
    }

    // Uses generateResponseAsync for real-time token streaming
    private suspend fun generateStreaming(
        engine: LlmInference,
        prompt: String,
        onToken: (String) -> Unit
    ): String {
        val deferred = CompletableDeferred<String>()
        var accumulated = ""
        runCatching {
            engine.generateResponseAsync(prompt) { partial, done ->
                if (partial != null) {
                    accumulated += partial
                    onToken(accumulated)
                }
                if (done && !deferred.isCompleted) deferred.complete(accumulated)
            }
        }.onFailure { e ->
            if (!deferred.isCompleted) deferred.completeExceptionally(e)
        }
        return deferred.await()
    }

    private fun buildPrompt(handle: String, content: String): String = """
You are an expert content summarizer. Summarize the following tweet thread by @$handle.

Instructions:
- Write a one-line title (max 80 chars) that captures the main idea, starting with "@$handle — "
- Write exactly 10 bullet points summarizing the key ideas
- Use the SAME LANGUAGE as the tweet content
- Be concise and informative
- Format your response EXACTLY as shown below

TITLE: [title here]
1. [point 1]
2. [point 2]
3. [point 3]
4. [point 4]
5. [point 5]
6. [point 6]
7. [point 7]
8. [point 8]
9. [point 9]
10. [point 10]

Tweet thread content:
$content

Response:
""".trimIndent()

    private fun parseResponse(raw: String): SummaryResult {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
        val titleLine = lines.firstOrNull { it.startsWith("TITLE:", ignoreCase = true) }
        val title = titleLine
            ?.removePrefix("TITLE:")?.removePrefix("title:")?.trim()
            ?: lines.firstOrNull() ?: "Sem título"

        val pointRegex = Regex("^(\\d{1,2})[.)\\-]\\s+(.+)")
        val points = lines.mapNotNull { line ->
            pointRegex.find(line)?.groupValues?.get(2)?.trim()
        }.take(10)

        val finalPoints = if (points.size < 10) {
            val remaining = lines
                .filterNot { it.startsWith("TITLE:", ignoreCase = true) }
                .filterNot { pointRegex.containsMatchIn(it) }
                .take(10 - points.size)
            points + remaining
        } else points

        val textForLang = (title + " " + finalPoints.joinToString(" ")).trim()
        val language = if (textForLang.length > 20) detectLanguage(textForLang) else "en"
        return SummaryResult(title = title, points = finalPoints.take(10), detectedLanguage = language)
    }

    private fun detectLanguage(text: String): String {
        val lower = text.lowercase()
        val ptMarkers = listOf(" que ", " com ", " para ", " uma ", " não ", " são ", " mais ", " por ", " dos ", " das ")
        val esMarkers = listOf(" que ", " con ", " para ", " una ", " los ", " las ", " más ", " por ", " del ", " está ")
        val enMarkers = listOf(" the ", " and ", " for ", " that ", " with ", " this ", " from ", " have ", " are ", " not ")
        val ptScore = ptMarkers.count { lower.contains(it) }
        val esScore = esMarkers.count { lower.contains(it) } - ptScore / 2
        val enScore = enMarkers.count { lower.contains(it) }
        return when {
            ptScore >= esScore && ptScore >= enScore -> "pt"
            esScore > ptScore && esScore >= enScore -> "es"
            else -> "en"
        }
    }

    fun close() {
        llmInference?.close()
        llmInference = null
    }
}
