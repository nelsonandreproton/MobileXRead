package com.mobilexread.scraper

data class TweetData(
    val originalUrl: String,
    val handle: String,
    val displayName: String,
    val mainContent: String,
    val threadContents: List<String>,
    val embeddedTweetTexts: List<String>,
    val publishedDateMillis: Long?
) {
    fun fullText(): String = buildString {
        append(mainContent)
        threadContents.forEach { append("\n\n").append(it) }
        embeddedTweetTexts.forEach { append("\n\n[linked tweet] ").append(it) }
    }
}
