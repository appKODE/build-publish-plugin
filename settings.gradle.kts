pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

rootProject.name = ("build-publish")

include(":example")
includeBuild("plugin-build")
