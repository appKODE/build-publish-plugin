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
    implementation(libs.agp)

    testImplementation(libs.junit)

    ksp(libs.moshiCodgen)
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "ru.kode.android.build-publish-novo.slack".removePrefix("$groupId.")
            version = project.version.toString()

            from(components["java"])
        }
    }
}
