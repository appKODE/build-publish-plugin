package ru.kode.android.build.publish.plugin.foundation

import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.git.mapper.toJson
import ru.kode.android.build.publish.plugin.foundation.utils.addAll
import ru.kode.android.build.publish.plugin.foundation.utils.addNamed
import ru.kode.android.build.publish.plugin.foundation.utils.runTask
import ru.kode.android.build.publish.plugin.foundation.utils.commit
import ru.kode.android.build.publish.plugin.foundation.utils.find
import ru.kode.android.build.publish.plugin.foundation.utils.initGit
import ru.kode.android.build.publish.plugin.foundation.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.foundation.utils.getFile
import java.io.File
import java.io.IOException

class GetLastTagFunctionalTest {
    @TempDir
    lateinit var tempDir: File
    private lateinit var projectDir: File

    @BeforeEach
    fun setup() {
        projectDir = File(tempDir, "test-project")
    }

    @Test
    @Throws(IOException::class)
    fun `creates tag file of debug build from one tag`() {
        projectDir.createAndroidProject()
        val givenTagName = "v1.0.0-debug"
        val givenCommitMessage = "Initial commit"
        val givenGetLastTagTask = "getLastTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAll()
        git.commit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val result: BuildResult = projectDir.runTask(givenGetLastTagTask)

        val expectedCommitSha = git.log().last().id
        val expectedBuildNumber = "0"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.0-debug"
        val expectedBuildVersion = "1.0"
        val expectedResult =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = "",
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()
        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
        assertEquals(givenTagBuildFile.readText(), expectedResult.trimMargin())
    }

    @Test
    @Throws(IOException::class)
    fun `creates tag file of release build from two tags with same VC`() {
        projectDir.createAndroidProject()
        val givenTagNameDebug = "v1.0.0-debug"
        val givenTagNameRelease = "v1.0.0-release"
        val givenCommitMessage = "Initial commit"
        val givenGetLastTagTaskRelease = "getLastTagRelease"
        val givenGetLastTagTaskDebug = "getLastTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-release.json")

        git.addAll()
        git.commit(givenCommitMessage)
        git.tag.addNamed(givenTagNameDebug)
        git.tag.addNamed(givenTagNameRelease)

        val releaseResult: BuildResult = projectDir.runTask(givenGetLastTagTaskRelease)

        projectDir.runTask(givenGetLastTagTaskDebug)

        val expectedCommitSha = git.log().last().id
        val expectedBuildNumber = "0"
        val expectedBuildVariant = "release"
        val expectedTagName = "v1.0.0-release"
        val expectedBuildVersion = "1.0"
        val expectedResult =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = "",
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()
        assertTrue(releaseResult.output.contains("BUILD SUCCESSFUL"))
        assertEquals(givenTagBuildFile.readText(), expectedResult.trimMargin())
    }

    @Test
    @Throws(IOException::class)
    fun `creates tag file of release build from two tags with different VC`() {
        projectDir.createAndroidProject()
        val givenTagNameDebug = "v1.0.1-debug"
        val givenTagNameRelease = "v1.0.0-release"
        val givenCommitMessage = "Initial commit"
        val givenGetLastTagTaskRelease = "getLastTagRelease"
        val givenGetLastTagTaskDebug = "getLastTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-release.json")

        git.addAll()
        git.commit(givenCommitMessage)
        git.tag.addNamed(givenTagNameDebug)
        git.tag.addNamed(givenTagNameRelease)

        val releaseResult: BuildResult = projectDir.runTask(givenGetLastTagTaskRelease)

        projectDir.runTask(givenGetLastTagTaskDebug)

        val expectedCommitSha = git.log().last().id
        val expectedBuildNumber = "0"
        val expectedBuildVariant = "release"
        val expectedTagName = "v1.0.0-release"
        val expectedBuildVersion = "1.0"
        val expectedResult =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = "",
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()
        assertTrue(releaseResult.output.contains("BUILD SUCCESSFUL"))
        assertEquals(givenTagBuildFile.readText(), expectedResult.trimMargin())
    }

    @Test
    @Throws(IOException::class)
    fun `creates tag file of debug build from two tags with different VC, where first number is equal`() {
        projectDir.createAndroidProject()
        val given1TagNameDebug = "v1.0.1-debug"
        val given2TagNameDebug = "v1.0.99-debug"
        val given3TagNameDebug = "v1.0.100-debug"
        val givenCommitMessage = "Initial commit"
        val givenGetLastTagTask = "getLastTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAll()
        git.commit(givenCommitMessage)
        git.tag.addNamed(given1TagNameDebug)
        git.tag.addNamed(given2TagNameDebug)
        git.tag.addNamed(given3TagNameDebug)

        val releaseResult: BuildResult = projectDir.runTask(givenGetLastTagTask)

        val expectedBuildNumber = "100"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.100-debug"
        val expectedCommitSha = git.tag.find(expectedTagName).id
        val expectedBuildVersion = "1.0"
        val expectedResult =
            Tag.Build(
                name = expectedTagName,
                commitSha = expectedCommitSha,
                message = "",
                buildVersion = expectedBuildVersion,
                buildVariant = expectedBuildVariant,
                buildNumber = expectedBuildNumber.toInt(),
            ).toJson()
        assertTrue(releaseResult.output.contains("BUILD SUCCESSFUL"))
        assertEquals(givenTagBuildFile.readText(), expectedResult.trimMargin())
    }
}
