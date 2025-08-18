plugins {
    id("kotlin-convention")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation("ru.kode.android:plugin-core")

    implementation(gradleApi())
    implementation(libs.agp)
    implementation(libs.grgitCore)
    implementation(libs.grgitGradle)

    testImplementation(gradleTestKit())
    testImplementation(platform(libs.junitBom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    website.set("https://github.com/appKODE/build-publish-plugin")
    vcsUrl.set("https://github.com/appKODE/build-publish-plugin")

    plugins {
        create("ru.kode.android.build-publish-novo.foundation") {
            id = "ru.kode.android.build-publish-novo.foundation"
            displayName = "Configure project output using tag and generate changelog"
            implementationClass = "ru.kode.android.build.publish.plugin.foundation.BuildPublishFoundationPlugin"
            version = project.version
            description = "Android plugin to configure output and changelog generation"
            tags.set(listOf("output", "publish", "changelog", "build"))
        }
        create("ru.kode.android.build-publish-novo.foundation.service") {
            id = "ru.kode.android.build-publish-novo.foundation.service"
            displayName = "Configure project output using tag and generate changelog "
            implementationClass = "ru.kode.android.build.publish.plugin.foundation.service.GitExecutorServicePlugin"
            version = project.version
            description = "Android plugin to configure output and changelog generation"
            tags.set(listOf("output", "publish", "changelog", "build"))
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

tasks.register("setupPluginUploadFromEnvironment") {
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

tasks.test {
    useJUnitPlatform()
}
