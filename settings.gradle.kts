pluginManagement {
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
