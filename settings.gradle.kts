pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

rootProject.name = ("firebase-publish-plugin")

include(":example")
includeBuild("plugin-build")
