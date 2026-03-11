package ru.kode.android.build.publish.plugin.foundation

import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.enity.BuildTagSnapshot
import ru.kode.android.build.publish.plugin.core.git.mapper.toJson
import ru.kode.android.build.publish.plugin.test.utils.BuildType
import ru.kode.android.build.publish.plugin.test.utils.FoundationConfig
import ru.kode.android.build.publish.plugin.test.utils.ManifestProperties
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

class AssembleBuildTypesTest {
    @TempDir
    lateinit var tempDir: File
    private lateinit var projectDir: File

    @BeforeEach
    fun setup() {
        projectDir = File(tempDir, "test-project")
    }

    @Test
    @Throws(IOException::class)
    fun `bundle creates renamed file of debug build from one tag, one commit, build types only`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenTagName = "v1.0.1-debug"
        val givenCommitMessage = "Initial commit"
        val givenBundleTask = "bundleDebug"
        val git = projectDir.initGit()

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTask(givenBundleTask)

        projectDir.getFile("app").printFilesRecursively()

        val bundleDir = projectDir.getFile("app/build/outputs/bundle/debug")
        val renamedFile = bundleDir.listFiles()
            ?.find { it.name.matches(Regex("autotest-debug-vc1-\\d{8}\\.aab")) }

        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded",
        )
        assertTrue(renamedFile != null && renamedFile.exists(), "Renamed Bundle file exists")
        assertTrue(renamedFile!!.length() > 0, "Renamed Bundle file is not empty")
    }

    @Test
    @Throws(IOException::class)
    fun `assemble creates tag file of debug build from one tag, one commit, build types only`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenTagName = "v1.0.1-debug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-snapshot-debug.json")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFile = apkDir.listFiles()
            ?.find { it.name.matches(Regex("autotest-debug-vc1-\\d{8}\\.apk")) }
            ?: throw AssertionError("Output file not found")

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.log().last().id
        val expectedBuildNumber = "1"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.1-debug"
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
            !result.output.contains("Task :app:getLastTagSnapshotRelease"),
            "Task getLastTagSnapshotRelease not executed",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagSnapshotDebug"),
            "Task getLastTagSnapshotDebug executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded",
        )
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
    fun `assemble creates tag file of debug build from one tag with patch version, one commit, build types only`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenTagName = "v1.0.0.1-debug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-snapshot-debug.json")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFile = apkDir.listFiles()
            ?.find { it.name.matches(Regex("autotest-debug-vc1-\\d{8}\\.apk")) }
            ?: throw AssertionError("Output file not found")

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.log().last().id
        val expectedBuildNumber = "1"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.0.1-debug"
        val expectedBuildVersion = "1.0.0"
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
                versionName = "1.0.0",
            )
        assertTrue(
            !result.output.contains("Task :app:getLastTagSnapshotRelease"),
            "Task getLastTagSnapshotRelease not executed",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagSnapshotDebug"),
            "Task getLastTagSnapshotDebug executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded",
        )
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
    fun `assemble not creates tag file of debug build from one ending with 0 tag, one commit, build types only`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenTagName = "v1.0.0-debug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-snapshot-debug.json")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTaskWithFail(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc0-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !result.output.contains("Task :app:getLastTagSnapshotRelease"),
            "Task getLastTagSnapshotRelease not executed",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagSnapshotDebug"),
            "Task getLastTagSnapshotDebug executed",
        )
        assertTrue(
            result.output.contains("BUILD FAILED"),
            "Build failed",
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
        assertTrue(!givenOutputFileExists, "Output file not exists")
    }

    @Test
    @Throws(IOException::class)
    fun `assemble creates tag file of debug build from multiple tags, different commits, build types only`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenFirstTagName = "v1.0.1-debug"
        val givenSecondTagName = "v1.0.2-debug"
        val givenFirstCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README"
        val givenAssembleTask = "assembleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-snapshot-debug.json")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenSecondCommitMessage)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFile = apkDir.listFiles()
            ?.find { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: throw AssertionError("Output file not found")

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedTagBuildFile =
            BuildTagSnapshot(
                current = Tag.Build(
                    name = "v1.0.2-debug",
                    commitSha = git.tag.findTag(givenSecondTagName).id,
                    message = "",
                    buildVersion = "1.0",
                    buildVariant = "debug",
                    buildNumber = 2,
                ),
                previousInOrder = Tag.Build(
                    name = "v1.0.1-debug",
                    commitSha = git.tag.findTag(givenFirstTagName).id,
                    message = "",
                    buildVersion = "1.0",
                    buildVariant = "debug",
                    buildNumber = 1,
                ),
                previousOnDifferentCommit = Tag.Build(
                    name = "v1.0.1-debug",
                    commitSha = git.tag.findTag(givenFirstTagName).id,
                    message = "",
                    buildVersion = "1.0",
                    buildVariant = "debug",
                    buildNumber = 1,
                )
            ).toJson()
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "2",
                versionName = "1.0",
            )
        assertTrue(
            !result.output.contains("Task :app:getLastTagSnapshotRelease"),
            "Task getLastTagSnapshotRelease not executed",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagSnapshotDebug"),
            "Task getLastTagSnapshotDebug executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded",
        )
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
    fun `assemble creates tag file of debug build from multiple tags, same commit, build types only`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenFirstTagName = "v1.0.1-debug"
        val givenSecondTagName = "v1.0.2-debug"
        val givenFirstCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-snapshot-debug.json")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFile = apkDir.listFiles()
            ?.find { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: throw AssertionError("Output file not found")

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedTagBuildFile =
            BuildTagSnapshot(
                current = Tag.Build(
                    name = "v1.0.2-debug",
                    commitSha = git.tag.findTag(givenSecondTagName).id,
                    message = "",
                    buildVersion = "1.0",
                    buildVariant = "debug",
                    buildNumber = 2,
                ),
                previousInOrder = Tag.Build(
                    name = "v1.0.1-debug",
                    commitSha = git.tag.findTag(givenFirstTagName).id,
                    message = "",
                    buildVersion = "1.0",
                    buildVariant = "debug",
                    buildNumber = 1,
                ),
                previousOnDifferentCommit = null
            ).toJson()
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "2",
                versionName = "1.0",
            )
        assertTrue(
            !result.output.contains("Task :app:getLastTagSnapshotRelease"),
            "Task getLastTagSnapshotRelease not executed",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagSnapshotDebug"),
            "Task getLastTagSnapshotDebug executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded",
        )
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
}
