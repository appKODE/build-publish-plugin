package ru.kode.android.build.publish.plugin.jira

import org.gradle.api.logging.Logger
import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Assertions.assertEquals
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
import ru.kode.android.build.publish.plugin.test.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.getFile
import ru.kode.android.build.publish.plugin.test.utils.initGit
import ru.kode.android.build.publish.plugin.test.utils.outputShouldContain
import ru.kode.android.build.publish.plugin.test.utils.runTask
import java.io.File
import java.io.IOException

/**
 * Live Jira test for the changelog issue-resolution feature: a commit carrying only a `CLOSES:` line
 * (no manual `CHANGELOG:` entry) must be resolved to the Jira issue's title and rendered into the
 * generated changelog. The expected title is fetched from the same Jira instance, so the assertion
 * stays valid regardless of the issue's current summary.
 */
class JiraIssueResolutionTest {
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
    fun `resolves a bare CLOSES reference to the jira issue title in the changelog`() {
        val projectKey = "AT"
        val issueNumber = "290"
        val issueKey = "AT-290"
        val expectedTitle =
            jiraController.getIssueSummary(issueKey)
                ?: error("Could not fetch summary for $issueKey — check Jira credentials/connectivity")

        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output = FoundationConfig.Output(baseFileName = "autotest"),
                    changelog =
                        FoundationConfig.Changelog(
                            issueNumberPattern = "AT-\\\\d+",
                            issueUrlPrefix = "${System.getProperty("JIRA_BASE_URL")}/browse/",
                            issueReferences =
                                listOf(
                                    FoundationConfig.Changelog.IssueReference(
                                        name = "closes",
                                        key = "CLOSES",
                                        numberPattern = "(\\\\d+|[A-Z]+-\\\\d+)",
                                    ),
                                ),
                        ),
                ),
            jiraConfig =
                JiraConfig(
                    auth =
                        JiraConfig.Auth(
                            baseUrl = System.getProperty("JIRA_BASE_URL"),
                            username = System.getProperty("JIRA_USER_NAME"),
                            password = System.getProperty("JIRA_USER_PASSWORD"),
                            projects = listOf(JiraConfig.RegistryProject(name = "main", projectKey = projectKey)),
                        ),
                    issueResolution =
                        JiraConfig.IssueResolution(
                            enabled = true,
                            fromInstances =
                                listOf(
                                    JiraConfig.IssueResolution.InstanceSelection(
                                        instanceName = "default",
                                        projectNames = listOf("main"),
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

        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 =
            """
            $issueKey: Add test readme

            CLOSES: $issueNumber
            """.trimIndent()
        val givenChangelogTask = "generateChangelogDebug"
        val git = projectDir.initGit()
        val givenChangelogFile = projectDir.getFile("app/build/changelog-debug.txt")

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)

        val changelogResult: BuildResult = projectDir.runTask(givenChangelogTask)

        changelogResult.outputShouldContain("Task :app:generateChangelogDebug")
        changelogResult.outputShouldContain("BUILD SUCCESSFUL")

        val expectedChangelogFile = "• [$issueKey] $expectedTitle"

        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Resolved changelog title",
        )
    }
}
