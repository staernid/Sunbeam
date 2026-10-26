plugins {
    id("sunbeam.android.library")
}

android {
    namespace = "io.github.staernid.sunbeam.core.data"
}

dependencies {
    api(project(":core:preferences"))
    implementation(project(":core:native"))

    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.annotation)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.androidx.core)
    testImplementation(libs.test.robolectric)
    testImplementation(libs.test.mockito)
    testImplementation(libs.kotlinx.coroutines.test)
}
