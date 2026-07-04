plugins {
    id("kotlin-convention")
    id("maven-publish")
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.vanniktech.maven.publish)
}

group = "ru.kode.android"
version = libs.versions.buildPublishShared.get()

dependencies {
    implementation(project(":plugin-core"))
    implementation(libs.okhttp)
    implementation(libs.okhttpLogging)
    implementation(libs.retrofit)
    implementation(libs.retrofitSerialization)
    implementation(libs.serializationJson)

    testImplementation(platform(libs.junitBom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.okhttpMockWebServer)
}

tasks.test {
    useJUnitPlatform()
}

mavenPublishing {
    coordinates(artifactId = "build-publish-novo-client-nextcloud")
    publishToMavenCentral()
    signAllPublications()
    pom {
        name.set("Build Publish Novo Client Nextcloud")
        description.set("Nextcloud HTTP client library for Build Publish plugins")
        inceptionYear.set("2025")
        url.set("https://github.com/appKODE/build-publish-plugin")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("rinekri")
                name.set("rinekri")
            }
        }
        scm {
            url.set("https://github.com/appKODE/build-publish-plugin")
            connection.set("scm:git:https://github.com/appKODE/build-publish-plugin.git")
            developerConnection.set("scm:git:ssh://git@github.com:appKODE/build-publish-plugin.git")
        }
    }
}
