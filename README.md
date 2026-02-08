# Vibe-On Android 📱

Native Android companion app for Vibe-On, designed to provide a premium, synchronized music experience on the go. Built with **Jetpack Compose**, **Kotlin Coroutines**, and a high-performance **Rust** core.

## ✨ Recent Enhancements

- **🍱 Bento-Themed Dashboard**: A modern, structured layout for quick access to your music library and statistics.
- **🎠 Auto-Playing Hero Carousel**: Featured albums showcased with smooth transitions and parallax effects.
- **✨ Shimmer Loading Experience**: Premium skeleton screens for a seamless data-loading feel.
- **🔄 Real-Time Desktop Sync**: Instant synchronization with the Vibe-On Desktop app via WebSockets and mDNS discovery.
- **🎤 Advanced Lyrics Support**: Synchronization with Desktop lyrics, including support for **Romaji** and translated LRC files.

## 🏗️ Architecture

This app uses a robust hybrid architecture:
- **Rust Core**: `vibe-on-core` library handles low-level P2P networking (libp2p + QUIC) and data processing.
- **Jetpack Compose**: Modern declarative UI with custom motion design and Material 3 Expressive theming.
- **UniFFI**: Automated binding generation between Rust and Kotlin for safety and speed.
- **Media3/ExoPlayer**: Industry-standard audio playback with a custom `P2PDataSource` for distributed streaming.

## 🚀 Prerequisites

- Android SDK (API 26+) & NDK
- Rust toolchain (`cargo`, `rustup`)
- Java JDK 17+

## 🛠️ Building

1. **Build Rust Core**:
   ```bash
   cd ../vibe-on/vibe-on-core
   cargo build --target aarch64-linux-android --release
   ```

2. **Setup Native Libraries**:
   Copy the generated `libvibe_on_core.so` to `app/src/main/jniLibs/arm64-v8a/`.

3. **Install & Run**:
   ```bash
   ./gradlew installDebug
   ```

## 📂 Project Structure

- **UI**: `app/src/main/java/moe/memesta/vibeon/ui/` (Compose screens and components)
- **Data**: `app/src/main/java/moe/memesta/vibeon/data/` (Repositories, WebSocket client, Models)
- **Theme**: `app/src/main/java/moe/memesta/vibeon/ui/theme/` (Design tokens and animations)
