plugins {
    id("kotlin-convention")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation("ru.kode.android:plugin-core")

    implementation(gradleApi())
    implementation(libs.play.publish)
    implementation(libs.google.auth)
    implementation(libs.agp)

    implementation(project(":plugin-foundation"))
}

gradlePlugin {
    website.set("https://github.com/appKODE/build-publish-plugin")
    vcsUrl.set("https://github.com/appKODE/build-publish-plugin")

    plugins {
        create("ru.kode.android.build-publish-novo.play") {
            id = "ru.kode.android.build-publish-novo.play"
            displayName = "Configure project with Google Play and changelogs"
            implementationClass = "ru.kode.android.build.publish.plugin.play.BuildPublishPlayPlugin"
            version = project.version
            description = "Android plugin to publish bundles to Google Play with changelogs"
            tags.set(listOf("google play", "publish", "changelog", "build"))
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "ru.kode.android.build-publish-novo.play".removePrefix("$groupId.")
            version = project.version.toString()

            from(components["java"])
        }
    }
}
