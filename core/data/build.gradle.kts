plugins {
    id("sunbeam.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.staernid.sunbeam.core.data"
}

dependencies {
    api(project(":core:preferences"))
    implementation(project(":core:native"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.annotation)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.androidx.core)
    testImplementation(libs.test.robolectric)
    testImplementation(libs.test.mockito)
    testImplementation(libs.kotlinx.coroutines.test)
}
