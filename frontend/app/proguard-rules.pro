
-keepclassmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep class com.pukaar.app.data.api.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
