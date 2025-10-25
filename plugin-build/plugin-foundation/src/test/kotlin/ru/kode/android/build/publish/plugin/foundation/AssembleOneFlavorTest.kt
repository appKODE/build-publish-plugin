package ru.kode.android.build.publish.plugin.foundation

import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
import ru.kode.android.build.publish.plugin.foundation.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.foundation.utils.currentDate
import ru.kode.android.build.publish.plugin.foundation.utils.extractManifestProperties
import ru.kode.android.build.publish.plugin.foundation.utils.find
import ru.kode.android.build.publish.plugin.foundation.utils.getFile
import ru.kode.android.build.publish.plugin.foundation.utils.initGit
import ru.kode.android.build.publish.plugin.foundation.utils.printFilesRecursively
import ru.kode.android.build.publish.plugin.foundation.utils.runTask
import ru.kode.android.build.publish.plugin.foundation.utils.runTaskWithFail
import java.io.File
import java.io.IOException

class AssembleOneFlavorTest {
    @TempDir
    lateinit var tempDir: File
    private lateinit var projectDir: File

    @BeforeEach
    fun setup() {
        projectDir = File(tempDir, "test-project")
    }

    @Test
    @Throws(IOException::class)
    fun `assemble creates tag file of debug build from one tag, one commit, one flavor`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenTagName = "v1.0.1-googleDebug"
        val givenCommitMessage = "Initial commit"
        val givenGetLastTagTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest-googleDebug-vc1-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTask(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.log().last().id
        val expectedBuildNumber = "1"
        val expectedBuildVariant = "googleDebug"
        val expectedTagName = "v1.0.1-googleDebug"
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
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "1",
                versionName = "v1.0.1-googleDebug",
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug executed",
        )
        assertTrue(
            !result.output.contains("Task :app:getLastTagGoogleRelease"),
            "Task getLastTagGoogleRelease not executed",
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
    fun `assemble not creates tag file of debug build from one tag ending with 0, one commit, one flavor`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenTagName = "v1.0.0-googleDebug"
        val givenCommitMessage = "Initial commit"
        val givenGetLastTagTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest-googleDebug-vc0-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTaskWithFail(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug executed",
        )
        assertTrue(
            !result.output.contains("Task :app:getLastTagGoogleRelease"),
            "Task getLastTagGoogleRelease not executed",
        )
        assertTrue(
            result.output.contains("BUILD FAILED"),
            "Build failed",
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
        assertTrue(!givenOutputFile.exists(), "Output file not exists")
    }

    @Test
    @Throws(IOException::class)
    fun `assemble creates tag file of debug build from multiple tags, different commits, one flavor`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenFirstTagName = "v1.0.1-googleDebug"
        val givenSecondTagName = "v1.0.2-googleDebug"
        val givenFirstCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README"
        val givenGetLastTagTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest-googleDebug-vc2-$currentDate.apk")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenSecondCommitMessage)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult = projectDir.runTask(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenSecondTagName).id
        val expectedBuildNumber = "2"
        val expectedBuildVariant = "googleDebug"
        val expectedTagName = "v1.0.2-googleDebug"
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
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "2",
                versionName = "v1.0.2-googleDebug",
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug executed",
        )
        assertTrue(
            !result.output.contains("Task :app:getLastTagGoogleRelease"),
            "Task getLastTagGoogleRelease not executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded",
        )
        assertEquals(
            givenTagBuildFile.readText(),
            expectedTagBuildFile.trimMargin(),
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
    fun `assemble creates tag file of debug build from multiple tags, same commit, one flavor`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenFirstTagName = "v1.0.1-googleDebug"
        val givenSecondTagName = "v1.0.2-googleDebug"
        val givenFirstCommitMessage = "Initial commit"
        val givenGetLastTagTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest-googleDebug-vc2-$currentDate.apk")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult = projectDir.runTask(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenSecondTagName).id
        val expectedBuildNumber = "2"
        val expectedBuildVariant = "googleDebug"
        val expectedTagName = "v1.0.2-googleDebug"
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
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "2",
                versionName = "v1.0.2-googleDebug",
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug executed",
        )
        assertTrue(
            !result.output.contains("Task :app:getLastTagGoogleRelease"),
            "Task getLastTagGoogleRelease not executed",
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
    fun `assemble not creates tag file of debug build from multiple tags, different version, same VC, different commits, one flavor`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenFirstTagName = "v1.0.1-googleDebug"
        val givenSecondTagName = "v1.2.1-googleDebug"
        val givenFirstCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README"
        val givenGetLastTagTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest-googleDebug-vc1-$currentDate.apk")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenSecondCommitMessage)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult = projectDir.runTaskWithFail(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug executed",
        )
        assertTrue(
            !result.output.contains("Task :app:getLastTagGoogleRelease"),
            "Task getLastTagGoogleRelease not executed",
        )
        assertTrue(
            result.output.contains("BUILD FAILED"),
            "Build failed",
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
        assertTrue(!givenOutputFile.exists(), "Output file not exists")
    }

    @Test
    @Throws(IOException::class)
    fun `assemble not creates tag file of debug build from multiple tags, different version, same VC, same commit, one flavor`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenFirstTagName = "v1.0.1-googleDebug"
        val givenSecondTagName = "v1.2.1-googleDebug"
        val givenFirstCommitMessage = "Initial commit"
        val givenGetLastTagTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest-googleDebug-vc1-$currentDate.apk")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult = projectDir.runTaskWithFail(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug executed",
        )
        assertTrue(
            !result.output.contains("Task :app:getLastTagGoogleRelease"),
            "Task getLastTagGoogleRelease not executed",
        )
        assertTrue(
            result.output.contains("BUILD FAILED"),
            "Build failed",
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
        assertTrue(!givenOutputFile.exists(), "Output file not exists")
    }

    @Test
    @Throws(IOException::class)
    fun `assemble not creates tag file of debug build from multiple tags, different version, same VC, wrong order, different commits, one flavor`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenFirstTagName = "v1.2.1-googleDebug"
        val givenSecondTagName = "v1.0.1-googleDebug"
        val givenFirstCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README"
        val givenGetLastTagTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest-googleDebug-vc1-$currentDate.apk")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenSecondCommitMessage)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult = projectDir.runTaskWithFail(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug executed",
        )
        assertTrue(
            !result.output.contains("Task :app:getLastTagGoogleRelease"),
            "Task getLastTagGoogleRelease not executed",
        )
        assertTrue(
            result.output.contains("BUILD FAILED"),
            "Build failed",
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
        assertTrue(!givenOutputFile.exists(), "Output file not exists")
    }

    @Test
    @Throws(IOException::class)
    fun `assemble not creates tag file of debug build from multiple tags, different version, same VC, wrong order, same commit, one flavor`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenFirstTagName = "v1.2.1-googleDebug"
        val givenSecondTagName = "v1.0.1-googleDebug"
        val givenFirstCommitMessage = "Initial commit"
        val givenGetLastTagTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest-googleDebug-vc1-$currentDate.apk")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult = projectDir.runTaskWithFail(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug executed",
        )
        assertTrue(
            !result.output.contains("Task :app:getLastTagGoogleRelease"),
            "Task getLastTagGoogleRelease not executed",
        )
        assertTrue(
            result.output.contains("BUILD FAILED"),
            "Build failed",
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
        assertTrue(!givenOutputFile.exists(), "Output file not exists")
    }

    @Test
    @Throws(IOException::class)
    fun `assemble creates tag file of debug build from multiple tags with wrong order, different commits, one flavor`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenFirstTagName = "v1.0.2-googleDebug"
        val givenSecondTagName = "v1.0.1-googleDebug"
        val givenFirstCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README"
        val givenGetLastTagTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest-googleDebug-vc2-$currentDate.apk")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenSecondCommitMessage)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult = projectDir.runTaskWithFail(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug executed",
        )
        assertTrue(
            !result.output.contains("Task :app:getLastTagGoogleRelease"),
            "Task getLastTagGoogleRelease not executed",
        )
        assertTrue(
            result.output.contains("BUILD FAILED"),
            "Build failed",
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
        assertTrue(!givenOutputFile.exists(), "Output file not exists")
    }

    @Test
    @Throws(IOException::class)
    fun `assemble creates tag file of release build from two tags with same version, same commit, one flavor`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenTagNameDebug = "v1.0.1-googleDebug"
        val givenTagNameRelease = "v1.0.1-googleRelease"
        val givenCommitMessage = "Initial commit"
        val givenGetLastTagTaskRelease = "assembleGoogleRelease"
        val givenGetLastTagTaskDebug = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleRelease.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/release/autotest-googleRelease-vc1-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagNameDebug)
        git.tag.addNamed(givenTagNameRelease)

        val releaseResult: BuildResult = projectDir.runTask(givenGetLastTagTaskRelease)
        projectDir.runTask(givenGetLastTagTaskDebug)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.log().last().id
        val expectedBuildNumber = "1"
        val expectedBuildVariant = "googleRelease"
        val expectedTagName = "v1.0.1-googleRelease"
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
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "1",
                versionName = "v1.0.1-googleRelease",
            )
        assertTrue(
            !releaseResult.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug not executed",
        )
        assertTrue(
            releaseResult.output.contains("Task :app:getLastTagGoogleRelease"),
            "Task getLastTagGoogleRelease executed",
        )
        assertTrue(
            releaseResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded",
        )
        assertEquals(
            givenTagBuildFile.readText(),
            expectedTagBuildFile.trimMargin(),
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
    fun `assemble not creates tag file of release build from two tags with same version ending with 0, same commit, one flavor`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenTagNameDebug = "v1.0.0-googleDebug"
        val givenTagNameRelease = "v1.0.0-googleRelease"
        val givenCommitMessage = "Initial commit"
        val givenGetLastTagTaskRelease = "assembleGoogleRelease"
        val givenGetLastTagTaskDebug = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleRelease.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/release/autotest-googleRelease-vc0-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagNameDebug)
        git.tag.addNamed(givenTagNameRelease)

        val releaseResult: BuildResult = projectDir.runTaskWithFail(givenGetLastTagTaskRelease)
        projectDir.runTaskWithFail(givenGetLastTagTaskDebug)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            !releaseResult.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug not executed",
        )
        assertTrue(
            releaseResult.output.contains("Task :app:getLastTagGoogleRelease"),
            "Task getLastTagGoogleRelease executed",
        )
        assertTrue(
            releaseResult.output.contains("BUILD FAILED"),
            "Build failed",
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
        assertTrue(!givenOutputFile.exists(), "Output file nit exists")
    }

    @Test
    @Throws(IOException::class)
    fun `assemble creates tag file of release build from two tags with same version, different commits, one flavor`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenTagNameDebug = "v1.0.1-googleDebug"
        val givenTagNameRelease = "v1.0.1-googleRelease"
        val givenFirstCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README"
        val givenGetLastTagTaskRelease = "assembleGoogleRelease"
        val givenGetLastTagTaskDebug = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleRelease.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/release/autotest-googleRelease-vc1-$currentDate.apk")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenTagNameDebug)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenSecondCommitMessage)
        git.tag.addNamed(givenTagNameRelease)

        val releaseResult: BuildResult = projectDir.runTask(givenGetLastTagTaskRelease)
        projectDir.runTask(givenGetLastTagTaskDebug)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagNameRelease).id
        val expectedBuildNumber = "1"
        val expectedBuildVariant = "googleRelease"
        val expectedTagName = "v1.0.1-googleRelease"
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
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "1",
                versionName = "v1.0.1-googleRelease",
            )
        assertTrue(
            !releaseResult.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug not executed",
        )
        assertTrue(
            releaseResult.output.contains("Task :app:getLastTagGoogleRelease"),
            "Task getLastTagGoogleRelease executed",
        )
        assertTrue(
            releaseResult.output.contains("BUILD SUCCESSFUL"),
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
    fun `assemble not creates tag file of release build from two tags with same version ending with 0, different commits, one flavor`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenTagNameDebug = "v1.0.0-googleDebug"
        val givenTagNameRelease = "v1.0.0-googleRelease"
        val givenFirstCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README"
        val givenGetLastTagTaskRelease = "assembleGoogleRelease"
        val givenGetLastTagTaskDebug = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleRelease.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/release/autotest-googleRelease-vc0-$currentDate.apk")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenTagNameDebug)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenSecondCommitMessage)
        git.tag.addNamed(givenTagNameRelease)

        val releaseResult: BuildResult = projectDir.runTaskWithFail(givenGetLastTagTaskRelease)
        projectDir.runTaskWithFail(givenGetLastTagTaskDebug)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            !releaseResult.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug not executed",
        )
        assertTrue(
            releaseResult.output.contains("Task :app:getLastTagGoogleRelease"),
            "Task getLastTagGoogleRelease executed",
        )
        assertTrue(
            releaseResult.output.contains("BUILD FAILED"),
            "Build succeeded",
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
        assertTrue(!givenOutputFile.exists(), "Output file not exists")
    }

    @Test
    @Throws(IOException::class)
    fun `assemble creates tag file of release build from two tags with different VC, same commit, one flavor`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenTagNameDebug = "v1.0.2-googleDebug"
        val givenTagNameRelease = "v1.0.1-googleRelease"
        val givenFirstCommitMessage = "Initial commit"
        val givenGetLastTagTaskRelease = "assembleGoogleRelease"
        val givenGetLastTagTaskDebug = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleRelease.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/release/autotest-googleRelease-vc1-$currentDate.apk")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenTagNameDebug)
        git.tag.addNamed(givenTagNameRelease)

        val releaseResult: BuildResult = projectDir.runTask(givenGetLastTagTaskRelease)
        projectDir.runTask(givenGetLastTagTaskDebug)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagNameRelease).id
        val expectedBuildNumber = "1"
        val expectedBuildVariant = "googleRelease"
        val expectedTagName = "v1.0.1-googleRelease"
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
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "1",
                versionName = "v1.0.1-googleRelease",
            )
        assertTrue(
            !releaseResult.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug not executed",
        )
        assertTrue(
            releaseResult.output.contains("Task :app:getLastTagGoogleRelease"),
            "Task getLastTagGoogleRelease executed",
        )
        assertTrue(
            releaseResult.output.contains("BUILD SUCCESSFUL"),
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
    fun `assemble creates tag file of release build from two tags with different VC, different commits, one flavor`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenTagNameDebug = "v1.0.2-googleDebug"
        val givenTagNameRelease = "v1.0.1-googleRelease"
        val givenFirstCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README"
        val givenGetLastTagTaskRelease = "assembleGoogleRelease"
        val givenGetLastTagTaskDebug = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleRelease.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/release/autotest-googleRelease-vc1-$currentDate.apk")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenTagNameDebug)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenSecondCommitMessage)
        git.tag.addNamed(givenTagNameRelease)

        val releaseResult: BuildResult = projectDir.runTask(givenGetLastTagTaskRelease)

        projectDir.runTask(givenGetLastTagTaskDebug)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagNameRelease).id
        val expectedBuildNumber = "1"
        val expectedBuildVariant = "googleRelease"
        val expectedTagName = "v1.0.1-googleRelease"
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
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "1",
                versionName = "v1.0.1-googleRelease",
            )
        assertTrue(
            !releaseResult.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug not executed",
        )
        assertTrue(
            releaseResult.output.contains("Task :app:getLastTagGoogleRelease"),
            "Task getLastTagGoogleRelease executed",
        )
        assertTrue(
            releaseResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded",
        )
        assertEquals(
            givenTagBuildFile.readText(),
            expectedTagBuildFile.trimMargin(),
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
    fun `assemble creates tag file of debug build from three tags with different VC, where first numbers are equal, different commits, one flavor`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val given1TagNameDebug = "v1.0.2-googleDebug"
        val given2TagNameDebug = "v1.0.99-googleDebug"
        val given3TagNameDebug = "v1.0.100-googleDebug"
        val givenFirstCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README N1"
        val givenThirdCommitMessage = "Add README N2"
        val givenGetLastTagTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest-googleDebug-vc100-$currentDate.apk")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(given1TagNameDebug)
        projectDir.getFile("app/README1.md").writeText("This is test project")
        git.addAllAndCommit(givenSecondCommitMessage)
        git.tag.addNamed(given2TagNameDebug)
        projectDir.getFile("app/README2.md").writeText("This is test project")
        git.addAllAndCommit(givenThirdCommitMessage)
        git.tag.addNamed(given3TagNameDebug)

        val releaseResult: BuildResult = projectDir.runTask(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedBuildNumber = "100"
        val expectedBuildVariant = "googleDebug"
        val expectedTagName = "v1.0.100-googleDebug"
        val expectedCommitSha = git.tag.find(expectedTagName).id
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
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "100",
                versionName = "v1.0.100-googleDebug",
            )
        assertTrue(
            releaseResult.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug executed",
        )
        assertTrue(
            !releaseResult.output.contains("Task :app:getLastTagGoogleRelease"),
            "Task getLastTagGoogleRelease not executed",
        )
        assertTrue(
            releaseResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded",
        )
        assertEquals(
            givenTagBuildFile.readText(),
            expectedTagBuildFile.trimMargin(),
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
    fun `assemble creates tag file of debug build from three tags with different VC, where first numbers are equal, same commit, one flavor`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val given1TagNameDebug = "v1.0.2-googleDebug"
        val given2TagNameDebug = "v1.0.99-googleDebug"
        val given3TagNameDebug = "v1.0.100-googleDebug"
        val givenCommitMessage = "Initial commit"
        val givenGetLastTagTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest-googleDebug-vc100-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(given1TagNameDebug)
        git.tag.addNamed(given2TagNameDebug)
        git.tag.addNamed(given3TagNameDebug)

        val releaseResult: BuildResult = projectDir.runTask(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedBuildNumber = "100"
        val expectedBuildVariant = "googleDebug"
        val expectedTagName = "v1.0.100-googleDebug"
        val expectedCommitSha = git.tag.find(expectedTagName).id
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
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "100",
                versionName = "v1.0.100-googleDebug",
            )
        assertTrue(
            releaseResult.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug executed",
        )
        assertTrue(
            !releaseResult.output.contains("Task :app:getLastTagGoogleRelease"),
            "Task getLastTagGoogleRelease not executed",
        )
        assertTrue(
            releaseResult.output.contains("BUILD SUCCESSFUL"),
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
    fun `assemble creates tag file of debug build from three tags with different VC, where first 2 numbers are equal, different commits, one flavor`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val given1TagNameDebug = "v1.1.1-googleDebug"
        val given2TagNameDebug = "v1.1.99-googleDebug"
        val given3TagNameDebug = "v1.1.100-googleDebug"
        val givenFistCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README N1"
        val givenThirdCommitMessage = "Add README N2"
        val givenGetLastTagTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest-googleDebug-vc100-$currentDate.apk")

        git.addAllAndCommit(givenFistCommitMessage)
        git.tag.addNamed(given1TagNameDebug)
        projectDir.getFile("app/README1.md").writeText("This is test project")
        git.addAllAndCommit(givenSecondCommitMessage)
        git.tag.addNamed(given2TagNameDebug)
        projectDir.getFile("app/README1.md").writeText("This is test project")
        git.addAllAndCommit(givenThirdCommitMessage)
        git.tag.addNamed(given3TagNameDebug)

        val releaseResult: BuildResult = projectDir.runTask(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedBuildNumber = "100"
        val expectedBuildVariant = "googleDebug"
        val expectedTagName = "v1.1.100-googleDebug"
        val expectedCommitSha = git.tag.find(expectedTagName).id
        val expectedBuildVersion = "1.1"
        val expectedTagBuildFile =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = "",
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "100",
                versionName = "v1.1.100-googleDebug",
            )
        assertTrue(
            releaseResult.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug executed",
        )
        assertTrue(
            !releaseResult.output.contains("Task :app:getLastTagGoogleRelease"),
            "Task getLastTagGoogleRelease not executed",
        )
        assertTrue(
            releaseResult.output.contains("BUILD SUCCESSFUL"),
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
    fun `assemble creates tag file of debug build from three tags with different VC, where first 2 numbers are equal, same commit, one flavor`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val given1TagNameDebug = "v1.1.1-googleDebug"
        val given2TagNameDebug = "v1.1.99-googleDebug"
        val given3TagNameDebug = "v1.1.100-googleDebug"
        val givenCommitMessage = "Initial commit"
        val givenGetLastTagTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest-googleDebug-vc100-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(given1TagNameDebug)
        git.tag.addNamed(given2TagNameDebug)
        git.tag.addNamed(given3TagNameDebug)

        val releaseResult: BuildResult = projectDir.runTask(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedBuildNumber = "100"
        val expectedBuildVariant = "googleDebug"
        val expectedTagName = "v1.1.100-googleDebug"
        val expectedCommitSha = git.tag.find(expectedTagName).id
        val expectedBuildVersion = "1.1"
        val expectedTagBuildFile =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = "",
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "100",
                versionName = "v1.1.100-googleDebug",
            )
        assertTrue(
            releaseResult.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug executed",
        )
        assertTrue(
            !releaseResult.output.contains("Task :app:getLastTagGoogleRelease"),
            "Task getLastTagGoogleRelease not executed",
        )
        assertTrue(
            releaseResult.output.contains("BUILD SUCCESSFUL"),
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
