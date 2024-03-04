@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.publish) apply false
    alias(libs.plugins.ksp) apply false
}

allprojects {
    group = "ru.kode.android"
    version = "1.2.0"
}

val dependsOnRecursivelyByName = { task: Task, name: String ->
    subprojects {
        this.tasks.matching { it.name == name }.forEach { t ->
            task.dependsOn(t)
        }
    }
}

tasks.register("clean", Delete::class.java) {
    delete(rootProject.buildDir)
    dependsOnRecursivelyByName(this, "clean")
}

tasks.register("preMerge") {
    group = "verification"

    dependsOnRecursivelyByName(this, "check")
    dependsOnRecursivelyByName(this, "validatePlugins")
    dependsOnRecursivelyByName(this, "ktlintFormat")
    dependsOnRecursivelyByName(this, "detektDebug")
}
