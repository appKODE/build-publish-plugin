package ru.kode.android.build.publish.plugin.foundation

import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.git.mapper.toJson
import ru.kode.android.build.publish.plugin.foundation.utils.BuildType
import ru.kode.android.build.publish.plugin.foundation.utils.FoundationConfig
import ru.kode.android.build.publish.plugin.foundation.utils.ProductFlavor
import ru.kode.android.build.publish.plugin.foundation.utils.addAllAndCommit
import ru.kode.android.build.publish.plugin.foundation.utils.addNamed
import ru.kode.android.build.publish.plugin.foundation.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.foundation.utils.getFile
import ru.kode.android.build.publish.plugin.foundation.utils.initGit
import ru.kode.android.build.publish.plugin.foundation.utils.runTask
import ru.kode.android.build.publish.plugin.foundation.utils.extractManifestProperties
import ru.kode.android.build.publish.plugin.foundation.utils.ManifestProperties
import ru.kode.android.build.publish.plugin.foundation.utils.currentDate
import ru.kode.android.build.publish.plugin.foundation.utils.printFilesRecursively
import java.io.File
import java.io.IOException
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_VERSION_CODE
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_VERSION_NAME

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
    fun `assembles file with defined which starts from baseFileName and ends with tag and date`() {
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
            "Task getLastTagGoogleDebug should be executed"
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build failed"
        )
        assertEquals(
            givenTagBuildFile.readText(),
            expectedTagBuildFile.trimMargin(),
            "Wrong tag found"
        )
        assertTrue(givenOutputFile.exists(), "Output file not found")
        assertTrue(givenOutputFile.length() > 0, "Output file is empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Wrong manifest properties"
        )
    }

    @Test
    @Throws(IOException::class)
    fun `assembles file with defined which starts from baseFileName and not ends with tag and date`() {
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
        assertFalse(
            result.output.contains("Task :app:getLastTagGoogleDebug"),
            "Task getLastTagGoogleDebug should be executed"
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build failed"
        )
        assertFalse(givenTagBuildFile.exists(), "Tag file found, but it should not be created")
        assertTrue(givenOutputFile.exists(), "Output file not found")
        assertTrue(givenOutputFile.length() > 0, "Output file is empty")
        assertEquals(
            expectedManifestProperties,
            givenOutputFileManifestProperties,
            "Wrong manifest properties"
        )
    }
}
