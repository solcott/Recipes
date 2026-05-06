
-printmapping build/release-mapping.txt

# Suppress specific notes for dynamic access and duplicate classes
-dontnote io.ktor.**
-dontnote org.slf4j.**
-dontnote androidx.**
-dontnote kotlin.**
-dontnote kotlinx.**
-dontnote META-INF**

# Suppress warnings for missing/dynamic references on Desktop
-dontwarn android.util.Log
-dontwarn io.ktor.utils.io.jvm.javaio.PollersKt

-dontwarn org.openjsse.**
-dontwarn com.oracle.svm.core.annotate.**
-dontwarn org.graalvm.nativeimage.**
-dontwarn dagger.hilt.**
-dontwarn software.amazon.lastmile.kotlin.inject.anvil.**

-keep class org.slf4j.** { *; }
-keep class androidx.sqlite.driver.bundled.** { *; }

### OkHttp

# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**

# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*

# OkHttp platform used only on JVM and when Conscrypt and other security providers are available.
# May be used with robolectric or deliberate use of Bouncy Castle on Android
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**

### Coroutines

# ServiceLoader support
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Most of volatile fields are updated with AFU and should not be mangled
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Same story for the standard library's SafeContinuation that also uses AtomicReferenceFieldUpdater
-keepclassmembers class kotlin.coroutines.SafeContinuation {
    volatile <fields>;
}

# These classes are only required by kotlinx.coroutines.debug.internal.AgentPremain, which is only loaded when
# kotlinx-coroutines-core is used as a Java agent, so these are not needed in contexts where ProGuard is used.
-dontwarn java.lang.instrument.ClassFileTransformer
-dontwarn sun.misc.SignalHandler
-dontwarn java.lang.instrument.Instrumentation
-dontwarn sun.misc.Signal

# Only used in `kotlinx.coroutines.internal.ExceptionsConstructor`.
# The case when it is not available is hidden in a `try`-`catch`, as well as a check for Android.
-dontwarn java.lang.ClassValue

# An annotation used for build tooling, won't be directly accessed.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Keep GC anchor fields that prevent the sharing coroutine from being collected.
-keepclassmembers class kotlinx.coroutines.flow.ReadonlySharedFlow {
    kotlinx.coroutines.Job job;
}
-keepclassmembers class kotlinx.coroutines.flow.ReadonlyStateFlow {
    kotlinx.coroutines.Job job;
}


#### Kotlinx Serialization

# Keep `Companion` object field of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
 -keepclassmembers @kotlinx.serialization.Serializable class ** {
    static ** Companion;
 }

# Keep names for named companion object from obfuscation
# Names of a class and of a field are important in lookup of named companion in runtime
-if @kotlinx.serialization.internal.NamedCompanion class *
-keepclassmembers class * {
    static <1> *;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Don't print notes about potential mistakes or omissions in the configuration for kotlinx-serialization classes
# See also https://github.com/Kotlin/kotlinx.serialization/issues/1900
-dontnote kotlinx.serialization.**
# Serialization core uses `java.lang.ClassValue` for caching inside these specified classes.
# If there is no `java.lang.ClassValue` (for example, in Android), then R8/ProGuard will print a warning.
# However, since in this case they will not be used, we can disable these warnings
-dontwarn kotlinx.serialization.internal.ClassValueReferences

# disable optimisation for descriptor field because in some versions of ProGuard, optimization generates incorrect bytecode that causes a verification error
# see https://github.com/Kotlin/kotlinx.serialization/issues/2719
-keepclassmembers public class **$$serializer {
    private ** descriptor;
}

# Rule to save runtime annotations on serializable class.
# If the R8 full mode is used, annotations are removed from classes-files.
#
# For the annotation serializer, it is necessary to read the `Serializable` annotation inside the serializer<T>() function - if it is present,
# then `SealedClassSerializer` is used, if absent, then `PolymorphicSerializer'.
#
# When using R8 full mode, all interfaces will be serialized using `PolymorphicSerializer`.
#
# see https://github.com/Kotlin/kotlinx.serialization/issues/2050

#-if @kotlinx.serialization.Serializable class **
#-keep, allowshrinking, allowoptimization, allowobfuscation, allowaccessmodification class <1>

# Rule to save runtime annotations on named companion class.
# If the R8 full mode is used, annotations are removed from classes-files.
#-if @kotlinx.serialization.internal.NamedCompanion class *
#-keep, allowshrinking, allowoptimization, allowobfuscation, allowaccessmodification class <1>

# Rule to save INSTANCE field and serializer function for Kotlin serializable objects.
#
# R8 full mode works differently if the instance is not explicitly accessed in the code.
#
# see https://github.com/Kotlin/kotlinx.serialization/issues/2861
# see https://issuetracker.google.com/issues/379996140

-keepclassmembers @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

###  Ktor

# Most of volatile fields are updated with AtomicFU and should not be mangled/removed
-keepclassmembers class io.ktor.** {
    volatile <fields>;
}

-keepclassmembernames class io.ktor.** {
    volatile <fields>;
}

# client engines are loaded using ServiceLoader so we need to keep them
-keep class io.ktor.client.engine.** implements io.ktor.client.HttpClientEngineContainer

# Serialization providers are also loaded using ServiceLoader
-keep class * implements io.ktor.serialization.kotlinx.KotlinxSerializationExtensionProvider

### OKIO

-keep class okio.** { *; }

### Coil

-keep class coil3.util.DecoderServiceLoaderTarget { *; }
-keep class coil3.util.FetcherServiceLoaderTarget { *; }
-keep class coil3.util.ServiceLoaderComponentRegistry { *; }
-keep class * implements coil3.util.DecoderServiceLoaderTarget { *; }
-keep class * implements coil3.util.FetcherServiceLoaderTarget { *; }