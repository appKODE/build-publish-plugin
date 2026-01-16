plugins {
    id("kotlin-convention")
    id("plugin-convention")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

base {
    archivesName.set("build-publish-novo-clickup")
}

dependencies {
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
        create("ru.kode.android.build-publish-novo.clickup") {
            id = "ru.kode.android.build-publish-novo.clickup"
            displayName = "Integrate ClickUp into Android project"
            implementationClass = "ru.kode.android.build.publish.plugin.clickup.BuildPublishClickUpPlugin"
            version = project.version
            description = "Android plugin to automate ClickUp operations"
            tags.set(listOf("clickup", "publish", "changelog", "build"))
        }
    }
}
