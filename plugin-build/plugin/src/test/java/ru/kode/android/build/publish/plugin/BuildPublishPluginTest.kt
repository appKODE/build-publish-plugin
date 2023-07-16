package ru.kode.android.build.publish.plugin

import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.service.TagService
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

class GetLastTagFunctionalTest {
    @TempDir
    lateinit var tempDir: File
    private lateinit var projectDir: File

    private lateinit var topSettingsFile: File
    private lateinit var topBuildFile: File
    private lateinit var appBuildFile: File

    @BeforeEach
    fun setup() {
        projectDir = File(tempDir, "test-project")
        topSettingsFile = projectFile("settings.gradle")
        topBuildFile = projectFile("build.gradle")
        appBuildFile = projectFile("app/build.gradle")
    }

    @Test
    @Throws(IOException::class)
    fun `creates tag file of debug build from one tag`() {
        prepareAndroidProject(
            topSettingsFile = topSettingsFile,
            topBuildFile = topBuildFile,
            appBuildFile = appBuildFile
        )
        val givenTagName = "v1.0.0-debug"
        val givenCommitMessage = "Initial commit"
        val givenGetLastTagTask = "getLastTagDebug"
        val git = Grgit.init(mapOf("dir" to projectDir))
        val givenTagBuildFile = projectFile("app/build/tag-build-debug.json")

        git.addAll()
        git.commit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(givenGetLastTagTask)
            .forwardOutput()
            .build()

        val expectedCommitSha = git.log().last().id
        val expectedBuildNumber = "0"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.0-debug"
        val expectedBuildVersion = "1.0"
        val expectedResult = """
            {
            "buildNumber":${expectedBuildNumber},
            "commitSha":"$expectedCommitSha",
            "message":"",
            "buildVariant":"$expectedBuildVariant",
            "buildVersion":"$expectedBuildVersion",
            "name":"$expectedTagName"}
        """.trimIndent().replace("\n", "")
        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
        assertEquals(givenTagBuildFile.readText(), expectedResult.trimMargin())
    }

    @Test
    @Throws(IOException::class)
    fun `creates tag file of release build from two tags with same VC`() {
        prepareAndroidProject(
            topSettingsFile = topSettingsFile,
            topBuildFile = topBuildFile,
            appBuildFile = appBuildFile
        )
        val givenTagNameDebug = "v1.0.0-debug"
        val givenTagNameRelease = "v1.0.0-release"
        val givenCommitMessage = "Initial commit"
        val givenGetLastTagTaskRelease = "getLastTagRelease"
        val givenGetLastTagTaskDebug = "getLastTagDebug"
        val git = Grgit.init(mapOf("dir" to projectDir))
        val givenTagBuildFile = projectFile("app/build/tag-build-release.json")

        git.addAll()
        git.commit(givenCommitMessage)
        git.tag.addNamed(givenTagNameDebug)
        git.tag.addNamed(givenTagNameRelease)

        val releaseResult: BuildResult = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(givenGetLastTagTaskRelease)
            .forwardOutput()
            .build()

        GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(givenGetLastTagTaskDebug)
            .forwardOutput()
            .build()

        val expectedCommitSha = git.log().last().id
        val expectedBuildNumber = "0"
        val expectedBuildVariant = "release"
        val expectedTagName = "v1.0.0-release"
        val expectedBuildVersion = "1.0"
        val expectedResult = """
            {
            "buildNumber":${expectedBuildNumber},
            "commitSha":"$expectedCommitSha",
            "message":"",
            "buildVariant":"$expectedBuildVariant",
            "buildVersion":"$expectedBuildVersion",
            "name":"$expectedTagName"}
        """.trimIndent().replace("\n", "")
        assertTrue(releaseResult.output.contains("BUILD SUCCESSFUL"))
        assertEquals(givenTagBuildFile.readText(), expectedResult.trimMargin())
    }

    @Test
    @Throws(IOException::class)
    fun `creates tag file of release build from two tags with different VC`() {
        prepareAndroidProject(
            topSettingsFile = topSettingsFile,
            topBuildFile = topBuildFile,
            appBuildFile = appBuildFile
        )
        val givenTagNameDebug = "v1.0.1-debug"
        val givenTagNameRelease = "v1.0.0-release"
        val givenCommitMessage = "Initial commit"
        val givenGetLastTagTaskRelease = "getLastTagRelease"
        val givenGetLastTagTaskDebug = "getLastTagDebug"
        val git = Grgit.init(mapOf("dir" to projectDir))
        val givenTagBuildFile = projectFile("app/build/tag-build-release.json")

        git.addAll()
        git.commit(givenCommitMessage)
        git.tag.addNamed(givenTagNameDebug)
        git.tag.addNamed(givenTagNameRelease)

        val releaseResult: BuildResult = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(givenGetLastTagTaskRelease)
            .forwardOutput()
            .build()

        GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(givenGetLastTagTaskDebug)
            .forwardOutput()
            .build()

        val expectedCommitSha = git.log().last().id
        val expectedBuildNumber = "0"
        val expectedBuildVariant = "release"
        val expectedTagName = "v1.0.0-release"
        val expectedBuildVersion = "1.0"
        val expectedResult = """
            {
            "buildNumber":${expectedBuildNumber},
            "commitSha":"$expectedCommitSha",
            "message":"",
            "buildVariant":"$expectedBuildVariant",
            "buildVersion":"$expectedBuildVersion",
            "name":"$expectedTagName"}
        """.trimIndent().replace("\n", "")
        assertTrue(releaseResult.output.contains("BUILD SUCCESSFUL"))
        assertEquals(givenTagBuildFile.readText(), expectedResult.trimMargin())
    }

    private fun projectFile(path: String): File {
        val file = File(projectDir, path)
        file.parentFile.mkdirs()
        return file
    }

    private fun prepareAndroidProject(
        topSettingsFile: File,
        topBuildFile: File,
        appBuildFile: File
    ) {
        val topSettingsFileContent = """
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
        val topBuildFileContent = """
                plugins {
                    id 'com.android.application' version '7.4.1' apply false
                    id 'com.android.library' version '7.4.1' apply false
                    id 'org.jetbrains.kotlin.android' version '1.8.10' apply false
                    id 'com.google.firebase.appdistribution' version '3.0.0' apply false
                    id 'ru.kode.android.build-publish' version '1.1.0-alpha18' apply false
                    id 'org.ajoberstar.grgit.service' version '5.0.0' apply false
                }
            """.trimIndent()
        writeFile(topBuildFile, topBuildFileContent)
        val appBuildFileContent = """
                plugins {
                    id 'com.android.application'
                    id 'com.google.firebase.appdistribution'
                    id 'ru.kode.android.build-publish'
                }
                
                android {
                    compileSdk 31
                
                    defaultConfig {
                        applicationId "com.example.build.types.android"
                        minSdk 31
                        targetSdk 31
                        versionCode 1
                        versionName "1.0"
                    }
                
                    buildTypes {
                        debug
                        release
                    }
                }
                
                buildPublish {
                    output {
                        register("default") {
                            baseFileName = "base-project-android"
                        }
                    }
                    changelog {
                        register("default") {
                            issueNumberPattern = "BASE-\\d+"
                            issueUrlPrefix = "https://jira.exmaple.ru/browse/"
                            commitMessageKey = "CHANGELOG"
                        }
                    }
                }
            """.trimIndent()
        writeFile(appBuildFile, appBuildFileContent)
    }

    @Throws(IOException::class)
    private fun writeFile(destination: File, content: String) {
        var output: BufferedWriter? = null
        try {
            output = BufferedWriter(FileWriter(destination))
            output.write(content)
        } finally {
            output?.close()
        }
    }
}

private fun Grgit.addAll() {
    this.add(mapOf("patterns" to setOf(".")))
}

private fun Grgit.commit(message: String) {
    this.commit(mapOf("message" to message))
}

private fun TagService.addNamed(name: String) {
    this.add(mapOf("name" to name))
}
