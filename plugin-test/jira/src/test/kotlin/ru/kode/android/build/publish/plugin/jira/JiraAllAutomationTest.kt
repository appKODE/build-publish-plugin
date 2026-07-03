// Long, descriptive backtick test names intentionally exceed the line limit.
@file:Suppress("ktlint:standard:max-line-length")

package ru.kode.android.build.publish.plugin.jira

import org.gradle.api.logging.Logger
import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.core.logger.pluginLoggerFromLogger
import ru.kode.android.build.publish.plugin.jira.controller.JiraController
import ru.kode.android.build.publish.plugin.jira.controller.factory.JiraControllerFactory
import ru.kode.android.build.publish.plugin.test.utils.AlwaysInfoLogger
import ru.kode.android.build.publish.plugin.test.utils.BuildType
import ru.kode.android.build.publish.plugin.test.utils.FoundationConfig
import ru.kode.android.build.publish.plugin.test.utils.JiraConfig
import ru.kode.android.build.publish.plugin.test.utils.addAllAndCommit
import ru.kode.android.build.publish.plugin.test.utils.addNamed
import ru.kode.android.build.publish.plugin.test.utils.awaitUntil
import ru.kode.android.build.publish.plugin.test.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.getFile
import ru.kode.android.build.publish.plugin.test.utils.initGit
import ru.kode.android.build.publish.plugin.test.utils.outputShouldContain
import ru.kode.android.build.publish.plugin.test.utils.outputShouldNotContain
import ru.kode.android.build.publish.plugin.test.utils.printFilesRecursively
import ru.kode.android.build.publish.plugin.test.utils.runTask
import java.io.File
import java.io.IOException

class JiraAllAutomationTest {
    private val logger: Logger = AlwaysInfoLogger()

    @TempDir
    lateinit var tempDir: File
    private lateinit var projectDir: File
    private lateinit var jiraController: JiraController

    @BeforeEach
    fun setup() {
        projectDir = File(tempDir, "test-project")
        jiraController =
            JiraControllerFactory.build(
                baseUrl = System.getProperty("JIRA_BASE_URL"),
                username = System.getProperty("JIRA_USER_NAME"),
                password = System.getProperty("JIRA_USER_PASSWORD"),
                logger = pluginLoggerFromLogger(logger),
            )
    }

    @Test
    @Throws(IOException::class)
    fun `jira status, label and fix version automation executes with automation config with multiple tasks, all correct status, status name lowercase`() {
        val projectKey = "AT"
        val givenIssueKey1 = "AT-297"
        val givenIssueKey2 = "AT-298"

        val availableIssueStatuses = jiraController.getProjectAvailableStatuses(projectKey)
        val inProgressStatus = availableIssueStatuses.find { it.name.contains("to do", ignoreCase = true) }
        val todoStatus = availableIssueStatuses.find { it.name.contains("in progress", ignoreCase = true) }

        assertTrue { inProgressStatus != null && todoStatus != null }

        val beforeAutomationStatusId = jiraController.getIssueStatus(givenIssueKey1)

        val targetAutomationStatusName =
            if (beforeAutomationStatusId?.id == todoStatus?.id) {
                inProgressStatus!!.name
            } else {
                todoStatus!!.name
            }

        val expectedIssueKey1 = "AT-297"
        val expectedIssueKey2 = "AT-298"

        val expectedFixVersion = "fix_1.0.2"
        val expectedIssueLabel = "fix_1.0.2"

        // Start from a clean slate: the automation under test creates the project version itself, so
        // any leftover "fix_1.0.2" from a previous run must be deleted first — otherwise the plugin's
        // createProjectVersion collides with a 400 "version already exists" and the fix version never
        // gets attached. Mirrors the cleanup in JiraFixVersionAutomationTest.
        val projectFixVersions = jiraController.getProjectVersions(projectKey)
        val fixVersion = projectFixVersions.find { it.name == expectedFixVersion }
        if (fixVersion != null) {
            jiraController.removeIssueFixVersion(givenIssueKey1, fixVersion.name)
            jiraController.removeIssueFixVersion(givenIssueKey2, fixVersion.name)
            jiraController.removeProjectVersion(fixVersion.id)
        }

        awaitUntil("fix version $expectedFixVersion absent from $givenIssueKey1") {
            !jiraController.getIssueFixVersions(givenIssueKey1).map { it.name }.contains(expectedFixVersion)
        }

        awaitUntil("fix version $expectedFixVersion absent from $givenIssueKey2") {
            !jiraController.getIssueFixVersions(givenIssueKey2).map { it.name }.contains(expectedFixVersion)
        }

        jiraController.removeIssueLabel(expectedIssueKey1, expectedIssueLabel)
        awaitUntil("label $expectedIssueLabel absent from $expectedIssueKey1") {
            !jiraController.getIssueLabels(expectedIssueKey1).contains(expectedIssueLabel)
        }

        jiraController.removeIssueLabel(expectedIssueKey2, expectedIssueLabel)
        awaitUntil("label $expectedIssueLabel absent from $expectedIssueKey2") {
            !jiraController.getIssueLabels(expectedIssueKey2).contains(expectedIssueLabel)
        }

        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog =
                        FoundationConfig.Changelog(
                            issueNumberPattern = "AT-\\\\d+",
                            issueUrlPrefix = "${System.getProperty("JIRA_BASE_URL")}/browse/",
                        ),
                ),
            jiraConfig =
                JiraConfig(
                    auth =
                        JiraConfig.Auth(
                            baseUrl = System.getProperty("JIRA_BASE_URL"),
                            username = System.getProperty("JIRA_USER_NAME"),
                            password = System.getProperty("JIRA_USER_PASSWORD"),
                        ),
                    automation =
                        JiraConfig.Automation.singleProject(
                            projectKey = projectKey,
                            labelPattern = "fix_%1\\\$s.%2\\\$s",
                            fixVersionPattern = "fix_%1\\\$s.%2\\\$s",
                            targetStatusName = targetAutomationStatusName.lowercase(),
                        ),
                ),
            topBuildFileContent =
                """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
                """.trimIndent(),
        )

        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 =
            """
            $givenIssueKey1: Add test readme
            
            CHANGELOG: [$givenIssueKey1] Задача для проверки работы BuildPublishPlugin с фиксверсией 2
            """.trimIndent()
        val givenCommitMessage3 =
            """
            $givenIssueKey2: Add test readme
            
            CHANGELOG: [$givenIssueKey2] Задача для проверки работы BuildPublishPlugin с фиксверсией 3
            """.trimIndent()

        val givenAssembleTask = "assembleDebug"
        val givenJiraAutomationTask = "jiraAutomationDebug"
        val git = projectDir.initGit()
        val givenChangelogFile = projectDir.getFile("app/build/changelog-debug.txt")

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage3)
        git.tag.addNamed(givenTagName2)

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val automationResult: BuildResult = projectDir.runTask(givenJiraAutomationTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists =
            apkDir.listFiles()
                ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
                ?: false

        assembleResult.outputShouldNotContain("Task :app:getLastTagSnapshotRelease")
        assembleResult.outputShouldContain("Task :app:getLastTagSnapshotDebug")
        assembleResult.outputShouldContain("BUILD SUCCESSFUL")
        automationResult.outputShouldContain("BUILD SUCCESSFUL")
        assertTrue(givenOutputFileExists, "Output file exists")

        val expectedChangelogFile =
            """
            • [$expectedIssueKey2] Задача для проверки работы BuildPublishPlugin с фиксверсией 3
            • [$expectedIssueKey1] Задача для проверки работы BuildPublishPlugin с фиксверсией 2
            """.trimIndent()

        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )

        awaitUntil("status of $expectedIssueKey1 is $targetAutomationStatusName") {
            jiraController.getIssueStatus(expectedIssueKey1)?.name == targetAutomationStatusName
        }
        awaitUntil("status of $expectedIssueKey2 is $targetAutomationStatusName") {
            jiraController.getIssueStatus(expectedIssueKey2)?.name == targetAutomationStatusName
        }

        awaitUntil("fix version $expectedFixVersion present on $expectedIssueKey1") {
            jiraController.getIssueFixVersions(expectedIssueKey1).map { it.name }.contains(expectedFixVersion)
        }
        awaitUntil("fix version $expectedFixVersion present on $expectedIssueKey2") {
            jiraController.getIssueFixVersions(expectedIssueKey2).map { it.name }.contains(expectedFixVersion)
        }

        awaitUntil("label $expectedIssueLabel present on $expectedIssueKey1") {
            jiraController.getIssueLabels(expectedIssueKey1).contains(expectedIssueLabel)
        }

        awaitUntil("label $expectedIssueLabel present on $expectedIssueKey2") {
            jiraController.getIssueLabels(expectedIssueKey2).contains(expectedIssueLabel)
        }
    }
}
