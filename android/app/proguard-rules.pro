# Reglas Proguard para Retrofit y Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class com.cmo.hospital.data.model.** { *; }
