# Keep stack trace filenames and line numbers for Play Console crash symbol mapping
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep Kotlinx Serialization classes, enums, and generated serializers
-keepattributes *Annotation*, InnerClasses, Signature
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
-keep enum com.fivebyfivelifting.app.data.model.** { *; }
-keep class com.fivebyfivelifting.app.data.local.** { *; }
-keep class com.fivebyfivelifting.app.domain.AppBackupSnapshot { *; }
