package com.mobilexread.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mobilexread.ui.screens.ProcessingScreen
import com.mobilexread.ui.theme.MobileXReadTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShareActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val url = extractUrl(intent)
        if (url == null) {
            finish()
            return
        }

        setContent {
            MobileXReadTheme {
                ProcessingScreen(
                    url = url,
                    onSuccess = { articleId ->
                        val intent = Intent(this, MainActivity::class.java).apply {
                            putExtra(MainActivity.EXTRA_ARTICLE_ID, articleId)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(intent)
                        finish()
                    },
                    onCancel = { finish() }
                )
            }
        }
    }

    private fun extractUrl(intent: Intent?): String? {
        val text = intent?.getStringExtra(Intent.EXTRA_TEXT) ?: return null
        val urlRegex = Regex("https?://(?:x\\.com|twitter\\.com)/[^\\s]+")
        return urlRegex.find(text)?.value ?: text.trim().takeIf {
            it.startsWith("http") && (it.contains("x.com") || it.contains("twitter.com"))
        }
    }
}
