plugins {
    id("kotlin-convention")
    id("maven-publish")
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.vanniktech.maven.publish)
}

group = "ru.kode.android"
version = libs.versions.buildPublishShared.get()

dependencies {
    implementation(gradleApi())
    implementation(libs.grgitCore)
    implementation(libs.okhttp)
    implementation(libs.okhttpLogging)
    implementation(libs.retrofit)
    implementation(libs.serializationJson)

    compileOnly(libs.agp)

    testImplementation(platform(libs.junitBom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(gradleTestKit())
}

tasks.test {
    useJUnitPlatform()
    // Gradle's ProjectBuilder needs deep reflection into the JDK on Java 16+.
    jvmArgs(
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
    )
}

mavenPublishing {
    coordinates(artifactId = "build-publish-novo-core")

    publishToMavenCentral()
    signAllPublications()

    pom {

        name.set("Build Publish Novo Core")
        description.set("Core library to use inside Build Publish plugins ")
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
                id.set("dmitrii.suzdalev.dz")
                name.set("DIma")
                email.set("dz@kode.ru")
            }
        }

        scm {
            url.set("https://github.com/appKODE/build-publish-plugin")
            connection.set("scm:git:https://github.com/appKODE/build-publish-plugin.git")
            developerConnection.set("scm:git:ssh://git@github.com:appKODE/build-publish-plugin.git")
        }
    }
}
