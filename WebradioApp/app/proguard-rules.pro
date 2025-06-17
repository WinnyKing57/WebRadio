# Default ProGuard rules for Android applications.
# Optimization is already handled by proguard-android-optimize.txt

# Keep application classes that are entry points
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class com.android.vending.licensing.ILicensingService

# Keep all Views that are inflated from XML.
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# Keep all activities with a default constructor.
-keep public class * extends android.app.Activity {
    public <init>();
}

# Keep all fragments with a default constructor.
-keep public class * extends androidx.fragment.app.Fragment {
    public <init>();
}
-keep public class * extends android.app.Fragment {
    public <init>();
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep parcelable classes
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# Keep members of classes marked with @Keep
-keepclassmembers class androidx.annotation.Keep
-keepclassmembers class com.google.android.material.internal.ParcelableSparseArray

# Kotlin specific rules
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata { *; } # Redundant but often seen
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepclassmembers class ** {
    @kotlin.jvm.JvmField <fields>;
    @kotlin.jvm.JvmStatic <methods>;
}
-keep enum kotlin.annotation.AnnotationTarget { *; } # Required for KSP
-keep enum kotlin.annotation.AnnotationRetention { *; } # Required for KSP


# Gson rules (for data models like RadioStation if used in Retrofit, SharedPreferences)
# Adjust com.example.webradioapp.model.** to your actual model package
-keep class com.example.webradioapp.model.** { *; }
-keepclassmembers class com.example.webradioapp.model.** { *; }
-keepattributes Signature # Needed for GSON
-keepattributes InnerClasses # Needed for GSON

# Retrofit and OkHttp
-dontwarn retrofit2.**
-keepclassmembers interface retrofit2.** { *; }
-keep class retrofit2.**
-keep class com.squareup.okhttp3.** { *; }
-keep interface com.squareup.okhttp3.** { *; }
-dontwarn com.squareup.okhttp3.**
-dontwarn okio.**

# Kotlinx Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler { *; }
-keepnames class kotlinx.coroutines.internal.FastServiceLoader { *; }
-keepclassmembernames class kotlinx.coroutines.flow.internal.AbstractSharedFlow {
  kotlinx.coroutines.flow.SharedFlowSlot[] Emitter;
}

# Glide (KSP should handle most, but these are common)
# -keep public class * implements com.bumptech.glide.module.GlideModule
# -keep public class * extends com.bumptech.glide.module.AppGlideModule
# -keep public enum com.bumptech.glide.load.ImageHeaderParser$ImageType { *; }
# -keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$ImageType { *; } # Older versions
# -keepclassmembers class * {
# @com.bumptech.glide.annotation.GlideExtension <methods>;
# }
# -keep public class com.bumptech.glide.GeneratedAppGlideModuleImpl extends com.bumptech.glide.GeneratedAppGlideModule {}
# KSP for Glide should make manual rules less necessary.
# If using ` Glide.with(context).load(resource).into(imageView);` and generated APIs, KSP handles it.
# Add specific rules if reflection or custom components are used with Glide.

# Room (KSP handles most of it)
# -keep class androidx.room.** { *; }
# -keepclassmembers class * { @androidx.room.PrimaryKey *; }
# -keepclassmembers class * { @androidx.room.Entity *; }
# -keepclassmembers class * { @androidx.room.Dao *; }
# -keepclassmembers class * { @androidx.room.Database *; }
# -keepclassmembers class * { @androidx.room.TypeConverter *; }
# -keepclassmembers class * { @androidx.room.Embedded *; }
# -keepclassmembers class * { @androidx.room.Relation *; }

# ExoPlayer / Media3
# Generally, Media3/ExoPlayer does not require extensive ProGuard rules if you are using it normally.
# However, if you use reflection or custom components, you might need to add specific rules.
# For default usage, these might not be strictly necessary but are common fallbacks:
-keep class androidx.media3.** { *; }
-keepinterface androidx.media3.** { *; }
-keepnames class androidx.media3.common.TrackSelectionParameters { *; } # If you're serializing or reflecting on this
-keepnames class androidx.media3.common.Tracks { *; }
-keepnames class androidx.media3.common.Format { *; }


# Google Play Services (Cast Framework specifically)
# Most Play Services libraries are ProGuard-friendly.
# Cast framework specific rules if needed (usually not for basic integration)
-keep class com.google.android.gms.cast.framework.** { *; }
-keep interface com.google.android.gms.cast.framework.** { *; }

# Keep custom OptionsProvider for Cast
-keep class com.example.webradioapp.CastOptionsProvider { <init>(); }


# Add any other specific rules your application might need below
# For example, if you use reflection for other classes, keep them here.
