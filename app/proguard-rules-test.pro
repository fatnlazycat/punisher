#-dontwarn sun.misc.Unsafe
-include proguard-rules.pro

-dontwarn org.mockito.internal.creation.bytebuddy.**
-dontwarn org.objenesis.instantiator.**

-keep class javax.inject.Provider { *; }
-keep class kotlin.collections.MapsKt* { *; }
-keep class kotlin.collections.ArraysKt* { *; }
-keep class kotlin.sequences.SequencesKt* { *; }

-keep class okhttp3.internal.* { *; }
-keepclassmembers class okhttp3.internal.Internal { static <methods>; }

-dontwarn net.bytebuddy.**
-dontwarn **module-info**
-dontwarn org.mockito.**

-dontwarn org.powermock.**
-dontwarn javassist.**
-keep class org.powermock.modules.junit4.legacy.internal.impl.PowerMockJUnit4LegacyRunnerDelegateImpl { *; }
#-keep class * { *; }

