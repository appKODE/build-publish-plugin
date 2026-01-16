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
import ru.kode.android.build.publish.plugin.jira.messages.failedToAddFixVersionMessage
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

class JiraFixVersionAutomationTest {

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
    fun `jira fix version automation executes with automation config and without added project version`() {
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
                    labelPattern = null,
                    fixVersionPattern = "fix_%1\\\$s.%2\\\$s",
                    targetStatusName = null
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )

        val givenIssueKey = "AT-290"
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

        val beforeAutomationFixVersions = jiraController.getIssueFixVersions(expectedIssueKey).map { it.name }
        assertTrue { !beforeAutomationFixVersions.contains(expectedFixVersion) }

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val automationResult: BuildResult = projectDir.runTask(givenJiraAutomationTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !assembleResult.output.contains("Task :app:getLastTagSnapshotRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagSnapshotDebug"),
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

        val afterAutomationFixVersion = jiraController.getIssueFixVersions(expectedIssueKey).map { it.name }
        assertTrue { afterAutomationFixVersion.contains(expectedFixVersion) }
    }

    @Test
    @Throws(IOException::class)
    fun `jira fix version automation executes with automation config and already added project version`() {
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
                    labelPattern = null,
                    fixVersionPattern = "fix_%1\\\$s.%2\\\$s",
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
            AT-290: Add test readme
            
            CHANGELOG: [AT-290] Задача для проверки работы BuildPublishPlugin с фиксверсией
        """.trimIndent()
        val givenAssembleTask = "assembleDebug"
        val givenJiraAutomationTask = "jiraAutomationDebug"
        val git = projectDir.initGit()
        val givenChangelogFile = projectDir.getFile("app/build/changelog-debug.txt")

        val expectedFixVersion = "fix_1.0.2"
        val expectedIssueKey = "AT-290"

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)

        val projectFixVersions = jiraController.getProjectVersions(projectKey)
        val fixVersion = projectFixVersions.find { it.name == expectedFixVersion }
        if (fixVersion == null) {
            val projectId = jiraController.getProjectId(projectKey)
            jiraController.createProjectVersion(projectId, expectedFixVersion)
        }
        jiraController.removeIssueFixVersion(expectedIssueKey, expectedFixVersion)

        val beforeAutomationFixVersions = jiraController.getIssueFixVersions(expectedIssueKey).map { it.name }
        assertTrue { !beforeAutomationFixVersions.contains(expectedFixVersion) }

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val automationResult: BuildResult = projectDir.runTask(givenJiraAutomationTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !assembleResult.output.contains("Task :app:getLastTagSnapshotRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagSnapshotDebug"),
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

        val afterAutomationFixVersions = jiraController.getIssueFixVersions(expectedIssueKey).map { it.name }
        assertTrue { afterAutomationFixVersions.contains(expectedFixVersion) }
    }

    @Test
    @Throws(IOException::class)
    fun `jira fix version automation executes with automation config and already attached plus added project version`() {
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
                    labelPattern = null,
                    fixVersionPattern = "fix_%1\\\$s.%2\\\$s",
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
            AT-290: Add test readme
            
            CHANGELOG: [AT-290] Задача для проверки работы BuildPublishPlugin с фиксверсией
        """.trimIndent()
        val givenAssembleTask = "assembleDebug"
        val givenJiraAutomationTask = "jiraAutomationDebug"
        val git = projectDir.initGit()
        val givenChangelogFile = projectDir.getFile("app/build/changelog-debug.txt")

        val expectedFixVersion = "fix_1.0.2"
        val expectedIssueKey = "AT-290"

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)

        val projectFixVersions = jiraController.getProjectVersions(projectKey)
        val fixVersion = projectFixVersions.find { it.name == expectedFixVersion }
        if (fixVersion == null) {
            val projectId = jiraController.getProjectId(projectKey)
            jiraController.createProjectVersion(projectId, expectedFixVersion)
        }
        jiraController.addIssueFixVersion(expectedIssueKey, expectedFixVersion)

        val beforeAutomationFixVersions = jiraController.getIssueFixVersions(expectedIssueKey).map { it.name }
        assertTrue { beforeAutomationFixVersions.contains(expectedFixVersion) }

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val automationResult: BuildResult = projectDir.runTask(givenJiraAutomationTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !assembleResult.output.contains("Task :app:getLastTagSnapshotRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagSnapshotDebug"),
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

        val afterAutomationFixVersions = jiraController.getIssueFixVersions(expectedIssueKey).map { it.name }
        assertTrue { afterAutomationFixVersions.contains(expectedFixVersion) }
    }

    @Test
    @Throws(IOException::class)
    fun `jira fix version automation executes with automation config, but without assemble`() {
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
                    labelPattern = null,
                    fixVersionPattern = "fix_%1\\\$s.%2\\\$s",
                    targetStatusName = null
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )
        val givenIssueKey = "AT-290"
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 = """
            $givenIssueKey: Add test readme
            
            CHANGELOG: [$givenIssueKey] Задача для проверки работы BuildPublishPlugin с фиксверсией
        """.trimIndent()
        val givenJiraAutomationTask = "jiraAutomationDebug"
        val git = projectDir.initGit()
        val givenChangelogFile = projectDir.getFile("app/build/changelog-debug.txt")

        val expectedIssueKey = "AT-290"
        val expectedFixVersion = "fix_1.0.2"

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

        val beforeAutomationFixVersions = jiraController.getIssueFixVersions(expectedIssueKey).map { it.name }
        assertTrue { !beforeAutomationFixVersions.contains(expectedFixVersion) }

        val automationResult: BuildResult = projectDir.runTask(givenJiraAutomationTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !automationResult.output.contains("Task :app:getLastTagSnapshotRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            automationResult.output.contains("Task :app:getLastTagSnapshotDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            automationResult.output.contains("BUILD SUCCESSFUL"),
            "Jira automation successful"
        )
        assertTrue(!givenOutputFileExists, "Output file not exists")

        val expectedChangelogFile =
            """
            • [$expectedIssueKey] Задача для проверки работы BuildPublishPlugin с фиксверсией
            """.trimIndent()

        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )

        val afterAutomationVersions = jiraController.getIssueFixVersions(expectedIssueKey).map { it.name }
        assertTrue { afterAutomationVersions.contains(expectedFixVersion) }
    }

    @Test
    @Throws(IOException::class)
    fun `jira fix version automation executes with automation config when jira task is not available`() {
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
                    labelPattern = null,
                    fixVersionPattern = "fix_%1\\\$s.%2\\\$s",
                    targetStatusName = null
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )
        val givenIssueKey = "AT-2909"
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

        val expectedIssueKey = "AT-2909"
        val expectedFixVersion = "fix_1.0.2"

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
            !assembleResult.output.contains("Task :app:getLastTagSnapshotRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagSnapshotDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build successful",
        )
        val output = automationResult.output
            .replace("\r\n", "\n")
            .replace("\r", "\n")
        assertTrue(
            output.contains("BUILD SUCCESSFUL"),
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
        assertTrue {
            output.contains(failedToAddFixVersionMessage(expectedIssueKey, expectedFixVersion))
        }
    }
}
