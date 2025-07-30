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
        create("ru.kode.android.build-publish.jira") {
            id = "ru.kode.android.build-publish.jira"
            displayName = "Configure project with Firebase App Distribution and changelogs"
            implementationClass = "ru.kode.android.build.publish.plugin.jira.BuildPublishJiraPlugin"
            version = project.version
            description = "Android plugin to publish bundles and apks to Firebase App Distribution with changelogs"
            tags.set(listOf("firebase", "publish", "changelog", "build"))
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "ru.kode.android.build-publish.jira".removePrefix("$groupId.")
            version = project.version.toString()

            from(components["java"])
        }
    }
}
