# Don't obfuscate code
-dontobfuscate

# Our code
-keep class io.github.staernid.sunbeam.binding.input.evdev.* {*;}

# KeyMapper - keep all VK_* fields for reflection
-keep class io.github.staernid.sunbeam.utils.KeyMapper {*;}

# KeyConfigHelper - keep classes and fields for Gson
-keep class io.github.staernid.sunbeam.utils.KeyConfigHelper {*;}
-keep class io.github.staernid.sunbeam.utils.KeyConfigHelper$ShortcutFile {*;}
-keep class io.github.staernid.sunbeam.utils.KeyConfigHelper$Shortcut {*;}

# Keep TensorFlow Lite GPU delegate classes that R8 might incorrectly remove
-keep class org.tensorflow.lite.gpu.** { *; }
-keep class org.opencv.** { *; }

# Profiles
-keep class io.github.staernid.sunbeam.profiles.ProfilesManager$ProfilesData {*;}
-keep class io.github.staernid.sunbeam.profiles.SettingsProfile {*;}

# Moonlight common
-keep class io.github.staernid.sunbeam.nvstream.jni.* {*;}

# Okio
-keep class sun.misc.Unsafe {*;}
-dontwarn java.nio.file.*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn okio.**

# BouncyCastle
-keep class org.bouncycastle.jcajce.provider.asymmetric.* {*;}
-keep class org.bouncycastle.jcajce.provider.asymmetric.util.* {*;}
-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.* {*;}
-keep class org.bouncycastle.jcajce.provider.digest.** {*;}
-keep class org.bouncycastle.jcajce.provider.symmetric.** {*;}
-keep class org.bouncycastle.jcajce.spec.* {*;}
-keep class org.bouncycastle.jce.** {*;}
-dontwarn javax.naming.**

# jMDNS
-dontwarn javax.jmdns.impl.DNSCache
-dontwarn org.slf4j.**

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**