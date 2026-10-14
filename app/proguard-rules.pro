# Keep JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Compose
-keep class androidx.compose.** { *; }
