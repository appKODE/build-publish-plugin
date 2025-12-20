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
    val isBuildPublication: Boolean = System.getenv("IS_BUILD_PUBLICATION")?.toBoolean() ?: false
    if (isBuildPublication) {
        dependencySubstitution {
            val libsCatalog = settings.extensions.getByType<VersionCatalogsExtension>()
            val pluginCoreVersion = libsCatalog.named("libs")
                .findVersion("plugin-core")
                .get()
                .requiredVersion
            val pluginCoreLibrary = libsCatalog.named("libs")
                .findLibrary("plugin-core")
                .get()
                .get()
                .module

            substitute(module("ru.kode.android:build-publish-novo-core"))
                .using(module("${pluginCoreLibrary.name}:${pluginCoreLibrary.group}:$pluginCoreVersion"))
                .because("Using plugin-core from libs.versions.toml in CI/CD build")
        }
    } else {
        dependencySubstitution {
            substitute(module("ru.kode.android:build-publish-novo-core"))
                .using(project(":plugin-core"))
        }
    }
}
