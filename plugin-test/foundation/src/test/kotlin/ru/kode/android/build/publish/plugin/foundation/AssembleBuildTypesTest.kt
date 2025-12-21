package ru.kode.android.build.publish.plugin.foundation

import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.git.mapper.toJson
import ru.kode.android.build.publish.plugin.test.utils.BuildType
import ru.kode.android.build.publish.plugin.test.utils.FoundationConfig
import ru.kode.android.build.publish.plugin.test.utils.ManifestProperties
import ru.kode.android.build.publish.plugin.test.utils.addAllAndCommit
import ru.kode.android.build.publish.plugin.test.utils.addNamed
import ru.kode.android.build.publish.plugin.test.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.extractManifestProperties
import ru.kode.android.build.publish.plugin.test.utils.find
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
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

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
                versionName = "1.0",
            )
        assertTrue(
            !result.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
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
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

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
                versionName = "1.0.0",
            )
        assertTrue(
            !result.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
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
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTaskWithFail(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc0-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !result.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
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
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

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

        val expectedCommitSha = git.tag.find(givenSecondTagName).id
        val expectedBuildNumber = "2"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.2-debug"
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
                versionName = "1.0",
            )
        assertTrue(
            !result.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
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
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

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

        val expectedCommitSha = git.tag.find(givenSecondTagName).id
        val expectedBuildNumber = "2"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.2-debug"
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
                versionName = "1.0",
            )
        assertTrue(
            !result.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
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
    fun `assemble not creates tag file of debug build from multiple tags, different version, same VC, different commits, build types only`() {
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
        val givenSecondTagName = "v1.2.1-debug"
        val givenFirstCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README"
        val givenAssembleTask = "assembleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenSecondCommitMessage)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult = projectDir.runTaskWithFail(givenAssembleTask)

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc1-\\d{8}\\.apk")) }
            ?: false

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            !result.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
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
    fun `assemble not creates tag file of debug build from multiple tags ending with 0, different version, same VC, different commits, build types only`() {
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
        val givenFirstTagName = "v1.0.0-debug"
        val givenSecondTagName = "v1.2.0-debug"
        val givenFirstCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README"
        val givenAssembleTask = "assembleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenSecondCommitMessage)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult = projectDir.runTaskWithFail(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc0-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !result.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
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
    fun `assemble not creates tag file of debug build from multiple tags, different version, same VC, same commit, build types only`() {
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
        val givenSecondTagName = "v1.2.1-debug"
        val givenFirstCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult = projectDir.runTaskWithFail(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc1-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !result.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
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
    fun `assemble not creates tag file of debug build from multiple tags ending with 0, different version, same VC, same commit, build types only`() {
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
        val givenFirstTagName = "v1.0.0-debug"
        val givenSecondTagName = "v1.2.0-debug"
        val givenFirstCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult = projectDir.runTaskWithFail(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc0-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !result.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
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
    fun `assemble not creates tag file of debug build from multiple tags, different version, same VC, wrong order, different commits, build types only`() {
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
        val givenFirstTagName = "v1.2.1-debug"
        val givenSecondTagName = "v1.0.1-debug"
        val givenFirstCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README"
        val givenAssembleTask = "assembleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenSecondCommitMessage)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult = projectDir.runTaskWithFail(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc1-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !result.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
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
    fun `assemble not creates tag file of debug build from multiple tags ending with 0, different version, same VC, wrong order, different commits, build types only`() {
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
        val givenFirstTagName = "v1.2.0-debug"
        val givenSecondTagName = "v1.0.0-debug"
        val givenFirstCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README"
        val givenAssembleTask = "assembleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenSecondCommitMessage)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult = projectDir.runTaskWithFail(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc0-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !result.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
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
    fun `assemble not creates tag file of debug build from multiple tags, different version, same VC, wrong order, same commit, build types only`() {
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
        val givenFirstTagName = "v1.2.1-debug"
        val givenSecondTagName = "v1.0.1-debug"
        val givenFirstCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult = projectDir.runTaskWithFail(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc1-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !result.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
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
    fun `assemble not creates tag file of debug build from multiple tags ending with 0, different version, same VC, wrong order, same commit, build types only`() {
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
        val givenFirstTagName = "v1.2.0-debug"
        val givenSecondTagName = "v1.0.0-debug"
        val givenFirstCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult = projectDir.runTaskWithFail(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc0-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !result.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
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
    fun `assemble not creates tag file of debug build from multiple tags with wrong order, different commits, build types only`() {
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
        val givenFirstTagName = "v1.0.2-debug"
        val givenSecondTagName = "v1.0.1-debug"
        val givenFirstCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README"
        val givenAssembleTask = "assembleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenSecondCommitMessage)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult = projectDir.runTaskWithFail(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !result.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
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
    fun `assemble creates tag file of release build from two tags with same version, same commit, build types only`() {
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
        val givenTagNameDebug = "v1.0.1-debug"
        val givenTagNameRelease = "v1.0.1-release"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTaskRelease = "assembleRelease"
        val givenAssembleTaskDebug = "assembleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-release.json")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagNameDebug)
        git.tag.addNamed(givenTagNameRelease)

        val releaseResult: BuildResult = projectDir.runTask(givenAssembleTaskRelease)

        projectDir.runTask(givenAssembleTaskDebug)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/release")
        val givenOutputFile = apkDir.listFiles()
            ?.find { it.name.matches(Regex("autotest-release-vc1-\\d{8}\\.apk")) }
            ?: throw AssertionError("Output file not found")

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.log().last().id
        val expectedBuildNumber = "1"
        val expectedBuildVariant = "release"
        val expectedTagName = "v1.0.1-release"
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
                versionName = "1.0",
            )
        assertTrue(
            !releaseResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug not executed",
        )
        assertTrue(
            releaseResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease executed",
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
    fun `assemble not creates tag file of release build from two tags with same version ending with 0, same commit, build types only`() {
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
        val givenTagNameDebug = "v1.0.0-debug"
        val givenTagNameRelease = "v1.0.0-release"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTaskRelease = "assembleRelease"
        val givenAssembleTaskDebug = "assembleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-release.json")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagNameDebug)
        git.tag.addNamed(givenTagNameRelease)

        val releaseResult: BuildResult = projectDir.runTaskWithFail(givenAssembleTaskRelease)

        projectDir.runTaskWithFail(givenAssembleTaskDebug)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/release")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-release-vc0-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !releaseResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug not executed",
        )
        assertTrue(
            releaseResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease executed",
        )
        assertTrue(
            releaseResult.output.contains("BUILD FAILED"),
            "Build failed",
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
        assertTrue(!givenOutputFileExists, "Output file not exists")
    }

    @Test
    @Throws(IOException::class)
    fun `assemble creates tag file of release build from two tags with same version, different commits, build types only`() {
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
        val givenTagNameDebug = "v1.0.1-debug"
        val givenTagNameRelease = "v1.0.1-release"
        val givenFirstCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README"
        val givenAssembleTaskRelease = "assembleRelease"
        val givenAssembleTaskDebug = "assembleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-release.json")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenTagNameDebug)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenSecondCommitMessage)
        git.tag.addNamed(givenTagNameRelease)

        val releaseResult: BuildResult = projectDir.runTask(givenAssembleTaskRelease)

        projectDir.runTask(givenAssembleTaskDebug)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/release")
        val givenOutputFile = apkDir.listFiles()
            ?.first { it.name.matches(Regex("autotest-release-vc1-\\d{8}\\.apk")) }
            ?: throw AssertionError("Output file not found")

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagNameRelease).id
        val expectedBuildNumber = "1"
        val expectedBuildVariant = "release"
        val expectedTagName = "v1.0.1-release"
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
                versionName = "1.0",
            )
        assertTrue(
            !releaseResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug not executed",
        )
        assertTrue(
            releaseResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease executed",
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
    fun `assemble not creates tag file of release build from two tags with same version ending with 0, different commits, build types only`() {
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
        val givenTagNameDebug = "v1.0.0-debug"
        val givenTagNameRelease = "v1.0.0-release"
        val givenFirstCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README"
        val givenAssembleTaskRelease = "assembleRelease"
        val givenAssembleTaskDebug = "assembleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-release.json")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenTagNameDebug)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenSecondCommitMessage)
        git.tag.addNamed(givenTagNameRelease)

        val releaseResult: BuildResult = projectDir.runTaskWithFail(givenAssembleTaskRelease)

        projectDir.runTaskWithFail(givenAssembleTaskDebug)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/release")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-release-vc0-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !releaseResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug not executed",
        )
        assertTrue(
            releaseResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease executed",
        )
        assertTrue(
            releaseResult.output.contains("BUILD FAILED"),
            "Build failed",
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
        assertTrue(!givenOutputFileExists, "Output file not exists")
    }

    @Test
    @Throws(IOException::class)
    fun `assemble creates tag file of release build from two tags with different VC, same commit, build types only`() {
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
        val givenTagNameDebug = "v1.0.2-debug"
        val givenTagNameRelease = "v1.0.1-release"
        val givenFirstCommitMessage = "Initial commit"
        val givenAssembleTaskRelease = "assembleRelease"
        val givenAssembleTaskDebug = "assembleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-release.json")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenTagNameDebug)
        git.tag.addNamed(givenTagNameRelease)

        val releaseResult: BuildResult = projectDir.runTask(givenAssembleTaskRelease)

        projectDir.runTask(givenAssembleTaskDebug)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/release")
        val givenOutputFile = apkDir.listFiles()
            ?.first { it.name.matches(Regex("autotest-release-vc1-\\d{8}\\.apk")) }
            ?: throw AssertionError("Output file not found")

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagNameRelease).id
        val expectedBuildNumber = "1"
        val expectedBuildVariant = "release"
        val expectedTagName = "v1.0.1-release"
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
                versionName = "1.0",
            )
        assertTrue(
            !releaseResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug not executed",
        )
        assertTrue(
            releaseResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease executed",
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
    fun `assemble creates tag file of release build from two tags with different VC, different commits, build types only`() {
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
        val givenTagNameDebug = "v1.0.2-debug"
        val givenTagNameRelease = "v1.0.1-release"
        val givenFirstCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README"
        val givenAssembleTaskRelease = "assembleRelease"
        val givenAssembleTaskDebug = "assembleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-release.json")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenTagNameDebug)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenSecondCommitMessage)
        git.tag.addNamed(givenTagNameRelease)

        val releaseResult: BuildResult = projectDir.runTask(givenAssembleTaskRelease)

        projectDir.runTask(givenAssembleTaskDebug)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/release")
        val givenOutputFile = apkDir.listFiles()
            ?.first { it.name.matches(Regex("autotest-release-vc1-\\d{8}\\.apk")) }
            ?: throw AssertionError("Output file not found")

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagNameRelease).id
        val expectedBuildNumber = "1"
        val expectedBuildVariant = "release"
        val expectedTagName = "v1.0.1-release"
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
                versionName = "1.0",
            )
        assertTrue(
            !releaseResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug not executed",
        )
        assertTrue(
            releaseResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease executed",
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
    fun `assemble creates tag file of debug build from three tags with different VC, where first numbers are equal, different commits, build types only`() {
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
        val given1TagNameDebug = "v1.0.2-debug"
        val given2TagNameDebug = "v1.0.99-debug"
        val given3TagNameDebug = "v1.0.100-debug"
        val givenFirstCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README N1"
        val givenThirdCommitMessage = "Add README N2"
        val givenAssembleTask = "assembleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(given1TagNameDebug)
        projectDir.getFile("app/README1.md").writeText("This is test project")
        git.addAllAndCommit(givenSecondCommitMessage)
        git.tag.addNamed(given2TagNameDebug)
        projectDir.getFile("app/README2.md").writeText("This is test project")
        git.addAllAndCommit(givenThirdCommitMessage)
        git.tag.addNamed(given3TagNameDebug)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFile = apkDir.listFiles()
            ?.first { it.name.matches(Regex("autotest-debug-vc100-\\d{8}\\.apk")) }
            ?: throw AssertionError("Output file not found")

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedBuildNumber = "100"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.100-debug"
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
                versionName = "1.0",
            )
        assertTrue(
            !result.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
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
    fun `assemble creates tag file of debug build from three tags with different VC, where first numbers are equal, same commit, build types only`() {
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
        val given1TagNameDebug = "v1.0.2-debug"
        val given2TagNameDebug = "v1.0.99-debug"
        val given3TagNameDebug = "v1.0.100-debug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(given1TagNameDebug)
        git.tag.addNamed(given2TagNameDebug)
        git.tag.addNamed(given3TagNameDebug)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFile = apkDir.listFiles()
            ?.first { it.name.matches(Regex("autotest-debug-vc100-\\d{8}\\.apk")) }
            ?: throw AssertionError("Output file not found")

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedBuildNumber = "100"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.100-debug"
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
                versionName = "1.0",
            )
        assertTrue(
            !result.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
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
    fun `assemble creates tag file of debug build from three tags with different VC, where first 2 numbers are equal, different commits, build types only`() {
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
        val given1TagNameDebug = "v1.1.1-debug"
        val given2TagNameDebug = "v1.1.99-debug"
        val given3TagNameDebug = "v1.1.100-debug"
        val givenFistCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README N1"
        val givenThirdCommitMessage = "Add README N2"
        val givenAssembleTask = "assembleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenFistCommitMessage)
        git.tag.addNamed(given1TagNameDebug)
        projectDir.getFile("app/README1.md").writeText("This is test project")
        git.addAllAndCommit(givenSecondCommitMessage)
        git.tag.addNamed(given2TagNameDebug)
        projectDir.getFile("app/README1.md").writeText("This is test project")
        git.addAllAndCommit(givenThirdCommitMessage)
        git.tag.addNamed(given3TagNameDebug)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFile = apkDir.listFiles()
            ?.first { it.name.matches(Regex("autotest-debug-vc100-\\d{8}\\.apk")) }
            ?: throw AssertionError("Output file not found")

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedBuildNumber = "100"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.1.100-debug"
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
                versionName = "1.1",
            )
        assertTrue(
            !result.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
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
    fun `assemble creates tag file of debug build from three tags with different VC, where first 2 numbers are equal, same commit, build types only`() {
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
        val given1TagNameDebug = "v1.1.1-debug"
        val given2TagNameDebug = "v1.1.99-debug"
        val given3TagNameDebug = "v1.1.100-debug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(given1TagNameDebug)
        git.tag.addNamed(given2TagNameDebug)
        git.tag.addNamed(given3TagNameDebug)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFile = apkDir.listFiles()
            ?.first { it.name.matches(Regex("autotest-debug-vc100-\\d{8}\\.apk")) }
            ?: throw AssertionError("Output file not found")

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedBuildNumber = "100"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.1.100-debug"
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
                versionName = "1.1",
            )
        assertTrue(
            !result.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
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
