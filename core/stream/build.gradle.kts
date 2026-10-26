plugins {
    id("sunbeam.android.library")
}

android {
    namespace = "io.github.staernid.sunbeam.core.stream"
}

dependencies {
    api(project(":core:native"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.annotation)

    testImplementation(libs.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
