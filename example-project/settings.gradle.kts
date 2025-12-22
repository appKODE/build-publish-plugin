import kotlin.jvm.optionals.getOrNull

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
        substitute(module("ru.kode.android:plugin-core"))
            .using(project(":plugin-core"))
    }
}