plugins {
    id("org.jetbrains.kotlin.plugin.compose")
}

val androidExtension = extensions.findByType<com.android.build.api.dsl.CommonExtension<*, *, *, *, *, *>>()
androidExtension?.apply {
    buildFeatures {
        compose = true
    }
}

val composeCompilerExtension = extensions.findByType<org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension>()
composeCompilerExtension?.apply {
    val stabilityConfigFile = rootProject.layout.projectDirectory.file("compose-stability.conf")
    if (stabilityConfigFile.asFile.exists()) {
        stabilityConfigurationFiles.add(stabilityConfigFile)
    }
}
