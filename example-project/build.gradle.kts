@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.agp) apply false
    alias(libs.plugins.grgit) apply false
    alias(libs.plugins.firebaseAppdistribution) apply false
    alias(libs.plugins.publish) apply false
}

tasks.register("assemble", Delete::class.java) {
    dependsOnRecursivelyByName(this, "assemble")
}

val dependsOnRecursivelyByName = { task: Task, name: String ->
    subprojects {
        this.tasks.matching { it.name == name }.forEach { t ->
            task.dependsOn(t)
        }
    }
}