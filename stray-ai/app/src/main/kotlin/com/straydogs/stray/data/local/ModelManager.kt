package com.straydogs.stray.data.local

import android.content.Context
import com.straydogs.stray.data.model.LocalModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val modelsDir: File
        get() = File(context.filesDir, "models").also { it.mkdirs() }

    fun getLocalModels(): List<LocalModel> {
        val dir = modelsDir
        if (!dir.exists()) return emptyList()

        return dir.listFiles { file -> file.extension == "gguf" }
            ?.map { file ->
                LocalModel(
                    id = file.nameWithoutExtension,
                    name = formatModelName(file.nameWithoutExtension),
                    filePath = file.absolutePath,
                    sizeBytes = file.length(),
                    quantization = extractQuantization(file.name)
                )
            }
            ?.sortedByDescending { it.sizeBytes }
            ?: emptyList()
    }

    fun getModelFile(modelId: String): File? {
        val file = File(modelsDir, "$modelId.gguf")
        return if (file.exists()) file else null
    }

    fun deleteModel(modelId: String): Boolean {
        val file = File(modelsDir, "$modelId.gguf")
        return file.delete()
    }

    fun getModelPath(modelId: String): String? =
        getModelFile(modelId)?.absolutePath

    fun modelExists(modelId: String): Boolean =
        File(modelsDir, "$modelId.gguf").exists()

    private fun formatModelName(nameWithoutExtension: String): String =
        nameWithoutExtension
            .replace('-', ' ')
            .replace('_', ' ')
            .split(' ')
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }

    private fun extractQuantization(fileName: String): String {
        val quantPattern = Regex("(Q[0-9]_[KM0-9]+|Q[0-9]+|F16|F32|IQ[0-9]+)", RegexOption.IGNORE_CASE)
        return quantPattern.find(fileName)?.value?.uppercase() ?: ""
    }
}
