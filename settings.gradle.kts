pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

rootProject.name = ("build-publish-plugin")

include(":example")
includeBuild("plugin-build")
