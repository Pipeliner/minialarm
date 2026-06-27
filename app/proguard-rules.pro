# MiniAlarm has no reflection and no runtime dependencies, so the defaults in
# proguard-android-optimize.txt are sufficient. Keep the launcher activity.
-keep class com.minialarm.MainActivity { *; }
