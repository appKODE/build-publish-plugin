plugins {
    id("kotlin-convention")
    id("plugin-convention")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

base {
    archivesName.set("build-publish-novo-confluence")
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
        create("ru.kode.android.build-publish-novo.confluence") {
            id = "ru.kode.android.build-publish-novo.confluence"
            displayName = "Integrate Confluence into Android project"
            implementationClass = "ru.kode.android.build.publish.plugin.confluence.BuildPublishConfluencePlugin"
            version = project.version
            description = "Android plugin to automate Confluence operations"
            tags.set(listOf("confluence", "publish", "changelog", "build"))
        }
    }
}
