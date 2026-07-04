package ru.kode.android.build.publish.plugin.jira

import org.gradle.api.logging.Logger
import org.gradle.testkit.runner.BuildResult
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
import ru.kode.android.build.publish.plugin.test.utils.runTask
import java.io.File

/**
 * Integration test (requires a live Jira instance via the JIRA_* system properties) covering the
 * multi-project automation path: the fix version is applied through a `projects { }` binding rather
 * than the top-level `projectKey`, and issues are routed to the project by their key prefix.
 */
class JiraProjectsAutomationTest {
    private val logger: Logger = AlwaysInfoLogger()

    @TempDir
    lateinit var tempDir: File
    private lateinit var projectDir: File
    private lateinit var jiraController: JiraController

    private fun jiraController(): JiraController =
        JiraControllerFactory.build(
            baseUrl = System.getProperty("JIRA_BASE_URL"),
            username = System.getProperty("JIRA_USER_NAME"),
            password = System.getProperty("JIRA_USER_PASSWORD"),
            logger = pluginLoggerFromLogger(logger),
        )

    @Test
    fun `fix version automation applies through a projects binding routed by issue key prefix`() {
        projectDir = File(tempDir, "test-project")
        jiraController = jiraController()
        val projectKey = "AT"

        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output = FoundationConfig.Output(baseFileName = "autotest"),
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
                        JiraConfig.Automation(
                            projects =
                                listOf(
                                    JiraConfig.Project(
                                        name = "main",
                                        projectKey = projectKey,
                                        fixVersionPattern = "fix_%1\\\$s.%2\\\$s",
                                    ),
                                ),
                        ),
                ),
            topBuildFileContent =
                """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
                """.trimIndent(),
        )

        val givenIssueKey = "AT-290"
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 =
            """
            $givenIssueKey: Add test readme

            CHANGELOG: [$givenIssueKey] Задача для проверки работы BuildPublishPlugin с фиксверсией
            """.trimIndent()
        val givenAssembleTask = "assembleDebug"
        val givenJiraAutomationTask = "jiraAutomationDebug"
        val git = projectDir.initGit()

        val expectedFixVersion = "fix_1.0.2"
        val expectedIssueKey = "AT-290"

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)

        val projectFixVersions = jiraController.getProjectVersions(projectKey)
        val fixVersion = projectFixVersions.find { it.name == expectedFixVersion }
        if (fixVersion != null) {
            jiraController.removeIssueFixVersion(expectedIssueKey, fixVersion.name)
            jiraController.removeProjectVersion(fixVersion.id)
        }

        awaitUntil("fix version $expectedFixVersion absent from $expectedIssueKey") {
            !jiraController.getIssueFixVersions(expectedIssueKey).map { it.name }.contains(expectedFixVersion)
        }

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val automationResult: BuildResult = projectDir.runTask(givenJiraAutomationTask)

        assembleResult.outputShouldContain("BUILD SUCCESSFUL")
        automationResult.outputShouldContain("BUILD SUCCESSFUL")

        awaitUntil("fix version $expectedFixVersion present on $expectedIssueKey") {
            jiraController.getIssueFixVersions(expectedIssueKey).map { it.name }.contains(expectedFixVersion)
        }
    }
}
