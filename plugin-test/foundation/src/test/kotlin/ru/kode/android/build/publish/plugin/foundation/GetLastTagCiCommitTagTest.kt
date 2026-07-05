package ru.kode.android.build.publish.plugin.foundation

import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.core.entity.BuildTagSnapshot
import ru.kode.android.build.publish.plugin.core.entity.Tag
import ru.kode.android.build.publish.plugin.core.git.mapper.toJson
import ru.kode.android.build.publish.plugin.test.utils.BuildType
import ru.kode.android.build.publish.plugin.test.utils.addAllAndCommit
import ru.kode.android.build.publish.plugin.test.utils.addNamed
import ru.kode.android.build.publish.plugin.test.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.findTag
import ru.kode.android.build.publish.plugin.test.utils.getFile
import ru.kode.android.build.publish.plugin.test.utils.initGit
import ru.kode.android.build.publish.plugin.test.utils.outputShouldContain
import ru.kode.android.build.publish.plugin.test.utils.runTask
import java.io.File
import java.io.IOException

class GetLastTagCiCommitTagTest {
    @TempDir
    lateinit var tempDir: File
    private lateinit var projectDir: File

    @BeforeEach
    fun setup() {
        projectDir = File(tempDir, "test-project")
    }

    @Test
    @Throws(IOException::class)
    fun `selects lower tag via CI_COMMIT_TAG when multiple tags on same commit`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenFirstTagName = "v1.0.1-debug"
        val givenSecondTagName = "v1.0.2-debug"
        val givenCommitMessage = "Initial commit"
        val givenGetLastTagTask = "getLastTagSnapshotDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-snapshot-debug.json")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult =
            projectDir.runTask(
                givenGetLastTagTask,
                environment = mapOf("CI_COMMIT_TAG" to givenFirstTagName),
            )

        // v1.0.1-debug is at the end of the sorted list [v1.0.2, v1.0.1],
        // so previousInOrder is null (no older tag exists)
        val expectedTagBuildFile =
            BuildTagSnapshot(
                current =
                    Tag.Build(
                        name = "v1.0.1-debug",
                        commitSha = git.tag.findTag(givenFirstTagName).id,
                        message = "",
                        buildVersion = "1.0",
                        buildVariant = "debug",
                        buildNumber = 1,
                    ),
                previousInOrder = null,
                previousOnDifferentCommit = null,
            ).toJson()
        result.outputShouldContain("BUILD SUCCESSFUL")
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `selects higher tag via CI_COMMIT_TAG when multiple tags on same commit`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenFirstTagName = "v1.0.1-debug"
        val givenSecondTagName = "v1.0.2-debug"
        val givenCommitMessage = "Initial commit"
        val givenGetLastTagTask = "getLastTagSnapshotDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-snapshot-debug.json")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult =
            projectDir.runTask(
                givenGetLastTagTask,
                environment = mapOf("CI_COMMIT_TAG" to givenSecondTagName),
            )

        val expectedTagBuildFile =
            BuildTagSnapshot(
                current =
                    Tag.Build(
                        name = "v1.0.2-debug",
                        commitSha = git.tag.findTag(givenSecondTagName).id,
                        message = "",
                        buildVersion = "1.0",
                        buildVariant = "debug",
                        buildNumber = 2,
                    ),
                previousInOrder =
                    Tag.Build(
                        name = "v1.0.1-debug",
                        commitSha = git.tag.findTag(givenFirstTagName).id,
                        message = "",
                        buildVersion = "1.0",
                        buildVariant = "debug",
                        buildNumber = 1,
                    ),
                previousOnDifferentCommit = null,
            ).toJson()
        result.outputShouldContain("BUILD SUCCESSFUL")
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `falls back to default sorting when CI_COMMIT_TAG is not set`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenFirstTagName = "v1.0.1-debug"
        val givenSecondTagName = "v1.0.2-debug"
        val givenCommitMessage = "Initial commit"
        val givenGetLastTagTask = "getLastTagSnapshotDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-snapshot-debug.json")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult = projectDir.runTask(givenGetLastTagTask)

        val expectedTagBuildFile =
            BuildTagSnapshot(
                current =
                    Tag.Build(
                        name = "v1.0.2-debug",
                        commitSha = git.tag.findTag(givenSecondTagName).id,
                        message = "",
                        buildVersion = "1.0",
                        buildVariant = "debug",
                        buildNumber = 2,
                    ),
                previousInOrder =
                    Tag.Build(
                        name = "v1.0.1-debug",
                        commitSha = git.tag.findTag(givenFirstTagName).id,
                        message = "",
                        buildVersion = "1.0",
                        buildVariant = "debug",
                        buildNumber = 1,
                    ),
                previousOnDifferentCommit = null,
            ).toJson()
        result.outputShouldContain("BUILD SUCCESSFUL")
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `ignores CI_COMMIT_TAG when it does not match build tag regex`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenFirstTagName = "v1.0.1-debug"
        val givenSecondTagName = "v1.0.2-debug"
        val givenCommitMessage = "Initial commit"
        val givenGetLastTagTask = "getLastTagSnapshotDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-snapshot-debug.json")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        git.tag.addNamed(givenSecondTagName)

        // CI_COMMIT_TAG is for a different variant (release), should be ignored for debug task
        val result: BuildResult =
            projectDir.runTask(
                givenGetLastTagTask,
                environment = mapOf("CI_COMMIT_TAG" to "v1.0.1-release"),
            )

        val expectedTagBuildFile =
            BuildTagSnapshot(
                current =
                    Tag.Build(
                        name = "v1.0.2-debug",
                        commitSha = git.tag.findTag(givenSecondTagName).id,
                        message = "",
                        buildVersion = "1.0",
                        buildVariant = "debug",
                        buildNumber = 2,
                    ),
                previousInOrder =
                    Tag.Build(
                        name = "v1.0.1-debug",
                        commitSha = git.tag.findTag(givenFirstTagName).id,
                        message = "",
                        buildVersion = "1.0",
                        buildVariant = "debug",
                        buildNumber = 1,
                    ),
                previousOnDifferentCommit = null,
            ).toJson()
        result.outputShouldContain("BUILD SUCCESSFUL")
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
    }
}
