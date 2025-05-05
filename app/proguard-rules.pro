# Keep mail classes
-keep class javax.** { *; }
-keep class com.sun.mail.** { *; }

# Keep location services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Keep camera classes
-keep class androidx.camera.** { *; }

# Keep app components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver