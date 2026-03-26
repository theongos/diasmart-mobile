# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep Room entities
-keep class com.diabeto.data.entity.** { *; }
-keep class com.diabeto.data.dao.** { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep Hilt
-keep class * extends dagger.hilt.android.HiltAndroidApp { *; }
-keep class * extends android.app.Application { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
-keep class * extends androidx.activity.ComponentActivity { *; }

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *; }

# Keep Java time
-dontwarn java.time.**
-keep class java.time.** { *; }

# General
-keepattributes Signature
-keepattributes Exceptions
-keepattributes RuntimeVisibleAnnotations
