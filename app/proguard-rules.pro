# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep Room entities (fields needed for reflection-based column mapping)
-keep class com.diabeto.data.entity.** { *; }

# Keep Room DAOs (interface methods needed by Room compiler)
-keep interface com.diabeto.data.dao.** { *; }

# Keep Hilt application class
-keep class * extends android.app.Application { *; }

# Keep Activities (needed for manifest references)
-keep class * extends androidx.activity.ComponentActivity {
    <init>(...);
}

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *; }

# Keep Java time (used by Room converters)
-dontwarn java.time.**
-keep class java.time.** { *; }

# Firebase (reflection-based deserialization)
-keep class com.google.firebase.firestore.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.internal.** { *; }
-dontwarn com.google.android.gms.**

# WebRTC (JNI native methods)
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# Gemini / Generative AI (reflection-based)
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# Credentials / Google Sign-In
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.** { *; }

# VoIP — keep service and connection classes (manifest-referenced)
-keep class com.diabeto.voip.VoipConnectionService { *; }
-keep class com.diabeto.voip.VoipConnection { *; }

# Notification receivers and services (manifest-referenced)
-keep class com.diabeto.notifications.ReminderReceiver { *; }
-keep class com.diabeto.notifications.ReminderReceiver$BootReceiver { *; }
-keep class com.diabeto.notifications.DiaSmartFCMService { *; }

# Widget classes (manifest-referenced)
-keep class com.diabeto.widget.** extends android.appwidget.AppWidgetProvider { *; }

# Service classes (manifest-referenced)
-keep class com.diabeto.service.StepCounterService { *; }

# Workers (referenced by WorkManager via class name)
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# SQLCipher (JNI native methods)
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

# MediaPipe LLM Inference (on-device Gemma)
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**
-dontwarn com.google.protobuf.Internal$ProtoMethodMayReturnNull
-dontwarn com.google.protobuf.Internal$ProtoNonnullApi
-dontwarn com.google.protobuf.ProtoField
-dontwarn com.google.protobuf.ProtoPresenceBits
-dontwarn com.google.protobuf.ProtoPresenceCheckedField

# General
-keepattributes Signature
-keepattributes Exceptions
-keepattributes RuntimeVisibleAnnotations
