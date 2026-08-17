# FlowIsland release ProGuard/R8 rules.
# Most androidx/Compose/Hilt/Room consumer rules ship with the libraries themselves;
# these are project-specific additions only.

# Keep Room entities & DAOs (reflection-free with KSP, but keep names for migrations/debugging)
-keep class com.flowisland.android.core.database.** { *; }

# Keep data models that cross the notification / widget / process boundary
-keep class com.flowisland.android.core.activity.model.** { *; }

# Kotlin coroutines / metadata
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-dontwarn kotlinx.coroutines.**

# Remove Log calls in release builds (never log personal data in release)
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
