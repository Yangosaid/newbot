#!/bin/bash
# Builds libtwoyi.so for arm64 with the Android NDK (replaces cargo-xdk).
set -e
SDK="${ANDROID_HOME:-/opt/android-sdk}"
NDK_DIR="${ANDROID_NDK_HOME:-$(ls -d "$SDK"/ndk/* | sort -V | tail -1)}"
BIN="$NDK_DIR/toolchains/llvm/prebuilt/linux-x86_64/bin"
export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$BIN/aarch64-linux-android27-clang"
export CC_aarch64_linux_android="$BIN/aarch64-linux-android27-clang"
export AR_aarch64_linux_android="$BIN/llvm-ar"
cargo build --release --target aarch64-linux-android
cp target/aarch64-linux-android/release/libtwoyi.so ../src/main/jniLibs/arm64-v8a/
