package com.mobilexread.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobilexread.llm.ModelManager
import com.mobilexread.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSetupModel: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val modelPath by viewModel.modelPath.collectAsState()

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
                        TextButton(onClick = {
                            viewModel.deleteModel()
                        }) {
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
                text = "EXTRACÇÃO DE CONTEÚDO",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            ListItem(
                headlineContent = { Text("Instâncias Nitter") },
                supportingContent = {
                    Text(
                        "Usa automaticamente a melhor instância disponível entre 7 alternativas",
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
