plugins {
    id("sunbeam.android.application")
    id("sunbeam.android.compose")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

android {
    ndkVersion = "27.0.12077973"
    namespace = "io.github.staernid.sunbeam"

    defaultConfig {
        applicationId = "io.github.staernid.sunbeam"
        versionName = "20.2.6"
        versionCode = 57

        ndk {
            debugSymbolLevel = "FULL"
        }

        val obtainiumUrl =
            "https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22io.github.staernid.sunbeam%22" +
                "%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fstaernid%2Fmoonlight-android%22%2C%22author%22%3A%22staernid%22" +
                "%2C%22name%22%3A%22Sunbeam%22%2C%22additionalSettings%22%3A%22%7B%5C%22apkFilterRegEx%5C%22%3A%5C%22release%5C%22" +
                "%2C%5C%22matchGroutToUse%5C%22%3A%5C%22%241%5C%22%2C%5C%22versionExtractionRegEx%5C%22%3A%5C%22v(.%2B)%5C%22%7D%22%7D"

        resValue(
            "string",
            "obtainium_app_url",
            obtainiumUrl,
        )
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_label", "Sunbeam (Debug)")
        }
        release {
            resValue("string", "app_label", "Sunbeam")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    bundle {
        language {
            enableSplit = false
        }
        density {
            enableSplit = false
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
        }
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(project(":core:native"))
    implementation(project(":core:stream"))
    implementation(project(":core:preferences"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":features:hosts"))
    implementation(project(":features:apps"))
    implementation(project(":features:settings"))
    implementation(project(":features:profiles"))
    implementation(project(":features:stream"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.bouncycastle.bcprov)
    implementation(libs.bouncycastle.bcpkix)
    implementation(libs.jcodec)
    implementation(libs.okhttp)
    implementation(libs.jmdns)
    implementation(libs.shield.controller.extensions)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.metro.runtime)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.androidx.core)
    testImplementation(libs.test.robolectric)
    testImplementation(libs.test.mockito)
    testImplementation(libs.test.compose.ui.junit4)
    testImplementation(libs.test.compose.ui.manifest)
    testImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.test.compose.ui.manifest)
}
