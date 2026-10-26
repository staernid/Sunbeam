plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("sunbeam.lint")
}

val androidExtension = extensions.getByType<com.android.build.api.dsl.LibraryExtension>()

configureCommonAndroid(androidExtension)

androidExtension.apply {
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}
