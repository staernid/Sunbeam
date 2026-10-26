plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("sunbeam.lint")
}

val androidExtension = extensions.getByType<com.android.build.api.dsl.ApplicationExtension>()

configureCommonAndroid(androidExtension)

androidExtension.apply {
    defaultConfig {
        targetSdk = 36
    }
}
