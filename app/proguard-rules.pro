# Keep JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep JNI bridge class names and members
-keep class com.fizzyhedgehog.sdocxconverter.NativeConverter { *; }

