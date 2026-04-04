# STRAY — Android AI Chat APK

**Brand:** Stray Dogs / SD Media — Red `#CC0000` + Black `#0A0A0A`  
**Target:** Android 8.0+ (API 26+), ARM64 + x86_64

## Build Requirements

- Android SDK 34
- Android NDK r26+
- CMake 3.22.1+
- Gradle 8.6
- JDK 17

## Setup

### 1. Clone llama.cpp

```bash
cd app/src/main/cpp
git clone https://github.com/ggerganov/llama.cpp llama
cd llama && git checkout b3900 && cd ..
```

> Without this step the project builds with a stub JNI library (no local inference).

### 2. Set SDK paths

Edit `local.properties`:

```properties
sdk.dir=/path/to/android-sdk
ndk.dir=/path/to/android-ndk
```

### 3. Build

```bash
./gradlew assembleDebug       # debug APK
./gradlew assembleRelease     # release APK (requires signing config)
```

APKs appear in `app/build/outputs/apk/`.

## Features

| Feature | Status |
|---|---|
| Local GGUF inference (llama.cpp JNI) | Requires llama.cpp |
| Hugging Face Inference API | Built-in |
| GGUF model browser + download | Built-in |
| Conversation history (Room DB) | Built-in |
| DataStore settings | Built-in |
| Space Mono terminal aesthetic | Built-in |

## Architecture

```
UI (Jetpack Compose) → ViewModels → Repositories → Data Sources
                                                  ├── Room DB (conversations/messages)
                                                  ├── DataStore (settings)
                                                  ├── LlamaEngine (JNI → llama.cpp)
                                                  └── HuggingFace API (Retrofit)
```

## Font Certs

The `font_certs.xml` placeholders must be replaced with real Google Fonts
provider certificates before release. See the
[Downloadable Fonts guide](https://developer.android.com/develop/ui/views/text-and-emoji/downloadable-fonts).
