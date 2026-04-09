package com.straydogs.stray.data.model

enum class InferenceMode {
    LOCAL,   // llama.cpp GGUF via JNI
    REMOTE   // Hugging Face Inference API
}
