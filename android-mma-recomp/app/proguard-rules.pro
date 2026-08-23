# kotlinx.serialization — les modèles Postgres reposent sur les @Serializable
# générés à la compilation ; sans ces règles, R8 supprime les $serializer
# internes et le (dé)sérialisation JSON casse silencieusement à l'exécution.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.mmarecomp.**$$serializer { *; }
-keepclassmembers class com.example.mmarecomp.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.mmarecomp.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class kotlinx.serialization.internal.** { *; }

# Ktor / OkHttp / coroutines — utilisés en interne par supabase-kt, fortement
# réflexifs. On garde des règles larges plutôt que de risquer un crash au
# runtime pour gagner quelques Ko.
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.**
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }

# supabase-kt
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**
