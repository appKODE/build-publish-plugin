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

include(":plugin-foundation")
include(":plugin-telegram")
include(":plugin-slack")
include(":plugin-clickup")
include(":plugin-jira")
include(":plugin-play")
include(":plugin-confluence")
include(":plugin-firebase")
includeBuild("../build-conventions")
includeBuild("../shared") {
    dependencySubstitution {
        substitute(module("ru.kode.android:plugin-core"))
            .using(project(":pugin-core"))
    }
}
