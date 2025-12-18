pluginManagement {
    repositories {
        mavenLocal()
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

rootProject.name = ("ru.kode.android.build.publish.plugin-test")

include("foundation")
include("jira")
include("telegram")
include("confluence")
include("utils")
include("clickup")
include("slack")
includeBuild("../plugin-build") {
    dependencySubstitution {
        substitute(module("ru.kode.android:plugin-foundation"))
            .using(project(":plugin-foundation"))
        substitute(module("ru.kode.android:plugin-jira"))
            .using(project(":plugin-jira"))
        substitute(module("ru.kode.android:plugin-telegram"))
            .using(project(":plugin-telegram"))
        substitute(module("ru.kode.android:plugin-confluence"))
            .using(project(":plugin-confluence"))
        substitute(module("ru.kode.android:plugin-clickup"))
            .using(project(":plugin-clickup"))
        substitute(module("ru.kode.android:plugin-slack"))
            .using(project(":plugin-slack"))
    }
}

includeBuild("../build-conventions")
includeBuild("../shared") {
    dependencySubstitution {
        substitute(module("ru.kode.android:plugin-core"))
            .using(project(":plugin-core"))
    }
}
