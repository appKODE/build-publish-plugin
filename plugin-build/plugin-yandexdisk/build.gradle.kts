plugins {
    id("kotlin-convention")
    id("plugin-convention")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

base {
    archivesName.set("build-publish-novo-yandexdisk")
}

dependencies {
    implementation(gradleApi())
    implementation(libs.client.yandexdisk)

    compileOnly(libs.agp)
    compileOnly(project(":plugin-foundation"))
}

gradlePlugin {
    website.set("https://github.com/appKODE/build-publish-plugin")
    vcsUrl.set("https://github.com/appKODE/build-publish-plugin")

    plugins {
        create("ru.kode.android.build-publish-novo.yandexdisk") {
            id = "ru.kode.android.build-publish-novo.yandexdisk"
            displayName = "Integrate Yandex Disk into Android project"
            implementationClass = "ru.kode.android.build.publish.plugin.yandexdisk.BuildPublishYandexDiskPlugin"
            version = project.version
            description = "Android plugin to upload builds and changelogs to Yandex Disk"
            tags.set(listOf("yandex", "yandexdisk", "publish", "changelog", "build"))
        }
    }
}
