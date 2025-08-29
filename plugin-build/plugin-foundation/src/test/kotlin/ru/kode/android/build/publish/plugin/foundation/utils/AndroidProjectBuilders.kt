package ru.kode.android.build.publish.plugin.foundation.utils

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

internal fun File.createAndroidProject(
    outputConfig: OutputConfig = OutputConfig(),
    changelogConfig: ChangelogConfig = ChangelogConfig(),
    buildTypes: List<BuildType>,
    productFlavors: List<ProductFlavor> = listOf(),
    topBuildFileContent: String? = null,
) {
    val topSettingsFile = this.getFile("settings.gradle")
    val topBuildFile = this.getFile("build.gradle")
    val appBuildFile = this.getFile("app/build.gradle")
    val androidManifestFile = this.getFile("app/src/main/AndroidManifest.xml")

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
    val buildTypesBlock = buildTypes
        .joinToString(separator = "\n") {
            """
                ${it.name} 
            """
        }
        .let {
            """
            buildTypes {
            $it
            }
            """
        }
    val flavorDimensionsBlock = productFlavors
        .mapTo(mutableSetOf()) { it.dimension }
        .joinToString { "\"$it\"" }
        .takeIf { it.isNotEmpty() }
        ?.let {
            "flavorDimensions += [$it]"
        }
        .orEmpty()
    val productFlavorsBlock = productFlavors
        .joinToString(separator = "\n") {
            """
                create("${it.name}") {
                    dimension = "${it.dimension}"
                }
            """
        }.let {
            """
            productFlavors {
            $it
            }
            """
        }
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
            
            $buildTypesBlock
            
            $flavorDimensionsBlock
            
            $productFlavorsBlock
        }
        
        buildPublishFoundation {
            outputCommon {
                baseFileName.set("${outputConfig.baseFileName}")
                ${outputConfig.useVersionsFromTag?.let { "useVersionsFromTag.set($it)" }.orEmpty()}
                ${outputConfig.useStubsForTagAsFallback?.let { "useStubsForTagAsFallback.set($it)" }.orEmpty()}
                ${outputConfig.useDefaultsForVersionsAsFallback?.let { "useDefaultsForVersionsAsFallback.set($it)" }.orEmpty()}
                ${outputConfig.buildTagPattern?.let { "buildTagPattern.set(\"$it\")" }.orEmpty()}
            }
            changelogCommon {
                issueNumberPattern.set("${changelogConfig.issueNumberPattern}")
                issueUrlPrefix.set("${changelogConfig.issueUrlPrefix}")
                commitMessageKey.set("${changelogConfig.commitMessageKey}")
            }
        }
        """.trimIndent()
            .also {
                println(it)
            }
    writeFile(appBuildFile, appBuildFileContent)
    val androidManifestFileContent = """
        <?xml version="1.0" encoding="utf-8"?>
        <manifest />
    """.trimIndent()
    writeFile(androidManifestFile, androidManifestFileContent)
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


internal data class BuildType(
    val name: String
)

internal data class ProductFlavor(
    val name: String,
    val dimension: String
)

internal data class OutputConfig(
    val baseFileName: String = "test-app",
    val useVersionsFromTag: Boolean? = null,
    val useStubsForTagAsFallback: Boolean? = null,
    val useDefaultsForVersionsAsFallback: Boolean? = null,
    val buildTagPattern: String? = null
)

internal data class ChangelogConfig(
    val issueNumberPattern: String = "TICKET-\\\\d+",
    val issueUrlPrefix: String = "https://jira.example.com/browse/",
    val commitMessageKey: String = "[CHANGELOG]",
)
