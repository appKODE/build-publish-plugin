plugins {
    id("kotlin-convention")
    id("plugin-convention")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("com.google.devtools.ksp")
}

base {
    archivesName.set("build-publish-novo-foundation")
}

dependencies {
    implementation(gradleApi())
    implementation(libs.grgitCore)
    implementation(libs.grgitGradle)

    compileOnly(libs.agp)
}

gradlePlugin {
    website.set("https://github.com/appKODE/build-publish-plugin")
    vcsUrl.set("https://github.com/appKODE/build-publish-plugin")

    plugins {
        create("ru.kode.android.build-publish-novo.foundation") {
            id = "ru.kode.android.build-publish-novo.foundation"
            displayName = "Configure project output using tag and generate changelog"
            implementationClass = "ru.kode.android.build.publish.plugin.foundation.BuildPublishFoundationPlugin"
            version = project.version
            description = "Android plugin to configure output and changelog generation"
            tags.set(listOf("output", "publish", "changelog", "build"))
        }

        create("ru.kode.android.build-publish-novo.foundation.service") {
            id = "ru.kode.android.build-publish-novo.foundation.service"
            displayName = "Configure project output using tag and generate changelog "
            implementationClass = "ru.kode.android.build.publish.plugin.foundation.service.git.GitExecutorServicePlugin"
            version = project.version
            description = "Android plugin to configure output and changelog generation"
            tags.set(listOf("output", "publish", "changelog", "build"))
        }
    }
}

publishing {
    repositories {
        mavenLocal()
    }
}

tasks.register("setupPluginUploadFromEnvironment") {
    doLast {
        val key = System.getenv("GRADLE_PUBLISH_KEY")
        val secret = System.getenv("GRADLE_PUBLISH_SECRET")

        if (key == null || secret == null) {
            throw GradleException("gradlePublishKey and/or gradlePublishSecret are not defined environment variables")
        }

        System.setProperty("gradle.publish.key", key)
        System.setProperty("gradle.publish.secret", secret)
    }
}
