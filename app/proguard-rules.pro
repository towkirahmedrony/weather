# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# Retrofit ships consumer rules; these keep the annotations/generic signatures it reads at runtime.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Moshi codegen: generated `*_JsonAdapter` classes are looked up reflectively by name.
-keepclasseswithmembers,allowobfuscation,includedescriptorclasses class * {
    public static com.squareup.moshi.JsonAdapter jsonAdapter(...);
}
-keepclassmembers class * extends com.squareup.moshi.JsonAdapter {
    <init>(...);
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep,allowobfuscation @interface com.squareup.moshi.JsonClass

# WorkManager instantiates workers by class name and persists that name across launches.
-keep class com.example.worker.WeatherSyncWorker { <init>(...); }

# Moshi resolves enum fields reflectively by constant name. R8 full mode otherwise unboxes
# enums to ints and drops the constants, which crashes adapter construction with
# NoSuchFieldException (observed: PressureTrend.RISING). Keep the models and their enums intact.
-keep class com.example.domain.model.** { *; }
