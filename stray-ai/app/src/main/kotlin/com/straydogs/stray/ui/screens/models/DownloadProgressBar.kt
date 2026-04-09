package com.straydogs.stray.ui.screens.models

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.straydogs.stray.data.local.DownloadState
import com.straydogs.stray.ui.theme.StrayRed

@Composable
fun DownloadProgressBar(
    state: DownloadState,
    modifier: Modifier = Modifier
) {
    when (state) {
        is DownloadState.Downloading -> {
            Column(modifier = modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "downloading...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(state.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = StrayRed
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = StrayRed,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
        is DownloadState.Error -> {
            Text(
                text = "error: ${state.message}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = modifier
            )
        }
        is DownloadState.Success -> {
            Text(
                text = "downloaded",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = modifier
            )
        }
        else -> {}
    }
}
