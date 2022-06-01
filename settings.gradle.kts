pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

rootProject.name = ("build-publish")

include(":example-build-types")
include(":example-dimensions")
include(":example-flavors")
includeBuild("plugin-build")
