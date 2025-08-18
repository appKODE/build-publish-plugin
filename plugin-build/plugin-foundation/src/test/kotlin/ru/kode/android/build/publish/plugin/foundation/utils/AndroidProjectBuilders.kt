package ru.kode.android.build.publish.plugin.foundation.utils

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

internal fun File.createAndroidProject(
    topBuildFileContent: String? = null,
) {
    val topSettingsFile = this.getFile("settings.gradle")
    val topBuildFile = this.getFile("build.gradle")
    val appBuildFile = this.getFile("app/build.gradle")

    if (topBuildFileContent != null) {
        topBuildFile.writeText(topBuildFileContent)
    }
    val topSettingsFileContent =
        """
        pluginManagement {
            repositories {
                google()
                mavenCentral()
                gradlePluginPortal()
            }
        }
        dependencyResolutionManagement {
            repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
            repositories {
                google()
                mavenCentral()
            }
        }
        rootProject.name = "My Application"
        include ':app'
        """.trimIndent()
    writeFile(topSettingsFile, topSettingsFileContent)
    val appBuildFileContent =
        """
        plugins {
            id 'com.android.application'
            id 'ru.kode.android.build-publish-novo.foundation'
        }
        
        android {
            namespace = "ru.kode.test"

            compileSdk 34
        
            defaultConfig {
                applicationId "com.example.build.types.android"
                minSdk 31
                targetSdk 34
                versionCode 1
                versionName "1.0"
            }
        
            buildTypes {
                debug
                release
            }
        }
        
        buildPublishFoundation {
            outputCommon {
                baseFileName.set("test-app")
            }
            changelogCommon {
                issueNumberPattern.set("TICKET-\\d+")
                issueUrlPrefix.set("https://jira.example.com/browse/")
                commitMessageKey.set("[CHANGELOG]")
            }
        }
        """.trimIndent()
    writeFile(appBuildFile, appBuildFileContent)
}

@Throws(IOException::class)
private fun writeFile(
    destination: File,
    content: String,
) {
    var output: BufferedWriter? = null
    try {
        output = BufferedWriter(FileWriter(destination))
        output.write(content)
    } finally {
        output?.close()
    }
}

internal fun File.getFile(path: String): File {
    val file = File(this, path)
    file.parentFile.mkdirs()
    return file
}

internal fun File.runTask(task: String): BuildResult {
    return GradleRunner.create()
        .withProjectDir(this)
        .withArguments(task)
        .withPluginClasspath()
        .forwardOutput()
        .build()
}
