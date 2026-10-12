# --- Keep line numbers for crash logs (ACRA / debugging) ---
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions,InnerClasses

# --- Project specific ---
-keep class android.iocl.dac_collector.Receivers.smsReceivers { *; }
-keep class android.iocl.dac_collector.AntiCrashLibCockroach.** { *; }

# Gson models (needed if using reflection)
-keep class android.iocl.dac_collector.ModelData.** { *; }

# Settings fragments
-keep class android.iocl.simple_keyboard.latin.settings.** { *; }

# R class
-keep class android.iocl.dac_collector.R

# --- Remove logs in release ---
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}
# Keep native methods
# Keep native methods
-keep class android.iocl.keepAlive.NativeDaemon {
    native <methods>;
}

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembernames class * {
    native <methods>;
}