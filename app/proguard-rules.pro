# Add project specific ProGuard rules here.

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson / JSON
-keep class com.google.gson.** { *; }
-keep class com.ppailab.cue.api.dto.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# DataStore
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Timber
-dontwarn org.jetbrains.annotations.**

# Keep data classes
-keep class com.ppailab.cue.api.ReplyCandidate { *; }

# Kotlin
-keep class kotlin.** { *; }
-dontwarn kotlin.**
