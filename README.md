# Vibe-on Android

Native Android companion app for Vibe-on, built with Jetpack Compose and Rust.

## Architecture

This app uses a hybrid architecture:
- **Rust Core**: `vibe-on-core` library handles P2P networking (libp2p + QUIC).
- **Kotlin/Compose**: UI and Android system integration.
- **UniFFI**: Generates bindings between Rust and Kotlin.
- **Media3/ExoPlayer**: Audio playback with custom `P2PDataSource`.

## Prerequisites

- Android SDK & NDK
- Rust toolchain (`cargo`, `rustup`)
- Java JDK 17+

## Building

1. **Build Rust Core**:
   ```bash
   cd ../vibe-on/vibe-on-core
   cargo build --target aarch64-linux-android --release
   ```

2. **Copy Shared Libraries**:
   Copy `libvibe_on_core.so` to `app/src/main/jniLibs/arm64-v8a/`.

3. **Build APK**:
   ```bash
   ./gradlew installDebug
   ```

## Development

- **UI Code**: `app/src/main/java/moe/memesta/vibeon/ui/`
- **P2P Integration**: `app/src/main/java/moe/memesta/vibeon/data/StreamRepository.kt`
- **Rust Bindings**: `app/src/main/java/uniffi/vibe_on_core/`
# vibe-on-android
