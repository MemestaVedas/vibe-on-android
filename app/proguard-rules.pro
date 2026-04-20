# ============================================================================
# Vibe-On ProGuard / R8 Rules — Tightened for APK size
# ============================================================================

# --- Native Libraries ---
# Preserve libtorrent4j native library bindings
-keep class org.libtorrent4j.swig.** { *; }
-keep class org.libtorrent4j.** { *; }
-keepclasseswithmembernames class org.libtorrent4j.swig.** { native <methods>; }

# Preserve JNA library classes
-keep class com.sun.jna.** { *; }
-keep interface com.sun.jna.** { *; }

# Keep all native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# --- Serialization ---
# Keep kotlinx.serialization (used by widget state)
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep enum values for serialization
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- Data Layer (JSON/WebSocket parsing uses reflection) ---
-keep class moe.memesta.vibeon.data.model.** { *; }
-keep class moe.memesta.vibeon.data.MediaSessionData { *; }
-keep class moe.memesta.vibeon.data.TrackInfo { *; }
-keep class moe.memesta.vibeon.data.QueueItem { *; }
-keep class moe.memesta.vibeon.data.PlaylistInfo { *; }
-keep class moe.memesta.vibeon.data.AlbumInfo { *; }
-keep class moe.memesta.vibeon.data.ArtistItemData { *; }
-keep class moe.memesta.vibeon.data.local.entity.** { *; }

# --- Widget (Glance serialization) ---
-keep class moe.memesta.vibeon.widget.** { *; }

# --- Kotlin Metadata ---
-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }

# --- App Identity ---
-keep class moe.memesta.vibeon.BuildConfig { *; }

# --- R8 Debug Info ---
-keepattributes LineNumberTable,SourceFile
-renamesourcefileattribute SourceFile

# --- Hilt / Dagger ---
# ViewModel constructors injected by Hilt
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# --- Suppress Warnings ---
# AWT classes referenced by JNA but not available on Android
-dontwarn java.awt.Component
-dontwarn java.awt.GraphicsEnvironment
-dontwarn java.awt.HeadlessException
-dontwarn java.awt.Window

# --- Resources ---
-keepclassmembers class **.R$* {
    public static <fields>;
}
