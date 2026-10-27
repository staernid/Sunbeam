pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "sunbeam"
includeBuild("plugins")
include(":core:native")
include(":core:stream")
include(":core:preferences")
include(":core:data")
include(":core:designsystem")
include(":app")
