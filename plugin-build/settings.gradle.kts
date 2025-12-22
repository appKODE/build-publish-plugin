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
        maven {
            url = uri("https://maven.pkg.github.com/appKODE/build-publish-plugin")

            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
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
    val isBuildPublication: Boolean = System.getenv("IS_BUILD_PUBLICATION")?.toBoolean() ?: false
    if (isBuildPublication) {
        dependencySubstitution {
            substitute(module("ru.kode.android:plugin-core"))
                .using(module("ru.kode.android:build-publish-novo-core:1.0.2"))
                .because("Using plugin-core from libs.versions.toml in CI/CD build")
        }
    } else {
        dependencySubstitution {
            substitute(module("ru.kode.android:plugin-core"))
                .using(project(":plugin-core"))
        }
    }
}