package ru.kode.android.build.publish.plugin.foundation

import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.core.enity.BuildTagSnapshot
import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.git.mapper.toJson
import ru.kode.android.build.publish.plugin.foundation.messages.mustBeUsedWithVersionMessage
import ru.kode.android.build.publish.plugin.foundation.validate.AgpVersions
import ru.kode.android.build.publish.plugin.test.utils.BuildType
import ru.kode.android.build.publish.plugin.test.utils.DefaultConfig
import ru.kode.android.build.publish.plugin.test.utils.FoundationConfig
import ru.kode.android.build.publish.plugin.test.utils.ManifestProperties
import ru.kode.android.build.publish.plugin.test.utils.ProductFlavor
import ru.kode.android.build.publish.plugin.test.utils.addAllAndCommit
import ru.kode.android.build.publish.plugin.test.utils.addNamed
import ru.kode.android.build.publish.plugin.test.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.extractManifestProperties
import ru.kode.android.build.publish.plugin.test.utils.getFile
import ru.kode.android.build.publish.plugin.test.utils.initGit
import ru.kode.android.build.publish.plugin.test.utils.printFilesRecursively
import ru.kode.android.build.publish.plugin.test.utils.resolveRequiredAgpJars
import ru.kode.android.build.publish.plugin.test.utils.runTask
import ru.kode.android.build.publish.plugin.test.utils.runTaskWithFail
import java.io.File
import java.io.IOException

class AssembleAgpVersionsTest {

    @TempDir
    lateinit var tempDir: File
    private lateinit var projectDir: File

    @BeforeEach
    fun setup() {
        projectDir = File(tempDir, "test-project")
    }

    @Test
    @Throws(IOException::class)
    fun `assemble creates tag file of debug build from one tag, one commit, build types only, agp 7_3_0`() {
        val agpClasspath = resolveRequiredAgpJars("7.3.0")

        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
            compileSdk = 33,
            defaultConfig = DefaultConfig(
                minSdk = 21,
                targetSdk = 33,
            )
        )
        val givenTagName = "v1.0.1-debug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDebug"
        val git = projectDir.initGit()

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTaskWithFail(
            givenAssembleTask,
            agpClasspath = agpClasspath,
            gradleVersion = "7.4"
        )

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            !result.output.contains("Task :app:getLastTagSnapshotRelease"),
            "Task getLastTagSnapshotRelease not executed",
        )
        assertTrue(
            !result.output.contains("Task :app:getLastTagSnapshotDebug"),
            "Task getLastTagSnapshotDebug executed",
        )
        assertTrue(
            result.output.contains("BUILD FAILED"),
            "Build failed",
        )
        val expectedMessage = mustBeUsedWithVersionMessage(AgpVersions.MIN_VERSION)
            .replace("|", "     |")
        assertTrue(
            result.output.contains(expectedMessage)
        )
    }

    @Test
    @Throws(IOException::class)
    fun `build succeed with default config versions when useVersionsFromTag, useStubsForTagAsFallback, useDefaultsForVersionsAsFallback are disabled and tag not exists, agp 7_4_0`() {
        val agpClasspath = resolveRequiredAgpJars("7.4.0")

        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            compileSdk = 34,
            defaultConfig =
                DefaultConfig(
                    versionCode = 10,
                    versionName = "v.1.0.10",
                    minSdk = 21,
                    targetSdk = 34,
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                            useVersionsFromTag = false,
                            useStubsForTagAsFallback = false,
                            useDefaultsForVersionsAsFallback = false,
                        ),
                ),
        )
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-snapshot-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest.apk")

        git.addAllAndCommit(givenCommitMessage)

        val result: BuildResult = projectDir.runTask(givenAssembleTask, agpClasspath = agpClasspath)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "10",
                versionName = "v.1.0.10",
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagSnapshotGoogleDebug"),
            "Task getLastTagSnapshotGoogleDebug executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertTrue(givenTagBuildFile.exists(), "Tag file exists")
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
    fun `assemble creates tag file of debug build from one tag, one commit, build types only, agp 7_4_0`() {
        val agpClasspath = resolveRequiredAgpJars("7.4.0")

        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
            compileSdk = 34,
            defaultConfig = DefaultConfig(
                minSdk = 21,
                targetSdk = 34,
            )
        )
        val givenTagName = "v1.0.1-debug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-snapshot-debug.json")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTask(
            givenAssembleTask,
            agpClasspath = agpClasspath,
            gradleVersion = "7.5"
        )

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/intermediates/apk/renameApkDebug")
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
    fun `assemble creates tag file of debug build from one tag, one commit, build types only, agp 8_0_2`() {
        val agpClasspath = resolveRequiredAgpJars("8.0.2")

        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
            compileSdk = 34,
            defaultConfig = DefaultConfig(
                minSdk = 21,
                targetSdk = 34,
            )
        )
        val givenTagName = "v1.0.1-debug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDebug"
        val git = projectDir.initGit()

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTask(
            givenAssembleTask,
            agpClasspath = agpClasspath,
            gradleVersion = "8.0.2"
        )

        projectDir.getFile("app").printFilesRecursively()

        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-snapshot-debug.json")

        val apkDir = projectDir.getFile("app/build/intermediates/apk/renameApkDebug")
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
    fun `assemble creates tag file of debug build from one tag, one commit, build types only, agp 8_1_0`() {
        val agpClasspath = resolveRequiredAgpJars("8.2.2")

        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
            compileSdk = 35,
            defaultConfig = DefaultConfig(
                minSdk = 21,
                targetSdk = 35,
            )
        )
        val givenTagName = "v1.0.1-debug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-snapshot-debug.json")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTask(givenAssembleTask, agpClasspath = agpClasspath)

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
    fun `assemble creates tag file of debug build from one tag, one commit, build types only, agp 8_2_2`() {
        val agpClasspath = resolveRequiredAgpJars("8.2.2")

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

        val result: BuildResult = projectDir.runTask(givenAssembleTask, agpClasspath = agpClasspath)

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
    fun `assemble creates tag file of debug build from one tag, one commit, build types only, agp 8_3_2`() {
        val agpClasspath = resolveRequiredAgpJars("8.3.2")

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

        val result: BuildResult = projectDir.runTask(givenAssembleTask, agpClasspath = agpClasspath)

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
    fun `assemble creates tag file of debug build from one tag, one commit, build types only, agp 8_4_2`() {
        val agpClasspath = resolveRequiredAgpJars("8.4.2")

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

        val result: BuildResult = projectDir.runTask(givenAssembleTask, agpClasspath = agpClasspath)

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
    fun `assemble creates tag file of debug build from one tag, one commit, build types only, agp 8_5_2`() {
        val agpClasspath = resolveRequiredAgpJars("8.5.2")

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

        val result: BuildResult = projectDir.runTask(givenAssembleTask, agpClasspath = agpClasspath)

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
    fun `assemble creates tag file of debug build from one tag, one commit, build types only, agp 8_6_1`() {
        val agpClasspath = resolveRequiredAgpJars("8.6.1")

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

        val result: BuildResult = projectDir.runTask(givenAssembleTask, agpClasspath = agpClasspath)

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
    fun `assemble creates tag file of debug build from one tag, one commit, build types only, agp 8_7_3`() {
        val agpClasspath = resolveRequiredAgpJars("8.7.3")

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

        val result: BuildResult = projectDir.runTask(givenAssembleTask, agpClasspath = agpClasspath)

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
    fun `assemble creates tag file of debug build from one tag, one commit, build types only, agp 8_8_2`() {
        val agpClasspath = resolveRequiredAgpJars("8.8.2")

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

        val result: BuildResult = projectDir.runTask(givenAssembleTask, agpClasspath = agpClasspath)

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
    fun `assemble creates tag file of debug build from one tag, one commit, build types only, agp 8_9_2`() {
        val agpClasspath = resolveRequiredAgpJars("8.10.1")

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

        val result: BuildResult = projectDir.runTask(givenAssembleTask, agpClasspath = agpClasspath)

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
    fun `assemble creates tag file of debug build from one tag, one commit, build types only, agp 8_10_1`() {
        val agpClasspath = resolveRequiredAgpJars("8.10.1")

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

        val result: BuildResult = projectDir.runTask(givenAssembleTask, agpClasspath = agpClasspath)

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
    fun `assemble creates tag file of debug build from one tag, one commit, build types only, agp 8_11_1`() {
        val agpClasspath = resolveRequiredAgpJars("8.11.1")

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

        val result: BuildResult = projectDir.runTask(givenAssembleTask, agpClasspath = agpClasspath)

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
    fun `assemble creates tag file of debug build from one tag, one commit, build types only, agp 8_12_2`() {
        val agpClasspath = resolveRequiredAgpJars("8.12.2")

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

        val result: BuildResult = projectDir.runTask(givenAssembleTask, agpClasspath = agpClasspath)

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
    fun `build succeed with default config versions when useVersionsFromTag, useStubsForTagAsFallback, useDefaultsForVersionsAsFallback are disabled and tag not exists, agp 8_13_0`() {
        val agpClasspath = resolveRequiredAgpJars("8.13.0")

        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            defaultConfig =
                DefaultConfig(
                    versionCode = 10,
                    versionName = "v.1.0.10",
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                            useVersionsFromTag = false,
                            useStubsForTagAsFallback = false,
                            useDefaultsForVersionsAsFallback = false,
                        ),
                ),
        )
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-snapshot-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest.apk")

        git.addAllAndCommit(givenCommitMessage)

        val result: BuildResult = projectDir.runTask(givenAssembleTask, agpClasspath = agpClasspath)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "10",
                versionName = "v.1.0.10",
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagSnapshotGoogleDebug"),
            "Task getLastTagSnapshotGoogleDebug executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertTrue(givenTagBuildFile.exists(), "Tag file exists")
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
    fun `assemble creates tag file of debug build from one tag, one commit, build types only, agp 8_13_0`() {
        val agpClasspath = resolveRequiredAgpJars("8.13.0")

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

        val result: BuildResult = projectDir.runTask(givenAssembleTask, agpClasspath = agpClasspath)

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
    fun `build succeed with default config versions when useVersionsFromTag, useStubsForTagAsFallback, useDefaultsForVersionsAsFallback are disabled and tag not exists, agp 9_0_0`() {
        val agpClasspath = resolveRequiredAgpJars("9.0.0")

        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            defaultConfig =
                DefaultConfig(
                    versionCode = 10,
                    versionName = "v.1.0.10",
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                            useVersionsFromTag = false,
                            useStubsForTagAsFallback = false,
                            useDefaultsForVersionsAsFallback = false,
                        ),
                ),
        )
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-snapshot-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest.apk")

        git.addAllAndCommit(givenCommitMessage)

        val result: BuildResult = projectDir.runTask(givenAssembleTask, agpClasspath = agpClasspath)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "10",
                versionName = "v.1.0.10",
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagSnapshotGoogleDebug"),
            "Task getLastTagSnapshotGoogleDebug executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertTrue(givenTagBuildFile.exists(), "Tag file exists")
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
    fun `assemble creates tag file of debug build from one tag, one commit, build types only, agp 9_0_0`() {
        val agpClasspath = resolveRequiredAgpJars("9.0.0")

        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
            topBuildFileContent = """
                plugins {
                    id("com.android.application") version "9.0.0" apply false
                }
            """.trimIndent()
        )
        val givenTagName = "v1.0.1-debug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-snapshot-debug.json")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTask(givenAssembleTask, agpClasspath = agpClasspath)

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
}
