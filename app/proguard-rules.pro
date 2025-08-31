# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class android.iocl.dac_collector.Receivers.smsReceivers {
    *;
}

-keep class android.iocl.dac_collector.AntiCrashLibCockroach.** {
 *;
}

-keep class android.iocl.dac_collector.ModelData.** { *; }



-keep class com.google.firebase.** { *; }
-keepattributes Exceptions,InnerClasses


-keep class android.iocl.dac_collector.R
-keep class android.iocl.simple_keyboard.latin.settings.SettingsFragment
-keep class android.iocl.simple_keyboard.latin.settings.LanguagesSettingsFragment
-keep class android.iocl.simple_keyboard.latin.settings.SingleLanguageSettingsFragment

