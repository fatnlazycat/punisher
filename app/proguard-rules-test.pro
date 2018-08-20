#-dontwarn sun.misc.Unsafe
-include proguard-rules.pro
-keep class javax.inject.Provider { *; }
-keep class kotlin.collections.MapsKt { *; }
#-keep class kotlin.Metadata { *; }