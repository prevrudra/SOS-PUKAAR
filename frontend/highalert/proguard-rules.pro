-keepclassmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclasseswithmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}
-keep class com.pukaar.highalert.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata { public <methods>; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn com.google.firebase.**
-dontwarn org.jetbrains.annotations.**
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.squareup.moshi.** { *; }
