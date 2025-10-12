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
import ru.kode.android.build.publish.plugin.foundation.utils.runTasks
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
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
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
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
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
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
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
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
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
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
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

    @Test
    @Throws(IOException::class)
    fun `build succeed with used custom build tag pattern and tag exists`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "cabinet", dimension = "default")),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                    useVersionsFromTag = true,
                    buildTagPatternBuilderFunctions = listOf(
                        "it.literal(\"cabinet\")",
                        "it.separator(\"+\")",
                        "it.anyBeforeDot()",
                        "it.buildVersion()",
                        "it.separator(\"-\")",
                        "it.buildVariantName()",
                    )
                )
            )
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

        val expectedManifestProperties = ManifestProperties(
            versionCode = "323",
            versionName = "cabinet+v1.0.323-cabinetDebug",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagCabinetDebug"),
            "Task getLastTagCabinetDebug executed"
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )
        assertTrue(givenTagBuildFile.exists(), "Tag file exists")
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
    fun `build failed with used custom build tag pattern and tag exists, but used wrong literal pattern`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "cabinet", dimension = "default")),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                    useVersionsFromTag = true,
                    buildTagPatternBuilderFunctions = listOf(
                        "it.literal(\"(\")",
                        "it.buildVersion()",
                        "it.separator(\"-\")",
                        "it.buildVariantName()",
                    )
                )
            )
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
            "Task getLastTagCabinetDebug not executed"
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
    fun `build failed with used custom build tag pattern and tag exists, but used pattern without buildVariantName`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "cabinet", dimension = "default")),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                    useVersionsFromTag = true,
                    buildTagPatternBuilderFunctions = listOf(
                        "it.literal(\"cabinet\")",
                        "it.separator(\"+\")",
                        "it.anyBeforeDot()",
                        "it.buildVersion()",
                        "it.separator(\"-\")",
                    )
                )
            )
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
            "Task getLastTagCabinetDebug not executed"
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
    fun `build failed with used custom build tag pattern and tag exists, but used pattern without buildVersion`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            productFlavors = listOf(ProductFlavor(name = "cabinet", dimension = "default")),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                    useVersionsFromTag = true,
                    buildTagPatternBuilderFunctions = listOf(
                        "it.literal(\"cabinet\")",
                        "it.separator(\"+\")",
                        "it.anyBeforeDot()",
                        "it.separator(\"-\")",
                        "it.buildVariantName()",
                    )
                )
            )
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
            "Task getLastTagCabinetDebug not executed"
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
    fun `build succeed with used custom build tag pattern and tag exists, but not last with common name`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"), BuildType("release"),
            ),
            productFlavors = listOf(
                ProductFlavor(name = "cabinet", dimension = "default"),
            ),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                    useVersionsFromTag = true,
                    buildTagPatternBuilderFunctions = listOf(
                        "it.literal(\"cabinet\")",
                        "it.separator(\"+\")",
                        "it.anyBeforeDot()",
                        "it.buildVersion()",
                        "it.separator(\"-\")",
                        "it.buildVariantName()",
                    )
                )
            )
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

        val expectedManifestProperties = ManifestProperties(
            versionCode = "323",
            versionName = "cabinet+v1.0.323-cabinetDebug",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagCabinetDebug"),
            "Task getLastTagCabinetDebug executed"
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )
        assertTrue(givenTagBuildFile.exists(), "Tag file exists")
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
    fun `build succeed with used custom build tag pattern and tag exists, but not last with different flavor`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"), BuildType("release"),
            ),
            productFlavors = listOf(
                ProductFlavor(name = "cabinet", dimension = "default"),
                ProductFlavor(name = "finance", dimension = "default"),
            ),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                    useVersionsFromTag = true,
                    buildTagPatternBuilderFunctions = listOf(
                        "it.literal(\"cabinet\")",
                        "it.separator(\"+\")",
                        "it.anyBeforeDot()",
                        "it.buildVersion()",
                        "it.separator(\"-\")",
                        "it.buildVariantName()",
                    )
                ),
                buildTypeOutput = "financeDebug" to FoundationConfig.Output(
                    baseFileName = "autotest",
                    useVersionsFromTag = true,
                    buildTagPatternBuilderFunctions = listOf(
                        "it.literal(\"finance\")",
                        "it.separator(\"+\")",
                        "it.anyBeforeDot()",
                        "it.buildVersion()",
                        "it.separator(\"-\")",
                        "it.buildVariantName()",
                    )
                ),
            )
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

        val expectedManifestProperties = ManifestProperties(
            versionCode = "323",
            versionName = "finance+v1.0.323-financeDebug",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagFinanceDebug"),
            "Task getLastTagFinanceDebug executed"
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )
        assertTrue(givenTagBuildFile.exists(), "Tag file exists")
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
    fun `generate changelog after assemble if all commits exists and formed using message key with one tag`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"), BuildType("release"),
            ),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                ),
                changelog = FoundationConfig.Changelog(
                    issueNumberPattern = "TICKET-\\\\d+",
                    issueUrlPrefix = "https://jira.example.com/browse/",
                    commitMessageKey = "CHANGELOG",
                )
            )
        )

        val givenTagName = "v1.0.0-debug"
        val givenAssembleTask = "assembleDebug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc0-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "CHANGELOG: [auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName)

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val changelogResult: BuildResult = projectDir.runTask(givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName).id
        val expectedBuildNumber = "0"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.0-debug"
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
            versionName = "v1.0.0-debug",
        )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed"
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )
        assertTrue(
            changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed"
        )
        assertTrue(
            changelogResult.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed"
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )

        val expectedChangelogFile = """
            • (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов
            • [auth flow]: wrap chat flow component in remember for prevent recomposition
            • Update pager behaviour
        """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality"
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality"
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
    fun `generate changelog after assemble simultaneously if all commits exists and formed using message key with one tag`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"), BuildType("release"),
            ),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                ),
                changelog = FoundationConfig.Changelog(
                    issueNumberPattern = "TICKET-\\\\d+",
                    issueUrlPrefix = "https://jira.example.com/browse/",
                    commitMessageKey = "CHANGELOG",
                )
            )
        )

        val givenTagName = "v1.0.0-debug"
        val givenAssembleTask = "assembleDebug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc0-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "CHANGELOG: [auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTasks(givenAssembleTask, givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName).id
        val expectedBuildNumber = "0"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.0-debug"
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
            versionName = "v1.0.0-debug",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed"
        )
        assertTrue(
            result.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed"
        )
        assertEquals(
            result.output
                .split("\n")
                .filter {
                    it.contains("Task :app:getLastTagDebug")
                        || it.contains("Task :app:generateChangelogDebug")
                }
                .size,
            2,
            "Each task executed without duplications"
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )

        val expectedChangelogFile = """
            • (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов
            • [auth flow]: wrap chat flow component in remember for prevent recomposition
            • Update pager behaviour
        """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality"
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality"
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality"
        )
    }

    // TODO: Should we trigger assemble automatically before getLastTag?
    @Test
    @Throws(IOException::class)
    fun `generate changelog without assemble if all commits exists and formed using message key with one tag`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"), BuildType("release"),
            ),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                ),
                changelog = FoundationConfig.Changelog(
                    issueNumberPattern = "TICKET-\\\\d+",
                    issueUrlPrefix = "https://jira.example.com/browse/",
                    commitMessageKey = "CHANGELOG",
                )
            )
        )

        val givenTagName = "v1.0.0-debug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc0-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "CHANGELOG: [auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName)

        val changelogResult: BuildResult = projectDir.runTask(givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha = git.tag.find(givenTagName).id
        val expectedBuildNumber = "0"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.0-debug"
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
            changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed"
        )
        assertTrue(
            changelogResult.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed"
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )

        val expectedChangelogFile = """
            • (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов
            • [auth flow]: wrap chat flow component in remember for prevent recomposition
            • Update pager behaviour
        """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality"
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality"
        )
        assertTrue(!givenOutputFile.exists(), "Output file not exists")
    }

    @Test
    @Throws(IOException::class)
    fun `generate changelog after assemble if all commits exists and partially formed using message key with one tag`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"), BuildType("release"),
            ),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                ),
                changelog = FoundationConfig.Changelog(
                    issueNumberPattern = "TICKET-\\\\d+",
                    issueUrlPrefix = "https://jira.example.com/browse/",
                    commitMessageKey = "CHANGELOG",
                )
            )
        )

        val givenTagName = "v1.0.0-debug"
        val givenAssembleTask = "assembleDebug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc0-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "[auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName)

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val changelogResult: BuildResult = projectDir.runTask(givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName).id
        val expectedBuildNumber = "0"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.0-debug"
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
            versionName = "v1.0.0-debug",
        )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed"
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )
        assertTrue(
            changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed"
        )
        assertTrue(
            changelogResult.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed"
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )

        val expectedChangelogFile = """
            • (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов
            • Update pager behaviour
        """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality"
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality"
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
    fun `generate changelog after assemble simultaneously if all commits exists and partially formed using message key with one tag`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"), BuildType("release"),
            ),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                ),
                changelog = FoundationConfig.Changelog(
                    issueNumberPattern = "TICKET-\\\\d+",
                    issueUrlPrefix = "https://jira.example.com/browse/",
                    commitMessageKey = "CHANGELOG",
                )
            )
        )

        val givenTagName = "v1.0.0-debug"
        val givenAssembleTask = "assembleDebug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc0-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "[auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTasks(givenAssembleTask, givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName).id
        val expectedBuildNumber = "0"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.0-debug"
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
            versionName = "v1.0.0-debug",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed"
        )
        assertTrue(
            result.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed"
        )
        assertEquals(
            result.output
                .split("\n")
                .filter {
                    it.contains("Task :app:getLastTagDebug")
                        || it.contains("Task :app:generateChangelogDebug")
                }
                .size,
            2,
            "Each task executed without duplications"
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )

        val expectedChangelogFile = """
            • (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов
            • Update pager behaviour
        """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality"
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality"
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality"
        )
    }

    // TODO: Should we trigger assemble automatically before getLastTag?
    @Test
    @Throws(IOException::class)
    fun `generate changelog without assemble if all commits exists and partially formed using message key with one tag`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"), BuildType("release"),
            ),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                ),
                changelog = FoundationConfig.Changelog(
                    issueNumberPattern = "TICKET-\\\\d+",
                    issueUrlPrefix = "https://jira.example.com/browse/",
                    commitMessageKey = "CHANGELOG",
                )
            )
        )

        val givenTagName = "v1.0.0-debug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc0-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "[auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName)

        val changelogResult: BuildResult = projectDir.runTask(givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha = git.tag.find(givenTagName).id
        val expectedBuildNumber = "0"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.0-debug"
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
            changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed"
        )
        assertTrue(
            changelogResult.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed"
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )

        val expectedChangelogFile = """
            • (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов
            • Update pager behaviour
        """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality"
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality"
        )
        assertTrue(!givenOutputFile.exists(), "Output file not exists")
    }

    // TODO

    @Test
    @Throws(IOException::class)
    fun `generate changelog after assemble if all commits exists and formed using message key with 2 tags`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"), BuildType("release"),
            ),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                ),
                changelog = FoundationConfig.Changelog(
                    issueNumberPattern = "TICKET-\\\\d+",
                    issueUrlPrefix = "https://jira.example.com/browse/",
                    commitMessageKey = "CHANGELOG",
                )
            )
        )

        val givenTagName1 = "v1.0.0-debug"
        val givenTagName2 = "v1.0.1-debug"
        val givenAssembleTask = "assembleDebug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc1-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "CHANGELOG: [auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName1)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage4 = "CHANGELOG: Add readme file"
        git.addAllAndCommit(changelogMessage4)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage5 = "CHANGELOG: [profile flow]: Add Compose screen with VM logic"
        git.addAllAndCommit(changelogMessage5)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage6 = "CHANGELOG: (TICKET-1815) [android] Add logic to open accounts"
        git.addAllAndCommit(changelogMessage6)
        git.tag.addNamed(givenTagName2)


        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val changelogResult: BuildResult = projectDir.runTask(givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName2).id
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
        val expectedManifestProperties = ManifestProperties(
            versionCode = "1",
            versionName = "v1.0.1-debug",
        )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed"
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )
        assertTrue(
            changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed"
        )
        assertTrue(
            changelogResult.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed"
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )

        val expectedChangelogFile = """
            • (TICKET-1815) [android] Add logic to open accounts
            • [profile flow]: Add Compose screen with VM logic
            • Add readme file
        """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality"
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality"
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
    fun `generate changelog after assemble simultaneously if all commits exists and formed using message key with 2 tags`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"), BuildType("release"),
            ),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                ),
                changelog = FoundationConfig.Changelog(
                    issueNumberPattern = "TICKET-\\\\d+",
                    issueUrlPrefix = "https://jira.example.com/browse/",
                    commitMessageKey = "CHANGELOG",
                )
            )
        )

        val givenTagName1 = "v1.0.0-debug"
        val givenTagName2 = "v1.0.1-debug"
        val givenAssembleTask = "assembleDebug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc1-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "CHANGELOG: [auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName1)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage4 = "CHANGELOG: Add readme file"
        git.addAllAndCommit(changelogMessage4)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage5 = "CHANGELOG: [profile flow]: Add Compose screen with VM logic"
        git.addAllAndCommit(changelogMessage5)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage6 = "CHANGELOG: (TICKET-1815) [android] Add logic to open accounts"
        git.addAllAndCommit(changelogMessage6)
        git.tag.addNamed(givenTagName2)

        val result: BuildResult = projectDir.runTasks(givenAssembleTask, givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName2).id
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
        val expectedManifestProperties = ManifestProperties(
            versionCode = "1",
            versionName = "v1.0.1-debug",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed"
        )
        assertTrue(
            result.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed"
        )
        assertEquals(
            result.output
                .split("\n")
                .filter {
                    it.contains("Task :app:getLastTagDebug")
                        || it.contains("Task :app:generateChangelogDebug")
                }
                .size,
            2,
            "Each task executed without duplications"
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )

        val expectedChangelogFile = """
            • (TICKET-1815) [android] Add logic to open accounts
            • [profile flow]: Add Compose screen with VM logic
            • Add readme file
        """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality"
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality"
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality"
        )
    }

    // TODO: Should we trigger assemble automatically before getLastTag?
    @Test
    @Throws(IOException::class)
    fun `generate changelog without assemble if all commits exists and formed using message key with 2 tags`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"), BuildType("release"),
            ),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                ),
                changelog = FoundationConfig.Changelog(
                    issueNumberPattern = "TICKET-\\\\d+",
                    issueUrlPrefix = "https://jira.example.com/browse/",
                    commitMessageKey = "CHANGELOG",
                )
            )
        )

        val givenTagName1 = "v1.0.0-debug"
        val givenTagName2 = "v1.0.1-debug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc1-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "CHANGELOG: [auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName1)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage4 = "CHANGELOG: Add readme file"
        git.addAllAndCommit(changelogMessage4)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage5 = "CHANGELOG: [profile flow]: Add Compose screen with VM logic"
        git.addAllAndCommit(changelogMessage5)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage6 = "CHANGELOG: (TICKET-1815) [android] Add logic to open accounts"
        git.addAllAndCommit(changelogMessage6)
        git.tag.addNamed(givenTagName2)

        val changelogResult: BuildResult = projectDir.runTask(givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha = git.tag.find(givenTagName2).id
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

        assertTrue(
            changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed"
        )
        assertTrue(
            changelogResult.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed"
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )

        val expectedChangelogFile = """
            • (TICKET-1815) [android] Add logic to open accounts
            • [profile flow]: Add Compose screen with VM logic
            • Add readme file
        """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality"
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality"
        )
        assertTrue(!givenOutputFile.exists(), "Output file not exists")
    }

    @Test
    @Throws(IOException::class)
    fun `generate changelog after assemble if all commits exists and partially formed using message key with 2 tags`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"), BuildType("release"),
            ),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                ),
                changelog = FoundationConfig.Changelog(
                    issueNumberPattern = "TICKET-\\\\d+",
                    issueUrlPrefix = "https://jira.example.com/browse/",
                    commitMessageKey = "CHANGELOG",
                )
            )
        )

        val givenTagName1 = "v1.0.0-debug"
        val givenTagName2 = "v1.0.1-debug"
        val givenAssembleTask = "assembleDebug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc1-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "[auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName1)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage4 = "CHANGELOG: Add readme file"
        git.addAllAndCommit(changelogMessage4)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage5 = "[profile flow]: Add Compose screen with VM logic"
        git.addAllAndCommit(changelogMessage5)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage6 = "CHANGELOG: (TICKET-1815) [android] Add logic to open accounts"
        git.addAllAndCommit(changelogMessage6)
        git.tag.addNamed(givenTagName2)

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val changelogResult: BuildResult = projectDir.runTask(givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName2).id
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
        val expectedManifestProperties = ManifestProperties(
            versionCode = "1",
            versionName = "v1.0.1-debug",
        )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed"
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )
        assertTrue(
            changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed"
        )
        assertTrue(
            changelogResult.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed"
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )

        val expectedChangelogFile = """
            • (TICKET-1815) [android] Add logic to open accounts
            • Add readme file
        """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality"
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality"
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
    fun `generate changelog after assemble simultaneously if all commits exists and partially formed using message key with 2 tags`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"), BuildType("release"),
            ),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                ),
                changelog = FoundationConfig.Changelog(
                    issueNumberPattern = "TICKET-\\\\d+",
                    issueUrlPrefix = "https://jira.example.com/browse/",
                    commitMessageKey = "CHANGELOG",
                )
            )
        )

        val givenTagName1 = "v1.0.0-debug"
        val givenTagName2 = "v1.0.1-debug"
        val givenAssembleTask = "assembleDebug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc1-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "[auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName1)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage4 = "CHANGELOG: Add readme file"
        git.addAllAndCommit(changelogMessage4)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage5 = "[profile flow]: Add Compose screen with VM logic"
        git.addAllAndCommit(changelogMessage5)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage6 = "CHANGELOG: (TICKET-1815) [android] Add logic to open accounts"
        git.addAllAndCommit(changelogMessage6)
        git.tag.addNamed(givenTagName2)

        val result: BuildResult = projectDir.runTasks(givenAssembleTask, givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val givenOutputFileManifestProperties = givenOutputFile.extractManifestProperties()

        val expectedCommitSha = git.tag.find(givenTagName2).id
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
        val expectedManifestProperties = ManifestProperties(
            versionCode = "1",
            versionName = "v1.0.1-debug",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed"
        )
        assertTrue(
            result.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed"
        )
        assertEquals(
            result.output
                .split("\n")
                .filter {
                    it.contains("Task :app:getLastTagDebug")
                        || it.contains("Task :app:generateChangelogDebug")
                }
                .size,
            2,
            "Each task executed without duplications"
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )

        val expectedChangelogFile = """
            • (TICKET-1815) [android] Add logic to open accounts
            • Add readme file
        """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality"
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality"
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
        assertTrue(givenOutputFile.length() > 0, "Output file is not empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Manifest properties equality"
        )
    }

    // TODO: Should we trigger assemble automatically before getLastTag?
    @Test
    @Throws(IOException::class)
    fun `generate changelog without assemble if all commits exists and partially formed using message key with 2 tags`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"), BuildType("release"),
            ),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "autotest",
                ),
                changelog = FoundationConfig.Changelog(
                    issueNumberPattern = "TICKET-\\\\d+",
                    issueUrlPrefix = "https://jira.example.com/browse/",
                    commitMessageKey = "CHANGELOG",
                )
            )
        )

        val givenTagName1 = "v1.0.0-debug"
        val givenTagName2 = "v1.0.1-debug"
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc1-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage1 = "CHANGELOG: Update pager behaviour"
        git.addAllAndCommit(changelogMessage1)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage2 = "[auth flow]: wrap chat flow component in remember for prevent recomposition"
        git.addAllAndCommit(changelogMessage2)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage3 = "CHANGELOG: (TICKET-1815) [ios] Открытие вклада. При переходе в форму с экрана продуктов"
        git.addAllAndCommit(changelogMessage3)
        git.tag.addNamed(givenTagName1)

        projectDir.getFile("app/README.md").writeText("This is test project 1")
        val changelogMessage4 = "CHANGELOG: Add readme file"
        git.addAllAndCommit(changelogMessage4)

        projectDir.getFile("app/README2.md").writeText("This is test project 2")
        val changelogMessage5 = "[profile flow]: Add Compose screen with VM logic"
        git.addAllAndCommit(changelogMessage5)

        projectDir.getFile("app/README3.md").writeText("This is test project 3")
        val changelogMessage6 = "CHANGELOG: (TICKET-1815) [android] Add logic to open accounts"
        git.addAllAndCommit(changelogMessage6)
        git.tag.addNamed(givenTagName2)

        val changelogResult: BuildResult = projectDir.runTask(givenChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha = git.tag.find(givenTagName2).id
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

        assertTrue(
            changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed"
        )
        assertTrue(
            changelogResult.output.contains("Task :app:generateChangelogDebug"),
            "Task generateChangelogDebug executed"
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeed"
        )

        val expectedChangelogFile = """
            • (TICKET-1815) [android] Add logic to open accounts
            • Add readme file
        """.trimIndent()

        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality"
        )
        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality"
        )
        assertTrue(!givenOutputFile.exists(), "Output file not exists")
    }

}
