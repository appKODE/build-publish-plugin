package ru.kode.android.build.publish.plugin.foundation

import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.enity.BuildTagSnapshot
import ru.kode.android.build.publish.plugin.core.git.mapper.toJson
import ru.kode.android.build.publish.plugin.test.utils.BuildType
import ru.kode.android.build.publish.plugin.test.utils.FoundationConfig
import ru.kode.android.build.publish.plugin.test.utils.ManifestProperties
import ru.kode.android.build.publish.plugin.test.utils.ProductFlavor
import ru.kode.android.build.publish.plugin.test.utils.addAllAndCommit
import ru.kode.android.build.publish.plugin.test.utils.addNamed
import ru.kode.android.build.publish.plugin.test.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.extractManifestProperties
import ru.kode.android.build.publish.plugin.test.utils.findTag
import ru.kode.android.build.publish.plugin.test.utils.getFile
import ru.kode.android.build.publish.plugin.test.utils.initGit
import ru.kode.android.build.publish.plugin.test.utils.printFilesRecursively
import ru.kode.android.build.publish.plugin.test.utils.runTask
import ru.kode.android.build.publish.plugin.test.utils.runTaskWithFail
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
            productFlavors =
                listOf(
                    ProductFlavor("demo", "environment"),
                    ProductFlavor("pro", "environment"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )

        val givenTagName1 = "v1.0.1-demoDebug"
        val givenTagName2 = "v1.0.2-proRelease"
        val givenCommitMessage = "Initial commit"
        val git = projectDir.initGit()

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)
        git.tag.addNamed(givenTagName2)

        val demoDebugResult = projectDir.runTask("assembleDemoDebug")
        val givenDemoDebugTagFile = projectDir.getFile("app/build/tag-build-snapshot-demoDebug.json")

        val debugApkDir = projectDir.getFile("app/build/outputs/apk/demo/debug")
        val givenDebugOutputFile = debugApkDir.listFiles()
            ?.first { it.name.matches(Regex("autotest-demoDebug-vc1-\\d{8}\\.apk")) }
            ?: throw AssertionError("Output file not found")

        val givenDebugOutputFileManifestProperties = givenDebugOutputFile.extractManifestProperties()

        val proReleaseResult = projectDir.runTask("assembleProRelease")
        val givenProReleaseTagFile = projectDir.getFile("app/build/tag-build-snapshot-proRelease.json")

        val releaseApkDir = projectDir.getFile("app/build/outputs/apk/pro/release")
        val givenReleaseOutputFile = releaseApkDir.listFiles()
            ?.first { it.name.matches(Regex("autotest-proRelease-vc2-\\d{8}\\.apk")) }
            ?: throw AssertionError("Output file not found")

        val givenReleaseOutputFileManifestProperties = givenReleaseOutputFile.extractManifestProperties()

        projectDir.getFile("app").printFilesRecursively()

        val expectedDemoDebugTagFile =
            BuildTagSnapshot(
                current = Tag.Build(
                    name = givenTagName1,
                    commitSha = git.tag.findTag(givenTagName1).id,
                    message = "",
                    buildVersion = "1.0",
                    buildVariant = "demoDebug",
                    buildNumber = 1,
                ),
                previousInOrder = null,
                previousOnDifferentCommit = null
            ).toJson()
        val expectedProDebugManifestProperties =
            ManifestProperties(
                versionCode = "1",
                versionName = "1.0",
            )
        assertTrue(
            demoDebugResult.output.contains("Task :app:getLastTagSnapshotDemoDebug"),
            "Task getLastTagSnapshotDemoDebug executed",
        )
        assertTrue(
            !demoDebugResult.output.contains("Task :app:getLastTagSnapshotProRelease"),
            "Task getLastTagSnapshotProRelease not executed",
        )
        assertTrue(
            demoDebugResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded",
        )
        assertEquals(
            expectedDemoDebugTagFile.trimMargin(),
            givenDemoDebugTagFile.readText(),
            "Tags debug equality",
        )
        assertTrue(givenDebugOutputFile.exists(), "Output file exists")
        assertTrue(givenDebugOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedProDebugManifestProperties,
            givenDebugOutputFileManifestProperties,
            "Manifest properties equality",
        )

        val expectedProReleaseCommitSha = git.tag.findTag(givenTagName2).id
        val expectedProReleaseTagFile =
            BuildTagSnapshot(
                current = Tag.Build(
                    name = givenTagName2,
                    commitSha = expectedProReleaseCommitSha,
                    message = "",
                    buildVersion = "1.0",
                    buildVariant = "proRelease",
                    buildNumber = 2,
                ),
                previousInOrder = null,
                previousOnDifferentCommit = null
            ).toJson()
        val expectedProReleaseManifestProperties =
            ManifestProperties(
                versionCode = "2",
                versionName = "1.0",
            )
        assertTrue(
            !proReleaseResult.output.contains("Task :app:getLastTagSnapshotDemoDebug"),
            "Task getLastTagSnapshotDemoDebug not executed",
        )
        assertTrue(
            proReleaseResult.output.contains("Task :app:getLastTagSnapshotProRelease"),
            "Task getLastTagSnapshotProRelease executed",
        )
        assertTrue(
            proReleaseResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded",
        )
        assertEquals(
            expectedProReleaseTagFile.trimMargin(),
            givenProReleaseTagFile.readText(),
            "Tags release equality",
        )
        assertTrue(givenReleaseOutputFile.exists(), "Output file exists")
        assertTrue(givenReleaseOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedProReleaseManifestProperties,
            givenReleaseOutputFileManifestProperties,
            "Manifest properties equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `assemble handles flavor dimensions correctly`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors =
                listOf(
                    ProductFlavor("demo", "environment"),
                    ProductFlavor("free", "tier"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )

        val givenTagName = "v1.0.1-demoFreeDebug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDemoFreeDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-snapshot-demoFreeDebug.json")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)
        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/demoFree/debug")
        val givenOutputFile = apkDir.listFiles()
            ?.first { it.name.matches(Regex("autotest-demoFreeDebug-vc1-\\d{8}\\.apk")) }
            ?: throw AssertionError("Output file not found")

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.log().last().id
        val expectedBuildNumber = "1"
        val expectedBuildVariant = "demoFreeDebug"
        val expectedTagName = "v1.0.1-demoFreeDebug"
        val expectedBuildVersion = "1.0"

        val expectedTagBuildFile =
            BuildTagSnapshot(
                current = Tag.Build(
                    name = expectedTagName,
                    commitSha = expectedCommitSha,
                    message = "",
                    buildVersion = expectedBuildVersion,
                    buildVariant = expectedBuildVariant,
                    buildNumber = expectedBuildNumber.toInt(),
                ),
                previousInOrder = null,
                previousOnDifferentCommit = null
            ).toJson()
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "1",
                versionName = "1.0",
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagSnapshotDemoFreeDebug"),
            "Task getLastTagSnapshotDemoFreeDebug executed",
        )
        assertTrue(
            !result.output.contains("Task :app:getLastTagSnapshotDemoFreeRelease"),
            "Task getLastTagSnapshotDemoFreeRelease not executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded",
        )
        assertTrue(givenTagBuildFile.exists(), "Tag file exists")
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `assemble not handles flavor dimensions correctly if tag ends with 0`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors =
                listOf(
                    ProductFlavor("demo", "environment"),
                    ProductFlavor("free", "tier"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )

        val givenTagName = "v1.0.0-demoFreeDebug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDemoFreeDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-snapshot-demoFreeDebug.json")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)
        val result: BuildResult = projectDir.runTaskWithFail(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/demoFree/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-demoFreeDebug-vc0-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            result.output.contains("Task :app:getLastTagSnapshotDemoFreeDebug"),
            "Task getLastTagSnapshotDemoFreeDebug executed",
        )
        assertTrue(
            !result.output.contains("Task :app:getLastTagSnapshotDemoFreeRelease"),
            "Task getLastTagSnapshotDemoFreeRelease not executed",
        )
        assertTrue(
            result.output.contains("BUILD FAILED"),
            "Build failed",
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
        assertTrue(!givenOutputFileExists, "Output file not exists")
    }
}
