# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/akihit/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-keep class com.freshdigitable.udonroad.module.realm.** { *; }

-dontwarn java.lang.invoke.*

# picasso
-dontwarn com.squareup.okhttp.**

# okhttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# twitter4j
-dontwarn twitter4j.**
-keep class twitter4j.conf.PropertyConfigurationFactory { *; }
-keep class twitter4j.TwitterImpl { *; }
-keep class twitter4j.AlternativeHttpClientImpl { *; }
-keep class twitter4j.DispatcherImpl { *; }

# dagger
-dontwarn com.google.errorprone.annotations.*