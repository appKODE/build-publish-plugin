package ru.kode.android.build.publish.plugin.clickup

import org.gradle.api.logging.Logger
import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.clickup.controller.ClickUpController
import ru.kode.android.build.publish.plugin.clickup.controller.factory.ClickUpControllerFactory
import ru.kode.android.build.publish.plugin.test.utils.AlwaysInfoLogger
import ru.kode.android.build.publish.plugin.test.utils.BuildType
import ru.kode.android.build.publish.plugin.test.utils.ClickUpConfig
import ru.kode.android.build.publish.plugin.test.utils.FoundationConfig
import ru.kode.android.build.publish.plugin.test.utils.addAllAndCommit
import ru.kode.android.build.publish.plugin.test.utils.addNamed
import ru.kode.android.build.publish.plugin.test.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.currentDate
import ru.kode.android.build.publish.plugin.test.utils.getFile
import ru.kode.android.build.publish.plugin.test.utils.initGit
import ru.kode.android.build.publish.plugin.test.utils.printFilesRecursively
import ru.kode.android.build.publish.plugin.test.utils.runTask
import java.io.File
import java.io.IOException

class ClickUpAllAutomationTest {

    private val logger: Logger = AlwaysInfoLogger()

    @TempDir
    lateinit var tempDir: File
    private lateinit var projectDir: File
    private lateinit var clickUpController: ClickUpController

    @BeforeEach
    fun setup() {
        projectDir = File(tempDir, "test-project")
        @Suppress("SpellCheckingInspection")
        clickUpController = ClickUpControllerFactory.build(
            token = System.getProperty("CLICKUP_TOKEN"),
            logger = logger,
        )
    }

    @Test
    @Throws(IOException::class)
    fun `clickup tag and fix version automation executes with automation config with multiple tasks, all correct status, status name lowercase`() {
        val workspaceName = "Ilia Nekrasov's Workspace"
        val clickUpTokenFile = projectDir.getFile("app/clickup_token.txt").apply {
            writeText(System.getProperty("CLICKUP_TOKEN"))
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
                        issueNumberPattern = "[a-z0-9]{9}",
                        issueUrlPrefix = "${System.getProperty("CLICKUP_BASE_URL")}/t/"
                    )
                ),
            clickUpConfig = ClickUpConfig(
                auth = ClickUpConfig.Auth(
                    apiTokenFilePath = clickUpTokenFile.name
                ),
                automation = ClickUpConfig.Automation(
                    workspaceName = workspaceName,
                    fixVersionPattern = "fix_%1\\\$s.%2\\\$s",
                    fixVersionFieldName = "Fix version",
                    tagPattern = "fix_%1\\\$s.%2\\\$s"
                )
            ),
            topBuildFileContent = """
            plugins {
                id 'ru.kode.android.build-publish-novo.foundation' apply false
            }
        """.trimIndent()
        )

        val givenIssueKey1 = "86c72yxu2"
        val givenIssueKey2 = "86c734dhf"
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 = """
                $givenIssueKey1: Add test readme 1
                
                CHANGELOG: [$givenIssueKey1] Задача 1 для проверки работы BuildPublishPlugin
            """.trimIndent()
        val givenCommitMessage3 = """
                $givenIssueKey2: Add test readme 2
                
                CHANGELOG: [$givenIssueKey2] Задача 2 для проверки работы BuildPublishPlugin
            """.trimIndent()

        val givenAssembleTask = "assembleDebug"
        val givenClickUpAutomationTask = "clickUpAutomationDebug"
        val git = projectDir.initGit()
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc2-$currentDate.apk")
        val givenChangelogFile = projectDir.getFile("app/build/changelog-debug.txt")

        val expectedTag = "fix_1.0.2"
        val expectedFixVersion = "fix_1.0.2"
        val fixVersionFieldId = clickUpController.getOrCreateCustomFieldId(workspaceName, "Fix version")

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.addAllAndCommit(givenCommitMessage3)
        git.tag.addNamed(givenTagName2)

        listOf(givenIssueKey1, givenIssueKey2).forEach { issueKey ->
            val tags = clickUpController.getTaskTags(issueKey).tags
            if (tags.contains(expectedTag)) {
                clickUpController.removeTag(issueKey, expectedTag)
            }

            clickUpController.clearCustomField(issueKey, fixVersionFieldId)
        }

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val automationResult: BuildResult = projectDir.runTask(givenClickUpAutomationTask)

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
            "ClickUp automation successful"
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")

        val expectedChangelogFile = """
            • [$givenIssueKey2] Задача 2 для проверки работы BuildPublishPlugin
            • [$givenIssueKey1] Задача 1 для проверки работы BuildPublishPlugin
            """.trimIndent()

        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )

        val tagsAfterAutomation1 = clickUpController.getTaskTags(givenIssueKey1).tags
        assertTrue(tagsAfterAutomation1.contains(expectedTag), "Expected tag $expectedTag to be added to the first task")

        val tagsAfterAutomation2 = clickUpController.getTaskTags(givenIssueKey2).tags
        assertTrue(tagsAfterAutomation2.contains(expectedTag), "Expected tag $expectedTag to be added to the second task")

        val fixVersions1 = clickUpController.getTaskFields(givenIssueKey1).fields
            .find { it.id == fixVersionFieldId }?.value
        assertEquals(expectedFixVersion, fixVersions1, "Expected fix version $expectedFixVersion to be set for first task")

        val fixVersions2 = clickUpController.getTaskFields(givenIssueKey2).fields
            .find { it.id == fixVersionFieldId }?.value
        assertEquals(expectedFixVersion, fixVersions2, "Expected fix version $expectedFixVersion to be set for second task")
    }
}
