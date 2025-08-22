package ru.kode.android.build.publish.plugin.foundation

import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.git.mapper.toJson
import ru.kode.android.build.publish.plugin.core.git.mapper.fromJson
import ru.kode.android.build.publish.plugin.foundation.utils.BuildType
import ru.kode.android.build.publish.plugin.foundation.utils.ProductFlavor
import ru.kode.android.build.publish.plugin.foundation.utils.addAllAndCommit
import ru.kode.android.build.publish.plugin.foundation.utils.addNamed
import ru.kode.android.build.publish.plugin.foundation.utils.runTask
import ru.kode.android.build.publish.plugin.foundation.utils.find
import ru.kode.android.build.publish.plugin.foundation.utils.initGit
import ru.kode.android.build.publish.plugin.foundation.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.foundation.utils.getFile
import java.io.File
import java.io.IOException

class AssembleTwoFlavorsTest {
    @TempDir
    lateinit var tempDir: File
    private lateinit var projectDir: File

    @BeforeEach
    fun setup() {
        projectDir = File(tempDir, "test-project")
    }

    @Test
    @Throws(IOException::class)
    fun `assemble creates tag file for flavor and build type combination`() {
        // Given
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor("demo", "environment"))
        )

        val givenTagName = "v1.0.0-demoDebug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDemoDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-demoDebug.json")

        // When
        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)
        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        // Then
        val expectedCommitSha = git.log().last().id
        val expectedBuildNumber = "0"
        val expectedBuildVariant = "demoDebug"
        val expectedTagName = "v1.0.0-demoDebug"
        val expectedBuildVersion = "1.0"
        val expectedTagBuildFile =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = "",
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()

        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build failed"
        )
        assertEquals(
            givenTagBuildFile.readText(),
            expectedTagBuildFile.trimMargin(),
            "Wrong tag found"
        )
    }

    @Test
    @Throws(IOException::class)
    fun `assemble handles multiple flavors and build types correctly`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(
                ProductFlavor("demo", "environment"),
                ProductFlavor("pro", "environment")
            )
        )

        val givenTagName1 = "v1.0.0-demoDebug"
        val givenTagName2 = "v1.0.1-proRelease"
        val givenCommitMessage = "Initial commit"
        val git = projectDir.initGit()

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)
        git.tag.addNamed(givenTagName2)

        val demoDebugResult = projectDir.runTask("assembleDemoDebug")
        val demoDebugTagFile = projectDir.getFile("app/build/tag-build-demoDebug.json")

        val proReleaseResult = projectDir.runTask("assembleProRelease")
        val proReleaseTagFile = projectDir.getFile("app/build/tag-build-proRelease.json")

        assertTrue(demoDebugResult.output.contains("BUILD SUCCESSFUL"))
        assertTrue(demoDebugTagFile.exists())

        assertTrue(proReleaseResult.output.contains("BUILD SUCCESSFUL"))
        assertTrue(proReleaseTagFile.exists())

        val expectedProReleaseCommitSha = git.tag.find(givenTagName2).id
        val expectedProReleaseTagFile =
            Tag.Build(
                name = givenTagName2,
                commitSha = expectedProReleaseCommitSha,
                message = "",
                buildVersion = "1.0",
                buildVariant = "proRelease",
                buildNumber = 1
            ).toJson()

        assertEquals(
            proReleaseTagFile.readText(),
            expectedProReleaseTagFile.trimMargin(),
            "proRelease tag file content mismatch"
        )
    }

    @Test
    @Throws(IOException::class)
    fun `assemble handles flavor dimensions correctly`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(
                ProductFlavor("demo", "environment"),
                ProductFlavor("free", "tier")
            )
        )

        val givenTagName = "v1.0.0-demoFreeDebug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDemoFreeDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-demoFreeDebug.json")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)
        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        val expectedCommitSha = git.log().last().id
        val expectedBuildVariant = "demoFreeDebug"

        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build failed"
        )
        assertTrue(
            givenTagBuildFile.exists(),
            "Tag file not created for multi-flavor build"
        )

        val tagBuild = fromJson(givenTagBuildFile)
        assertEquals(expectedCommitSha, tagBuild.commitSha)
        assertEquals(givenTagName, tagBuild.name)
        assertEquals(expectedBuildVariant, tagBuild.buildVariant)
    }
}
