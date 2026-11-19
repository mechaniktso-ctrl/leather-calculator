# Compose и AppCompat
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
# JSON (org.json входит в платформу)
-keepclassmembers class * { @androidx.compose.runtime.Composable <methods>; }
