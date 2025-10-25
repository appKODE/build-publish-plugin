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
import ru.kode.android.build.publish.plugin.foundation.utils.addAllAndCommit
import ru.kode.android.build.publish.plugin.foundation.utils.addNamed
import ru.kode.android.build.publish.plugin.foundation.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.foundation.utils.getFile
import ru.kode.android.build.publish.plugin.foundation.utils.initGit
import ru.kode.android.build.publish.plugin.foundation.utils.printFilesRecursively
import ru.kode.android.build.publish.plugin.foundation.utils.runTask
import java.io.File
import java.io.IOException

class PrintLastIncreasedTagTest {
    @TempDir
    lateinit var tempDir: File
    private lateinit var projectDir: File

    @BeforeEach
    fun setup() {
        projectDir = File(tempDir, "test-project")
    }

    @Test
    @Throws(IOException::class)
    fun `print last increased tag with all same numbers`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenTagName = "v1.1.1-debug"
        val givenCommitMessage = "Initial commit"
        val givenGetLastTagTask = "printLastIncreasedTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTask(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha = git.log().last().id
        val expectedBuildNumber = "1"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.1.1-debug"
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
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded"
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed"
        )
        assertTrue(
            result.output.contains("v1.1.2-debug"),
            "Contains increased build number tag"
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality"
        )
    }

    @Test
    @Throws(IOException::class)
    fun `print last increased tag with two same numbers at the end`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenTagName = "v0.1.1-debug"
        val givenCommitMessage = "Initial commit"
        val givenGetLastTagTask = "printLastIncreasedTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTask(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha = git.log().last().id
        val expectedBuildNumber = "1"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v0.1.1-debug"
        val expectedBuildVersion = "0.1"
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
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded"
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed"
        )
        assertTrue(
            result.output.contains("v0.1.2-debug"),
            "Contains increased build number tag"
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality"
        )
    }

    @Test
    @Throws(IOException::class)
    fun `print last increased tag with two same numbers at start and end`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenTagName = "v1.0.1-debug"
        val givenCommitMessage = "Initial commit"
        val givenGetLastTagTask = "printLastIncreasedTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTask(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

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
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded"
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed"
        )
        assertTrue(
            result.output.contains("v1.0.2-debug"),
            "Contains increased build number tag"
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality"
        )
    }

    @Test
    @Throws(IOException::class)
    fun `print last increased tag with one number at the end`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenTagName = "v0.0.1-debug"
        val givenCommitMessage = "Initial commit"
        val givenGetLastTagTask = "printLastIncreasedTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTask(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha = git.log().last().id
        val expectedBuildNumber = "1"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v0.0.1-debug"
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
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded"
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed"
        )
        assertTrue(
            result.output.contains("v0.0.2-debug"),
            "Contains increased build number tag"
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality"
        )
    }

    @Test
    @Throws(IOException::class)
    fun `print last increased tag with one number at the end with patch in version`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenTagName = "v0.0.1.1-debug"
        val givenCommitMessage = "Initial commit"
        val givenGetLastTagTask = "printLastIncreasedTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTask(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha = git.log().last().id
        val expectedBuildNumber = "1"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v0.0.1.1-debug"
        val expectedBuildVersion = "0.0.1"
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
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded"
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed"
        )
        assertTrue(
            result.output.contains("v0.0.1.2-debug"),
            "Contains increased build number tag"
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality"
        )
    }
}
