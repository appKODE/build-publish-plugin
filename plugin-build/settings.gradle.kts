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
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = ("ru.kode.android.build.publish.novo.plugin")

include(":plugin-base")
include(":plugin-core")
include(":plugin-telegram")
include(":plugin-slack")
include(":plugin-appcenter")
include(":plugin-clickup")
include(":plugin-jira")
include(":plugin-play")
include(":plugin-confluence")
include(":plugin-firebase")
includeBuild("../build-conventions")