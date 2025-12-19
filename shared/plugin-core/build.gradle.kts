plugins {
    id("kotlin-convention")
    id("maven-publish")
}

group = "ru.kode.android"
version = "1.0.0"

dependencies {
    implementation(gradleApi())
    implementation(libs.agp)
    implementation(libs.grgitCore)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "ru.kode.android.build-publish-novo-core".removePrefix("$groupId.")
            version = project.version.toString()

            from(components["java"])
        }
    }
}
