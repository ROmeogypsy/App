package com.straydogs.stray.util

import android.content.Context
import java.io.File

object FileUtil {

    fun getModelsDir(context: Context): File =
        File(context.filesDir, "models").also { it.mkdirs() }

    fun formatFileSize(bytes: Long): String = when {
        bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.0f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024 -> "%.0f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }

    fun sanitizeFilename(name: String): String =
        name.replace(Regex("[^a-zA-Z0-9._-]"), "_")

    fun getFileExtension(path: String): String =
        File(path).extension.lowercase()
}
