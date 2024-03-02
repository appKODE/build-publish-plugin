plugins {
    id("kotlin-convention")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation(kotlin("stdlib-jdk7"))
    implementation(gradleApi())
    implementation(libs.agp)
    implementation(libs.firebaseAppdistribution)
    implementation(libs.okhttp)
    implementation(libs.okhttpLogging)
    implementation(libs.moshi)
    implementation(libs.retrofit)
    implementation(libs.retrofitMoshi)
    implementation(libs.grgitCore)
    implementation(libs.grgitGradle)

    testImplementation(libs.junit)

    ksp(libs.moshiCodgen)
}

gradlePlugin {
    plugins {
        create("ru.kode.android.build-publish") {
            id = "ru.kode.android.build-publish"
            implementationClass = "ru.kode.android.build.publish.plugin.BuildPublishPlugin"
            version = project.version
        }
    }
}

// Configuration Block for the Plugin Marker artifact on Plugin Central
pluginBundle {
    website = "https://github.com/appKODE/build-publish-plugin"
    vcsUrl = "https://github.com/appKODE/build-publish-plugin"
    description = "Android plugin to publish bundles and apks to Firebase App Distribution with changelogs"
    tags = listOf("firebase", "publish", "changelog", "build")

    plugins {
        getByName("ru.kode.android.build-publish") {
            displayName = "Configure project with Firebase App Distribution and changelogs"
        }
    }

    mavenCoordinates {
        groupId = project.group.toString()
        artifactId = "ru.kode.android.build-publish".removePrefix("$groupId.")
        version = project.version.toString()
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
