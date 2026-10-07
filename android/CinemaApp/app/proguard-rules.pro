# ProGuard rules for RNPS Home Cinema

-keep class com.cineplex.app.data.model.** { *; }
-keepclassmembers class com.cineplex.app.data.model.** { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn kotlinx.serialization.**
