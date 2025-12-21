# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

# Gson
-keep class com.google.gson.** { *; }
-keep interface com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.polaralias.letsdoit.data.remote.** { *; } # Keep DTOs

# Room
-keep class androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# Hilt
-keep class com.polaralias.letsdoit.LetsDoItApp { *; }
-keep class dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper$1
-keep class dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-keep public class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper

# Compose
-keep class androidx.compose.ui.platform.WrappedComposition { *; }

# Coroutines
-keep class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keep class kotlinx.coroutines.android.AndroidExceptionPreHandler { *; }
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }

# ViewModel
-keep class * extends androidx.lifecycle.ViewModel { *; }
