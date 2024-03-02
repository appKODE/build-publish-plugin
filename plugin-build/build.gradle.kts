import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import io.gitlab.arturbosch.detekt.Detekt

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.publish) apply false
    alias(libs.plugins.versionsManes) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt) apply true
    alias(libs.plugins.ktlint) apply true
}

allprojects {
    group = PluginCoordinates.GROUP
    version = PluginCoordinates.VERSION

    apply {
        plugin("io.gitlab.arturbosch.detekt")
        plugin("org.jlleitschuh.gradle.ktlint")
    }

    ktlint {
        debug.set(false)
        verbose.set(true)
        android.set(false)
        outputToConsole.set(true)
        ignoreFailures.set(false)
        disabledRules.set(setOf("import-ordering"))
        enableExperimentalRules.set(true)
        filter {
            exclude("**/generated/**")
            include("**/kotlin/**")
        }
    }

    detekt {
        config = rootProject.files("../config/detekt/detekt.yml")
    }
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true)
        html.outputLocation.set(file("build/reports/detekt.html"))
    }
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}

fun isNonStable(version: String) = "^[0-9,.v-]+(-r)?$".toRegex().matches(version).not()

tasks.register("clean", Delete::class.java) {
    delete(rootProject.buildDir)
}
