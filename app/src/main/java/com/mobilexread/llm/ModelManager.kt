package com.mobilexread.llm

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val MODEL_PATH_KEY = stringPreferencesKey("model_path")
        // Kaggle download page for Gemma 3 1B LiteRT/MediaPipe model
        const val MODEL_DOWNLOAD_URL =
            "https://www.kaggle.com/models/google/gemma/frameworks/tfLite/variations/gemma3-1b-it-gpu-int4"
    }

    private val modelsDir: File
        get() = File(context.filesDir, "models").also { it.mkdirs() }

    val modelPath: Flow<String?> = dataStore.data.map { prefs ->
        prefs[MODEL_PATH_KEY]?.takeIf { File(it).exists() }
    }

    suspend fun importModel(inputStream: InputStream, sourceExtension: String = "task"): String {
        val ext = sourceExtension.trimStart('.').ifBlank { "task" }
        val destination = File(modelsDir, "gemma3_1b_int4.$ext")
        inputStream.use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        val path = destination.absolutePath
        dataStore.edit { it[MODEL_PATH_KEY] = path }
        return path
    }

    fun getModelPathSync(): String? =
        listOf("task", "bin", "litertlm").firstNotNullOfOrNull { ext ->
            File(modelsDir, "gemma3_1b_int4.$ext").takeIf { it.exists() }
        }?.absolutePath

    fun deleteModel() {
        listOf("task", "bin", "litertlm").forEach { ext ->
            File(modelsDir, "gemma3_1b_int4.$ext").delete()
        }
    }

    fun modelFileSizeMb(): Long =
        listOf("task", "bin", "litertlm").firstNotNullOfOrNull { ext ->
            File(modelsDir, "gemma3_1b_int4.$ext").takeIf { it.exists() }
        }?.let { it.length() / (1024 * 1024) } ?: 0L
}
