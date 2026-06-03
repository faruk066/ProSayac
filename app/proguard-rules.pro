# ProGuard rules for ProSayac

# Keep Room entities
-keep class com.prosayac.app.data.local.entity.** { *; }

# Keep domain models for serialization
-keep class com.prosayac.app.domain.model.** { *; }

# Apache POI
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.openxmlformats.schemas.**
-dontwarn org.etsi.uri.**
-dontwarn org.apache.xmlbeans.**
-dontwarn com.graphbuilder.**
-dontwarn net.sf.saxon.**
-dontwarn javax.xml.stream.**
-dontwarn org.osgi.**
-dontwarn aQute.bnd.**
-dontwarn java.awt.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.apache.commons.logging.**

# USB Serial
-keep class com.hoho.android.usbserial.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}