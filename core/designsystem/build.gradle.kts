plugins {
    id("sunbeam.android.library")
    id("sunbeam.android.compose")
}

android {
    namespace = "io.github.staernid.sunbeam.core.designsystem"
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material3.windowsizeclass)
    api(libs.androidx.compose.material.icons.core)
    api(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.test.compose.ui.manifest)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.androidx.core)
    testImplementation(libs.test.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.test.compose.ui.junit4)
}
