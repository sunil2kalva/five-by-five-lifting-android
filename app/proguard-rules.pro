# Keep Kotlinx Serialization classes and Room entities intact
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.stronglifts5x5.app.**$$serializer { *; }
-keepclassmembers class com.stronglifts5x5.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.stronglifts5x5.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
