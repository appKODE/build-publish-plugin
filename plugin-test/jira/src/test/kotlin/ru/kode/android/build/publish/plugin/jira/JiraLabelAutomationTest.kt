package ru.kode.android.build.publish.plugin.jira

import org.gradle.api.logging.Logger
import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.jira.controller.JiraController
import ru.kode.android.build.publish.plugin.jira.controller.factory.JiraControllerFactory
import ru.kode.android.build.publish.plugin.test.utils.AlwaysInfoLogger
import ru.kode.android.build.publish.plugin.test.utils.BuildType
import ru.kode.android.build.publish.plugin.test.utils.FoundationConfig
import ru.kode.android.build.publish.plugin.test.utils.JiraConfig
import ru.kode.android.build.publish.plugin.test.utils.addAllAndCommit
import ru.kode.android.build.publish.plugin.test.utils.addNamed
import ru.kode.android.build.publish.plugin.test.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.currentDate
import ru.kode.android.build.publish.plugin.test.utils.getFile
import ru.kode.android.build.publish.plugin.test.utils.initGit
import ru.kode.android.build.publish.plugin.test.utils.printFilesRecursively
import ru.kode.android.build.publish.plugin.test.utils.runTask
import ru.kode.android.build.publish.plugin.test.utils.runTaskWithFail
import java.io.File
import java.io.IOException

class JiraLabelAutomationTest {

    private val logger: Logger = AlwaysInfoLogger()

    @TempDir
    lateinit var tempDir: File
    private lateinit var projectDir: File
    private lateinit var jiraController: JiraController

    @BeforeEach
    fun setup() {
        projectDir = File(tempDir, "test-project")
        jiraController = JiraControllerFactory.build(
            baseUrl = System.getProperty("JIRA_BASE_URL"),
            username = System.getProperty("JIRA_USER_NAME"),
            password = System.getProperty("JIRA_USER_PASSWORD"),
            logger = logger
        )
    }

    @Test
    @Throws(IOException::class)
    fun `jira automation not available without automation config`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
            jiraConfig = JiraConfig(
                auth = JiraConfig.Auth(
                    baseUrl = System.getProperty("JIRA_BASE_URL"),
                    username = System.getProperty("JIRA_USER_NAME"),
                    password = System.getProperty("JIRA_USER_PASSWORD")
                ),
                automation = null
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )
        val givenTagName = "v1.0.1-debug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDebug"
        val givenJiraAutomationTask = "jiraAutomationDebug"
        val git = projectDir.initGit()
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc1-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val assembleResult: BuildResult = projectDir.runTaskWithFail(givenAssembleTask)
        val automationResult: BuildResult = projectDir.runTaskWithFail(givenJiraAutomationTask)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            !assembleResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            !assembleResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug not executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD FAILED"),
            "Build failed",
        )
        assertTrue(
            automationResult.output.contains("BUILD FAILED"),
            "Jira automation failed"
        )
        assertTrue(!givenOutputFile.exists(), "Output file not exists")
    }

    @Test
    @Throws(IOException::class)
    fun `jira label automation executes with automation config`() {
        val projectKey = "AT"

        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog = FoundationConfig.Changelog(
                        issueNumberPattern = "AT-\\\\d+",
                        issueUrlPrefix = "${System.getProperty("JIRA_BASE_URL")}/browse/"
                    )
                ),
            jiraConfig = JiraConfig(
                auth = JiraConfig.Auth(
                    baseUrl = System.getProperty("JIRA_BASE_URL"),
                    username = System.getProperty("JIRA_USER_NAME"),
                    password = System.getProperty("JIRA_USER_PASSWORD")
                ),
                automation = JiraConfig.Automation(
                    projectKey = projectKey,
                    labelPattern = "fix_%1\\\$s.%2\\\$s",
                    fixVersionPattern = null,
                    targetStatusName = null
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )
        val givenIssueKey = "AT-289"
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 = """
            $givenIssueKey: Add test readme
            
            CHANGELOG: [$givenIssueKey] Задача для проверки работы BuildPublishPlugin с лейблом
        """.trimIndent()
        val givenAssembleTask = "assembleDebug"
        val givenJiraAutomationTask = "jiraAutomationDebug"
        val git = projectDir.initGit()
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc2-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        val expectedIssueKey = "AT-289"
        val expectedIssueLabel = "fix_1.0.2"

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)

        jiraController.removeIssueLabel(expectedIssueKey, expectedIssueLabel)
        val beforeAutomationLabels = jiraController.getIssueLabels(expectedIssueKey)
        assertTrue { !beforeAutomationLabels.contains(expectedIssueLabel) }

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val automationResult: BuildResult = projectDir.runTask(givenJiraAutomationTask)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            !assembleResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build successful",
        )
        assertTrue(
            automationResult.output.contains("BUILD SUCCESSFUL"),
            "Jira automation successful"
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")

        val expectedChangelogFile =
            """
            • [$expectedIssueKey] Задача для проверки работы BuildPublishPlugin с лейблом
            """.trimIndent()

        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )

        val afterAutomationLabels = jiraController.getIssueLabels(expectedIssueKey)
        assertTrue { afterAutomationLabels.contains(expectedIssueLabel) }
    }

    @Test
    @Throws(IOException::class)
    fun `jira label automation executes with automation config and already added label`() {
        val projectKey = "AT"

        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog = FoundationConfig.Changelog(
                        issueNumberPattern = "AT-\\\\d+",
                        issueUrlPrefix = "${System.getProperty("JIRA_BASE_URL")}/browse/"
                    )
                ),
            jiraConfig = JiraConfig(
                auth = JiraConfig.Auth(
                    baseUrl = System.getProperty("JIRA_BASE_URL"),
                    username = System.getProperty("JIRA_USER_NAME"),
                    password = System.getProperty("JIRA_USER_PASSWORD")
                ),
                automation = JiraConfig.Automation(
                    projectKey = projectKey,
                    labelPattern = "fix_%1\\\$s.%2\\\$s",
                    fixVersionPattern = null,
                    targetStatusName = null
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )
        val givenIssueKey = "AT-289"
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 = """
            $givenIssueKey: Add test readme
            
            CHANGELOG: [$givenIssueKey] Задача для проверки работы BuildPublishPlugin с лейблом
        """.trimIndent()
        val givenAssembleTask = "assembleDebug"
        val givenJiraAutomationTask = "jiraAutomationDebug"
        val git = projectDir.initGit()
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc2-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        val expectedIssueKey = "AT-289"
        val expectedIssueLabel = "fix_1.0.2"

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)

        jiraController.addIssueLabel(expectedIssueKey, expectedIssueLabel)
        val beforeAutomationLabels = jiraController.getIssueLabels(expectedIssueKey)
        assertTrue { beforeAutomationLabels.contains(expectedIssueLabel) }

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val automationResult: BuildResult = projectDir.runTask(givenJiraAutomationTask)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            !assembleResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build successful",
        )
        assertTrue(
            automationResult.output.contains("BUILD SUCCESSFUL"),
            "Jira automation successful"
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")

        val expectedChangelogFile =
            """
            • [$expectedIssueKey] Задача для проверки работы BuildPublishPlugin с лейблом
            """.trimIndent()

        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )

        val afterAutomationLabels = jiraController.getIssueLabels(expectedIssueKey)
        assertTrue { afterAutomationLabels.contains(expectedIssueLabel) }
    }

    @Test
    @Throws(IOException::class)
    fun `jira label automation executes with automation config, but without assemble`() {
        val projectKey = "AT"

        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog = FoundationConfig.Changelog(
                        issueNumberPattern = "AT-\\\\d+",
                        issueUrlPrefix = "${System.getProperty("JIRA_BASE_URL")}/browse/"
                    )
                ),
            jiraConfig = JiraConfig(
                auth = JiraConfig.Auth(
                    baseUrl = System.getProperty("JIRA_BASE_URL"),
                    username = System.getProperty("JIRA_USER_NAME"),
                    password = System.getProperty("JIRA_USER_PASSWORD")
                ),
                automation = JiraConfig.Automation(
                    projectKey = projectKey,
                    labelPattern = "fix_%1\\\$s.%2\\\$s",
                    fixVersionPattern = null,
                    targetStatusName = null
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 = """
            AT-289: Add test readme
            
            CHANGELOG: [AT-289] Задача для проверки работы BuildPublishPlugin с лейблом
        """.trimIndent()
        val givenJiraAutomationTask = "jiraAutomationDebug"
        val git = projectDir.initGit()
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc2-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        val expectedIssueKey = "AT-289"
        val expectedIssueLabel = "fix_1.0.2"

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)

        jiraController.removeIssueLabel(expectedIssueKey, expectedIssueLabel)
        val beforeAutomationLabels = jiraController.getIssueLabels(expectedIssueKey)
        assertTrue { !beforeAutomationLabels.contains(expectedIssueLabel) }

        val automationResult: BuildResult = projectDir.runTask(givenJiraAutomationTask)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            !automationResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            automationResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            automationResult.output.contains("BUILD SUCCESSFUL"),
            "Jira automation successful"
        )
        assertTrue(!givenOutputFile.exists(), "Output file not exists")

        val expectedChangelogFile =
            """
            • [$expectedIssueKey] Задача для проверки работы BuildPublishPlugin с лейблом
            """.trimIndent()

        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )

        val afterAutomationLabels = jiraController.getIssueLabels(expectedIssueKey)
        assertTrue { afterAutomationLabels.contains(expectedIssueLabel) }
    }

    @Test
    @Throws(IOException::class)
    fun `jira label automation executes with automation config when jira task is not available`() {
        val projectKey = "AT"

        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog = FoundationConfig.Changelog(
                        issueNumberPattern = "AT-\\\\d+",
                        issueUrlPrefix = "${System.getProperty("JIRA_BASE_URL")}/browse/"
                    )
                ),
            jiraConfig = JiraConfig(
                auth = JiraConfig.Auth(
                    baseUrl = System.getProperty("JIRA_BASE_URL"),
                    username = System.getProperty("JIRA_USER_NAME"),
                    password = System.getProperty("JIRA_USER_PASSWORD")
                ),
                automation = JiraConfig.Automation(
                    projectKey = projectKey,
                    labelPattern = "fix_%1\\\$s.%2\\\$s",
                    fixVersionPattern = null,
                    targetStatusName = null
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )
        val givenLabelKey = "AT-2899"
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 = """
            $givenLabelKey: Add test readme
            
            CHANGELOG: [$givenLabelKey] Задача для проверки работы BuildPublishPlugin с лейблом
        """.trimIndent()
        val givenAssembleTask = "assembleDebug"
        val givenJiraAutomationTask = "jiraAutomationDebug"
        val git = projectDir.initGit()
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc2-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog.txt")

        val expectedLabelKey = "AT-2899"

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val automationResult: BuildResult = projectDir.runTask(givenJiraAutomationTask)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            !assembleResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build successful",
        )
        assertTrue(
            automationResult.output.contains("BUILD SUCCESSFUL"),
            "Jira automation successful"
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")

        val expectedChangelogFile =
            """
            • [$expectedLabelKey] Задача для проверки работы BuildPublishPlugin с лейблом
            """.trimIndent()

        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )
        assertTrue {
            automationResult.output
                .contains(
                    """
                        Failed to add label for $expectedLabelKey
                        Unknown(code=404, reason={"errorMessages":["Issue Does Not Exist"],"errors":{}})
                    """.trimIndent()
                )
        }
    }
}
