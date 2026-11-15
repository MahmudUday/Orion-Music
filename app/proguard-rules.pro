# ORION MUSIC ProGuard / R8 Keep Rules

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Preserve kotlinx.serialization serializers for offline settings and JSON backup/restore
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.data.model.**$$serializer { *; }
-keep,includedescriptorclasses class com.example.data.local.**$$serializer { *; }

# Preserve Room entities and generated implementations
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
