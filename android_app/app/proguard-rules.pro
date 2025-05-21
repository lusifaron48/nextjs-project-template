# Add project specific ProGuard rules here.

# Keep the application class
-keep class com.example.photoorganizer.** { *; }

# TensorFlow Lite rules
-keep class org.tensorflow.lite.** { *; }
-keepclassmembers class org.tensorflow.lite.** { *; }

# Keep model files
-keep class * extends org.tensorflow.lite.task.vision.classifier.ImageClassifier { *; }
-keepclassmembers class * extends org.tensorflow.lite.task.vision.classifier.ImageClassifier { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep parcelable classes
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep serializable classes
-keepnames class * implements java.io.Serializable

# Keep R classes
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
