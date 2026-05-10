package com.mobilexread.raindrop

import com.mobilexread.llm.ModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RaindropRepository @Inject constructor(
    private val httpClient: OkHttpClient,
    private val modelManager: ModelManager
) {
    companion object {
        private const val API_BASE = "https://api.raindrop.io/rest/v1"
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }

    suspend fun sendBookmark(
        url: String,
        title: String,
        excerpt: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val apiKey = modelManager.raindropApiKey.firstOrNull()
        if (apiKey.isNullOrBlank()) return@withContext Result.failure(
            IllegalStateException("Raindrop API key não configurada")
        )

        val body = JSONObject().apply {
            put("link", url)
            put("title", title)
            put("excerpt", excerpt.take(1000))
            put("tags", JSONArray().apply { put("mobilexread") })
        }.toString().toRequestBody(JSON_MEDIA)

        val request = Request.Builder()
            .url("$API_BASE/raindrop")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Raindrop API erro ${response.code}: ${response.body?.string()?.take(200)}")
                }
            }
        }
    }
}
