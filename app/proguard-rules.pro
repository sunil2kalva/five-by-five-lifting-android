# Keep Kotlinx Serialization classes and Room entities intact
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.fivebyfivelifting.app.**$$serializer { *; }
-keepclassmembers class com.fivebyfivelifting.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.fivebyfivelifting.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
