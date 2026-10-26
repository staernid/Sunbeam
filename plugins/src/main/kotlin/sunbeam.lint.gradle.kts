plugins {
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
}

val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

ktlint {
    android.set(true)
    ignoreFailures.set(false)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}

detekt {
    toolVersion = libsCatalog.findVersion("detekt").get().requiredVersion
    buildUponDefaultConfig = true
    allRules = false
    val detektConfig = rootProject.file("detekt.yml")
    if (detektConfig.exists()) {
        config.setFrom(files(detektConfig))
    }
}
