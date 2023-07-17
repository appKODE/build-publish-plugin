import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    kotlin("jvm")
    id("kotlin-kapt")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
}

dependencies {
    implementation(kotlin("stdlib-jdk7"))
    implementation(gradleApi())
    implementation("com.android.tools.build:gradle:${BuildPluginsVersion.ANDROID_PLUGIN}")
    implementation("com.google.firebase:firebase-appdistribution-gradle:${BuildPluginsVersion.APP_DISTRIBUTION_PLUGIN}")
    implementation("com.squareup.okhttp3:okhttp:${BuildPluginsVersion.OK_HTTP}")
    implementation("com.squareup.okhttp3:logging-interceptor:${BuildPluginsVersion.OK_HTTP}")
    implementation("com.squareup.moshi:moshi:${BuildPluginsVersion.MOSHI}")
    implementation("com.squareup.retrofit2:retrofit:${BuildPluginsVersion.RETROFIT}")
    implementation("com.squareup.retrofit2:converter-moshi:${BuildPluginsVersion.RETROFIT}")
    implementation("org.ajoberstar.grgit:grgit-core:${BuildPluginsVersion.GRGIT_PLUGIN}")
    implementation("org.ajoberstar.grgit:grgit-gradle:${BuildPluginsVersion.GRGIT_PLUGIN}")

    testImplementation(kotlin("test-junit5"))
    testImplementation(TestingLib.JUNIT)
    testImplementation(gradleTestKit())

    kapt("com.squareup.moshi:moshi-kotlin-codegen:${BuildPluginsVersion.MOSHI}")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

gradlePlugin {
    plugins {
        create(PluginCoordinates.ID) {
            id = PluginCoordinates.ID
            implementationClass = PluginCoordinates.IMPLEMENTATION_CLASS
            version = PluginCoordinates.VERSION
        }
    }
}

// Configuration Block for the Plugin Marker artifact on Plugin Central
pluginBundle {
    website = PluginBundle.WEBSITE
    vcsUrl = PluginBundle.VCS
    description = PluginBundle.DESCRIPTION
    tags = PluginBundle.TAGS

    plugins {
        getByName(PluginCoordinates.ID) {
            displayName = PluginBundle.DISPLAY_NAME
        }
    }

    mavenCoordinates {
        groupId = PluginCoordinates.GROUP
        artifactId = PluginCoordinates.ID.removePrefix("$groupId.")
        version = PluginCoordinates.VERSION
    }
}

project.tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.ExperimentalStdlibApi"
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

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging.showStandardStreams = true
    testLogging.events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
}
