package ru.kode.android.build.publish.plugin.foundation

import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.git.mapper.toJson
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_BUILD_VERSION
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_TAG_COMMIT_MESSAGE
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_TAG_COMMIT_SHA
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_TAG_NAME
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_VERSION_CODE
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_VERSION_NAME
import ru.kode.android.build.publish.plugin.foundation.utils.BuildType
import ru.kode.android.build.publish.plugin.foundation.utils.DefaultConfig
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

class FoundationAssembleTest {
    @TempDir
    lateinit var tempDir: File
    private lateinit var projectDir: File

    @BeforeEach
    fun setup() {
        projectDir = File(tempDir, "test-project")
    }

    @Test
    @Throws(IOException::class)
    fun `build succeed with last tag when default config is used and tag exists`() {
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
        val givenTagName = "v1.0.321-googleDebug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest-googleDebug-vc321-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.log().last().id
        val expectedBuildNumber = "321"
        val expectedBuildVariant = "googleDebug"
        val expectedTagName = "v1.0.321-googleDebug"
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
                versionCode = "321",
                versionName = "v1.0.321-googleDebug",
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
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
    fun `build succeed with last tag when default config is used, tag exists and useVersionsFromTag is enabled`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                            useVersionsFromTag = true,
                        ),
                ),
        )
        val givenTagName = "v1.0.321-googleDebug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest-googleDebug-vc321-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.log().last().id
        val expectedBuildNumber = "321"
        val expectedBuildVariant = "googleDebug"
        val expectedTagName = "v1.0.321-googleDebug"
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
                versionCode = "321",
                versionName = "v1.0.321-googleDebug",
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
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
    fun `build succeed with default tag when default config is used and tag not exists`() {
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
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest-googleDebug-vc1-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedTagBuildFile =
            Tag.Build(
                name = DEFAULT_TAG_NAME.format("googleDebug"),
                commitSha = DEFAULT_TAG_COMMIT_SHA,
                message = DEFAULT_TAG_COMMIT_MESSAGE,
                buildVersion = DEFAULT_BUILD_VERSION,
                buildVariant = "googleDebug",
                buildNumber = DEFAULT_VERSION_CODE,
            ).toJson()
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = DEFAULT_VERSION_CODE.toString(),
                versionName = DEFAULT_TAG_NAME.format("googleDebug"),
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
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
    fun `build succeed with default tag when default config is used, tag not exists and useVersionsFromTag is enabled`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                            useVersionsFromTag = true,
                        ),
                ),
        )
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest-googleDebug-vc1-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedTagBuildFile =
            Tag.Build(
                name = DEFAULT_TAG_NAME.format("googleDebug"),
                commitSha = DEFAULT_TAG_COMMIT_SHA,
                message = DEFAULT_TAG_COMMIT_MESSAGE,
                buildVersion = DEFAULT_BUILD_VERSION,
                buildVariant = "googleDebug",
                buildNumber = DEFAULT_VERSION_CODE,
            ).toJson()
        val expectedManifestProperties =
            ManifestProperties(
                versionCode = DEFAULT_VERSION_CODE.toString(),
                versionName = DEFAULT_TAG_NAME.format("googleDebug"),
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
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
    fun `build succeed with last tag when different tags in between of android formated tags is used`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("internal"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenTagName1 = "v0.0.206-internal"
        val givenTagName2 = "build/3.0.0-4088"

        val givenTagName3 = "v0.0.207-internal"
        val givenTagName4 = "v0.0.208-internal"
        val givenTagName5 = "v0.0.209-internal"

        val givenCommitMessage = "[CEB-1854] Fix invalid system bars background on web form screen"
        val givenAssembleTask = "assembleInternal"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-internal.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/internal/autotest-internal-vc209-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)
        git.tag.addNamed(givenTagName2)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        git.addAllAndCommit("[pfm] Update pager behaviour")
        git.tag.addNamed(givenTagName3)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        git.addAllAndCommit("[auth flow]: wrap chat flow component in remember for prevent recomposition")
        git.tag.addNamed(givenTagName4)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        git.addAllAndCommit("[CEB-1815] [ios] Открытие вклада. При переходе в форму с экрана продуктов")
        git.tag.addNamed(givenTagName5)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName5).id
        val expectedBuildNumber = "209"
        val expectedBuildVariant = "internal"
        val expectedTagName = "v0.0.209-internal"
        val expectedBuildVersion = "0.0"
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
                versionCode = "209",
                versionName = "v0.0.209-internal",
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagInternal"),
            "Task getLastTagInternal executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `build succeed with previous tag when postfix applied tags at the end of android formated tags is used`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("internal"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenTagName1 = "v1.0.0-release"

        val givenTagName2 = "v1.0.1-release"
        val givenTagName3 = "v1.0.2-release-androidAuto"

        val givenCommitMessage = "[CEB-1854] Fix invalid system bars background on web form screen"
        val givenAssembleTask = "assembleRelease"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-release.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/release/autotest-release-vc1-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        git.addAllAndCommit("[pfm] Update pager behaviour")
        git.tag.addNamed(givenTagName2)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        git.addAllAndCommit("[auth flow]: wrap chat flow component in remember for prevent recomposition")
        git.tag.addNamed(givenTagName3)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName2).id
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
                versionName = "v1.0.1-release",
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `build succeed with previous tag when postfix applied tags at the end of android formated tags is used, same commit`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("internal"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenTagName1 = "v1.0.0-release"

        val givenTagName2 = "v1.0.1-release"
        val givenTagName3 = "v1.0.2-release-androidAuto"

        val givenCommitMessage = "[CEB-1854] Fix invalid system bars background on web form screen"
        val givenAssembleTask = "assembleRelease"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-release.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/release/autotest-release-vc1-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        git.addAllAndCommit("[pfm] Update pager behaviour")
        git.tag.addNamed(givenTagName2)
        git.tag.addNamed(givenTagName3)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName2).id
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
                versionName = "v1.0.1-release",
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `build succeed with last tag when postfix applied tags at the end of android formated tags is used and custom pattern used`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("internal"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                            buildTagPatternBuilderFunctions =
                                listOf(
                                    "it.anyBeforeDot()",
                                    "it.buildVersion()",
                                    "it.separator(\"-\")",
                                    "it.buildVariantName()",
                                    "it.optionalSeparator(\"-\")",
                                    "it.anyOptionalSymbols()",
                                ),
                        ),
                ),
        )
        val givenTagName1 = "v1.0.0-release"

        val givenTagName2 = "v1.0.1-release"
        val givenTagName3 = "v1.0.2-release-androidAuto"

        val givenCommitMessage = "[CEB-1854] Fix invalid system bars background on web form screen"
        val givenAssembleTask = "assembleRelease"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-release.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/release/autotest-release-vc2-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        git.addAllAndCommit("[pfm] Update pager behaviour")
        git.tag.addNamed(givenTagName2)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        git.addAllAndCommit("[auth flow]: wrap chat flow component in remember for prevent recomposition")
        git.tag.addNamed(givenTagName3)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName3).id
        val expectedBuildNumber = "2"
        val expectedBuildVariant = "release"
        val expectedTagName = "v1.0.2-release-androidAuto"
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
                versionName = "v1.0.2-release-androidAuto",
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `build succeed with last tag when postfix applied tags at the end of android formated tags is used and custom pattern used, same commit`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("internal"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                            buildTagPatternBuilderFunctions =
                                listOf(
                                    "it.anyBeforeDot()",
                                    "it.buildVersion()",
                                    "it.separator(\"-\")",
                                    "it.buildVariantName()",
                                    "it.optionalSeparator(\"-\")",
                                    "it.anyOptionalSymbols()",
                                ),
                        ),
                ),
        )
        val givenTagName1 = "v1.0.0-release"

        val givenTagName2 = "v1.0.1-release"
        val givenTagName3 = "v1.0.2-release-androidAuto"

        val givenCommitMessage = "[CEB-1854] Fix invalid system bars background on web form screen"
        val givenAssembleTask = "assembleRelease"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-release.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/release/autotest-release-vc2-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        git.addAllAndCommit("[pfm] Update pager behaviour")
        git.tag.addNamed(givenTagName2)
        git.tag.addNamed(givenTagName3)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName3).id
        val expectedBuildNumber = "2"
        val expectedBuildVariant = "release"
        val expectedTagName = "v1.0.2-release-androidAuto"
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
                versionName = "v1.0.2-release-androidAuto",
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `build succeed with last tag when postfix applied tags in between of android formated tags is used and custom pattern used`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("internal"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                            buildTagPatternBuilderFunctions =
                                listOf(
                                    "it.anyBeforeDot()",
                                    "it.buildVersion()",
                                    "it.separator(\"-\")",
                                    "it.buildVariantName()",
                                    "it.optionalSeparator(\"-\")",
                                    "it.anyOptionalSymbols()",
                                ),
                        ),
                ),
        )
        val givenTagName1 = "v1.0.0-release"

        val givenTagName2 = "v1.0.1-release-androidAuto"
        val givenTagName3 = "v1.0.2-release"

        val givenCommitMessage = "[CEB-1854] Fix invalid system bars background on web form screen"
        val givenAssembleTask = "assembleRelease"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-release.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/release/autotest-release-vc2-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        git.addAllAndCommit("[pfm] Update pager behaviour")
        git.tag.addNamed(givenTagName2)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        git.addAllAndCommit("[auth flow]: wrap chat flow component in remember for prevent recomposition")
        git.tag.addNamed(givenTagName3)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName3).id
        val expectedBuildNumber = "2"
        val expectedBuildVariant = "release"
        val expectedTagName = "v1.0.2-release"
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
                versionName = "v1.0.2-release",
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `build succeed with last tag when postfix applied tags in between of android formated tags is used and custom pattern used, same commit`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("internal"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                            buildTagPatternBuilderFunctions =
                                listOf(
                                    "it.anyBeforeDot()",
                                    "it.buildVersion()",
                                    "it.separator(\"-\")",
                                    "it.buildVariantName()",
                                    "it.optionalSeparator(\"-\")",
                                    "it.anyOptionalSymbols()",
                                ),
                        ),
                ),
        )
        val givenTagName1 = "v1.0.0-release"

        val givenTagName2 = "v1.0.1-release-androidAuto"
        val givenTagName3 = "v1.0.2-release"

        val givenCommitMessage = "[CEB-1854] Fix invalid system bars background on web form screen"
        val givenAssembleTask = "assembleRelease"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-release.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/release/autotest-release-vc2-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        projectDir.getFile("app/README2.md").writeText("This is test project 2")

        git.addAllAndCommit("[pfm] Update pager behaviour")
        git.tag.addNamed(givenTagName2)
        git.tag.addNamed(givenTagName3)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName3).id
        val expectedBuildNumber = "2"
        val expectedBuildVariant = "release"
        val expectedTagName = "v1.0.2-release"
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
                versionName = "v1.0.2-release",
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `build succeed with last build number tag when different tags in between of android formated tags is used, different build version, correct build number order`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("internal"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenTagName1 = "v1.0.206-internal"
        val givenTagName2 = "build/3.0.0-4088"

        val givenTagName3 = "v3.0.207-internal"
        val givenTagName4 = "v2.0.208-internal"
        val givenTagName5 = "v0.0.209-internal"

        val givenCommitMessage = "[CEB-1854] Fix invalid system bars background on web form screen"
        val givenAssembleTask = "assembleInternal"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-internal.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/internal/autotest-internal-vc209-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)
        git.tag.addNamed(givenTagName2)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        git.addAllAndCommit("[pfm] Update pager behaviour")
        git.tag.addNamed(givenTagName3)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        git.addAllAndCommit("[auth flow]: wrap chat flow component in remember for prevent recomposition")
        git.tag.addNamed(givenTagName4)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        git.addAllAndCommit("[CEB-1815] [ios] Открытие вклада. При переходе в форму с экрана продуктов")
        git.tag.addNamed(givenTagName5)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName5).id
        val expectedBuildNumber = "209"
        val expectedBuildVariant = "internal"
        val expectedTagName = "v0.0.209-internal"
        val expectedBuildVersion = "0.0"
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
                versionCode = "209",
                versionName = "v0.0.209-internal",
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagInternal"),
            "Task getLastTagInternal executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `build succeed with last build number tag when different tags in between of android formated tags is used, different build version, correct build number order, same commit`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("internal"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenTagName1 = "v1.0.206-internal"
        val givenTagName2 = "build/3.0.0-4088"

        val givenTagName3 = "v3.0.207-internal"
        val givenTagName4 = "v2.0.208-internal"
        val givenTagName5 = "v0.0.209-internal"

        val givenCommitMessage = "[CEB-1854] Fix invalid system bars background on web form screen"
        val givenAssembleTask = "assembleInternal"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-internal.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/internal/autotest-internal-vc209-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)
        git.tag.addNamed(givenTagName2)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        git.addAllAndCommit("Update readme files")
        git.tag.addNamed(givenTagName3)
        git.tag.addNamed(givenTagName4)
        git.tag.addNamed(givenTagName5)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName5).id
        val expectedBuildNumber = "209"
        val expectedBuildVariant = "internal"
        val expectedTagName = "v0.0.209-internal"
        val expectedBuildVersion = "0.0"
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
                versionCode = "209",
                versionName = "v0.0.209-internal",
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagInternal"),
            "Task getLastTagInternal executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `build succeed with last build number tag when different tags in between of android formated tags is used, different build version, incorrect build number order, same commit`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("internal"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenTagName1 = "v1.0.206-internal"
        val givenTagName2 = "build/3.0.0-4088"

        val givenTagName3 = "v3.0.207-internal"
        val givenTagName4 = "v2.0.209-internal"
        val givenTagName5 = "v0.0.208-internal"

        val givenCommitMessage = "[CEB-1854] Fix invalid system bars background on web form screen"
        val givenAssembleTask = "assembleInternal"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-internal.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/internal/autotest-internal-vc209-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)
        git.tag.addNamed(givenTagName2)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        git.addAllAndCommit("Update readme files")
        git.tag.addNamed(givenTagName3)
        git.tag.addNamed(givenTagName4)
        git.tag.addNamed(givenTagName5)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName4).id
        val expectedBuildNumber = "209"
        val expectedBuildVariant = "internal"
        val expectedTagName = "v2.0.209-internal"
        val expectedBuildVersion = "2.0"
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
                versionCode = "209",
                versionName = "v2.0.209-internal",
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagInternal"),
            "Task getLastTagInternal executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `builds succeed one after another with last build number tag when different tags in between of android formated tags is used, different build version, incorrect build number order, same commit`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("internal"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenTagName1 = "v1.0.206-internal"
        val givenTagName2 = "build/3.0.0-4088"

        val givenTagName3 = "v3.0.207-internal"
        val givenTagName4 = "v2.0.208-internal"
        val givenTagName5 = "v0.0.209-internal"

        val givenCommitMessage = "[CEB-1854] Fix invalid system bars background on web form screen"
        val givenAssembleTask = "assembleInternal"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-internal.json")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)
        git.tag.addNamed(givenTagName2)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        projectDir.getFile("app/README3.md").writeText("This is test project 3")

        git.addAllAndCommit("Update readme files")
        git.tag.addNamed(givenTagName3)
        git.tag.addNamed(givenTagName4)

        val givenOutputFile1 = projectDir.getFile("app/build/outputs/apk/internal/autotest-internal-vc208-$currentDate.apk")
        val assemble1: BuildResult = projectDir.runTask(givenAssembleTask)
        val givenOutputFileManifestProperties1 = givenOutputFile1.extractManifestProperties()

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha1 = git.tag.find(givenTagName4).id
        val expectedBuildNumber1 = "208"
        val expectedBuildVariant1 = "internal"
        val expectedTagName1 = "v2.0.208-internal"
        val expectedBuildVersion1 = "2.0"
        val expectedTagBuildFile1 =
            Tag.Build(
                name = expectedTagName1,
                commitSha = expectedCommitSha1,
                message = "",
                buildVersion = expectedBuildVersion1,
                buildVariant = expectedBuildVariant1,
                buildNumber = expectedBuildNumber1.toInt(),
            ).toJson()
        val expectedManifestProperties1 =
            ManifestProperties(
                versionCode = "208",
                versionName = "v2.0.208-internal",
            )
        assertTrue(
            assemble1.output.contains("Task :app:getLastTagInternal"),
            "Task getLastTagInternal executed",
        )
        assertTrue(
            assemble1.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertEquals(
            expectedTagBuildFile1.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertTrue(givenOutputFile1.exists(), "Output file exists")
        assertTrue(givenOutputFile1.length() > 0, "Output file is empty")
        assertEquals(
            expectedManifestProperties1,
            givenOutputFileManifestProperties1,
            "Manifest properties equality",
        )

        git.tag.addNamed(givenTagName5)

        val givenOutputFile2 = projectDir.getFile("app/build/outputs/apk/internal/autotest-internal-vc209-$currentDate.apk")
        val assemble2: BuildResult = projectDir.runTask(givenAssembleTask)
        val givenOutputFileManifestProperties2 = givenOutputFile2.extractManifestProperties()

        val expectedCommitSha2 = git.tag.find(givenTagName5).id
        val expectedBuildNumber2 = "209"
        val expectedBuildVariant2 = "internal"
        val expectedTagName2 = "v0.0.209-internal"
        val expectedBuildVersion2 = "0.0"
        val expectedTagBuildFile2 =
            Tag.Build(
                name = expectedTagName2,
                commitSha = expectedCommitSha2,
                message = "",
                buildVersion = expectedBuildVersion2,
                buildVariant = expectedBuildVariant2,
                buildNumber = expectedBuildNumber2.toInt(),
            ).toJson()
        val expectedManifestProperties2 =
            ManifestProperties(
                versionCode = "209",
                versionName = "v0.0.209-internal",
            )
        assertTrue(
            assemble2.output.contains("Task :app:getLastTagInternal"),
            "Task getLastTagInternal executed",
        )
        assertTrue(
            assemble2.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertEquals(
            expectedTagBuildFile2.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
        assertTrue(givenOutputFile2.exists(), "Output file exists")
        assertTrue(givenOutputFile2.length() > 0, "Output file is empty")
        assertEquals(
            expectedManifestProperties2,
            givenOutputFileManifestProperties2,
            "Manifest properties equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `build failed when different tags in between of android formated tags is used, different build version, wrong build number order, different commits`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("internal"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenTagName1 = "v1.0.206-internal"
        val givenTagName2 = "build/3.0.0-4088"

        val givenTagName3 = "v1.0.207-internal"
        val givenTagName4 = "v1.0.209-internal"
        val givenTagName5 = "v1.0.208-internal"

        val givenCommitMessage = "[CEB-1854] Fix invalid system bars background on web form screen"
        val givenAssembleTask = "assembleInternal"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-internal.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/internal/autotest-internal-vc209-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)
        git.tag.addNamed(givenTagName2)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        git.addAllAndCommit("[pfm] Update pager behaviour")
        git.tag.addNamed(givenTagName3)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        git.addAllAndCommit("[auth flow]: wrap chat flow component in remember for prevent recomposition")
        git.tag.addNamed(givenTagName4)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        git.addAllAndCommit("[CEB-1815] [ios] Открытие вклада. При переходе в форму с экрана продуктов")
        git.tag.addNamed(givenTagName5)

        val result: BuildResult = projectDir.runTaskWithFail(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            result.output.contains("Task :app:getLastTagInternal"),
            "Task getLastTagInternal executed",
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
    fun `build failed when different tags in between of android formated tags is used, different build version, wrong build number, different commits with different datetime`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("internal"),
                    BuildType("release"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
        )
        val givenTagName1 = "v1.0.206-internal"
        val givenTagName2 = "build/3.0.0-4088"

        val givenTagName3 = "v1.0.207-internal"
        val givenTagName4 = "v1.0.209-internal"
        val givenTagName5 = "v1.0.208-internal"

        val givenCommitMessage = "[CEB-1854] Fix invalid system bars background on web form screen"
        val givenAssembleTask = "assembleInternal"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-internal.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/internal/autotest-internal-vc209-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)
        git.tag.addNamed(givenTagName2)
        Thread.sleep(1000L)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        git.addAllAndCommit("[pfm] Update pager behaviour")
        git.tag.addNamed(givenTagName3)
        Thread.sleep(1000L)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        git.addAllAndCommit("[auth flow]: wrap chat flow component in remember for prevent recomposition")
        git.tag.addNamed(givenTagName4)
        Thread.sleep(1000L)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        git.addAllAndCommit("[CEB-1815] [ios] Открытие вклада. При переходе в форму с экрана продуктов")
        git.tag.addNamed(givenTagName5)

        val result: BuildResult = projectDir.runTaskWithFail(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            result.output.contains("Task :app:getLastTagInternal"),
            "Task getLastTagInternal executed",
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
    fun `build succeed with default version when useVersionsFromTag is disabled and tag exists`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                            useVersionsFromTag = false,
                        ),
                ),
        )
        val givenTagName = "v1.0.321-googleDebug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedManifestProperties =
            ManifestProperties(
                versionCode = DEFAULT_VERSION_CODE.toString(),
                versionName = DEFAULT_VERSION_NAME,
            )
        assertTrue(
            !result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug not executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
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
    fun `build succeed with default version when useVersionsFromTag is disabled and tag not exists`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                            useVersionsFromTag = false,
                        ),
                ),
        )
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest.apk")

        git.addAllAndCommit(givenCommitMessage)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedManifestProperties =
            ManifestProperties(
                versionCode = DEFAULT_VERSION_CODE.toString(),
                versionName = DEFAULT_VERSION_NAME,
            )
        assertTrue(
            !result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug not executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
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
    fun `build failed with default version when useVersionsFromTag is enabled and useStubsForTagAsFallback is disabled and tag not exists`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                            useVersionsFromTag = true,
                            useStubsForTagAsFallback = false,
                        ),
                ),
        )
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest.apk")

        git.addAllAndCommit(givenCommitMessage)

        val result: BuildResult = projectDir.runTaskWithFail(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug executed",
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
    fun `build succeed with last version when useVersionsFromTag is enabled and useStubsForTagAsFallback is disabled and tag exists`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                            useVersionsFromTag = true,
                            useStubsForTagAsFallback = false,
                        ),
                ),
        )
        val givenTagName = "v1.0.321-googleDebug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest-googleDebug-vc321-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.log().last().id
        val expectedBuildNumber = "321"
        val expectedBuildVariant = "googleDebug"
        val expectedTagName = "v1.0.321-googleDebug"
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
                versionCode = "321",
                versionName = "v1.0.321-googleDebug",
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
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
    fun `build succeed with default version when useVersionsFromTag, useStubsForTagAsFallback are disabled and tag not exists`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                            useVersionsFromTag = false,
                            useStubsForTagAsFallback = false,
                        ),
                ),
        )
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest.apk")

        git.addAllAndCommit(givenCommitMessage)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedManifestProperties =
            ManifestProperties(
                versionCode = DEFAULT_VERSION_CODE.toString(),
                versionName = DEFAULT_VERSION_NAME,
            )
        assertTrue(
            !result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug not executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
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
    fun `build succeed with default version when useVersionsFromTag, useStubsForTagAsFallback are disabled and tag exists`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                            useVersionsFromTag = false,
                            useStubsForTagAsFallback = false,
                        ),
                ),
        )
        val givenTagName = "v1.0.321-googleDebug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedManifestProperties =
            ManifestProperties(
                versionCode = DEFAULT_VERSION_CODE.toString(),
                versionName = DEFAULT_VERSION_NAME,
            )
        assertTrue(
            !result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug not executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
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
    fun `build succeed with no versions when useVersionsFromTag, useStubsForTagAsFallback, useDefaultsForVersionsAsFallback are disabled and tag not exists`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            defaultConfig =
                DefaultConfig(
                    versionCode = null,
                    versionName = null,
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
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest.apk")

        git.addAllAndCommit(givenCommitMessage)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "",
                versionName = "",
            )
        assertTrue(
            !result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug not executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
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
    fun `build succeed with no versions when useVersionsFromTag, useStubsForTagAsFallback, useDefaultsForVersionsAsFallback are disabled and tag exists`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            defaultConfig =
                DefaultConfig(
                    versionCode = null,
                    versionName = null,
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
        val givenTagName = "v1.0.321-googleDebug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "",
                versionName = "",
            )
        assertTrue(
            !result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug not executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
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
    fun `build succeed with default config versions when useVersionsFromTag, useStubsForTagAsFallback, useDefaultsForVersionsAsFallback are disabled and tag not exists`() {
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
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest.apk")

        git.addAllAndCommit(givenCommitMessage)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "10",
                versionName = "v.1.0.10",
            )
        assertTrue(
            !result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug not executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
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
    fun `build succeed with default config versions when useVersionsFromTag, useStubsForTagAsFallback, useDefaultsForVersionsAsFallback are disabled and tag exists`() {
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
        val givenTagName = "v1.0.321-googleDebug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleGoogleDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-googleDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/google/debug/autotest.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "10",
                versionName = "v.1.0.10",
            )
        assertTrue(
            !result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug not executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed",
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
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
    fun `build succeed with used custom build tag pattern and tag exists`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "cabinet", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                            useVersionsFromTag = true,
                            buildTagPatternBuilderFunctions =
                                listOf(
                                    "it.literal(\"cabinet\")",
                                    "it.separator(\"+\")",
                                    "it.anyBeforeDot()",
                                    "it.buildVersion()",
                                    "it.separator(\"-\")",
                                    "it.buildVariantName()",
                                ),
                        ),
                ),
        )
        val givenTagName = "cabinet+v1.0.323-cabinetDebug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleCabinetDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-cabinetDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/cabinet/debug/autotest-cabinetDebug-vc323-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "323",
                versionName = "cabinet+v1.0.323-cabinetDebug",
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagCabinetDebug"),
            "Task getLastTagCabinetDebug executed",
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
    fun `build failed with used custom build tag pattern and tag exists, but used wrong literal pattern`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "cabinet", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                            useVersionsFromTag = true,
                            buildTagPatternBuilderFunctions =
                                listOf(
                                    "it.literal(\"(\")",
                                    "it.buildVersion()",
                                    "it.separator(\"-\")",
                                    "it.buildVariantName()",
                                ),
                        ),
                ),
        )
        val givenTagName = "cabinet+v1.0.323-cabinetDebug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleCabinetDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-cabinetDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/cabinet/debug/autotest-cabinetDebug-vc1-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTaskWithFail(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            !result.output.contains("Task :app:getLastTagCabinetDebug"),
            "Task getLastTagCabinetDebug not executed",
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
    fun `build failed with used custom build tag pattern and tag exists, but used pattern without buildVariantName`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "cabinet", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                            useVersionsFromTag = true,
                            buildTagPatternBuilderFunctions =
                                listOf(
                                    "it.literal(\"cabinet\")",
                                    "it.separator(\"+\")",
                                    "it.anyBeforeDot()",
                                    "it.buildVersion()",
                                    "it.separator(\"-\")",
                                ),
                        ),
                ),
        )
        val givenTagName = "cabinet+v1.0.323-cabinetDebug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleCabinetDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-cabinetDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/cabinet/debug/autotest-cabinetDebug-vc323-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTaskWithFail(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            !result.output.contains("Task :app:getLastTagCabinetDebug"),
            "Task getLastTagCabinetDebug not executed",
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
    fun `build failed with used custom build tag pattern and tag exists, but used pattern without buildVersion`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "cabinet", dimension = "default")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                            useVersionsFromTag = true,
                            buildTagPatternBuilderFunctions =
                                listOf(
                                    "it.literal(\"cabinet\")",
                                    "it.separator(\"+\")",
                                    "it.anyBeforeDot()",
                                    "it.separator(\"-\")",
                                    "it.buildVariantName()",
                                ),
                        ),
                ),
        )
        val givenTagName = "cabinet+v1.0.323-cabinetDebug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleCabinetDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-cabinetDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/cabinet/debug/autotest-cabinetDebug-vc323-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTaskWithFail(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            !result.output.contains("Task :app:getLastTagCabinetDebug"),
            "Task getLastTagCabinetDebug not executed",
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
    fun `build succeed with used custom build tag pattern and tag exists, but not last with common name`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("release"),
                ),
            productFlavors =
                listOf(
                    ProductFlavor(name = "cabinet", dimension = "default"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                            useVersionsFromTag = true,
                            buildTagPatternBuilderFunctions =
                                listOf(
                                    "it.literal(\"cabinet\")",
                                    "it.separator(\"+\")",
                                    "it.anyBeforeDot()",
                                    "it.buildVersion()",
                                    "it.separator(\"-\")",
                                    "it.buildVariantName()",
                                ),
                        ),
                ),
        )
        val givenTagName1 = "cabinet+v1.0.322-cabinetDebug"
        val givenTagName2 = "cabinet+v1.0.323-cabinetDebug"
        val givenTagName3 = "v1.0.323-cabinetDebug"
        val givenCommitMessage = "Initial commit"
        val given2CommitMessage = "Add README 1"
        val given3CommitMessage = "Add README 2"
        val givenAssembleTask = "assembleCabinetDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-cabinetDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/cabinet/debug/autotest-cabinetDebug-vc323-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README1.md").writeText("This is test project")
        git.addAllAndCommit(given2CommitMessage)
        git.tag.addNamed(givenTagName2)
        projectDir.getFile("app/README2.md").writeText("This is test project")
        git.addAllAndCommit(given3CommitMessage)
        git.tag.addNamed(givenTagName3)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "323",
                versionName = "cabinet+v1.0.323-cabinetDebug",
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagCabinetDebug"),
            "Task getLastTagCabinetDebug executed",
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
    fun `build succeed with used custom build tag pattern and tag exists, but not last with different flavor`() {
        projectDir.createAndroidProject(
            buildTypes =
                listOf(
                    BuildType("debug"),
                    BuildType("release"),
                ),
            productFlavors =
                listOf(
                    ProductFlavor(name = "cabinet", dimension = "default"),
                    ProductFlavor(name = "finance", dimension = "default"),
                ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                            useVersionsFromTag = true,
                            buildTagPatternBuilderFunctions =
                                listOf(
                                    "it.literal(\"cabinet\")",
                                    "it.separator(\"+\")",
                                    "it.anyBeforeDot()",
                                    "it.buildVersion()",
                                    "it.separator(\"-\")",
                                    "it.buildVariantName()",
                                ),
                        ),
                    buildTypeOutput =
                        "financeDebug" to
                            FoundationConfig.Output(
                                baseFileName = "autotest",
                                useVersionsFromTag = true,
                                buildTagPatternBuilderFunctions =
                                    listOf(
                                        "it.literal(\"finance\")",
                                        "it.separator(\"+\")",
                                        "it.anyBeforeDot()",
                                        "it.buildVersion()",
                                        "it.separator(\"-\")",
                                        "it.buildVariantName()",
                                    ),
                            ),
                ),
        )
        val givenTagName1 = "finance+v1.0.323-financeDebug"
        val givenTagName2 = "cabinet+v1.0.322-cabinetDebug"
        val givenTagName3 = "cabinet+v1.0.323-cabinetDebug"
        val givenCommitMessage = "Initial commit"
        val given2CommitMessage = "Add README 1"
        val given3CommitMessage = "Add README 2"
        val givenAssembleTask = "assembleFinanceDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-financeDebug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/finance/debug/autotest-financeDebug-vc323-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README1.md").writeText("This is test project")
        git.addAllAndCommit(given2CommitMessage)
        git.tag.addNamed(givenTagName2)
        projectDir.getFile("app/README2.md").writeText("This is test project")
        git.addAllAndCommit(given3CommitMessage)
        git.tag.addNamed(givenTagName3)

        val result: BuildResult = projectDir.runTask(givenAssembleTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedManifestProperties =
            ManifestProperties(
                versionCode = "323",
                versionName = "finance+v1.0.323-financeDebug",
            )
        assertTrue(
            result.output.contains("Task :app:getLastTagFinanceDebug"),
            "Task getLastTagFinanceDebug executed",
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
}
