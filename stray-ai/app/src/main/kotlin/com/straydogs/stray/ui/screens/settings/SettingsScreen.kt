package com.straydogs.stray.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.straydogs.stray.data.model.InferenceMode
import com.straydogs.stray.ui.theme.StrayRed
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var tokenVisible by remember { mutableStateOf(false) }
    var tokenInput by remember(uiState.hfApiToken) { mutableStateOf(uiState.hfApiToken) }
    var remoteModelInput by remember(uiState.remoteModelId) { mutableStateOf(uiState.remoteModelId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("settings", style = MaterialTheme.typography.titleLarge) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Inference Mode
            item {
                SectionLabel("INFERENCE MODE")
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InferenceMode.entries.forEach { mode ->
                        FilterChip(
                            selected = uiState.inferenceMode == mode,
                            onClick = { viewModel.setInferenceMode(mode) },
                            label = { Text(mode.name.lowercase()) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StrayRed,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // HF API Token
            item {
                SectionLabel("HUGGING FACE API TOKEN")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("hf_...") },
                    visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Row {
                            TextButton(onClick = { tokenVisible = !tokenVisible }) {
                                Text(if (tokenVisible) "hide" else "show", color = StrayRed, style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(onClick = { viewModel.setHfApiToken(tokenInput) }) {
                                Text("save", color = StrayRed, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    },
                    singleLine = true
                )
            }

            // Remote Model
            if (uiState.inferenceMode == InferenceMode.REMOTE) {
                item {
                    SectionLabel("REMOTE MODEL ID")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = remoteModelInput,
                        onValueChange = { remoteModelInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("org/model-name") },
                        trailingIcon = {
                            TextButton(onClick = { viewModel.setRemoteModelId(remoteModelInput) }) {
                                Text("save", color = StrayRed, style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        singleLine = true
                    )
                }
            }

            // Generation Parameters
            item {
                SectionLabel("GENERATION")
                Spacer(modifier = Modifier.height(8.dp))

                SliderSetting(
                    label = "Temperature",
                    value = uiState.temperature,
                    valueRange = 0f..2f,
                    displayValue = "%.2f".format(uiState.temperature),
                    onValueChange = viewModel::setTemperature
                )

                Spacer(modifier = Modifier.height(8.dp))

                SliderSetting(
                    label = "Top-P",
                    value = uiState.topP,
                    valueRange = 0f..1f,
                    displayValue = "%.2f".format(uiState.topP),
                    onValueChange = viewModel::setTopP
                )

                Spacer(modifier = Modifier.height(8.dp))

                SliderSetting(
                    label = "Max Tokens",
                    value = uiState.maxTokens.toFloat(),
                    valueRange = 64f..4096f,
                    steps = 62,
                    displayValue = uiState.maxTokens.toString(),
                    onValueChange = { viewModel.setMaxTokens(it.roundToInt()) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SliderSetting(
                    label = "Context Size",
                    value = uiState.contextSize.toFloat(),
                    valueRange = 512f..8192f,
                    steps = 30,
                    displayValue = uiState.contextSize.toString(),
                    onValueChange = { viewModel.setContextSize(it.roundToInt()) }
                )
            }

            // App Info
            item {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "STRAY v1.0.0 — SD Media",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = StrayRed
    )
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit,
    steps: Int = 0,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = displayValue,
                style = MaterialTheme.typography.labelLarge,
                color = StrayRed
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = StrayRed,
                activeTrackColor = StrayRed
            )
        )
    }
}
