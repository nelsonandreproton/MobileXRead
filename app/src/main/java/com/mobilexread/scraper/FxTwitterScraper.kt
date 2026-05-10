package com.mobilexread.scraper

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FxTwitterScraper @Inject constructor(
    private val httpClient: OkHttpClient
) {
    private val urlRegex = Regex("(?:x\\.com|twitter\\.com)/([^/?#]+)/status/(\\d+)")
    private val anonUrlRegex = Regex("(?:x\\.com|twitter\\.com)/i/status/(\\d+)")

    suspend fun scrape(twitterUrl: String): TweetData = withContext(Dispatchers.IO) {
        // Anonymous URL (/i/status/) — no username, call API with placeholder and let response supply it
        val anonMatch = anonUrlRegex.find(twitterUrl)
        if (anonMatch != null) {
            val statusId = anonMatch.groupValues[1]
            val apiUrl = "https://api.fxtwitter.com/i/status/$statusId"
            val body = fetch(apiUrl)
            return@withContext parseFxTwitterJson(body, twitterUrl)
        }

        val match = urlRegex.find(twitterUrl)
            ?: throw IllegalArgumentException("URL inválido: $twitterUrl")
        val handle = match.groupValues[1]
        val statusId = match.groupValues[2]
        val canonicalUrl = "https://x.com/$handle/status/$statusId"
        val body = fetch("https://api.fxtwitter.com/$handle/status/$statusId")
        parseFxTwitterJson(body, canonicalUrl)
    }

    private fun fetch(apiUrl: String): String {
        val request = Request.Builder().url(apiUrl).build()
        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("FxTwitter HTTP ${response.code}")
            response.body?.string() ?: throw Exception("FxTwitter: resposta vazia")
        }
    }

    private fun parseFxTwitterJson(json: String, originalUrl: String): TweetData {
        val root = JsonParser.parseString(json).asJsonObject
        val code = root.get("code")?.asInt
        if (code != 200) {
            val message = root.get("message")?.asString ?: "erro desconhecido"
            throw Exception("FxTwitter code=$code: $message")
        }
        val tweet = root.getAsJsonObject("tweet")
            ?: throw Exception("FxTwitter: resposta sem campo 'tweet'")

        val author = tweet.getAsJsonObject("author")
        val handle = author?.get("screen_name")?.takeIf { !it.isJsonNull }?.asString ?: ""
        val displayName = author?.get("name")?.takeIf { !it.isJsonNull }?.asString ?: handle

        // Canonical URL: prefer the one returned by FxTwitter (has the real username)
        val canonicalUrl = tweet.get("url")?.takeIf { !it.isJsonNull }?.asString
            ?: originalUrl

        val timestamp = tweet.get("created_timestamp")?.takeIf { !it.isJsonNull }?.asLong?.times(1000L)
            ?: parseRfc1123(tweet.get("created_at")?.takeIf { !it.isJsonNull }?.asString)

        // X Article — content is in tweet.article instead of tweet.text
        val article = tweet.getAsJsonObject("article")
        if (article != null) {
            val content = extractArticleText(article)
            if (content.isBlank()) throw Exception("FxTwitter: article sem texto")
            return TweetData(
                originalUrl = canonicalUrl,
                handle = handle,
                displayName = displayName,
                mainContent = content,
                threadContents = emptyList(),
                embeddedTweetTexts = emptyList(),
                publishedDateMillis = timestamp
            )
        }

        // Regular tweet
        val text = tweet.get("text")?.takeIf { !it.isJsonNull }?.asString?.takeIf { it.isNotBlank() }
            ?: throw Exception("FxTwitter: tweet sem texto")

        return TweetData(
            originalUrl = canonicalUrl,
            handle = handle,
            displayName = displayName,
            mainContent = text,
            threadContents = emptyList(),
            embeddedTweetTexts = emptyList(),
            publishedDateMillis = timestamp
        )
    }

    /**
     * Extracts readable text from an X Article.
     * Structure: article.title + article.content.blocks[].text (ordered)
     */
    private fun extractArticleText(article: JsonObject): String {
        val title = article.get("title")?.takeIf { !it.isJsonNull }?.asString ?: ""
        val blocks = article.getAsJsonObject("content")
            ?.getAsJsonArray("blocks") ?: return title
        val body = blocks.joinToString("\n\n") { block ->
            block.asJsonObject.get("text")?.takeIf { !it.isJsonNull }?.asString?.trim() ?: ""
        }.trim()
        return if (title.isNotBlank()) "$title\n\n$body" else body
    }

    private fun parseRfc1123(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val fmt = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return runCatching { fmt.parse(raw)?.time }.getOrNull()
    }
}
