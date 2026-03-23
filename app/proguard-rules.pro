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

# Keep serialization classes
-keep class * implements java.io.Serializable { *; }

# Keep enum values for serialization
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Kotlin metadata
-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }

# Keep BuildConfig
-keep class moe.memesta.vibeon.BuildConfig { *; }

# Keep R8 compatibility
-keepattributes LineNumberTable,SourceFile
-renamesourcefileattribute SourceFile

# Don't warn about missing classes from optional dependencies

# Don't warn about AWT classes (not available on Android but used by JNA)
-dontwarn java.awt.Component
-dontwarn java.awt.GraphicsEnvironment
-dontwarn java.awt.HeadlessException
-dontwarn java.awt.Window

# Keep application classes
-keepclassmembers class moe.memesta.vibeon.** {
    *** *(...);
}

# Preserve Jetpack Compose
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# Keep data classes
-keep class moe.memesta.vibeon.data.** { *; }
-keepclassmembers class moe.memesta.vibeon.data.** {
    *** *(...);
}

# Preserve ViewModel and related classes
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# Keep Android resources
-keepclassmembers class **.R$* {
    public static <fields>;
}
