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

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "ru.kode.android.build-publish-novo.jira".removePrefix("$groupId.")
            version = project.version.toString()

            from(components["java"])
        }
    }
}
