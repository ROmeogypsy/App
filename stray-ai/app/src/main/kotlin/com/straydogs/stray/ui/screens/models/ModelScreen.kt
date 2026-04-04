package com.straydogs.stray.ui.screens.models

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.straydogs.stray.ui.theme.StrayRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelScreen(
    onBack: () -> Unit,
    viewModel: ModelViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("models", style = MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Local Models Section
            item {
                Text(
                    text = "LOCAL MODELS",
                    style = MaterialTheme.typography.labelLarge,
                    color = StrayRed
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState.localModels.isEmpty()) {
                item {
                    Text(
                        text = "no local models — download below",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(uiState.localModels, key = { it.id }) { model ->
                    LocalModelCard(
                        model = model,
                        isActive = model.id == uiState.activeModelId,
                        onSelect = { viewModel.setActiveModel(model.id) },
                        onDelete = { viewModel.deleteModel(model.id) }
                    )
                }
            }

            // Remote Models Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "HUGGING FACE GGUF",
                    style = MaterialTheme.typography.labelLarge,
                    color = StrayRed
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Search bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("search models...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    trailingIcon = {
                        TextButton(onClick = { viewModel.searchModels(uiState.searchQuery) }) {
                            Text("search", color = StrayRed, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState.isSearching) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = StrayRed, modifier = Modifier.size(32.dp))
                    }
                }
            } else {
                items(uiState.remoteModels, key = { it.id }) { model ->
                    val downloadState = uiState.downloadStates[model.id]
                    RemoteModelCard(
                        model = model,
                        downloadState = downloadState,
                        onDownload = { filename -> viewModel.downloadModel(model.id, filename) }
                    )
                }
            }

            uiState.error?.let { error ->
                item {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
