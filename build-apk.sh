#!/usr/bin/env bash
# build-apk.sh — Manual WebView APK build for SD Media (com.sdmedia.kennel)
# Bypasses Gradle/AGP (which requires maven.google.com) using raw toolchain:
#   aapt2, kotlinc, dalvik-exchange (dx), zipalign, apksigner
#
# Prerequisites:
#   /opt/android-sdk/build-tools/34.0.0/  — aapt2, zipalign, apksigner
#   /opt/android-sdk/platforms/android-34/android.jar
#   /opt/kotlinc/bin/kotlinc
#   /usr/bin/dalvik-exchange              — dex tool
#   /tmp/kotlin-stdlib-dex.jar            — Kotlin stdlib (no multi-release entries)
#   /tmp/stray-debug.jks                  — debug keystore (alias: androiddebugkey, pass: android)

set -e

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="/tmp/webview-apk-build"
WEBVIEW_SRC="$APP_DIR/android-webview"
DIST="$APP_DIR/dist"
RELEASE_DIR="$APP_DIR/release"

AAPT2="/opt/android-sdk/build-tools/34.0.0/aapt2"
ZIPALIGN="/opt/android-sdk/build-tools/34.0.0/zipalign"
APKSIGNER="/opt/android-sdk/build-tools/34.0.0/apksigner"
ANDROID_JAR="/opt/android-sdk/platforms/android-34/android.jar"
KOTLINC="/opt/kotlinc/bin/kotlinc"
DEX="/usr/bin/dalvik-exchange"
KOTLIN_STDLIB="/tmp/kotlin-stdlib-dex.jar"
KEYSTORE="/tmp/stray-debug.jks"

echo "=== SD Media APK build ==="

# --- Phase 1: Build web assets ---
echo "[1/7] Building Vite/React app..."
cd "$APP_DIR"
npm run build

# --- Phase 2: Setup build dirs ---
echo "[2/7] Setting up build directory..."
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/compiled-res" "$BUILD_DIR/dex" "$BUILD_DIR/gen" "$BUILD_DIR/assets/www"

# Copy icons
for dpi in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
  mkdir -p "$BUILD_DIR/res/mipmap-$dpi"
  cp "$APP_DIR/android/app/src/main/res/mipmap-$dpi/ic_launcher.png" "$BUILD_DIR/res/mipmap-$dpi/"
done

# Copy web assets
cp -r "$DIST"/* "$BUILD_DIR/assets/www/"

# --- Phase 3: Compile resources ---
echo "[3/7] Compiling resources with aapt2..."
"$AAPT2" compile --dir "$BUILD_DIR/res" -o "$BUILD_DIR/compiled-res/"

"$AAPT2" link \
  -o "$BUILD_DIR/resources-base.apk" \
  -I "$ANDROID_JAR" \
  --manifest "$WEBVIEW_SRC/AndroidManifest.xml" \
  --min-sdk-version 26 \
  --target-sdk-version 34 \
  --version-code 1 \
  --version-name "1.0" \
  -R "$BUILD_DIR/compiled-res/"*.flat \
  --auto-add-overlay \
  --java "$BUILD_DIR/gen"

# --- Phase 4: Compile Kotlin ---
echo "[4/7] Compiling Kotlin source..."
"$KOTLINC" \
  "$WEBVIEW_SRC/src/com/sdmedia/kennel/MainActivity.kt" \
  "$BUILD_DIR/gen/com/sdmedia/kennel/R.java" \
  -classpath "$ANDROID_JAR" \
  -d "$BUILD_DIR/app.jar"

# --- Phase 5: Convert to DEX ---
echo "[5/7] Converting to DEX..."
"$DEX" --dex \
  --min-sdk-version=26 \
  --output="$BUILD_DIR/dex/classes.dex" \
  "$BUILD_DIR/app.jar" \
  "$KOTLIN_STDLIB"

# --- Phase 6: Package APK ---
echo "[6/7] Packaging APK..."
cp "$BUILD_DIR/resources-base.apk" "$BUILD_DIR/stray-unsigned.apk"

# Add web assets uncompressed (critical for Android WebView file:// access)
cd "$BUILD_DIR/assets"
zip -0 -r "$BUILD_DIR/stray-unsigned.apk" www/

# Add classes.dex
cd "$BUILD_DIR/dex"
zip -j "$BUILD_DIR/stray-unsigned.apk" classes.dex

# zipalign
"$ZIPALIGN" -v -p 4 \
  "$BUILD_DIR/stray-unsigned.apk" \
  "$BUILD_DIR/stray-aligned.apk"

# Sign
"$APKSIGNER" sign \
  --ks "$KEYSTORE" \
  --ks-key-alias androiddebugkey \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out "$BUILD_DIR/sdmedia-debug.apk" \
  "$BUILD_DIR/stray-aligned.apk"

# --- Phase 7: Copy output ---
echo "[7/7] Copying to release/..."
mkdir -p "$RELEASE_DIR"
cp "$BUILD_DIR/sdmedia-debug.apk" "$RELEASE_DIR/sdmedia-debug.apk"

echo ""
echo "=== BUILD COMPLETE ==="
echo "Output: $RELEASE_DIR/sdmedia-debug.apk"
ls -lh "$RELEASE_DIR/sdmedia-debug.apk"
