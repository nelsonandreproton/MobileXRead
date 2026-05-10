package com.mobilexread.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.mobilexread.scraper.TweetData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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

    suspend fun loadModel(): Unit = withContext(Dispatchers.IO) {
        withTimeout(540_000L) { // 9 min — under WorkManager's 10 min hard limit
            loadMutex.withLock {
                if (llmInference != null) return@withLock
                val path = modelManager.getModelPathSync()
                    ?: throw IllegalStateException("Modelo não encontrado. Configura o modelo nas definições.")
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(path)
                    .setMaxTokens(2048)
                    .build()
                llmInference = LlmInference.createFromOptions(context, options)
            }
        }
    }

    private fun newSession(engine: LlmInference): LlmInferenceSession {
        val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTemperature(0.7f)
            .setTopK(40)
            .build()
        return LlmInferenceSession.createFromOptions(engine, sessionOptions)
    }

    suspend fun summarize(
        tweetData: TweetData,
        onPartialResult: (String) -> Unit = {}
    ): SummaryResult = withContext(Dispatchers.IO) {
        // loadModel() handles the mutex internally — don't hold the lock here
        if (llmInference == null) loadModel()
        val engine = llmInference
            ?: throw IllegalStateException("Modelo não carregado após loadModel()")
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
        val session = newSession(engine)
        runCatching {
            session.addQueryChunk(prompt)
            session.generateResponseAsync { partial, done ->
                if (partial != null) {
                    accumulated += partial
                    onToken(accumulated)
                }
                if (done && !deferred.isCompleted) deferred.complete(accumulated)
            }
        }.onFailure { e ->
            if (!deferred.isCompleted) deferred.completeExceptionally(e)
        }
        return try {
            deferred.await()
        } finally {
            runCatching { session.close() }
        }
    }

    private fun buildPrompt(handle: String, content: String): String = """
Summarize the tweet below. Do NOT write any preamble or commentary — output ONLY the structured response.

Rules:
- TITLE: one descriptive sentence (max 80 chars) capturing the core idea; do NOT start with "@$handle", "Okay", "Sure", "Here", or conversational words
- 10 bullet points with the key ideas
- Use the SAME LANGUAGE as the tweet content

TITLE: <one-sentence summary of the main idea>
1. <key point>
2. <key point>
3. <key point>
4. <key point>
5. <key point>
6. <key point>
7. <key point>
8. <key point>
9. <key point>
10. <key point>

Tweet by @$handle:
$content

TITLE:""".trimIndent()

    private fun parseResponse(raw: String): SummaryResult {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }

        // Model continues directly after the "TITLE:" prompt suffix, so the first
        // non-blank line is the title text; fall back to an explicit TITLE: tag.
        val titleLine = lines.firstOrNull { it.startsWith("TITLE:", ignoreCase = true) }
        val title = when {
            titleLine != null -> titleLine.removePrefix("TITLE:").removePrefix("title:").trim()
            lines.isNotEmpty() && !Regex("^\\d{1,2}[.)\\-]").containsMatchIn(lines[0]) -> lines[0]
            else -> "Sem título"
        }

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
