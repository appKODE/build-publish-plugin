@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.agp) apply false
    alias(libs.plugins.firebaseAppdistribution) apply false
    alias(libs.plugins.grgit) apply false
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
    dependsOn(gradle.includedBuild("plugin-build").task(":clean"))
}

tasks.register("preMerge") {
    group = "verification"

    dependsOnRecursivelyByName(this, "ktlintFormat")
    dependsOnRecursivelyByName(this, "detektDebug")

    dependsOn(gradle.includedBuild("plugin-build").task(":preMerge"))
}
