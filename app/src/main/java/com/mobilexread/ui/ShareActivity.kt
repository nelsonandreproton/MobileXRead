package com.mobilexread.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.mobilexread.ui.viewmodel.ShareState
import com.mobilexread.ui.viewmodel.ShareViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ShareActivity : ComponentActivity() {

    private val viewModel: ShareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start observing state immediately
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is ShareState.Idle -> Unit // waiting for enqueue() call
                    is ShareState.Queued -> {
                        Toast.makeText(
                            this@ShareActivity,
                            "Adicionado à fila de processamento",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                    is ShareState.AlreadyQueued -> {
                        val msg = when (state.status) {
                            "PENDING", "PROCESSING" -> "Já está a ser processado"
                            "COMPLETED" -> "Este link já foi processado"
                            "FAILED" -> "Este link falhou — abre a app para tentar novamente"
                            else -> "Link já existe"
                        }
                        Toast.makeText(this@ShareActivity, msg, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    is ShareState.Error -> {
                        Toast.makeText(this@ShareActivity, state.message, Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val url = extractUrl(intent)
        if (url == null) {
            Toast.makeText(this, "Link do X/Twitter não reconhecido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        viewModel.enqueue(url)
    }

    private fun extractUrl(intent: Intent?): String? {
        val text = intent?.getStringExtra(Intent.EXTRA_TEXT) ?: return null
        val urlRegex = Regex("https?://(?:x\\.com|twitter\\.com)/[^\\s]+")
        return urlRegex.find(text)?.value ?: text.trim().takeIf {
            it.startsWith("http") && (it.contains("x.com") || it.contains("twitter.com"))
        }
    }
}
