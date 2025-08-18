plugins {
    id("kotlin-convention")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation("ru.kode.android:plugin-core")

    implementation(gradleApi())
    implementation(libs.okhttp)
    implementation(libs.okhttpLogging)
    implementation(libs.moshi)
    implementation(libs.retrofit)
    implementation(libs.retrofitMoshi)
    implementation(libs.agp)

    testImplementation(gradleTestKit())
    testImplementation(platform(libs.junitBom))
    testImplementation("org.junit.jupiter:junit-jupiter")

    ksp(libs.moshiCodgen)
}

gradlePlugin {
    website.set("https://github.com/appKODE/build-publish-plugin")
    vcsUrl.set("https://github.com/appKODE/build-publish-plugin")

    plugins {
        create("ru.kode.android.build-publish-novo.clickup") {
            id = "ru.kode.android.build-publish-novo.clickup"
            displayName = "Integrate ClickUp into Android project"
            implementationClass = "ru.kode.android.build.publish.plugin.clickup.BuildPublishClickUpPlugin"
            version = project.version
            description = "Android plugin to automate ClickUp operations"
            tags.set(listOf("clickup", "publish", "changelog", "build"))
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "ru.kode.android.build-publish-novo.clickup".removePrefix("$groupId.")
            version = project.version.toString()

            from(components["java"])
        }
    }
}
