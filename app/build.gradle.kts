plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    ndkVersion = "27.0.12077973"

    compileSdk = 36

    namespace = "io.github.staernid.sunbeam"

    defaultConfig {
        minSdk = 21
        targetSdk = 34

        versionName = "20.2.6"
        versionCode = 57

        ndk {
            debugSymbolLevel = "FULL"
        }
    }

    flavorDimensions += "root"

    buildFeatures {
        buildConfig = true
    }

    productFlavors {
        create("root") {
            maxSdk = 25

            externalNativeBuild {
                ndkBuild {
                    arguments += "PRODUCT_FLAVOR=root"
                }
            }

            resValue("string", "obtainium_app_url", "data:text/html;base64,PGgxPlJvb3QgYnVpbGQgaXMgbm90IGF2YWlsYWJsZTwvaDE+")
            applicationId = "io.github.staernid.sunbeam.root"
            dimension = "root"
            buildConfigField("boolean", "ROOT_BUILD", "true")
        }

        create("nonRoot_game") {
            externalNativeBuild {
                ndkBuild {
                    arguments += "PRODUCT_FLAVOR=nonRoot"
                }
            }

            resValue(
                "string",
                "obtainium_app_url",
                "https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22io.github.staernid.sunbeam%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fstaernid%2Fmoonlight-android%22%2C%22author%22%3A%22staernid%22%2C%22name%22%3A%22Sunbeam%22%2C%22additionalSettings%22%3A%22%7B%5C%22apkFilterRegEx%5C%22%3A%5C%22nonRoot%5C%22%2C%5C%22matchGroutToUse%5C%22%3A%5C%22%241%5C%22%2C%5C%22versionExtractionRegEx%5C%22%3A%5C%22v(.%2B)%5C%22%7D%22%7D"
            )
            applicationId = "io.github.staernid.sunbeam"
            dimension = "root"
            buildConfigField("boolean", "ROOT_BUILD", "false")
        }
    }

    compileOptions {
        encoding = "UTF-8"
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    lint {
        disable += "MissingTranslation"
        lintConfig = file("lint.xml")
    }

    bundle {
        language {
            enableSplit = false
        }
        density {
            enableSplit = false
        }
    }

    packaging {
        resources {
            pickFirsts += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_label", "Sunbeam (Debug)")
            resValue("string", "app_label_root", "Sunbeam (Root Debug)")
            resValue("string", "app_label_game", "Sunbeam (Game Debug)")

            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
        release {
            applicationIdSuffix = ""
            resValue("string", "app_label", "Sunbeam")
            resValue("string", "app_label_root", "Sunbeam (Root)")
            resValue("string", "app_label_game", "Sunbeam (Game)")

            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    externalNativeBuild {
        ndkBuild {
            path = file("src/main/jni/Android.mk")
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(libs.bouncycastle.bcprov)
    implementation(libs.bouncycastle.bcpkix)
    implementation(libs.jcodec)
    implementation(libs.okhttp)
    implementation(libs.jmdns)
    implementation(libs.shield.controller.extensions)
    implementation(libs.gson)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)
    implementation(libs.search.preference)
    implementation(libs.mp.android.chart)
    implementation(libs.litert)
    implementation(libs.litert.gpu)
    implementation(libs.opencv)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.androidx.core)
    testImplementation(libs.test.robolectric)
    testImplementation(libs.test.mockito)
}
