plugins {
    id("kotlin-convention")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
}

dependencies {
    implementation(gradleApi())
    implementation(libs.grgitCore)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "ru.kode.android.build-publish.core".removePrefix("$groupId.")
            version = project.version.toString()

            from(components["java"])
        }
    }
}
