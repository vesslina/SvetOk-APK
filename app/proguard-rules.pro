# ════════════════════════════════════════════════════════
#  СветОк — ProGuard / R8 rules
# ════════════════════════════════════════════════════════

# ── Kotlin Serialization (Ktor использует её для JSON) ──
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Companion object каждого @Serializable класса
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# serializer() в companion object
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# INSTANCE для object-классов с @Serializable
-if @kotlinx.serialization.Serializable class ** {
    static ** INSTANCE;
}
-keepclassmembers class <1> {
    static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

-keepattributes RuntimeVisibleAnnotations, AnnotationDefault
-dontwarn kotlinx.serialization.**

# ── Room ─────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.**

# ── Ktor ─────────────────────────────────────────────────
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ── Koin ─────────────────────────────────────────────────
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# ── OsmDroid ─────────────────────────────────────────────
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# ── Firebase / FCM ───────────────────────────────────────
# Firebase имеет встроенные правила в AAR — доп. правил не нужно.
# Но на всякий случай оставляем:
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ── Классы нашего приложения ─────────────────────────────
# Оставить все data-классы в пакете data (Room entities, API DTOs)
-keep class ru.svetok.app.data.** { *; }

# ── Отладочная информация в релизе ───────────────────────
# Раскомментировать если нужны читаемые stacktrace в crashlytics:
# -keepattributes SourceFile, LineNumberTable
# -renamesourcefileattribute SourceFile
