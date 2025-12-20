plugins {
    id("kotlin-convention")
    id("maven-publish")
}

group = "ru.kode.android"

dependencies {
    implementation(gradleApi())
    implementation(libs.agp)
    implementation(libs.grgitCore)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
}

publishing {
    repositories {
        mavenLocal()

        if (System.getenv("MAVEN_URL") != null) {
            maven {
                url = uri(System.getenv("MAVEN_URL"))
                credentials {
                    username = System.getenv("MAVEN_USERNAME")
                    password = System.getenv("MAVEN_PASSWORD")
                }
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "build-publish-novo-core"
            version = project.version.toString()

            from(components["java"])
        }
    }
}
