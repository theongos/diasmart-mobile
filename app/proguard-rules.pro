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

# Keep Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Keep WebRTC
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# Keep Gemini / Generative AI
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# Keep Credentials / Google Sign-In
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.** { *; }

# Keep VoIP / Telecom classes
-keep class com.diabeto.voip.** { *; }

# Keep notification classes
-keep class com.diabeto.notifications.** { *; }

# Keep widget classes
-keep class com.diabeto.widget.** { *; }

# Keep service classes
-keep class com.diabeto.service.** { *; }

# Keep sync worker
-keep class com.diabeto.sync.** { *; }

# General
-keepattributes Signature
-keepattributes Exceptions
-keepattributes RuntimeVisibleAnnotations
