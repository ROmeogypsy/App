// Stub JNI implementation used when llama.cpp source is not present.
// This allows the project to compile without the native library.
// Replace by cloning llama.cpp into app/src/main/cpp/llama before building for real.

#include <jni.h>
#include <android/log.h>

#define LOG_TAG "StrayLlamaStub"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_straydogs_stray_data_local_LlamaEngine_loadModel(
    JNIEnv*, jobject, jstring, jint, jint
) {
    LOGE("Stub: llama.cpp not compiled. Clone llama.cpp into app/src/main/cpp/llama");
    return JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_straydogs_stray_data_local_LlamaEngine_unloadModel(JNIEnv*, jobject) {}

JNIEXPORT void JNICALL
Java_com_straydogs_stray_data_local_LlamaEngine_abortGeneration(JNIEnv*, jobject) {}

JNIEXPORT void JNICALL
Java_com_straydogs_stray_data_local_LlamaEngine_generateTokens(
    JNIEnv* env, jobject, jstring, jint, jfloat, jfloat, jobject callback
) {
    jclass cbClass = env->GetObjectClass(callback);
    jmethodID onDone = env->GetMethodID(cbClass, "onDone", "()V");
    env->CallVoidMethod(callback, onDone);
}

JNIEXPORT jboolean JNICALL
Java_com_straydogs_stray_data_local_LlamaEngine_isLoaded(JNIEnv*, jobject) {
    return JNI_FALSE;
}

} // extern "C"
