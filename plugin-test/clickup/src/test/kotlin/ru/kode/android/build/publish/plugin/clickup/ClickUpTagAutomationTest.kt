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
import ru.kode.android.build.publish.plugin.core.logger.PluginLogger
import ru.kode.android.build.publish.plugin.test.utils.AlwaysInfoLogger
import ru.kode.android.build.publish.plugin.test.utils.BuildType
import ru.kode.android.build.publish.plugin.test.utils.ClickUpConfig
import ru.kode.android.build.publish.plugin.test.utils.FoundationConfig
import ru.kode.android.build.publish.plugin.test.utils.addAllAndCommit
import ru.kode.android.build.publish.plugin.test.utils.addNamed
import ru.kode.android.build.publish.plugin.test.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.getFile
import ru.kode.android.build.publish.plugin.test.utils.initGit
import ru.kode.android.build.publish.plugin.test.utils.printFilesRecursively
import ru.kode.android.build.publish.plugin.test.utils.runTask
import ru.kode.android.build.publish.plugin.test.utils.runTaskWithFail
import java.io.File
import java.io.IOException

class ClickUpTagAutomationTest {

    private val logger: Logger = AlwaysInfoLogger()

    @TempDir
    lateinit var tempDir: File
    private lateinit var projectDir: File
    private lateinit var clickUpController: ClickUpController

    @BeforeEach
    fun setup() {
        projectDir = File(tempDir, "test-project")
        clickUpController = ClickUpControllerFactory.build(
            token = System.getProperty("CLICKUP_TOKEN"),
            logger = object : PluginLogger {
                override fun info(message: String, exception: Throwable?) {
                    logger.info(message, exception)
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
            },
        )
    }

    @Test
    @Throws(IOException::class)
    fun `clickup automation not available without automation config`() {
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
                ),
            clickUpConfig = ClickUpConfig(
                auth = ClickUpConfig.Auth(
                    apiTokenFilePath = clickUpTokenFile.name
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
        val givenClickUpAutomationTask = "clickUpAutomationDebug"
        val git = projectDir.initGit()

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val assembleResult: BuildResult = projectDir.runTaskWithFail(givenAssembleTask)
        val automationResult: BuildResult = projectDir.runTaskWithFail(givenClickUpAutomationTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc1-\\d{8}\\.apk")) }
            ?: false

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
            "ClickUp automation failed"
        )
        assertTrue(!givenOutputFileExists, "Output file not exists")
    }

    @Test
    @Throws(IOException::class)
    fun `clickup tag automation executes with automation config without proxy`() {
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
                        issueNumberPattern = "[a-zA-Z0-9]+\\\\d+",
                        issueUrlPrefix = "${System.getProperty("CLICKUP_BASE_URL")}/t/"
                    )
                ),
            clickUpConfig = ClickUpConfig(
                auth = ClickUpConfig.Auth(
                    apiTokenFilePath = clickUpTokenFile.name
                ),
                automation = ClickUpConfig.Automation(
                    workspaceName = workspaceName,
                    fixVersionPattern = null,
                    fixVersionFieldName = null,
                    tagPattern = "fix_%1\\\$s.%2\\\$s"
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )

        val givenIssueKey = "86c72yxu4"
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 = """
            $givenIssueKey: Add test readme
            
            CHANGELOG: [$givenIssueKey] Задача для проверки работы BuildPublishPlugin с тегами
        """.trimIndent()
        val givenAssembleTask = "assembleDebug"
        val givenClickUpAutomationTask = "clickUpAutomationDebug"
        val git = projectDir.initGit()
        val givenChangelogFile = projectDir.getFile("app/build/changelog-debug.txt")

        val expectedTag = "fix_1.0.2"
        val expectedIssueKey = "86c72yxu4"

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)

        val tagsBeforeAutomation = clickUpController.getTaskTags(expectedIssueKey).tags
        if (tagsBeforeAutomation.contains(expectedTag)) {
            clickUpController.removeTag(expectedIssueKey, expectedTag)
        }

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val automationResult: BuildResult = projectDir.runTask(givenClickUpAutomationTask)

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
            "ClickUp automation successful"
        )
        assertTrue(givenOutputFileExists, "Output file exists")

        val expectedChangelogFile =
            """
            • [$expectedIssueKey] Задача для проверки работы BuildPublishPlugin с тегами
            """.trimIndent()

        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )

        val tagsAfterAutomation = clickUpController.getTaskTags(expectedIssueKey).tags
        assertTrue(tagsAfterAutomation.contains(expectedTag), "Expected tag $expectedTag to be added to the task")
    }

    @Test
    @Throws(IOException::class)
    fun `clickup tag automation executes with automation config with proxy`() {
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
                        issueNumberPattern = "[a-zA-Z0-9]+\\\\d+",
                        issueUrlPrefix = "${System.getProperty("CLICKUP_BASE_URL")}/t/"
                    )
                ),
            clickUpConfig = ClickUpConfig(
                auth = ClickUpConfig.Auth(
                    apiTokenFilePath = clickUpTokenFile.name
                ),
                automation = ClickUpConfig.Automation(
                    workspaceName = workspaceName,
                    fixVersionPattern = null,
                    fixVersionFieldName = null,
                    tagPattern = "fix_%1\\\$s.%2\\\$s"
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )

        val givenIssueKey = "86c72yxu4"
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 = """
            $givenIssueKey: Add test readme
            
            CHANGELOG: [$givenIssueKey] Задача для проверки работы BuildPublishPlugin с тегами
        """.trimIndent()
        val givenAssembleTask = "assembleDebug"
        val givenClickUpAutomationTask = "clickUpAutomationDebug"
        val git = projectDir.initGit()
        val givenChangelogFile = projectDir.getFile("app/build/changelog-debug.txt")

        val expectedTag = "fix_1.0.2"
        val expectedIssueKey = "86c72yxu4"

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)

        val tagsBeforeAutomation = clickUpController.getTaskTags(expectedIssueKey).tags
        if (tagsBeforeAutomation.contains(expectedTag)) {
            clickUpController.removeTag(expectedIssueKey, expectedTag)
        }

        val proxyProps = mapOf(
            "https.proxyUser" to System.getProperty("PROXY_USER"),
            "https.proxyPassword" to System.getProperty("PROXY_PASSWORD"),
            "https.proxyHost" to System.getProperty("PROXY_HOST"),
            "https.proxyPort" to System.getProperty("PROXY_PORT")
        )
        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask, proxyProps)
        val automationResult: BuildResult = projectDir.runTask(givenClickUpAutomationTask, proxyProps)

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
            "ClickUp automation successful"
        )
        assertTrue(givenOutputFileExists, "Output file exists")

        val expectedChangelogFile =
            """
            • [$expectedIssueKey] Задача для проверки работы BuildPublishPlugin с тегами
            """.trimIndent()

        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )

        val tagsAfterAutomation = clickUpController.getTaskTags(expectedIssueKey).tags
        assertTrue(tagsAfterAutomation.contains(expectedTag), "Expected tag $expectedTag to be added to the task")
    }

    @Test
    @Throws(IOException::class)
    fun `clickup tag automation executes with automation config and without assemble without proxy`() {
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
                        issueNumberPattern = "[a-zA-Z0-9]+\\\\d+",
                        issueUrlPrefix = "${System.getProperty("CLICKUP_BASE_URL")}/t/"
                    )
                ),
            clickUpConfig = ClickUpConfig(
                auth = ClickUpConfig.Auth(
                    apiTokenFilePath = clickUpTokenFile.name
                ),
                automation = ClickUpConfig.Automation(
                    workspaceName = workspaceName,
                    fixVersionPattern = null,
                    fixVersionFieldName = null,
                    tagPattern = "fix_%1\\\$s.%2\\\$s"
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )

        val givenIssueKey = "86c72yxu4"
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 = """
            $givenIssueKey: Add test readme
            
            CHANGELOG: [$givenIssueKey] Задача для проверки работы BuildPublishPlugin с тегами
        """.trimIndent()
        val givenClickUpAutomationTask = "clickUpAutomationDebug"
        val git = projectDir.initGit()
        val givenChangelogFile = projectDir.getFile("app/build/changelog-debug.txt")

        val expectedTag = "fix_1.0.2"
        val expectedIssueKey = "86c72yxu4"

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)

        val tagsBeforeAutomation = clickUpController.getTaskTags(expectedIssueKey).tags
        if (tagsBeforeAutomation.contains(expectedTag)) {
            clickUpController.removeTag(expectedIssueKey, expectedTag)
        }

        val automationResult: BuildResult = projectDir.runTask(givenClickUpAutomationTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

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
            "ClickUp automation successful"
        )
        assertTrue(!givenOutputFileExists, "Output file not exists")

        val expectedChangelogFile =
            """
            • [$expectedIssueKey] Задача для проверки работы BuildPublishPlugin с тегами
            """.trimIndent()

        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )

        val tagsAfterAutomation = clickUpController.getTaskTags(expectedIssueKey).tags
        assertTrue(tagsAfterAutomation.contains(expectedTag), "Expected tag $expectedTag to be added to the task")
    }

    @Test
    @Throws(IOException::class)
    fun `clickup tag automation executes with automation config and without assemble with proxy`() {
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
                        issueNumberPattern = "[a-zA-Z0-9]+\\\\d+",
                        issueUrlPrefix = "${System.getProperty("CLICKUP_BASE_URL")}/t/"
                    )
                ),
            clickUpConfig = ClickUpConfig(
                auth = ClickUpConfig.Auth(
                    apiTokenFilePath = clickUpTokenFile.name
                ),
                automation = ClickUpConfig.Automation(
                    workspaceName = workspaceName,
                    fixVersionPattern = null,
                    fixVersionFieldName = null,
                    tagPattern = "fix_%1\\\$s.%2\\\$s"
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )

        val givenIssueKey = "86c72yxu4"
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 = """
            $givenIssueKey: Add test readme
            
            CHANGELOG: [$givenIssueKey] Задача для проверки работы BuildPublishPlugin с тегами
        """.trimIndent()
        val givenClickUpAutomationTask = "clickUpAutomationDebug"
        val git = projectDir.initGit()
        val givenChangelogFile = projectDir.getFile("app/build/changelog-debug.txt")

        val expectedTag = "fix_1.0.2"
        val expectedIssueKey = "86c72yxu4"

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)

        val tagsBeforeAutomation = clickUpController.getTaskTags(expectedIssueKey).tags
        if (tagsBeforeAutomation.contains(expectedTag)) {
            clickUpController.removeTag(expectedIssueKey, expectedTag)
        }

        val proxyProps = mapOf(
            "https.proxyUser" to System.getProperty("PROXY_USER"),
            "https.proxyPassword" to System.getProperty("PROXY_PASSWORD"),
            "https.proxyHost" to System.getProperty("PROXY_HOST"),
            "https.proxyPort" to System.getProperty("PROXY_PORT")
        )
        val automationResult: BuildResult = projectDir.runTask(givenClickUpAutomationTask, proxyProps)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

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
            "ClickUp automation successful"
        )
        assertTrue(!givenOutputFileExists, "Output file not exists")

        val expectedChangelogFile =
            """
            • [$expectedIssueKey] Задача для проверки работы BuildPublishPlugin с тегами
            """.trimIndent()

        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )

        val tagsAfterAutomation = clickUpController.getTaskTags(expectedIssueKey).tags
        assertTrue(tagsAfterAutomation.contains(expectedTag), "Expected tag $expectedTag to be added to the task")
    }

    @Test
    @Throws(IOException::class)
    fun `clickup tag automation executes when task already has tag without proxy`() {
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
                        issueNumberPattern = "[a-zA-Z0-9]+\\\\d+",
                        issueUrlPrefix = "${System.getProperty("CLICKUP_BASE_URL")}/t/"
                    )
                ),
            clickUpConfig = ClickUpConfig(
                auth = ClickUpConfig.Auth(
                    apiTokenFilePath = clickUpTokenFile.name
                ),
                automation = ClickUpConfig.Automation(
                    workspaceName = workspaceName,
                    fixVersionPattern = null,
                    fixVersionFieldName = null,
                    tagPattern = "fix_%1\\\$s.%2\\\$s"
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )

        val givenIssueKey = "86c72yxu4"
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 = """
            $givenIssueKey: Add test readme
            
            CHANGELOG: [$givenIssueKey] Задача для проверки работы BuildPublishPlugin с тегами
        """.trimIndent()
        val givenAssembleTask = "assembleDebug"
        val givenClickUpAutomationTask = "clickUpAutomationDebug"
        val git = projectDir.initGit()
        val givenChangelogFile = projectDir.getFile("app/build/changelog-debug.txt")

        val expectedTag = "fix_1.0.2"
        val expectedIssueKey = "86c72yxu4"

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)

        val tagsBeforeAutomation = clickUpController.getTaskTags(expectedIssueKey).tags

        if (!tagsBeforeAutomation.contains(expectedTag)) {
            clickUpController.addTagToTask(expectedIssueKey, expectedTag)
        }

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val automationResult: BuildResult = projectDir.runTask(givenClickUpAutomationTask)

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
            "ClickUp automation successful"
        )
        assertTrue(givenOutputFileExists, "Output file exists")

        val expectedChangelogFile =
            """
            • [$expectedIssueKey] Задача для проверки работы BuildPublishPlugin с тегами
            """.trimIndent()

        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )

        val tagsAfterAutomation = clickUpController.getTaskTags(expectedIssueKey).tags
        assertTrue(tagsAfterAutomation.contains(expectedTag), "Expected tag $expectedTag to remain on the task")
    }

    @Test
    @Throws(IOException::class)
    fun `clickup tag automation executes when task already has tag with proxy`() {
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
                        issueNumberPattern = "[a-zA-Z0-9]+\\\\d+",
                        issueUrlPrefix = "${System.getProperty("CLICKUP_BASE_URL")}/t/"
                    )
                ),
            clickUpConfig = ClickUpConfig(
                auth = ClickUpConfig.Auth(
                    apiTokenFilePath = clickUpTokenFile.name
                ),
                automation = ClickUpConfig.Automation(
                    workspaceName = workspaceName,
                    fixVersionPattern = null,
                    fixVersionFieldName = null,
                    tagPattern = "fix_%1\\\$s.%2\\\$s"
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )

        val givenIssueKey = "86c72yxu4"
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 = """
            $givenIssueKey: Add test readme
            
            CHANGELOG: [$givenIssueKey] Задача для проверки работы BuildPublishPlugin с тегами
        """.trimIndent()
        val givenAssembleTask = "assembleDebug"
        val givenClickUpAutomationTask = "clickUpAutomationDebug"
        val git = projectDir.initGit()
        val givenChangelogFile = projectDir.getFile("app/build/changelog-debug.txt")

        val expectedTag = "fix_1.0.2"
        val expectedIssueKey = "86c72yxu4"

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)

        val tagsBeforeAutomation = clickUpController.getTaskTags(expectedIssueKey).tags

        if (!tagsBeforeAutomation.contains(expectedTag)) {
            clickUpController.addTagToTask(expectedIssueKey, expectedTag)
        }

        val proxyProps = mapOf(
            "https.proxyUser" to System.getProperty("PROXY_USER"),
            "https.proxyPassword" to System.getProperty("PROXY_PASSWORD"),
            "https.proxyHost" to System.getProperty("PROXY_HOST"),
            "https.proxyPort" to System.getProperty("PROXY_PORT")
        )
        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask, proxyProps)
        val automationResult: BuildResult = projectDir.runTask(givenClickUpAutomationTask, proxyProps)

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
            "ClickUp automation successful"
        )
        assertTrue(givenOutputFileExists, "Output file exists")

        val expectedChangelogFile =
            """
            • [$expectedIssueKey] Задача для проверки работы BuildPublishPlugin с тегами
            """.trimIndent()

        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )

        val tagsAfterAutomation = clickUpController.getTaskTags(expectedIssueKey).tags
        assertTrue(tagsAfterAutomation.contains(expectedTag), "Expected tag $expectedTag to remain on the task")
    }

    @Test
    @Throws(IOException::class)
    fun `clickup tag automation executes when task already has tag and without assemble without proxy`() {
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
                        issueNumberPattern = "[a-zA-Z0-9]+\\\\d+",
                        issueUrlPrefix = "${System.getProperty("CLICKUP_BASE_URL")}/t/"
                    )
                ),
            clickUpConfig = ClickUpConfig(
                auth = ClickUpConfig.Auth(
                    apiTokenFilePath = clickUpTokenFile.name
                ),
                automation = ClickUpConfig.Automation(
                    workspaceName = workspaceName,
                    fixVersionPattern = null,
                    fixVersionFieldName = null,
                    tagPattern = "fix_%1\\\$s.%2\\\$s"
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )

        val givenIssueKey = "86c72yxu4"
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 = """
            $givenIssueKey: Add test readme
            
            CHANGELOG: [$givenIssueKey] Задача для проверки работы BuildPublishPlugin с тегами
        """.trimIndent()
        val givenClickUpAutomationTask = "clickUpAutomationDebug"
        val git = projectDir.initGit()
        val givenChangelogFile = projectDir.getFile("app/build/changelog-debug.txt")

        val expectedTag = "fix_1.0.2"
        val expectedIssueKey = "86c72yxu4"

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)

        val tagsBeforeAutomation = clickUpController.getTaskTags(expectedIssueKey).tags

        if (!tagsBeforeAutomation.contains(expectedTag)) {
            clickUpController.addTagToTask(expectedIssueKey, expectedTag)
        }

        val automationResult: BuildResult = projectDir.runTask(givenClickUpAutomationTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

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
            "ClickUp automation successful"
        )
        assertTrue(!givenOutputFileExists, "Output file not exists")

        val expectedChangelogFile =
            """
            • [$expectedIssueKey] Задача для проверки работы BuildPublishPlugin с тегами
            """.trimIndent()

        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )

        val tagsAfterAutomation = clickUpController.getTaskTags(expectedIssueKey).tags
        assertTrue(tagsAfterAutomation.contains(expectedTag), "Expected tag $expectedTag to remain on the task")
    }

    @Test
    @Throws(IOException::class)
    fun `clickup tag automation executes when task already has tag and without assemble with proxy`() {
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
                        issueNumberPattern = "[a-zA-Z0-9]+\\\\d+",
                        issueUrlPrefix = "${System.getProperty("CLICKUP_BASE_URL")}/t/"
                    )
                ),
            clickUpConfig = ClickUpConfig(
                auth = ClickUpConfig.Auth(
                    apiTokenFilePath = clickUpTokenFile.name
                ),
                automation = ClickUpConfig.Automation(
                    workspaceName = workspaceName,
                    fixVersionPattern = null,
                    fixVersionFieldName = null,
                    tagPattern = "fix_%1\\\$s.%2\\\$s"
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )

        val givenIssueKey = "86c72yxu4"
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 = """
            $givenIssueKey: Add test readme
            
            CHANGELOG: [$givenIssueKey] Задача для проверки работы BuildPublishPlugin с тегами
        """.trimIndent()
        val givenClickUpAutomationTask = "clickUpAutomationDebug"
        val git = projectDir.initGit()
        val givenChangelogFile = projectDir.getFile("app/build/changelog-debug.txt")

        val expectedTag = "fix_1.0.2"
        val expectedIssueKey = "86c72yxu4"

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)

        val tagsBeforeAutomation = clickUpController.getTaskTags(expectedIssueKey).tags

        if (!tagsBeforeAutomation.contains(expectedTag)) {
            clickUpController.addTagToTask(expectedIssueKey, expectedTag)
        }

        val proxyProps = mapOf(
            "https.proxyUser" to System.getProperty("PROXY_USER"),
            "https.proxyPassword" to System.getProperty("PROXY_PASSWORD"),
            "https.proxyHost" to System.getProperty("PROXY_HOST"),
            "https.proxyPort" to System.getProperty("PROXY_PORT")
        )
        val automationResult: BuildResult = projectDir.runTask(givenClickUpAutomationTask, proxyProps)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

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
            "ClickUp automation successful"
        )
        assertTrue(!givenOutputFileExists, "Output file not exists")

        val expectedChangelogFile =
            """
            • [$expectedIssueKey] Задача для проверки работы BuildPublishPlugin с тегами
            """.trimIndent()

        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )

        val tagsAfterAutomation = clickUpController.getTaskTags(expectedIssueKey).tags
        assertTrue(tagsAfterAutomation.contains(expectedTag), "Expected tag $expectedTag to remain on the task")
    }
}
