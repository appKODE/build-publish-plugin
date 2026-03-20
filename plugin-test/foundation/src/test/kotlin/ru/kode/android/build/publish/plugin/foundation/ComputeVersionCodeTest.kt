package ru.kode.android.build.publish.plugin.foundation

import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.core.strategy.DEFAULT_VERSION_CODE
import ru.kode.android.build.publish.plugin.test.utils.BuildType
import ru.kode.android.build.publish.plugin.test.utils.FoundationConfig
import ru.kode.android.build.publish.plugin.test.utils.addAllAndCommit
import ru.kode.android.build.publish.plugin.test.utils.addNamed
import ru.kode.android.build.publish.plugin.test.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.extractManifestProperties
import ru.kode.android.build.publish.plugin.test.utils.getFile
import ru.kode.android.build.publish.plugin.test.utils.initGit
import ru.kode.android.build.publish.plugin.test.utils.printFilesRecursively
import ru.kode.android.build.publish.plugin.test.utils.runTask
import java.io.File
import java.io.IOException

class ComputeVersionCodeTest {
    @TempDir
    lateinit var tempDir: File
    private lateinit var projectDir: File

    @BeforeEach
    fun setup() {
        projectDir = File(tempDir, "test-project")
    }

    @Test
    @Throws(IOException::class)
    fun `SemanticVersionFlattenedCodeStrategy works with 2-part version`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "test-app",
                    versionCodeStrategy = "ru.kode.android.build.publish.plugin.core.strategy.SemanticVersionFlattenedCodeStrategy.INSTANCE",
                ),
            ),
        )
        val givenTagName = "v1.2.3-release"
        val git = projectDir.initGit()

        git.addAllAndCommit("Initial commit")
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTask("assembleRelease")

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/release")
        val apkFile = apkDir.listFiles()
            ?.first { it.name.endsWith(".apk") }
            ?: throw AssertionError("APK file not found")
        val manifestProperties = apkFile.extractManifestProperties()

        // v1.2.3-release -> buildVersion="1.2", buildNumber=3
        // (1 * 1000 + 2) * 1000 + 3 = 1002003
        val expectedVersionCode = ((1 * 1000 + 2) * 1000 + 3).toString()

        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded",
        )
        assertEquals(
            expectedVersionCode,
            manifestProperties.versionCode,
            "Version code should be computed as (major * 1000 + minor) * 1000 + buildNumber",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `SemanticVersionFlattenedCodeStrategy works with 3-part version`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "test-app",
                    versionCodeStrategy = "ru.kode.android.build.publish.plugin.core.strategy.SemanticVersionFlattenedCodeStrategy.INSTANCE",
                ),
            ),
        )
        val givenTagName = "v4.0.0.100-release"
        val git = projectDir.initGit()

        git.addAllAndCommit("Initial commit")
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTask("assembleRelease")

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/release")
        val apkFile = apkDir.listFiles()
            ?.first { it.name.endsWith(".apk") }
            ?: throw AssertionError("APK file not found")
        val manifestProperties = apkFile.extractManifestProperties()

        // v4.0.0.100-release -> buildVersion="4.0.0", buildNumber=100
        // (4 * 1000 + 0) * 1000 + 100 = 4000100
        val expectedVersionCode = ((4 * 1000 + 0) * 1000 + 100).toString()

        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded",
        )
        assertEquals(
            expectedVersionCode,
            manifestProperties.versionCode,
            "Version code should be computed as (major * 1000 + minor) * 1000 + buildNumber for 3-part version",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `SemanticVersionFlattenedCodeStrategy works with 3-part version and non-zero minor`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "test-app",
                    versionCodeStrategy = "ru.kode.android.build.publish.plugin.core.strategy.SemanticVersionFlattenedCodeStrategy.INSTANCE",
                ),
            ),
        )
        val givenTagName = "v2.5.1.42-release"
        val git = projectDir.initGit()

        git.addAllAndCommit("Initial commit")
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTask("assembleRelease")

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/release")
        val apkFile = apkDir.listFiles()
            ?.first { it.name.endsWith(".apk") }
            ?: throw AssertionError("APK file not found")
        val manifestProperties = apkFile.extractManifestProperties()

        // v2.5.1.42-release -> buildVersion="2.5.1", buildNumber=42
        // (2 * 1000 + 5) * 1000 + 42 = 2005042
        val expectedVersionCode = ((2 * 1000 + 5) * 1000 + 42).toString()

        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded",
        )
        assertEquals(
            expectedVersionCode,
            manifestProperties.versionCode,
            "Version code should be computed as (major * 1000 + minor) * 1000 + buildNumber for 3-part version",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `SemanticVersionFlattenedCodeStrategy falls back to default when no tag`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig = FoundationConfig(
                output = FoundationConfig.Output(
                    baseFileName = "test-app",
                    useVersionsFromTag = true,
                    useDefaultsForVersionsAsFallback = true,
                    versionCodeStrategy = "ru.kode.android.build.publish.plugin.core.strategy.SemanticVersionFlattenedCodeStrategy.INSTANCE",
                ),
            ),
        )
        val git = projectDir.initGit()

        git.addAllAndCommit("Initial commit")
        // No tag created

        val result: BuildResult = projectDir.runTask("assembleRelease")

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/release")
        val apkFile = apkDir.listFiles()
            ?.first { it.name.endsWith(".apk") }
            ?: throw AssertionError("APK file not found")
        val manifestProperties = apkFile.extractManifestProperties()

        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded",
        )
        assertEquals(
            DEFAULT_VERSION_CODE.toString(),
            manifestProperties.versionCode,
            "Version code should fall back to DEFAULT_VERSION_CODE when no tag exists",
        )
    }
}
