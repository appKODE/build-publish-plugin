plugins {
    id("kotlin-convention")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

version = libs.versions.buildPublish.get()

base {
    archivesName.set("build-publish-novo.telegram")
}

dependencies {
    implementation("ru.kode.android:plugin-core")

    implementation(gradleApi())
    implementation(libs.okhttp)
    implementation(libs.okhttpLogging)
    implementation(libs.retrofit)
    implementation(libs.retrofitSerialization)
    implementation(libs.agp)
    implementation(libs.serializationJson)

    implementation(project(":plugin-foundation"))
}

gradlePlugin {
    website.set("https://github.com/appKODE/build-publish-plugin")
    vcsUrl.set("https://github.com/appKODE/build-publish-plugin")

    plugins {
        create("ru.kode.android.build-publish-novo.telegram") {
            id = "ru.kode.android.build-publish-novo.telegram"
            displayName = "Integrate Telegram into Android project"
            implementationClass = "ru.kode.android.build.publish.plugin.telegram.BuildPublishTelegramPlugin"
            version = project.version
            description = "Android plugin to automate Telegram operations"
            tags.set(listOf("telegram", "publish", "changelog", "build"))
        }
    }
}
