package com.mobilexread.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilexread.llm.ModelManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.asStateFlow

sealed class ModelImportState {
    data object Idle : ModelImportState()
    data object Importing : ModelImportState()
    data object Success : ModelImportState()
    data class Error(val message: String) : ModelImportState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelManager: ModelManager
) : ViewModel() {

    val modelPath: StateFlow<String?> = modelManager.modelPath
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val raindropApiKey: StateFlow<String?> = modelManager.raindropApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _importState = MutableStateFlow<ModelImportState>(ModelImportState.Idle)
    val importState: StateFlow<ModelImportState> = _importState

    private val _raindropSaveState = MutableStateFlow<String?>(null)
    val raindropSaveState: StateFlow<String?> = _raindropSaveState.asStateFlow()

    fun saveRaindropApiKey(key: String) {
        viewModelScope.launch {
            modelManager.saveRaindropApiKey(key)
            _raindropSaveState.value = "saved"
        }
    }

    fun clearRaindropApiKey() {
        viewModelScope.launch {
            modelManager.saveRaindropApiKey("")
        }
    }

    fun resetRaindropSaveState() {
        _raindropSaveState.value = null
    }

    fun importModelFromUri(uri: Uri) {
        viewModelScope.launch {
            _importState.value = ModelImportState.Importing
            runCatching {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("Não foi possível abrir o ficheiro")
                val ext = uri.lastPathSegment?.substringAfterLast('.', "") ?: "task"
                modelManager.importModel(inputStream, ext)
                _importState.value = ModelImportState.Success
            }.onFailure { e ->
                _importState.value = ModelImportState.Error(
                    e.message ?: "Erro ao importar o modelo"
                )
            }
        }
    }

    fun deleteModel() {
        modelManager.deleteModel()
    }

    fun resetImportState() {
        _importState.value = ModelImportState.Idle
    }

    fun getModelSizeMb(): Long = modelManager.modelFileSizeMb()
}
