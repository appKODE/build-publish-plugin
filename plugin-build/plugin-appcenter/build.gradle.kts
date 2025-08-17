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

    testImplementation(libs.junit)

    ksp(libs.moshiCodgen)
}

gradlePlugin {
    website.set("https://github.com/appKODE/build-publish-plugin")
    vcsUrl.set("https://github.com/appKODE/build-publish-plugin")

    plugins {
        create("ru.kode.android.build-publish-novo.appcenter") {
            id = "ru.kode.android.build-publish-novo.appcenter"
            displayName = "Configure project with AppCenter and changelogs"
            implementationClass = "ru.kode.android.build.publish.plugin.appcenter.BuildPublishAppCenterPlugin"
            version = project.version
            description = "Android plugin to publish bundles and apks to AppCenter with changelogs"
            tags.set(listOf("appcenter", "publish", "changelog", "build"))
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "ru.kode.android.build-publish-novo.appcenter".removePrefix("$groupId.")
            version = project.version.toString()

            from(components["java"])
        }
    }
}
