package com.straydogs.stray.ui.screens.models

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.straydogs.stray.data.local.DownloadState
import com.straydogs.stray.data.model.HFModel
import com.straydogs.stray.data.model.LocalModel
import com.straydogs.stray.ui.theme.StrayDarkContainer
import com.straydogs.stray.ui.theme.StrayRed

@Composable
fun LocalModelCard(
    model: LocalModel,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isActive) StrayRed else MaterialTheme.colorScheme.outline

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(StrayDarkContainer)
            .border(width = if (isActive) 1.5.dp else 0.5.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = model.name, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (model.quantization.isNotBlank()) {
                    Text(
                        text = model.quantization,
                        style = MaterialTheme.typography.labelSmall,
                        color = StrayRed
                    )
                }
                Text(
                    text = formatBytes(model.sizeBytes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (isActive) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Active",
                tint = StrayRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        TextButton(onClick = onSelect) {
            Text(if (isActive) "active" else "select", color = if (isActive) StrayRed else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RemoteModelCard(
    model: HFModel,
    downloadState: DownloadState?,
    onDownload: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFilenameDialog by remember { mutableStateOf(false) }
    var filenameInput by remember { mutableStateOf("") }

    if (showFilenameDialog) {
        AlertDialog(
            onDismissRequest = { showFilenameDialog = false },
            title = { Text("Enter GGUF filename") },
            text = {
                Column {
                    Text(
                        text = "e.g. model-Q4_K_M.gguf",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = filenameInput,
                        onValueChange = { filenameInput = it },
                        placeholder = { Text("filename.gguf") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (filenameInput.isNotBlank()) {
                            onDownload(filenameInput.trim())
                            showFilenameDialog = false
                            filenameInput = ""
                        }
                    }
                ) {
                    Text("Download", color = StrayRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFilenameDialog = false; filenameInput = "" }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(StrayDarkContainer)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = model.id, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "${formatDownloads(model.downloads)} dl",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${model.likes} likes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            when (downloadState) {
                is DownloadState.Downloading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = StrayRed,
                        strokeWidth = 2.dp
                    )
                }
                else -> {
                    IconButton(onClick = { showFilenameDialog = true }) {
                        Icon(Icons.Default.Download, contentDescription = "Download", tint = StrayRed)
                    }
                }
            }
        }
        if (downloadState != null && downloadState !is DownloadState.Idle) {
            Spacer(modifier = Modifier.height(8.dp))
            DownloadProgressBar(state = downloadState)
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.0f MB".format(bytes / 1_048_576.0)
    else -> "%.0f KB".format(bytes / 1024.0)
}

private fun formatDownloads(count: Int): String = when {
    count >= 1_000_000 -> "%.1fM".format(count / 1_000_000.0)
    count >= 1_000 -> "%.0fK".format(count / 1_000.0)
    else -> count.toString()
}
