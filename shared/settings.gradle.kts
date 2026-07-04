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

include(":plugin-core")
include(":client-slack")
include(":client-telegram")
include(":client-nextcloud")
include(":client-jira")
include(":client-confluence")
include(":client-clickup")
includeBuild("../build-conventions")
