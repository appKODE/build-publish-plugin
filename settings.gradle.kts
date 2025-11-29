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

includeBuild("shared")
includeBuild("example-project")
includeBuild("example-plugin")
includeBuild("plugin-build")
includeBuild("plugin-test")
includeBuild("build-conventions")