plugins {
    id("kotlin-convention")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation(project(":plugin-core"))

    implementation(gradleApi())
    implementation(libs.okhttp)
    implementation(libs.okhttpLogging)
    implementation(libs.moshi)
    implementation(libs.retrofit)
    implementation(libs.retrofitMoshi)

    testImplementation(libs.junit)

    ksp(libs.moshiCodgen)
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "ru.kode.android.build-publish-novo.telegram".removePrefix("$groupId.")
            version = project.version.toString()

            from(components["java"])
        }
    }
}
