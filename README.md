<div align="center">

<!-- Hero Header -->
<picture>
  <source media="(prefers-color-scheme: dark)" srcset="public/VIBE-ON-mobile1.png">
  <img src="public/VIBE-ON-mobile1.png" alt="VIBE-ON! Android" width="280" style="filter: drop-shadow(0 0 40px rgba(36, 200, 216, 0.4));" />
</picture>

# VIBE-ON! Android

### **Your Music Universe, In the Palm of Your Hand**

*The ultimate mobile companion—engineered for synchronization, designed for audiophiles, and powered by a high-performance Rust core.*

<p align="center">
  <a href="#quick-links">🚀 Quick Links</a> •
  <a href="#experience-vibe-on">📱 Experience</a> •
  <a href="#the-tech-behind-the-magic">⚡ Tech Stack</a> •
  <a href="#the-fellowship-collaborators">🤝 Collaborators</a>
</p>

<br />

<!-- Badge Matrix -->
<p align="center">
  <img src="https://img.shields.io/badge/Android-API_26+-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 8.0+" />
  <img src="https://img.shields.io/badge/Kotlin-Native-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Rust-🦀-DEA584?style=for-the-badge&logo=rust&logoColor=white" alt="Rust" />
  <img src="https://img.shields.io/badge/Compose-Jetpack-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Desktop_Companion-Available-24C8D8?style=for-the-badge" alt="Desktop Companion" />
  <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" alt="MIT License" />
</p>

<br />

<!-- App Preview -->
<img src="public/VIBE-ON-mobile1.png" alt="VIBE-ON! Mobile Interface" width="40%" style="border-radius: 24px; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.8);" />

</div>

<br />

---

<br />

## <a id="quick-links"></a>🎯 Quick Links

<table>
<tr>
<td align="center" width="33%">

### 💻 [**Desktop App**](https://github.com/MemestaVedas/vibe-on)
The core ecosystem that powers your music library and synchronization.

</td>
<td align="center" width="33%">

### 🛠️ [**Developer Setup**](#building)
Setup your local environment and build the Rust core.

</td>
<td align="center" width="33%">

### 📖 [**Documentation**](#architecture)
Hybrid architecture details and UniFFI bindings.

</td>
</tr>
</table>

<br />

---

<br />

## <a id="experience-vibe-on"></a>🎬 Experience VIBE-ON! Mobile

<div align="center">
  <h3>🌊 Pure Native. Pure Vibe.</h3>
  <p><i>The companion app that doesn't just play music—it elevates it.</i></p>
</div>

<br />

<table>
<tr>
<td width="50%" valign="top">

### 🍱 **Bento-Themed Dashboard**

**Precision at Your Fingertips**

A modern, structured layout designed for discovery and control. The Bento dashboard gives you instant access to your recents, libraries, and stats.

- 🎯 **Structured Discovery** — Grid-based navigation for maximum efficiency
- 🧠 **Auto-Playing Carousel** — Your favorite albums showcased beautifully
- 🎭 **Shimmer Experiences** — Premium loading animations that feel alive
- ⚡ **Instant Access** — One tap away from your entire collection

</td>
<td width="50%" valign="top">

### 🔄 **Real-Time Desktop Sync**

**Stay Connected, Everywhere**

VIBE-ON! Mobile isn't an island. It's a high-speed node in your personal music mesh, powered by WebSockets and mDNS.

- 📶 **mDNS Discovery** — Finds your desktop app automatically on the network
- 🎮 **Remote Controller** — Use your phone to drive the desktop experience
- 🔗 **Unified Progress** — Syncs your listening status across devices
- ⚡ **Low Latency** — Real-time updates with zero lag

</td>
</tr>
</table>

<br />

<div align="center">

### 🎤 **The Smart Lyrics Engine**

**Synchronized Romaji & Translations**

The advanced lyrics engine from desktop, optimized for the handheld experience.

- 🌐 **Global Database** — Leverages LRCLIB for the perfect match
- 📖 **Multilingual Support** — Seamlessly switch between Original, Romaji, and Translated lyrics
- 🎨 **Aesthetic Typography** — Readable, expressive, and dynamically themed
- ⚡ **Pixel-Perfect Timing** — Real-time scrolling that matches every beat

<br />

### 🛰️ **Handheld Neural Engine**

*Your mobile device as a high-performance P2P Node*

</div>

<table>
<tr>
<td align="center" width="25%">

**📡**<br />
**P2P Syncing**<br />
<sub>Powered by libp2p + QUIC for direct secure library transfer</sub>

</td>
<td align="center" width="25%">

**🏗️**<br />
**Rust Core**<br />
<sub>Heavy lifting handled by a native Rust engine for speed</sub>

</td>
<td align="center" width="25%">

**💎**<br />
**Material 3**<br />
<sub>Full adherence to M3 Expressive design principles</sub>

</td>
<td align="center" width="25%">

**🔊**<br />
**Media3 Stack**<br />
<sub>Industrial grade playback engine for high-fidelity audio</sub>

</td>
</tr>
</table>

<br />

---

<br />

## 🎨 Design Philosophy

<div align="center">
  
**Fun Material 3 Expressive UI**

Built with Jetpack Compose for the most responsive and beautiful Android experience.

</div>

<br />

<table>
<tr>
<td align="center" width="33%">

### 🫧 **Organic Motion**

Declarative animations in Compose create a sense of weight and tactile feedback that native apps deserve.

</td>
<td align="center" width="33%">

### 📐 **32px Corner Hard-Edge**

Following the VIBE-ON! identity with extra-large rounding, creating a friendly yet professional aesthetic.

</td>
<td align="center" width="33%">

### 🌈 **Dynamic Theming**

The UI visually responds to your music, creating an immersive atmosphere that changes with every track.

</td>
</tr>
</table>

<br />

<div align="center">

```
┌─────────────────────────────────────────────────────────────┐
│  "We don't just build apps. We create experiences           │
│   that respect your ears, your eyes, and your vibe."        │
│                                                             │
│                                    — Built by MemestaVedas  │
└─────────────────────────────────────────────────────────────┘
```

</div>

<br />

---

<br />

## <a id="the-tech-behind-the-magic"></a>⚡ The Tech Behind The Magic

<div align="center">

**Engineered for Speed. Crafted for Portability.**

VIBE-ON! Android leverages a hybrid stack to bring desktop-class networking to your pocket.

</div>

<br />

### 🏗️ Architecture Stack

<table>
<tr>
<td width="50%">

#### **🦀 Native Foundation (Rust Core)**
```kotlin
// vibe-on-core via UniFFI
- libp2p + QUIC for P2P networking
- Secure data processing
- High-performance file indexing
- Native-speed cryptography
```

</td>
<td width="50%">

#### **⚛️ Modern Android (Kotlin + Compose)**
```kotlin
// Jetpack Compose 1.7+
- Declarative UI efficiency
- Kotlin Coroutines for async tasks
- Media3/ExoPlayer for playback
- Material 3 Expressive Theming
```

</td>
</tr>
</table>

<br />

### 🧰 Complete Technology Matrix

| **Category** | **Technologies** |
|:-------------|:-----------------|
| **Core Engine** | Rust • UniFFI • Tokio Runtime |
| **Networking** | libp2p • WebSockets • mDNS Discovery |
| **UI Framework** | Jetpack Compose • Kotlin • Material 3 |
| **Media Player** | Android Media3 • ExoPlayer |
| **Async Logic** | Kotlin Coroutines • StateFlow / SharedFlow |
| **Image Loading** | Coil for Compose |
| **Dependency Injection** | Hilt / Dagger |

<br />

---

<br />

## 🚀 Building from Source

### <a id="building"></a>🛠️ Developer Setup

<details>
<summary><b>📋 Click to expand developer guide</b></summary>

<br />

#### Prerequisites

- **Android SDK** (API 26+) & NDK
- **Rust Toolchain** (via [rustup.rs](https://rustup.rs))
- **Java JDK 17+**
- **Android Studio** (Ladybug or later recommended)

<br />

#### Step-by-Step Setup

**1️⃣ Build the Rust Core**
Navigate to the Rust core directory (usually in the desktop repo or a shared module) and build for Android:
```bash
cargo build --target aarch64-linux-android --release
```

**2️⃣ Setup Native Libraries**
Copy the generated `.so` file to the app's `jniLibs` directory:
```bash
cp target/aarch64-linux-android/release/libvibe_on_core.so app/src/main/jniLibs/arm64-v8a/
```

**3️⃣ Run through Gradle**
```bash
./gradlew installDebug
```

</details>

<br />

---

<br />

## <a id="the-fellowship-collaborators"></a>🤝 The Fellowship (Collaborators)

The architects and engineers behind the VIBE-ON! universe.

<div align="center">

| Collaborator | Role | Github |
|:--- |:--- |:--- |
| **MemestaVedas** | Founder & Lead Architect | [@MemestaVedas](https://github.com/MemestaVedas) |

</div>

<br />

## 🤝 Reach Out

Whether you have questions, suggestions, or just want to chat about the project, feel free to get in touch!

<br />

<div align="center">

**Discord:** `@memestavedas`

</div>

<br />

---

<br />

## 📜 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

<br />

---

<br />

<p align="center">
  <strong>Created with 💙 by <a href="https://github.com/MemestaVedas">MemestaVedas</a></strong><br />
  <sub>Built for Audiophiles by an Engineer with Taste</sub>
</p>

<br />

<div align="center">
<img src="public/VIBE-ON-mobile1.png" alt="VIBE-ON!" width="64" style="filter: grayscale(100%) opacity(20%);" />
</div>
