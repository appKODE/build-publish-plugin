plugins {
    id("kotlin-convention")
    id("plugin-convention")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

base {
    archivesName.set("build-publish-novo-vk")
}

dependencies {
    implementation(gradleApi())
    implementation(libs.client.vk)

    compileOnly(libs.agp)
    compileOnly(project(":plugin-foundation"))
}

gradlePlugin {
    website.set("https://github.com/appKODE/build-publish-plugin")
    vcsUrl.set("https://github.com/appKODE/build-publish-plugin")

    plugins {
        create("ru.kode.android.build-publish-novo.vk") {
            id = "ru.kode.android.build-publish-novo.vk"
            displayName = "Integrate VK into Android project"
            implementationClass = "ru.kode.android.build.publish.plugin.vk.BuildPublishVkPlugin"
            version = project.version
            description = "Android plugin to send builds to VK messages"
            tags.set(listOf("vk", "vkontakte", "publish", "build"))
        }
    }
}
