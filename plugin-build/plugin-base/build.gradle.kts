plugins {
    id("kotlin-convention")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation(project(":plugin-core"))
    implementation(project(":plugin-telegram"))
    implementation(project(":plugin-slack"))
    implementation(project(":plugin-appcenter"))
    implementation(project(":plugin-clickup"))
    implementation(project(":plugin-jira"))
    implementation(project(":plugin-play"))
    implementation(project(":plugin-confluence"))
    implementation(project(":plugin-firebase"))

    implementation(gradleApi())
    implementation(libs.agp)
    implementation(libs.grgitCore)
    implementation(libs.grgitGradle)
    testImplementation(libs.junit)
}

gradlePlugin {
    website.set("https://github.com/appKODE/build-publish-plugin")
    vcsUrl.set("https://github.com/appKODE/build-publish-plugin")

    plugins {
        create("ru.kode.android.build-publish-novo.base") {
            id = "ru.kode.android.build-publish-novo.base"
            displayName = "Configure project with Firebase App Distribution and changelogs"
            implementationClass = "ru.kode.android.build.publish.plugin.base.BuildPublishPluginBase"
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
            artifactId = "ru.kode.android.build-publish-novo".removePrefix("$groupId.")
            version = project.version.toString()

            from(components["java"])
        }
    }
}

tasks.create("setupPluginUploadFromEnvironment") {
    doLast {
        val key = System.getenv("GRADLE_PUBLISH_KEY")
        val secret = System.getenv("GRADLE_PUBLISH_SECRET")

        if (key == null || secret == null) {
            throw GradleException("gradlePublishKey and/or gradlePublishSecret are not defined environment variables")
        }

        System.setProperty("gradle.publish.key", key)
        System.setProperty("gradle.publish.secret", secret)
    }
}
