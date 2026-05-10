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
        const val MODEL_FILENAME = "gemma3_1b_int4.bin"
        // Kaggle download page for Gemma 3 1B LiteRT/MediaPipe model
        const val MODEL_DOWNLOAD_URL =
            "https://www.kaggle.com/models/google/gemma/frameworks/tfLite/variations/gemma3-1b-it-gpu-int4"
    }

    private val modelsDir: File
        get() = File(context.filesDir, "models").also { it.mkdirs() }

    val modelPath: Flow<String?> = dataStore.data.map { prefs ->
        prefs[MODEL_PATH_KEY]?.takeIf { File(it).exists() }
    }

    suspend fun importModel(inputStream: InputStream): String {
        val destination = File(modelsDir, MODEL_FILENAME)
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
        File(modelsDir, MODEL_FILENAME).takeIf { it.exists() }?.absolutePath

    fun deleteModel() {
        File(modelsDir, MODEL_FILENAME).delete()
    }

    fun modelFileSizeMb(): Long =
        File(modelsDir, MODEL_FILENAME).takeIf { it.exists() }
            ?.let { it.length() / (1024 * 1024) } ?: 0L
}
