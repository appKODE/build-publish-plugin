plugins {
    id("kotlin-convention")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
}

group = "ru.kode.android"
version = "1.0.0"

dependencies {
    implementation(gradleApi())
    implementation("ru.kode.android:plugin-core")
    implementation(libs.agp)
}

gradlePlugin {
    website.set("https://github.com/appKODE/build-publish-plugin")
    vcsUrl.set("https://github.com/appKODE/build-publish-plugin")

    plugins {
        create("ru.kode.android.build-publish-example.print-tag") {
            id = "ru.kode.android.build-publish-example.print-tag"
            displayName = "Print info about tag"
            implementationClass = "com.example.customplugin.CustomBuildPublishPlugin"
            version = project.version
            description = "Test plugin for publish"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "ru.kode.android.build-publish-example.print-tag".removePrefix("$groupId.")
            version = project.version.toString()

            from(components["java"])
        }
    }
}
