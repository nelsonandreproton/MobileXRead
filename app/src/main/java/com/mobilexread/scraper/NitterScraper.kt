package com.mobilexread.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NitterScraper @Inject constructor(
    private val httpClient: OkHttpClient
) {
    private val instances = listOf(
        "nitter.privacydev.net",
        "nitter.poast.org",
        "nitter.space",
        "nitter.1d4.us",
        "nitter.kavin.rocks",
        "nitter.unixfox.eu",
        "nitter.tiekoetter.com"
    )

    suspend fun scrape(twitterUrl: String, maxEmbedDepth: Int = 2): TweetData =
        withContext(Dispatchers.IO) {
            val (handle, statusId) = parseUrl(twitterUrl)
            var lastError: Exception = Exception("No instances available")
            for (instance in instances) {
                runCatching {
                    scrapeFromInstance(instance, handle, statusId, maxEmbedDepth)
                }.onSuccess { return@withContext it }
                    .onFailure { lastError = it as? Exception ?: Exception(it.message) }
            }
            throw lastError
        }

    private fun parseUrl(url: String): Pair<String, String> {
        val regex = Regex("(?:x\\.com|twitter\\.com)/([^/?#]+)/status/(\\d+)")
        val match = regex.find(url) ?: throw IllegalArgumentException("URL inválido: $url")
        return match.groupValues[1] to match.groupValues[2]
    }

    private suspend fun scrapeFromInstance(
        instance: String,
        handle: String,
        statusId: String,
        maxEmbedDepth: Int
    ): TweetData = withContext(Dispatchers.IO) {
        val url = "https://$instance/$handle/status/$statusId"
        val doc = fetchDoc(url)
        parseTweetDoc(doc, "https://x.com/$handle/status/$statusId", instance, maxEmbedDepth)
    }

    private fun fetchDoc(url: String): Document {
        val request = Request.Builder().url(url).build()
        val body = httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            response.body?.string() ?: throw Exception("Resposta vazia")
        }
        return Jsoup.parse(body, url)
    }

    private suspend fun parseTweetDoc(
        doc: Document,
        originalUrl: String,
        instance: String,
        remainingDepth: Int
    ): TweetData {
        val mainTweet = doc.selectFirst(".main-tweet") ?: doc.selectFirst(".timeline-item")
            ?: throw Exception("Tweet não encontrado no HTML")

        val handle = mainTweet.selectFirst(".username")?.text()?.removePrefix("@") ?: ""
        val displayName = mainTweet.selectFirst(".fullname")?.text() ?: handle
        val mainContent = mainTweet.selectFirst(".tweet-content")?.text() ?: ""
        val publishedDate = parseDate(mainTweet.selectFirst(".tweet-date a")?.attr("title"))

        // Thread items by the same author following the main tweet
        val threadContents = doc.select(".timeline-item:not(.main-tweet)")
            .filter { el ->
                val user = el.selectFirst(".username")?.text()?.removePrefix("@")
                user != null && user.equals(handle, ignoreCase = true)
            }
            .mapNotNull { it.selectFirst(".tweet-content")?.text() }
            .filter { it.isNotBlank() }

        // Embedded tweet links within any tweet content in the thread
        val embeddedLinks = mutableListOf<String>()
        doc.select(".main-tweet .tweet-content a[href*='/status/'], .timeline-item .tweet-content a[href*='/status/']")
            .mapNotNull { a ->
                val href = a.attr("abs:href").ifBlank { a.attr("href") }
                val m = Regex("/([^/?#]+)/status/(\\d+)").find(href) ?: return@mapNotNull null
                "https://$instance/${m.groupValues[1]}/status/${m.groupValues[2]}"
            }
            .distinctBy { it }
            .filterNot { it.contains("/$handle/status/") }
            .take(5)
            .also { embeddedLinks.addAll(it) }

        // Recursively resolve embedded tweets
        val embeddedTexts = if (remainingDepth > 0) {
            embeddedLinks.mapNotNull { embedUrl ->
                runCatching {
                    val embedDoc = fetchDoc(embedUrl)
                    parseTweetDoc(embedDoc, embedUrl, instance, remainingDepth - 1).fullText()
                }.getOrNull()
            }
        } else emptyList()

        return TweetData(
            originalUrl = originalUrl,
            handle = handle,
            displayName = displayName,
            mainContent = mainContent,
            threadContents = threadContents,
            embeddedTweetTexts = embeddedTexts,
            publishedDateMillis = publishedDate
        )
    }

    private fun parseDate(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        // Nitter date format: "Jan 15, 2024 · 3:45 PM UTC"
        val cleaned = raw.replace(" · ", " ").replace(" UTC", "")
        val formats = listOf(
            SimpleDateFormat("MMM d, yyyy h:mm a", Locale.ENGLISH),
            SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.ENGLISH),
            SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)
        )
        return formats.firstNotNullOfOrNull { fmt ->
            runCatching { fmt.parse(cleaned)?.time }.getOrNull()
        }
    }
}
