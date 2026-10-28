plugins {
    id("sunbeam.android.library")
    id("sunbeam.android.compose")
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "io.github.staernid.sunbeam.features.profiles"
}

roborazzi {
    outputDir.set(file("src/test/screenshots"))
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    api(project(":core:designsystem"))
    api(project(":core:data"))
    api(project(":core:preferences"))

    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.test.compose.ui.manifest)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.androidx.core)
    testImplementation(libs.test.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.test.compose.ui.junit4)
    testImplementation(libs.test.compose.ui.manifest)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}
