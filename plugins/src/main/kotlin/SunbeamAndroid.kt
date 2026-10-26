import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.configureCommonAndroid(
    extension: CommonExtension<*, *, *, *, *, *>
) {
    extension.apply {
        compileSdk = 36

        defaultConfig {
            minSdk = 21
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compileOptions {
            encoding = "UTF-8"
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
            isCoreLibraryDesugaringEnabled = true
        }

        lint {
            disable += "MissingTranslation"
            val lintConfigFile = project.file("lint.xml")
            if (lintConfigFile.exists()) {
                lintConfig = lintConfigFile
            }
        }

        testOptions {
            unitTests.isIncludeAndroidResources = true
        }

        packaging {
            resources {
                pickFirsts += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
                excludes += "/META-INF/*.md"
            }
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    dependencies {
        add("coreLibraryDesugaring", libs.findLibrary("desugar-jdk-libs").get())
    }
}
