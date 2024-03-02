pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

rootProject.name = ("build-publish")

include(":example:app")
include(":example:build-types")
include(":example:build-types-gradle")
include(":example:dimensions")
include(":example:flavors")
includeBuild("plugin-build")
includeBuild("build-conventions")