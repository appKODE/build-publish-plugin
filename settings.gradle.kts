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
}

rootProject.name = ("build-publish")

includeBuild("shared")
includeBuild("example-project")
includeBuild("example-plugin")
includeBuild("plugin-build")
includeBuild("plugin-test")
includeBuild("build-conventions")