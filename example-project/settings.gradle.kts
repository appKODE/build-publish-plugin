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

rootProject.name = ("ru.kode.android.build.publish.example-project")

include("app")
include("build-types")
include("build-types-gradle")
include("dimensions")
include("flavors")
includeBuild("../plugin-build")
includeBuild("../example-plugin")
includeBuild("../build-conventions")
includeBuild("../shared") {
    dependencySubstitution {
        substitute(module("ru.kode.android:build-publish-novo-core")).using(project(":plugin-core"))
        substitute(module("ru.kode.android:build-publish-novo-client-slack")).using(project(":client-slack"))
        substitute(module("ru.kode.android:build-publish-novo-client-telegram")).using(project(":client-telegram"))
        substitute(module("ru.kode.android:build-publish-novo-client-nextcloud")).using(project(":client-nextcloud"))
        substitute(module("ru.kode.android:build-publish-novo-client-jira")).using(project(":client-jira"))
        substitute(module("ru.kode.android:build-publish-novo-client-confluence")).using(project(":client-confluence"))
        substitute(module("ru.kode.android:build-publish-novo-client-clickup")).using(project(":client-clickup"))
    }
}