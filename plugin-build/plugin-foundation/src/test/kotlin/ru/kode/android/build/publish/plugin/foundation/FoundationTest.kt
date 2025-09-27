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
import ru.kode.android.build.publish.plugin.foundation.utils.BuildType
import ru.kode.android.build.publish.plugin.foundation.utils.FoundationConfig
import ru.kode.android.build.publish.plugin.foundation.utils.DefaultConfig
import ru.kode.android.build.publish.plugin.foundation.utils.ProductFlavor
import ru.kode.android.build.publish.plugin.foundation.utils.addAllAndCommit
import ru.kode.android.build.publish.plugin.foundation.utils.addNamed
import ru.kode.android.build.publish.plugin.foundation.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.foundation.utils.getFile
import ru.kode.android.build.publish.plugin.foundation.utils.initGit
import ru.kode.android.build.publish.plugin.foundation.utils.runTask
import ru.kode.android.build.publish.plugin.foundation.utils.runTaskWithFail
import ru.kode.android.build.publish.plugin.foundation.utils.extractManifestProperties
import ru.kode.android.build.publish.plugin.foundation.utils.ManifestProperties
import ru.kode.android.build.publish.plugin.foundation.utils.currentDate
import ru.kode.android.build.publish.plugin.foundation.utils.printFilesRecursively
import java.io.File
import java.io.IOException
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_VERSION_CODE
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_VERSION_NAME
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_TAG_NAME
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_TAG_COMMIT_SHA
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_TAG_COMMIT_MESSAGE
import ru.kode.android.build.publish.plugin.foundation.utils.find

class FoundationTest {
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
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                )
            )
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
        val expectedManifestProperties = ManifestProperties(
            versionCode = "321",
            versionName = "v1.0.321-googleDebug",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug executed"
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )
        assertEquals(
            givenTagBuildFile.readText(),
            expectedTagBuildFile.trimMargin(),
            "Tags equality"
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality"
        )
    }

    @Test
    @Throws(IOException::class)
    fun `build succeed with last tag when default config is used, tag exists and useVersionsFromTag is enabled`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                    useVersionsFromTag = true
                )
            )
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
        val expectedManifestProperties = ManifestProperties(
            versionCode = "321",
            versionName = "v1.0.321-googleDebug",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug executed"
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )
        assertEquals(
            givenTagBuildFile.readText(),
            expectedTagBuildFile.trimMargin(),
            "Tags equality"
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality"
        )
    }

    @Test
    @Throws(IOException::class)
    fun `build succeed with default tag when default config is used and tag not exists`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                )
            )
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
        val expectedManifestProperties = ManifestProperties(
            versionCode = DEFAULT_VERSION_CODE.toString(),
            versionName = DEFAULT_TAG_NAME.format("googleDebug"),
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug executed"
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )
        assertEquals(
            givenTagBuildFile.readText(),
            expectedTagBuildFile.trimMargin(),
            "Tags equality"
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality"
        )
    }

    @Test
    @Throws(IOException::class)
    fun `build succeed with default tag when default config is used, tag not exists and useVersionsFromTag is enabled`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                    useVersionsFromTag = true
                )
            )
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
        val expectedManifestProperties = ManifestProperties(
            versionCode = DEFAULT_VERSION_CODE.toString(),
            versionName = DEFAULT_TAG_NAME.format("googleDebug"),
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug executed"
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )
        assertEquals(
            givenTagBuildFile.readText(),
            expectedTagBuildFile.trimMargin(),
            "Tags equality"
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality"
        )
    }

    @Test
    @Throws(IOException::class)
    fun `build succeed with last tag when different tags in between of android formated tags is used`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"),
                BuildType("internal"),
                BuildType("release")
            ),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                )
            )
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
        val expectedManifestProperties = ManifestProperties(
            versionCode = "209",
            versionName = "v0.0.209-internal",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagInternal"),
            "Task getLastTagInternal executed"
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )
        assertEquals(
            givenTagBuildFile.readText(),
            expectedTagBuildFile.trimMargin(),
            "Tags equality"
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality"
        )
    }

    @Test
    @Throws(IOException::class)
    fun `build succeed with default version when useVersionsFromTag is disabled and tag exists`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                    useVersionsFromTag = false
                )
            )
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

        val expectedManifestProperties = ManifestProperties(
            versionCode = DEFAULT_VERSION_CODE.toString(),
            versionName = DEFAULT_VERSION_NAME,
        )
        assertTrue(
           !result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug not executed"
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality"
        )
    }

    @Test
    @Throws(IOException::class)
    fun `build succeed with default version when useVersionsFromTag is disabled and tag not exists`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                    useVersionsFromTag = false
                )
            )
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

        val expectedManifestProperties = ManifestProperties(
            versionCode = DEFAULT_VERSION_CODE.toString(),
            versionName = DEFAULT_VERSION_NAME,
        )
        assertTrue(
            !result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug not executed"
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality"
        )
    }

    @Test
    @Throws(IOException::class)
    fun `build failed with default version when useVersionsFromTag is enabled and useStubsForTagAsFallback is disabled and tag not exists`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                    useVersionsFromTag = true,
                    useStubsForTagAsFallback = false
                )
            )
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
            "Task getLastTagGoogleDebug executed"
        )
        assertTrue(
            result.output.contains("BUILD FAILED"),
            "Build failed"
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
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                    useVersionsFromTag = true,
                    useStubsForTagAsFallback = false
                )
            )
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
        val expectedManifestProperties = ManifestProperties(
            versionCode = "321",
            versionName = "v1.0.321-googleDebug",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug executed"
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )
        assertEquals(
            givenTagBuildFile.readText(),
            expectedTagBuildFile.trimMargin(),
            "Tags equality"
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality"
        )
    }

    @Test
    @Throws(IOException::class)
    fun `build succeed with default version when useVersionsFromTag, useStubsForTagAsFallback are disabled and tag not exists`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                    useVersionsFromTag = false,
                    useStubsForTagAsFallback = false
                )
            )
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

        val expectedManifestProperties = ManifestProperties(
            versionCode = DEFAULT_VERSION_CODE.toString(),
            versionName = DEFAULT_VERSION_NAME,
        )
        assertTrue(
            !result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug not executed"
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality"
        )
    }

    @Test
    @Throws(IOException::class)
    fun `build succeed with default version when useVersionsFromTag, useStubsForTagAsFallback are disabled and tag exists`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                    useVersionsFromTag = false,
                    useStubsForTagAsFallback = false
                )
            )
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

        val expectedManifestProperties = ManifestProperties(
            versionCode = DEFAULT_VERSION_CODE.toString(),
            versionName = DEFAULT_VERSION_NAME,
        )
        assertTrue(
            !result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug not executed"
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality"
        )
    }

    @Test
    @Throws(IOException::class)
    fun `build succeed with no versions when useVersionsFromTag, useStubsForTagAsFallback, useDefaultsForVersionsAsFallback are disabled and tag not exists`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            defaultConfig = DefaultConfig(
                versionCode = null,
                versionName = null
            ),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                    useVersionsFromTag = false,
                    useStubsForTagAsFallback = false,
                    useDefaultsForVersionsAsFallback = false
                )
            )
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

        val expectedManifestProperties = ManifestProperties(
            versionCode = "",
            versionName = "",
        )
        assertTrue(
            !result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug not executed"
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality"
        )
    }

    @Test
    @Throws(IOException::class)
    fun `build succeed with no versions when useVersionsFromTag, useStubsForTagAsFallback, useDefaultsForVersionsAsFallback are disabled and tag exists`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            defaultConfig = DefaultConfig(
                versionCode = null,
                versionName = null
            ),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                    useVersionsFromTag = false,
                    useStubsForTagAsFallback = false,
                    useDefaultsForVersionsAsFallback = false
                )
            )
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

        val expectedManifestProperties = ManifestProperties(
            versionCode = "",
            versionName = "",
        )
        assertTrue(
            !result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug not executed"
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality"
        )
    }

    @Test
    @Throws(IOException::class)
    fun `build succeed with default config versions when useVersionsFromTag, useStubsForTagAsFallback, useDefaultsForVersionsAsFallback are disabled and tag not exists`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            defaultConfig = DefaultConfig(
                versionCode = 10,
                versionName = "v.1.0.10"
            ),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                    useVersionsFromTag = false,
                    useStubsForTagAsFallback = false,
                    useDefaultsForVersionsAsFallback = false
                )
            )
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

        val expectedManifestProperties = ManifestProperties(
            versionCode = "10",
            versionName = "v.1.0.10",
        )
        assertTrue(
            !result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug not executed"
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality"
        )
    }

    @Test
    @Throws(IOException::class)
    fun `build succeed with default config versions when useVersionsFromTag, useStubsForTagAsFallback, useDefaultsForVersionsAsFallback are disabled and tag exists`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "google", dimension = "default")),
            defaultConfig = DefaultConfig(
                versionCode = 10,
                versionName = "v.1.0.10"
            ),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                    useVersionsFromTag = false,
                    useStubsForTagAsFallback = false,
                    useDefaultsForVersionsAsFallback = false
                )
            )
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

        val expectedManifestProperties = ManifestProperties(
            versionCode = "10",
            versionName = "v.1.0.10",
        )
        assertTrue(
            !result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug not executed"
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality"
        )
    }

}
