# Crashlytics: keep line numbers so release stack traces stay readable,
# but hide original source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

# Firestore maps these classes by reflection (toObject / add / set).
# Field and getter/setter names must survive obfuscation or data silently stops mapping.
-keep class com.example.fintrac.Bill { *; }
-keep class com.example.fintrac.ChallengeItem { *; }
-keep class com.example.fintrac.JarFirestoreModel { *; }
-keep class com.example.fintrac.JarTransaction { *; }
-keep class com.example.fintrac.NotificationItem { *; }
-keep class com.example.fintrac.Transaction { *; }
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}

# Strip debug/verbose/info logging from release builds (errors and warnings stay)
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# iText uses SLF4J for logging; the optional backend is not bundled (logging becomes a no-op)
-dontwarn org.slf4j.impl.**

# iText loads some components (fonts, image handlers) reflectively; keep it intact so PDF
# export works in minified release builds
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**
