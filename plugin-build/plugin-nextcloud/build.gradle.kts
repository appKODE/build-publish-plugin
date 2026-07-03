plugins {
    id("kotlin-convention")
    id("plugin-convention")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

base {
    archivesName.set("build-publish-novo-nextcloud")
}

dependencies {
    implementation(gradleApi())
    implementation(libs.client.nextcloud)

    compileOnly(libs.agp)
    compileOnly(project(":plugin-foundation"))
}

gradlePlugin {
    website.set("https://github.com/appKODE/build-publish-plugin")
    vcsUrl.set("https://github.com/appKODE/build-publish-plugin")

    plugins {
        create("ru.kode.android.build-publish-novo.nextcloud") {
            id = "ru.kode.android.build-publish-novo.nextcloud"
            displayName = "Integrate Nextcloud into Android project"
            implementationClass = "ru.kode.android.build.publish.plugin.nextcloud.BuildPublishNextcloudPlugin"
            version = project.version
            description = "Android plugin to automate Nextcloud operations"
            tags.set(listOf("nextcloud", "publish", "changelog", "build"))
        }
    }
}
