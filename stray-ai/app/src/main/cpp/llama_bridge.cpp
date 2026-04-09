#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <thread>
#include <atomic>
#include "llama.h"
#include "common/common.h"

#define LOG_TAG "StrayLlama"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global state
static llama_model* g_model = nullptr;
static llama_context* g_ctx = nullptr;
static std::atomic<bool> g_abort(false);

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_straydogs_stray_data_local_LlamaEngine_loadModel(
    JNIEnv* env, jobject, jstring modelPath, jint nCtx, jint nThreads
) {
    if (g_model != nullptr) {
        llama_free(g_ctx);
        llama_free_model(g_model);
        g_model = nullptr;
        g_ctx = nullptr;
    }

    const char* path = env->GetStringUTFChars(modelPath, nullptr);

    llama_model_params mparams = llama_model_default_params();
    mparams.n_gpu_layers = 0; // CPU-only on Android

    g_model = llama_load_model_from_file(path, mparams);
    env->ReleaseStringUTFChars(modelPath, path);

    if (!g_model) {
        LOGE("Failed to load model");
        return JNI_FALSE;
    }

    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx = nCtx;
    cparams.n_threads = nThreads;
    cparams.n_threads_batch = nThreads;

    g_ctx = llama_new_context_with_model(g_model, cparams);

    if (!g_ctx) {
        LOGE("Failed to create context");
        llama_free_model(g_model);
        g_model = nullptr;
        return JNI_FALSE;
    }

    LOGI("Model loaded. Context size: %d", nCtx);
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_straydogs_stray_data_local_LlamaEngine_unloadModel(
    JNIEnv*, jobject
) {
    if (g_ctx) { llama_free(g_ctx); g_ctx = nullptr; }
    if (g_model) { llama_free_model(g_model); g_model = nullptr; }
    LOGI("Model unloaded");
}

JNIEXPORT void JNICALL
Java_com_straydogs_stray_data_local_LlamaEngine_abortGeneration(
    JNIEnv*, jobject
) {
    g_abort.store(true);
}

JNIEXPORT void JNICALL
Java_com_straydogs_stray_data_local_LlamaEngine_generateTokens(
    JNIEnv* env, jobject obj, jstring prompt, jint maxTokens, jfloat temp,
    jfloat topP, jobject callback
) {
    if (!g_model || !g_ctx) {
        LOGE("Model not loaded");
        return;
    }

    g_abort.store(false);

    const char* p = env->GetStringUTFChars(prompt, nullptr);
    std::string promptStr(p);
    env->ReleaseStringUTFChars(prompt, p);

    // Tokenize
    std::vector<llama_token> tokens(promptStr.length() + 1);
    int nTokens = llama_tokenize(
        g_model, promptStr.c_str(), promptStr.length(),
        tokens.data(), tokens.size(), true, false
    );
    if (nTokens < 0) { LOGE("Tokenization failed"); return; }
    tokens.resize(nTokens);

    // Eval prompt
    llama_batch batch = llama_batch_init(512, 0, 1);
    for (int i = 0; i < nTokens; i++) {
        llama_batch_add(batch, tokens[i], i, {0}, false);
    }
    batch.logits[batch.n_tokens - 1] = true;

    if (llama_decode(g_ctx, batch) != 0) {
        LOGE("Decode failed"); llama_batch_free(batch); return;
    }

    // Sampling setup
    llama_sampler* sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(temp));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(topP, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(42));

    // Callback method IDs
    jclass cbClass = env->GetObjectClass(callback);
    jmethodID onToken = env->GetMethodID(cbClass, "onToken", "(Ljava/lang/String;)V");
    jmethodID onDone = env->GetMethodID(cbClass, "onDone", "()V");

    int nCur = nTokens;
    while (nCur < nTokens + maxTokens && !g_abort.load()) {
        llama_token newToken = llama_sampler_sample(sampler, g_ctx, -1);

        if (llama_token_is_eog(g_model, newToken)) break;

        char buf[256];
        int n = llama_token_to_piece(g_model, newToken, buf, sizeof(buf), 0, false);
        if (n > 0) {
            buf[n] = '\0';
            jstring tokenStr = env->NewStringUTF(buf);
            env->CallVoidMethod(callback, onToken, tokenStr);
            env->DeleteLocalRef(tokenStr);
        }

        llama_batch singleBatch = llama_batch_init(1, 0, 1);
        llama_batch_add(singleBatch, newToken, nCur, {0}, true);
        llama_decode(g_ctx, singleBatch);
        llama_batch_free(singleBatch);
        nCur++;
    }

    llama_sampler_free(sampler);
    llama_batch_free(batch);
    env->CallVoidMethod(callback, onDone);
}

JNIEXPORT jboolean JNICALL
Java_com_straydogs_stray_data_local_LlamaEngine_isLoaded(
    JNIEnv*, jobject
) {
    return (g_model != nullptr && g_ctx != nullptr) ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
