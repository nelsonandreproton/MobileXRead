package com.mobilexread.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobilexread.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSetupModel: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val modelPath by viewModel.modelPath.collectAsState()
    val savedRaindropKey by viewModel.raindropApiKey.collectAsState()
    val raindropSaveState by viewModel.raindropSaveState.collectAsState()

    var raindropKeyInput by rememberSaveable { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }

    LaunchedEffect(savedRaindropKey) {
        if (raindropKeyInput.isBlank() && savedRaindropKey != null) {
            raindropKeyInput = savedRaindropKey!!
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Definições") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "MODELO DE IA LOCAL",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (modelPath != null) {
                ListItem(
                    headlineContent = { Text("Gemma 3 1B INT4") },
                    supportingContent = {
                        Text(
                            "${viewModel.getModelSizeMb()} MB · ${modelPath!!.substringAfterLast('/')}",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    trailingContent = {
                        TextButton(onClick = { viewModel.deleteModel() }) {
                            Text("Remover", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            } else {
                ListItem(
                    headlineContent = { Text("Modelo não configurado") },
                    supportingContent = {
                        Text(
                            "O modelo Gemma 3 1B (~700 MB) é necessário para gerar resumos",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    trailingContent = {
                        TextButton(onClick = onSetupModel) {
                            Text("Configurar")
                        }
                    }
                )
            }
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "RAINDROP.IO",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Quando o processamento termina, o artigo é guardado automaticamente no Raindrop.io. Obtém o token em raindrop.io → Definições → Integrações → Criar app de teste.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = raindropKeyInput,
                    onValueChange = {
                        raindropKeyInput = it
                        viewModel.resetRaindropSaveState()
                    },
                    label = { Text("Token da API") },
                    placeholder = { Text("Cole aqui o token…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                imageVector = if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showKey) "Ocultar token" else "Mostrar token"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        viewModel.saveRaindropApiKey(raindropKeyInput)
                    })
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { viewModel.saveRaindropApiKey(raindropKeyInput) },
                        enabled = raindropKeyInput.isNotBlank()
                    ) {
                        Text("Guardar")
                    }
                    if (savedRaindropKey != null) {
                        TextButton(onClick = {
                            viewModel.clearRaindropApiKey()
                            raindropKeyInput = ""
                        }) {
                            Text("Remover", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (raindropSaveState == "saved") {
                        Text(
                            text = "Guardado",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "EXTRACÇÃO DE CONTEÚDO",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            ListItem(
                headlineContent = { Text("Fonte: FxTwitter") },
                supportingContent = {
                    Text(
                        "Os tweets são extraídos via api.fxtwitter.com (sem autenticação). Apenas o tweet partilhado é processado — threads e tweets ligados não são suportados.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            )
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ACERCA",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            ListItem(
                headlineContent = { Text("Versão") },
                supportingContent = { Text("1.0.0") }
            )
            ListItem(
                headlineContent = { Text("Modelo") },
                supportingContent = { Text("Gemma 3 1B INT4 via MediaPipe LLM Inference") }
            )
        }
    }
}
