plugins {
    id("kotlin-convention")
    id("plugin-convention")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

base {
    archivesName.set("build-publish-novo-slack")
}

dependencies {
    implementation(gradleApi())
    implementation(libs.okhttp)
    implementation(libs.okhttpLogging)
    implementation(libs.retrofit)
    implementation(libs.retrofitSerialization)
    implementation(libs.serializationJson)

    compileOnly(libs.agp)
    compileOnly(project(":plugin-foundation"))
}

gradlePlugin {
    website.set("https://github.com/appKODE/build-publish-plugin")
    vcsUrl.set("https://github.com/appKODE/build-publish-plugin")

    plugins {
        create("ru.kode.android.build-publish-novo.slack") {
            id = "ru.kode.android.build-publish-novo.slack"
            displayName = "Integrate Slack into Android project"
            implementationClass = "ru.kode.android.build.publish.plugin.slack.BuildPublishSlackPlugin"
            version = project.version
            description = "Android plugin to automate Slack operations"
            tags.set(listOf("slack", "publish", "changelog", "build"))
        }
    }
}
