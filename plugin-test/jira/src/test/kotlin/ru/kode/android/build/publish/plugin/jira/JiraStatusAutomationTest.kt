package ru.kode.android.build.publish.plugin.jira

import org.gradle.api.logging.Logger
import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.core.logger.PluginLogger
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
import ru.kode.android.build.publish.plugin.test.utils.printFilesRecursively
import ru.kode.android.build.publish.plugin.test.utils.runTask
import java.io.File
import java.io.IOException

class JiraStatusAutomationTest {

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
            logger = object : PluginLogger {
                override val bodyLogging: Boolean get() = false

                override fun info(message: String, exception: Throwable?) {
                    logger.info(message)
                }

                override fun warn(message: String) {
                    logger.warn(message)
                }

                override fun error(message: String, exception: Throwable?) {
                    logger.error(message, exception)
                }

                override fun quiet(message: String) {
                    logger.quiet(message)
                }
            }
        )
    }

    @Test
    @Throws(IOException::class)
    fun `jira status automation executes with automation config`() {
        val projectKey = "AT"
        val givenIssueKey = "AT-292"

        val availableIssueStatuses = jiraController.getProjectAvailableStatuses(projectKey)
        val inProgressStatus = availableIssueStatuses.find { it.name.contains("to do", ignoreCase = true) }
        val todoStatus = availableIssueStatuses.find { it.name.contains("in progress", ignoreCase = true) }

        assertTrue { inProgressStatus != null && todoStatus != null }

        val beforeAutomationStatusId = jiraController.getIssueStatus(givenIssueKey)

        val targetAutomationStatusName = if (beforeAutomationStatusId?.id == todoStatus?.id) {
            inProgressStatus!!.name
        } else {
            todoStatus!!.name
        }

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
                    labelPattern = null,
                    fixVersionPattern = null,
                    targetStatusName = targetAutomationStatusName
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
            $givenIssueKey: Add test readme
            
            CHANGELOG: [$givenIssueKey] Задача для проверки работы BuildPublishPlugin с фиксверсией
        """.trimIndent()
        val givenAssembleTask = "assembleDebug"
        val givenJiraAutomationTask = "jiraAutomationDebug"
        val git = projectDir.initGit()
        val givenChangelogFile = projectDir.getFile("app/build/changelog-debug.txt")

        val expectedIssueKey = "AT-292"

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val automationResult: BuildResult = projectDir.runTask(givenJiraAutomationTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

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
        assertTrue(givenOutputFileExists, "Output file exists")

        val expectedChangelogFile =
            """
            • [$expectedIssueKey] Задача для проверки работы BuildPublishPlugin с фиксверсией
            """.trimIndent()

        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )

        val afterAutomationStatus = jiraController.getIssueStatus(expectedIssueKey)
        assertTrue { afterAutomationStatus?.name == targetAutomationStatusName }
    }

    @Test
    @Throws(IOException::class)
    fun `jira status automation executes with automation config with multiple tasks, mixed correct and incorrect status`() {
        val projectKey = "AT"
        // Active task
        val givenIssueKey1 = "AT-292"
        // Closed task
        val givenIssueKey2 = "AT-291"

        val availableIssueStatuses = jiraController.getProjectAvailableStatuses(projectKey)
        val inProgressStatus = availableIssueStatuses.find { it.name.contains("to do", ignoreCase = true) }
        val todoStatus = availableIssueStatuses.find { it.name.contains("in progress", ignoreCase = true) }

        assertTrue { inProgressStatus != null && todoStatus != null }

        val beforeAutomationStatusId = jiraController.getIssueStatus(givenIssueKey1)

        val targetAutomationStatusName1 = if (beforeAutomationStatusId?.id == todoStatus?.id) {
            inProgressStatus!!.name
        } else {
            todoStatus!!.name
        }
        val targetAutomationStatusName2 = jiraController.getIssueStatus(givenIssueKey2)?.name

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
                    labelPattern = null,
                    fixVersionPattern = null,
                    targetStatusName = targetAutomationStatusName1
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
            $givenIssueKey1: Add test readme
            
            CHANGELOG: [$givenIssueKey1] Задача для проверки работы BuildPublishPlugin с фиксверсией
        """.trimIndent()
        val givenCommitMessage3 = """
            $givenIssueKey2: Add test readme
            
            CHANGELOG: [$givenIssueKey2] Задача для проверки работы BuildPublishPlugin с фиксверсией закрытая
        """.trimIndent()
        val givenAssembleTask = "assembleDebug"
        val givenJiraAutomationTask = "jiraAutomationDebug"
        val git = projectDir.initGit()
        val givenChangelogFile = projectDir.getFile("app/build/changelog-debug.txt")

        val expectedIssueKey1 = "AT-292"
        val expectedIssueKey2 = "AT-291"

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        projectDir.getFile("app/README1.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage3)
        git.tag.addNamed(givenTagName2)

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val automationResult: BuildResult = projectDir.runTask(givenJiraAutomationTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

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
        assertTrue(givenOutputFileExists, "Output file exists")

        val expectedChangelogFile =
            """
            • [$expectedIssueKey2] Задача для проверки работы BuildPublishPlugin с фиксверсией закрытая
            • [$expectedIssueKey1] Задача для проверки работы BuildPublishPlugin с фиксверсией
            """.trimIndent()

        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )

        val afterAutomationStatus1 = jiraController.getIssueStatus(expectedIssueKey1)
        assertTrue { afterAutomationStatus1?.name == targetAutomationStatusName1 }
        val afterAutomationStatus2 = jiraController.getIssueStatus(expectedIssueKey2)
        assertTrue { afterAutomationStatus2?.name == targetAutomationStatusName2 }
    }

    @Test
    @Throws(IOException::class)
    fun `jira status automation executes with automation config with multiple tasks, all correct status`() {
        val projectKey = "AT"
        val givenIssueKey1 = "AT-293"
        val givenIssueKey2 = "AT-294"
        val givenIssueKey3 = "AT-295"
        val givenIssueKey4 = "AT-296"

        val availableIssueStatuses = jiraController.getProjectAvailableStatuses(projectKey)
        val inProgressStatus = availableIssueStatuses.find { it.name.contains("to do", ignoreCase = true) }
        val todoStatus = availableIssueStatuses.find { it.name.contains("in progress", ignoreCase = true) }

        assertTrue { inProgressStatus != null && todoStatus != null }

        val beforeAutomationStatusId = jiraController.getIssueStatus(givenIssueKey1)

        val targetAutomationStatusName = if (beforeAutomationStatusId?.id == todoStatus?.id) {
            inProgressStatus!!.name
        } else {
            todoStatus!!.name
        }

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
                    labelPattern = null,
                    fixVersionPattern = null,
                    targetStatusName = targetAutomationStatusName
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
            $givenIssueKey1: Add test readme
            
            CHANGELOG: [$givenIssueKey1] Задача для проверки работы BuildPublishPlugin с фиксверсией 2
        """.trimIndent()
        val givenCommitMessage3 = """
            $givenIssueKey2: Add test readme
            
            CHANGELOG: [$givenIssueKey2] Задача для проверки работы BuildPublishPlugin с фиксверсией 3
        """.trimIndent()
        val givenCommitMessage4 = """
            $givenIssueKey3: Add test readme
            
            CHANGELOG: [$givenIssueKey3] Задача для проверки работы BuildPublishPlugin с фиксверсией 4
        """.trimIndent()
        val givenCommitMessage5 = """
            $givenIssueKey4: Add test readme
            
            CHANGELOG: [$givenIssueKey4] Задача для проверки работы BuildPublishPlugin с фиксверсией 5
        """.trimIndent()
        val givenAssembleTask = "assembleDebug"
        val givenJiraAutomationTask = "jiraAutomationDebug"
        val git = projectDir.initGit()
        val givenChangelogFile = projectDir.getFile("app/build/changelog-debug.txt")

        val expectedIssueKey1 = "AT-293"
        val expectedIssueKey2 = "AT-294"
        val expectedIssueKey3 = "AT-295"
        val expectedIssueKey4 = "AT-296"

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage3)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage4)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage5)
        git.tag.addNamed(givenTagName2)

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val automationResult: BuildResult = projectDir.runTask(givenJiraAutomationTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

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
        assertTrue(givenOutputFileExists, "Output file exists")

        val expectedChangelogFile =
            """
            • [$expectedIssueKey4] Задача для проверки работы BuildPublishPlugin с фиксверсией 5
            • [$expectedIssueKey3] Задача для проверки работы BuildPublishPlugin с фиксверсией 4
            • [$expectedIssueKey2] Задача для проверки работы BuildPublishPlugin с фиксверсией 3
            • [$expectedIssueKey1] Задача для проверки работы BuildPublishPlugin с фиксверсией 2
            """.trimIndent()

        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )

        val afterAutomationStatus1 = jiraController.getIssueStatus(expectedIssueKey1)
        assertTrue { afterAutomationStatus1?.name == targetAutomationStatusName }
        val afterAutomationStatus2 = jiraController.getIssueStatus(expectedIssueKey2)
        assertTrue { afterAutomationStatus2?.name == targetAutomationStatusName }
        val afterAutomationStatus3 = jiraController.getIssueStatus(expectedIssueKey3)
        assertTrue { afterAutomationStatus3?.name == targetAutomationStatusName }
        val afterAutomationStatus4 = jiraController.getIssueStatus(expectedIssueKey4)
        assertTrue { afterAutomationStatus4?.name == targetAutomationStatusName }
    }

    @Test
    @Throws(IOException::class)
    fun `jira status automation executes with automation config with multiple tasks, all correct status, status name lowercase`() {
        val projectKey = "AT"
        val givenIssueKey1 = "AT-293"
        val givenIssueKey2 = "AT-294"
        val givenIssueKey3 = "AT-295"
        val givenIssueKey4 = "AT-296"

        val availableIssueStatuses = jiraController.getProjectAvailableStatuses(projectKey)
        val inProgressStatus = availableIssueStatuses.find { it.name.contains("to do", ignoreCase = true) }
        val todoStatus = availableIssueStatuses.find { it.name.contains("in progress", ignoreCase = true) }

        assertTrue { inProgressStatus != null && todoStatus != null }

        val beforeAutomationStatusId = jiraController.getIssueStatus(givenIssueKey1)

        val targetAutomationStatusName = if (beforeAutomationStatusId?.id == todoStatus?.id) {
            inProgressStatus!!.name
        } else {
            todoStatus!!.name
        }

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
                    labelPattern = null,
                    fixVersionPattern = null,
                    targetStatusName = targetAutomationStatusName.lowercase()
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
            $givenIssueKey1: Add test readme
            
            CHANGELOG: [$givenIssueKey1] Задача для проверки работы BuildPublishPlugin с фиксверсией 2
        """.trimIndent()
        val givenCommitMessage3 = """
            $givenIssueKey2: Add test readme
            
            CHANGELOG: [$givenIssueKey2] Задача для проверки работы BuildPublishPlugin с фиксверсией 3
        """.trimIndent()
        val givenCommitMessage4 = """
            $givenIssueKey3: Add test readme
            
            CHANGELOG: [$givenIssueKey3] Задача для проверки работы BuildPublishPlugin с фиксверсией 4
        """.trimIndent()
        val givenCommitMessage5 = """
            $givenIssueKey4: Add test readme
            
            CHANGELOG: [$givenIssueKey4] Задача для проверки работы BuildPublishPlugin с фиксверсией 5
        """.trimIndent()
        val givenAssembleTask = "assembleDebug"
        val givenJiraAutomationTask = "jiraAutomationDebug"
        val git = projectDir.initGit()
        val givenChangelogFile = projectDir.getFile("app/build/changelog-debug.txt")

        val expectedIssueKey1 = "AT-293"
        val expectedIssueKey2 = "AT-294"
        val expectedIssueKey3 = "AT-295"
        val expectedIssueKey4 = "AT-296"

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage3)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage4)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage5)
        git.tag.addNamed(givenTagName2)

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val automationResult: BuildResult = projectDir.runTask(givenJiraAutomationTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

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
        assertTrue(givenOutputFileExists, "Output file exists")

        val expectedChangelogFile =
            """
            • [$expectedIssueKey4] Задача для проверки работы BuildPublishPlugin с фиксверсией 5
            • [$expectedIssueKey3] Задача для проверки работы BuildPublishPlugin с фиксверсией 4
            • [$expectedIssueKey2] Задача для проверки работы BuildPublishPlugin с фиксверсией 3
            • [$expectedIssueKey1] Задача для проверки работы BuildPublishPlugin с фиксверсией 2
            """.trimIndent()

        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )

        val afterAutomationStatus1 = jiraController.getIssueStatus(expectedIssueKey1)
        assertTrue { afterAutomationStatus1?.name == targetAutomationStatusName }
        val afterAutomationStatus2 = jiraController.getIssueStatus(expectedIssueKey2)
        assertTrue { afterAutomationStatus2?.name == targetAutomationStatusName }
        val afterAutomationStatus3 = jiraController.getIssueStatus(expectedIssueKey3)
        assertTrue { afterAutomationStatus3?.name == targetAutomationStatusName }
        val afterAutomationStatus4 = jiraController.getIssueStatus(expectedIssueKey4)
        assertTrue { afterAutomationStatus4?.name == targetAutomationStatusName }
    }

}
