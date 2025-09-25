package ru.kode.android.build.publish.plugin.foundation

import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.git.mapper.toJson
import ru.kode.android.build.publish.plugin.foundation.utils.BuildType
import ru.kode.android.build.publish.plugin.foundation.utils.FoundationConfig
import ru.kode.android.build.publish.plugin.foundation.utils.ManifestProperties
import ru.kode.android.build.publish.plugin.foundation.utils.ProductFlavor
import ru.kode.android.build.publish.plugin.foundation.utils.addAllAndCommit
import ru.kode.android.build.publish.plugin.foundation.utils.addNamed
import ru.kode.android.build.publish.plugin.foundation.utils.runTask
import ru.kode.android.build.publish.plugin.foundation.utils.find
import ru.kode.android.build.publish.plugin.foundation.utils.initGit
import ru.kode.android.build.publish.plugin.foundation.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.foundation.utils.currentDate
import ru.kode.android.build.publish.plugin.foundation.utils.extractManifestProperties
import ru.kode.android.build.publish.plugin.foundation.utils.getFile
import ru.kode.android.build.publish.plugin.foundation.utils.printFilesRecursively
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
    fun `assemble handles multiple flavors and build types correctly`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(
                ProductFlavor("demo", "environment"),
                ProductFlavor("pro", "environment")
            ),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                )
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
        val givenDemoDebugTagFile = projectDir.getFile("app/build/tag-build-demoDebug.json")
        val givenDebugOutputFile = projectDir.getFile("app/build/outputs/apk/demo/debug/autotest-demoDebug-vc0-$currentDate.apk")
        val givenDebugOutputFileManifestProperties = givenDebugOutputFile.extractManifestProperties()

        val proReleaseResult = projectDir.runTask("assembleProRelease")
        val givenProReleaseTagFile = projectDir.getFile("app/build/tag-build-proRelease.json")
        val givenReleaseOutputFile = projectDir.getFile("app/build/outputs/apk/pro/release/autotest-proRelease-vc1-$currentDate.apk")
        val givenReleaseOutputFileManifestProperties = givenReleaseOutputFile.extractManifestProperties()

        projectDir.getFile("app").printFilesRecursively()

        val expectedDemoDebugCommitSha = git.tag.find(givenTagName1).id
        val expectedDemoDebugTagFile =
            Tag.Build(
                name = givenTagName1,
                commitSha = expectedDemoDebugCommitSha,
                message = "",
                buildVersion = "1.0",
                buildVariant = "demoDebug",
                buildNumber = 0
            ).toJson()
        val expectedProDebugManifestProperties = ManifestProperties(
            versionCode = "",
            versionName = "v1.0.0-demoDebug",
        )
        assertTrue(
            demoDebugResult.output.contains("Task :app:getLastTagDemoDebug"),
            "Task getLastTagDemoDebug executed"
        )
        assertTrue(
            !demoDebugResult.output.contains("Task :app:getLastTagProRelease"),
            "Task getLastTagProRelease not executed"
        )
        assertTrue(
            demoDebugResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded"
        )
        assertEquals(
            expectedDemoDebugTagFile.trimMargin(),
            givenDemoDebugTagFile.readText(),
            "Tags equality"
        )
        assertTrue(givenDebugOutputFile.exists(), "Output file exists")
        assertTrue(givenDebugOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedProDebugManifestProperties,
            givenDebugOutputFileManifestProperties,
            "Manifest properties equality"
        )

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
        val expectedProReleaseManifestProperties = ManifestProperties(
            versionCode = "1",
            versionName = "v1.0.1-proRelease",
        )
        assertTrue(
            !proReleaseResult.output.contains("Task :app:getLastTagDemoDebug"),
            "Task getLastTagDemoDebug not executed"
        )
        assertTrue(
            proReleaseResult.output.contains("Task :app:getLastTagProRelease"),
            "Task getLastTagProRelease executed"
        )
        assertTrue(
            proReleaseResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded"
        )
        assertEquals(
            expectedProReleaseTagFile.trimMargin(),
            givenProReleaseTagFile.readText(),
            "Tags equality"
        )
        assertTrue(givenReleaseOutputFile.exists(), "Output file exists")
        assertTrue(givenReleaseOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedProReleaseManifestProperties,
            givenReleaseOutputFileManifestProperties,
            "Manifest properties equality"
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
            ),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                )
            )
        )

        val givenTagName = "v1.0.0-demoFreeDebug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDemoFreeDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-demoFreeDebug.json")
        val givenDebugOutputFile = projectDir.getFile("app/build/outputs/apk/demoFree/debug/autotest-demoFreeDebug-vc0-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)
        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenDebugOutputFile.extractManifestProperties()

        val expectedCommitSha = git.log().last().id
        val expectedBuildNumber = "0"
        val expectedBuildVariant = "demoFreeDebug"
        val expectedTagName = "v1.0.0-demoFreeDebug"
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
        val expectedManifestProperties = ManifestProperties(
            versionCode = "",
            versionName = "v1.0.0-demoFreeDebug",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDemoFreeDebug"),
            "Task getLastTagDemoFreeDebug executed"
        )
        assertTrue(
            !result.output.contains("Task :app:getLastTagDemoFreeRelease"),
            "Task getLastTagDemoFreeRelease not executed"
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded"
        )
        assertTrue(givenTagBuildFile.exists(), "Tag file exists")
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality"
        )
        assertTrue(givenDebugOutputFile.exists(), "Output file exists")
        assertTrue(givenDebugOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality"
        )
    }
}
