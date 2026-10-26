plugins {
    id("sunbeam.android.library")
}

android {
    namespace = "io.github.staernid.sunbeam.core.bridge"
    ndkVersion = "27.0.12077973"

    externalNativeBuild {
        ndkBuild {
            path = file("src/main/jni/Android.mk")
        }
    }
}
