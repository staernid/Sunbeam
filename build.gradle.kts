plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.metro) apply false
    alias(libs.plugins.roborazzi) apply false
}

// Aggregate all variant/unit tests under the root `test` task
val testTask = tasks.register("test") {
    group = "verification"
    description = "Runs every unit-test task in all sub-projects"
}

subprojects {
    tasks.matching { it.name.endsWith("UnitTest") }.configureEach {
        testTask.configure {
            dependsOn(this@configureEach)
        }
    }

    tasks.withType<Test>().configureEach {
        outputs.upToDateWhen { false }
        testLogging {
            events("passed", "failed", "skipped")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showStandardStreams = true
        }
    }
}
