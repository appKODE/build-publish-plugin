plugins {
    id("kotlin-convention")
    id("plugin-convention")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

base {
    archivesName.set("build-publish-novo-jira")
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
        create("ru.kode.android.build-publish-novo.jira") {
            id = "ru.kode.android.build-publish-novo.jira"
            displayName = "Integrate Jira into Android project"
            implementationClass = "ru.kode.android.build.publish.plugin.jira.BuildPublishJiraPlugin"
            version = project.version
            description = "Android plugin to automate Jira operations"
            tags.set(listOf("jira", "publish", "changelog", "build"))
        }
    }
}
