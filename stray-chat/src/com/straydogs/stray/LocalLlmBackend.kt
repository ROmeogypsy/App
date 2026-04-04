package com.straydogs.stray

import java.io.File

/**
 * Local on-device inference via llama.cpp JNI bridge.
 *
 * In this build (no NDK), the JNI bridge is not compiled in.
 * Full implementation lives in stray-ai/app/src/main/cpp/llama_bridge.cpp.
 *
 * To enable real inference:
 *   1. Build stray-ai/ with NDK (requires maven.google.com for AGP)
 *   2. Replace the stub body below with: LlamaEngine(modelPath).generateStream(...)
 */
class LocalLlmBackend(filesDir: File? = null) : LlmBackend {
    override val id = "local_gguf"
    override val displayName = "Local Model"

    private val modelFile: File? = findModel(filesDir)
    override val isReady = modelFile != null && JNI_AVAILABLE

    companion object {
        /** Set to true once llama.cpp native lib is loaded */
        val JNI_AVAILABLE = runCatching {
            System.loadLibrary("llama-bridge")
            true
        }.getOrDefault(false)

        private val SEARCH_DIRS = listOf(
            "/sdcard/stray",
            "/sdcard/Download/stray",
            "/sdcard/Download"
        )

        fun findModel(appFiles: File?): File? {
            val dirs = buildList {
                appFiles?.let { add(it) }
                addAll(SEARCH_DIRS.map { File(it) })
            }
            for (dir in dirs) {
                if (!dir.isDirectory) continue
                dir.listFiles()
                    ?.filter { it.name.endsWith(".gguf") }
                    ?.maxByOrNull { it.length() }  // prefer largest (most capable)
                    ?.let { return it }
            }
            return null
        }
    }

    @Volatile private var cancelled = false

    override fun generate(
        messages: List<Message>,
        onToken: (String) -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        cancelled = false

        if (modelFile == null) {
            onError(
                "No GGUF model found.\n\n" +
                "Place a .gguf file in /sdcard/stray/ then restart STRAY.\n" +
                "Recommended: Mistral-7B-Instruct-v0.2.Q4_K_M.gguf"
            )
            onDone()
            return
        }

        if (!JNI_AVAILABLE) {
            onError(
                "Native llama.cpp library not loaded.\n\n" +
                "This APK was built without NDK support. " +
                "Build stray-ai/ with the Android Gradle Plugin to enable local inference."
            )
            onDone()
            return
        }

        // ── Real implementation (when NDK is compiled in) ──────────────────
        // val engine = LlamaEngine()
        // engine.loadModel(modelFile.absolutePath)
        // engine.generateStream(buildPrompt(messages)) { token ->
        //     if (cancelled) { engine.abortGeneration(); return@generateStream }
        //     onToken(token)
        // }
        // engine.unloadModel()
        // onDone()
        // ──────────────────────────────────────────────────────────────────
    }

    override fun cancel() { cancelled = true }

    override fun statusLine() = when {
        !JNI_AVAILABLE -> "native lib not available"
        modelFile != null -> modelFile.name
        else -> "no model — drop .gguf to /sdcard/stray/"
    }
}
