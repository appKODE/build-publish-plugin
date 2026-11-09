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
include("agp-versions")
include("jira")
include("telegram")
include("confluence")
include("nextcloud")
include("vk")
include("yandexdisk")
include("utils")
include("clickup")
include("slack")
include("firebase")
include("play")
include("sender")
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
        substitute(module("ru.kode.android:plugin-nextcloud"))
            .using(project(":plugin-nextcloud"))
        substitute(module("ru.kode.android:plugin-vk"))
            .using(project(":plugin-vk"))
        substitute(module("ru.kode.android:plugin-yandexdisk"))
            .using(project(":plugin-yandexdisk"))
        substitute(module("ru.kode.android:plugin-clickup"))
            .using(project(":plugin-clickup"))
        substitute(module("ru.kode.android:plugin-slack"))
            .using(project(":plugin-slack"))
        substitute(module("ru.kode.android:plugin-firebase"))
            .using(project(":plugin-firebase"))
        substitute(module("ru.kode.android:plugin-play"))
            .using(project(":plugin-play"))
        substitute(module("ru.kode.android:plugin-sender"))
            .using(project(":plugin-sender"))
    }
}

includeBuild("../build-conventions")
includeBuild("../shared") {
    dependencySubstitution {
        substitute(module("ru.kode.android:build-publish-novo-core")).using(project(":plugin-core"))
        substitute(module("ru.kode.android:build-publish-novo-client-slack")).using(project(":client-slack"))
        substitute(module("ru.kode.android:build-publish-novo-client-telegram")).using(project(":client-telegram"))
        substitute(module("ru.kode.android:build-publish-novo-client-nextcloud")).using(project(":client-nextcloud"))
        substitute(module("ru.kode.android:build-publish-novo-client-vk")).using(project(":client-vk"))
        substitute(module("ru.kode.android:build-publish-novo-client-yandexdisk")).using(project(":client-yandexdisk"))
        substitute(module("ru.kode.android:build-publish-novo-client-jira")).using(project(":client-jira"))
        substitute(module("ru.kode.android:build-publish-novo-client-confluence")).using(project(":client-confluence"))
        substitute(module("ru.kode.android:build-publish-novo-client-clickup")).using(project(":client-clickup"))
        substitute(module("ru.kode.android:build-publish-novo-client-firebase")).using(project(":client-firebase"))
    }
}
