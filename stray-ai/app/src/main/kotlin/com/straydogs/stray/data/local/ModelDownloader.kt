package com.straydogs.stray.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val progress: Float, val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
    data class Success(val filePath: String) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

@Singleton
class ModelDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val modelManager: ModelManager
) {

    fun downloadModel(
        url: String,
        modelId: String,
        apiToken: String = ""
    ): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0f, 0, 0))

        val destFile = File(modelManager.modelsDir, "$modelId.gguf")
        val tempFile = File(modelManager.modelsDir, "$modelId.gguf.tmp")

        val requestBuilder = Request.Builder().url(url)
        if (apiToken.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $apiToken")
        }

        try {
            val response = okHttpClient.newCall(requestBuilder.build()).execute()

            if (!response.isSuccessful) {
                emit(DownloadState.Error("Download failed: ${response.code}"))
                return@flow
            }

            val body = response.body ?: run {
                emit(DownloadState.Error("Empty response body"))
                return@flow
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            FileOutputStream(tempFile).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
                        emit(DownloadState.Downloading(progress, downloadedBytes, totalBytes))
                    }
                }
            }

            tempFile.renameTo(destFile)
            emit(DownloadState.Success(destFile.absolutePath))

        } catch (e: Exception) {
            tempFile.delete()
            emit(DownloadState.Error(e.message ?: "Download failed"))
        }
    }.flowOn(Dispatchers.IO)

    fun buildHFDownloadUrl(repoId: String, filename: String): String =
        "https://huggingface.co/$repoId/resolve/main/$filename"
}
