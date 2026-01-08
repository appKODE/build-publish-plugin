package ru.kode.android.build.publish.plugin.foundation

import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.git.mapper.toJson
import ru.kode.android.build.publish.plugin.core.messages.finTagsByRegexAfterSortingMessage
import ru.kode.android.build.publish.plugin.core.strategy.DEFAULT_TAG_PATTERN
import ru.kode.android.build.publish.plugin.core.util.getBuildNumber
import ru.kode.android.build.publish.plugin.test.utils.BuildType
import ru.kode.android.build.publish.plugin.test.utils.addAllAndCommit
import ru.kode.android.build.publish.plugin.test.utils.addAnnotated
import ru.kode.android.build.publish.plugin.test.utils.addNamed
import ru.kode.android.build.publish.plugin.test.utils.checkoutCommitDetached
import ru.kode.android.build.publish.plugin.test.utils.commitAmend
import ru.kode.android.build.publish.plugin.test.utils.commitWithDate
import ru.kode.android.build.publish.plugin.test.utils.createAndSwitchBranch
import ru.kode.android.build.publish.plugin.test.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.currentBranch
import ru.kode.android.build.publish.plugin.test.utils.findTag
import ru.kode.android.build.publish.plugin.test.utils.getFile
import ru.kode.android.build.publish.plugin.test.utils.initGit
import ru.kode.android.build.publish.plugin.test.utils.printFilesRecursively
import ru.kode.android.build.publish.plugin.test.utils.runTask
import ru.kode.android.build.publish.plugin.test.utils.runTaskWithFail
import ru.kode.android.build.publish.plugin.test.utils.switchBranch
import java.io.File
import java.io.IOException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.text.format

class GetLastTagBuildTypesTest {
    @TempDir
    lateinit var tempDir: File
    private lateinit var projectDir: File

    @BeforeEach
    fun setup() {
        projectDir = File(tempDir, "test-project")
    }

    @Test
    @Throws(IOException::class)
    fun `creates tag file of debug build from one tag, one commit, build types only`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenTagName = "v1.0.1-debug"
        val givenCommitMessage = "Initial commit"
        val givenGetLastTagTask = "getLastTagDebug"
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
            "Build succeeded",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `creates tag file of debug build from one tag, one commit, build types only, different branches`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenTagName3 = "v1.0.3-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 = "Update readme 1"
        val givenCommitMessage3 = "Update readme 2"
        val givenGetLastTagTask = "getLastTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        git.createAndSwitchBranch("feature")
        assertEquals(git.currentBranch(), "feature")
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)
        git.switchBranch("master")
        assertEquals(git.currentBranch(), "master")
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage3)
        git.tag.addNamed(givenTagName3)

        val result: BuildResult = projectDir.runTask(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha = git.tag.findTag(givenTagName3).id
        val expectedBuildNumber = "3"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.3-debug"
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
            "Build succeeded",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `creates tag file of debug build from one tag, one commit, build types only, amend first commit with branch switch`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenTagName3 = "v1.0.3-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 = "Update readme 1"
        val givenCommitMessage3 = "Update readme 2"
        val givenGetLastTagTask = "getLastTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        git.commitAmend("$givenCommitMessage1 amend")
        git.createAndSwitchBranch("feature")
        assertEquals(git.currentBranch(), "feature")
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)
        git.switchBranch("master")
        assertEquals(git.currentBranch(), "master")
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage3)
        git.tag.addNamed(givenTagName3)

        val result: BuildResult = projectDir.runTask(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha = git.tag.findTag(givenTagName3).id
        val expectedBuildNumber = "3"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.3-debug"
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
            "Build succeeded",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `creates tag file of debug build from one tag, one commit, build types only, amend first commit without branch switch`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenTagName3 = "v1.0.3-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 = "Update readme 1"
        val givenCommitMessage3 = "Update readme 2"
        val givenGetLastTagTask = "getLastTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        assertEquals(git.currentBranch(), "master")

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        git.commitAmend("$givenCommitMessage1 amend")
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage3)
        git.tag.addNamed(givenTagName3)

        val result: BuildResult = projectDir.runTask(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha = git.tag.findTag(givenTagName3).id
        val expectedBuildNumber = "3"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.3-debug"
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
            "Build succeeded",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `creates tag file of debug build from one tag, one commit, build types only, amend second commit with branch switch`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenTagName3 = "v1.0.3-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 = "Update readme 1"
        val givenCommitMessage3 = "Update readme 2"
        val givenGetLastTagTask = "getLastTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        git.createAndSwitchBranch("feature")
        assertEquals(git.currentBranch(), "feature")
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)
        git.commitAmend("$givenCommitMessage2 amend")
        git.switchBranch("master")
        assertEquals(git.currentBranch(), "master")
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage3)
        git.tag.addNamed(givenTagName3)

        val result: BuildResult = projectDir.runTask(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha = git.tag.findTag(givenTagName3).id
        val expectedBuildNumber = "3"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.3-debug"
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
            "Build succeeded",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `creates tag file of debug build from one tag, one commit, build types only, amend second commit without branch switch`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenTagName3 = "v1.0.3-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 = "Update readme 1"
        val givenCommitMessage3 = "Update readme 2"
        val givenGetLastTagTask = "getLastTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        assertEquals(git.currentBranch(), "master")

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)
        git.commitAmend("$givenCommitMessage2 amend")
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage3)
        git.tag.addNamed(givenTagName3)

        val result: BuildResult = projectDir.runTask(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha = git.tag.findTag(givenTagName3).id
        val expectedBuildNumber = "3"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.3-debug"
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
            "Build succeeded",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `creates tag file of debug build from one tag, one commit, build types only, amend last commit with branch switch`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenTagName3 = "v1.0.3-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 = "Update readme 1"
        val givenCommitMessage3 = "Update readme 2"
        val givenGetLastTagTask = "getLastTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        git.createAndSwitchBranch("feature")
        assertEquals(git.currentBranch(), "feature")
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)
        git.switchBranch("master")
        assertEquals(git.currentBranch(), "master")
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage3)
        git.tag.addNamed(givenTagName3)
        git.commitAmend("$givenCommitMessage3 amend")

        val result: BuildResult = projectDir.runTask(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha = git.tag.findTag(givenTagName3).id
        val expectedBuildNumber = "3"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.3-debug"
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
            "Build succeeded",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `creates tag file of debug build from one tag, one commit, build types only, amend last commit without branch switch`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenTagName3 = "v1.0.3-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 = "Update readme 1"
        val givenCommitMessage3 = "Update readme 2"
        val givenGetLastTagTask = "getLastTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        assertEquals(git.currentBranch(), "master")

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage3)
        git.tag.addNamed(givenTagName3)
        git.commitAmend("$givenCommitMessage3 amend")

        val result: BuildResult = projectDir.runTask(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha = git.tag.findTag(givenTagName3).id
        val expectedBuildNumber = "3"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.3-debug"
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
            "Build succeeded",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `replays full git history with a lot of tags and selects latest valid tag by expected sorting`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"),
                BuildType("internal"),
                BuildType("release"),
            ),
        )

        val git = projectDir.initGit()
        val actualTagBuildFile = projectDir.getFile("app/build/tag-build-internal.json")

        git.addAllAndCommit("initial commit")

        val logLines =
            javaClass.classLoader
                .getResourceAsStream("git/tag-diagnose.log")
                ?.bufferedReader()
                ?.readLines()
                ?: error("tag-diagnose.log not found")

        val tagEvents = parseHistory(logLines)
        val commitMap = mutableMapOf<String, String>()

        tagEvents
            .sortedBy { it.createdAt.toInstant().toEpochMilli() }
            .forEachIndexed { index, tag ->
                val recreatedCommitSha = commitMap.getOrPut(tag.commitSha) {
                    git.commitWithDate(
                        message = "replay commit ${tag.commitSha.take(7)}",
                        date = tag.createdAt.toInstant(),
                    ).id
                }

                git.checkoutCommitDetached(recreatedCommitSha)

                when (tag.type) {
                    TagType.LIGHTWEIGHT ->
                        git.tag.addNamed(tag.name)

                    TagType.ANNOTATED ->
                        git.tag.addAnnotated(tag.name)
                }

                if (tag.isBroken) {
                    git.commitAmend("rewrite after ${tag.name}")
                }
            }

        val result = projectDir.runTask("getLastTagInternal")

        val buildTagRegex = DEFAULT_TAG_PATTERN.format("internal").toRegex()

        result.output.contains(
            finTagsByRegexAfterSortingMessage(
                git.tag.list().sortedByDescending { it.name.getBuildNumber(buildTagRegex) }
            )
        )

        assertTrue(result.output.contains("BUILD SUCCESSFUL"))

        val expectedTagName = tagEvents
            .maxBy { it.name.getBuildNumber(buildTagRegex) }
            .name

        val expectedTagBuildFile =
            Tag.Build(
                name = expectedTagName,
                commitSha = git.tag.findTag(expectedTagName).id,
                message = "",
                buildVersion = expectedTagName.substringAfter("v").substringBeforeLast("."),
                buildVariant = "internal",
                buildNumber = expectedTagName.substringAfterLast(".").substringBeforeLast("-").toInt(),
            ).toJson()

        assertEquals(expectedTagBuildFile, actualTagBuildFile.readText())
    }

    @Test
    @Throws(IOException::class)
    fun `creates tag file of debug build from multiple tags, different commits, build types only`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenFirstTagName = "v1.0.1-debug"
        val givenSecondTagName = "v1.0.2-debug"
        val givenFirstCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README"
        val givenGetLastTagTask = "getLastTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenSecondCommitMessage)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult = projectDir.runTask(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha = git.tag.findTag(givenSecondTagName).id
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
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `creates tag file of debug build from multiple tags, same commit, build types only`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenFirstTagName = "v1.0.1-debug"
        val givenSecondTagName = "v1.0.2-debug"
        val givenFirstCommitMessage = "Initial commit"
        val givenGetLastTagTask = "getLastTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult = projectDir.runTask(givenGetLastTagTask)

        val expectedCommitSha = git.tag.findTag(givenSecondTagName).id
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
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `not creates tag file of debug build from multiple tags, different version, same VC, different commits, build types only`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenFirstTagName = "v1.0.1-debug"
        val givenSecondTagName = "v1.2.1-debug"
        val givenFirstCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README"
        val givenGetLastTagTask = "getLastTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenSecondCommitMessage)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult = projectDir.runTaskWithFail(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            result.output.contains("BUILD FAILED"),
            "Build failed",
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
    }

    @Test
    @Throws(IOException::class)
    fun `not creates tag file of debug build from multiple tags, different version, same VC, same commit, build types only`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenFirstTagName = "v1.0.1-debug"
        val givenSecondTagName = "v1.2.1-debug"
        val givenFirstCommitMessage = "Initial commit"
        val givenGetLastTagTask = "getLastTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult = projectDir.runTaskWithFail(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            result.output.contains("BUILD FAILED"),
            "Build failed",
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
    }

    @Test
    @Throws(IOException::class)
    fun `not creates tag file of debug build from multiple tags, different version, same VC, wrong order, different commits, build types only`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenFirstTagName = "v1.2.1-debug"
        val givenSecondTagName = "v1.0.1-debug"
        val givenFirstCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README"
        val givenGetLastTagTask = "getLastTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenSecondCommitMessage)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult = projectDir.runTaskWithFail(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            result.output.contains("BUILD FAILED"),
            "Build failed",
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
    }

    @Test
    @Throws(IOException::class)
    fun `not creates tag file of debug build from multiple tags, different version, same VC, wrong order, same commit, build types only`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenFirstTagName = "v1.2.1-debug"
        val givenSecondTagName = "v1.0.1-debug"
        val givenFirstCommitMessage = "Initial commit"
        val givenGetLastTagTask = "getLastTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult = projectDir.runTaskWithFail(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            result.output.contains("BUILD FAILED"),
            "Build failed",
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not found")
    }

    @Test
    @Throws(IOException::class)
    fun `creates tag file of debug build from multiple tags with wrong order, different commits, build types only`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenFirstTagName = "v1.0.2-debug"
        val givenSecondTagName = "v1.0.1-debug"
        val givenFirstCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README"
        val givenGetLastTagTask = "getLastTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenFirstTagName)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenSecondCommitMessage)
        git.tag.addNamed(givenSecondTagName)

        val result: BuildResult = projectDir.runTaskWithFail(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            result.output.contains("BUILD FAILED"),
            "Build failed",
        )
        assertTrue(!givenTagBuildFile.exists(), "Tag file not exists")
    }

    @Test
    @Throws(IOException::class)
    fun `creates tag file of release build from two tags with same version, same commit, build types only`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenTagNameDebug = "v1.0.1-debug"
        val givenTagNameRelease = "v1.0.1-release"
        val givenCommitMessage = "Initial commit"
        val givenGetLastTagTaskRelease = "getLastTagRelease"
        val givenGetLastTagTaskDebug = "getLastTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-release.json")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagNameDebug)
        git.tag.addNamed(givenTagNameRelease)

        val releaseResult: BuildResult = projectDir.runTask(givenGetLastTagTaskRelease)
        projectDir.runTask(givenGetLastTagTaskDebug)

        projectDir.getFile("app").printFilesRecursively()

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
        assertTrue(
            releaseResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `creates tag file of release build from two tags with same version, different commits, build types only`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenTagNameDebug = "v1.0.1-debug"
        val givenTagNameRelease = "v1.0.1-release"
        val givenFirstCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README"
        val givenGetLastTagTaskRelease = "getLastTagRelease"
        val givenGetLastTagTaskDebug = "getLastTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-release.json")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenTagNameDebug)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenSecondCommitMessage)
        git.tag.addNamed(givenTagNameRelease)

        val releaseResult: BuildResult = projectDir.runTask(givenGetLastTagTaskRelease)
        projectDir.runTask(givenGetLastTagTaskDebug)

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha = git.tag.findTag(givenTagNameRelease).id
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
        assertTrue(
            releaseResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `creates tag file of release build from two tags with different VC, same commit, build types only`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenTagNameDebug = "v1.0.2-debug"
        val givenTagNameRelease = "v1.0.1-release"
        val givenFirstCommitMessage = "Initial commit"
        val givenGetLastTagTaskRelease = "getLastTagRelease"
        val givenGetLastTagTaskDebug = "getLastTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-release.json")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenTagNameDebug)
        git.tag.addNamed(givenTagNameRelease)

        val releaseResult: BuildResult = projectDir.runTask(givenGetLastTagTaskRelease)
        projectDir.runTask(givenGetLastTagTaskDebug)

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha = git.tag.findTag(givenTagNameRelease).id
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
        assertTrue(
            releaseResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `creates tag file of release build from two tags with different VC, different commits, build types only`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val givenTagNameDebug = "v1.0.2-debug"
        val givenTagNameRelease = "v1.0.1-release"
        val givenFirstCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README"
        val givenGetLastTagTaskRelease = "getLastTagRelease"
        val givenGetLastTagTaskDebug = "getLastTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-release.json")

        git.addAllAndCommit(givenFirstCommitMessage)
        git.tag.addNamed(givenTagNameDebug)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenSecondCommitMessage)
        git.tag.addNamed(givenTagNameRelease)

        val releaseResult: BuildResult = projectDir.runTask(givenGetLastTagTaskRelease)
        projectDir.runTask(givenGetLastTagTaskDebug)

        projectDir.getFile("app").printFilesRecursively()

        val expectedCommitSha = git.tag.findTag(givenTagNameRelease).id
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
        assertTrue(
            releaseResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `creates tag file of debug build from three tags with different VC, where first numbers are equal, different commits, build types only`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val given1TagNameDebug = "v1.0.2-debug"
        val given2TagNameDebug = "v1.0.99-debug"
        val given3TagNameDebug = "v1.0.100-debug"
        val givenFirstCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README N1"
        val givenThirdCommitMessage = "Add README N2"
        val givenGetLastTagTask = "getLastTagDebug"
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

        val releaseResult: BuildResult = projectDir.runTask(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedBuildNumber = "100"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.100-debug"
        val expectedCommitSha = git.tag.findTag(expectedTagName).id
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
            releaseResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `creates tag file of debug build from three tags with different VC, where first numbers are equal, same commit, build types only`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val given1TagNameDebug = "v1.0.2-debug"
        val given2TagNameDebug = "v1.0.99-debug"
        val given3TagNameDebug = "v1.0.100-debug"
        val givenCommitMessage = "Initial commit"
        val givenGetLastTagTask = "getLastTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(given1TagNameDebug)
        git.tag.addNamed(given2TagNameDebug)
        git.tag.addNamed(given3TagNameDebug)

        val releaseResult: BuildResult = projectDir.runTask(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedBuildNumber = "100"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.0.100-debug"
        val expectedCommitSha = git.tag.findTag(expectedTagName).id
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
            releaseResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `creates tag file of debug build from three tags with different VC, where first 2 numbers are equal, different commits, build types only`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val given1TagNameDebug = "v1.1.1-debug"
        val given2TagNameDebug = "v1.1.99-debug"
        val given3TagNameDebug = "v1.1.100-debug"
        val givenFistCommitMessage = "Initial commit"
        val givenSecondCommitMessage = "Add README N1"
        val givenThirdCommitMessage = "Add README N2"
        val givenGetLastTagTask = "getLastTagDebug"
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

        val releaseResult: BuildResult = projectDir.runTask(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedBuildNumber = "100"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.1.100-debug"
        val expectedCommitSha = git.tag.findTag(expectedTagName).id
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
            releaseResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `creates tag file of debug build from three tags with different VC, where first 2 numbers are equal, same commit, build types only`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
        )
        val given1TagNameDebug = "v1.1.1-debug"
        val given2TagNameDebug = "v1.1.99-debug"
        val given3TagNameDebug = "v1.1.100-debug"
        val givenCommitMessage = "Initial commit"
        val givenGetLastTagTask = "getLastTagDebug"
        val git = projectDir.initGit()
        val givenTagBuildFile = projectDir.getFile("app/build/tag-build-debug.json")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(given1TagNameDebug)
        git.tag.addNamed(given2TagNameDebug)
        git.tag.addNamed(given3TagNameDebug)

        val releaseResult: BuildResult = projectDir.runTask(givenGetLastTagTask)

        projectDir.getFile("app").printFilesRecursively()

        val expectedBuildNumber = "100"
        val expectedBuildVariant = "debug"
        val expectedTagName = "v1.1.100-debug"
        val expectedCommitSha = git.tag.findTag(expectedTagName).id
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
            releaseResult.output.contains("BUILD SUCCESSFUL"),
            "Build succeeded",
        )
        assertEquals(
            expectedTagBuildFile.trimMargin(),
            givenTagBuildFile.readText(),
            "Tags equality",
        )
    }
}

private data class TagEvent(
    val name: String,
    val type: TagType,
    val createdAt: ZonedDateTime,
    val commitSha: String,
    val isBroken: Boolean,
)

private enum class TagType {
    LIGHTWEIGHT,
    ANNOTATED,
}

private val TAG_HEADER_REGEX =
    Regex("""^(\S+)\s*\[(OK|BROKEN)]\s*$""")

private val TYPE_REGEX =
    Regex("""^\s*type\s*:\s*(\w+)""")

private val CREATED_REGEX =
    Regex("""^\s*created\s*:\s*(.+)$""")

private val COMMIT_REGEX =
    Regex("""^\s*commit\s*:\s*([a-f0-9]{40})""")

private fun parseHistory(lines: List<String>): List<TagEvent> {
    val result = mutableListOf<TagEvent>()

    var name: String? = null
    var type: TagType? = null
    var createdAt: ZonedDateTime? = null
    var isBroken = false
    var commitSha: String? = null

    fun flush() {
        if (name != null && type != null && createdAt != null && commitSha != null) {
            result += TagEvent(
                name = name!!,
                type = type!!,
                createdAt = createdAt!!,
                commitSha = commitSha!!,
                isBroken = isBroken,
            )
        }
        name = null
        type = null
        createdAt = null
        isBroken = false
    }

    for (rawLine in lines) {
        val line = rawLine.trimStart()

        if (line.isBlank()) {
            flush()
            continue
        }

        TAG_HEADER_REGEX.matchEntire(line)?.let {
            flush()
            name = it.groupValues[1]
            isBroken = it.groupValues[2] == "BROKEN"
            return@let
        }

        TYPE_REGEX.find(line)?.let {
            type = when (it.groupValues[1]) {
                "annotated" -> TagType.ANNOTATED
                else -> TagType.LIGHTWEIGHT
            }
        }

        CREATED_REGEX.find(line)?.let {
            createdAt = ZonedDateTime.parse(
                it.groupValues[1],
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z"),
            )
        }
        COMMIT_REGEX.find(line)?.let {
            commitSha = it.groupValues[1]
        }
    }

    flush()
    return result
}
