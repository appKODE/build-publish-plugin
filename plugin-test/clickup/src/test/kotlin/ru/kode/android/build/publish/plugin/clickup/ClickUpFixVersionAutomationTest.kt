package ru.kode.android.build.publish.plugin.clickup

import org.gradle.api.logging.Logger
import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
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
import java.io.File
import java.io.IOException

@Disabled // It disabled because free account has fixed amount of fields
class ClickUpFixVersionAutomationTest {

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
                override val bodyLogging: Boolean get() = false

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
    fun `clickup fix version automation executes with automation config and without added list field`() {
        val workspaceName = "Ilia Nekrasov\'s Workspace"

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
                    fixVersionPattern = "fix_%1\\\$s.%2\\\$s",
                    fixVersionFieldName = "Fix version",
                    tagPattern = null,
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )

        val givenIssueKey = "86c72yxu3"
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 = """
            $givenIssueKey: Add test readme
            
            CHANGELOG: [$givenIssueKey] Задача для проверки работы BuildPublishPlugin с фиксверсией
        """.trimIndent()
        val givenAssembleTask = "assembleDebug"
        val givenClickUpAutomationTask = "clickUpAutomationDebug"
        val git = projectDir.initGit()
        val givenChangelogFile = projectDir.getFile("app/build/changelog-debug.txt")

        val expectedFixVersion = "fix_1.0.2"
        val expectedIssueKey = "86c72yxu3"

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)

        val fixVersionId = clickUpController.getOrCreateCustomFieldId(workspaceName, "Fix version")
        clickUpController.clearCustomField(expectedIssueKey, fixVersionId)
        clickUpController.deleteCustomFieldFromList(workspaceName, fixVersionId)

        val beforeAutomationFixVersions = clickUpController.getTaskFields(expectedIssueKey).fields
        assertTrue { beforeAutomationFixVersions.all { it.id == fixVersionId && it.value.isNullOrBlank() } }

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
            • [$expectedIssueKey] Задача для проверки работы BuildPublishPlugin с фиксверсией
            """.trimIndent()

        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )

        val afterAutomationFixVersion = clickUpController.getTaskFields(expectedIssueKey).fields
        assertTrue { afterAutomationFixVersion.all { it.id == fixVersionId && it.value == expectedFixVersion } }
    }

    @Test
    @Throws(IOException::class)
    fun `clickup fix version automation executes with automation config and without added list field and assemble`() {
        val workspaceName = "Ilia Nekrasov\'s Workspace"

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
                    fixVersionPattern = "fix_%1\\\$s.%2\\\$s",
                    fixVersionFieldName = "Fix version",
                    tagPattern = null,
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )

        val givenIssueKey = "86c72yxu3"
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 = """
            $givenIssueKey: Add test readme
            
            CHANGELOG: [$givenIssueKey] Задача для проверки работы BuildPublishPlugin с фиксверсией
        """.trimIndent()
        val givenClickUpAutomationTask = "clickUpAutomationDebug"
        val git = projectDir.initGit()
        val givenChangelogFile = projectDir.getFile("app/build/changelog-debug.txt")

        val expectedFixVersion = "fix_1.0.2"
        val expectedIssueKey = "86c72yxu3"

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)

        val fixVersionId = clickUpController.getOrCreateCustomFieldId(workspaceName, "Fix version")
        clickUpController.clearCustomField(expectedIssueKey, fixVersionId)
        clickUpController.deleteCustomFieldFromList(workspaceName, fixVersionId)

        val beforeAutomationFixVersions = clickUpController.getTaskFields(expectedIssueKey).fields
        assertTrue { beforeAutomationFixVersions.all { it.id == fixVersionId && it.value.isNullOrBlank() } }

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
            • [$expectedIssueKey] Задача для проверки работы BuildPublishPlugin с фиксверсией
            """.trimIndent()

        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )

        val afterAutomationFixVersion = clickUpController.getTaskFields(expectedIssueKey).fields
        assertTrue { afterAutomationFixVersion.all { it.id == fixVersionId && it.value == expectedFixVersion } }
    }

    @Test
    @Throws(IOException::class)
    fun `clickup fix version automation executes with automation config and already added list field`() {
        val workspaceName = "Ilia Nekrasov\'s Workspace"

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
                    fixVersionPattern = "fix_%1\\\$s.%2\\\$s",
                    fixVersionFieldName = "Fix version",
                    tagPattern = null,
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )
        val givenIssueKey = "86c72yxu3"
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 = """
            86c72yxu3: Add test readme
            
            CHANGELOG: [$givenIssueKey] Задача для проверки работы BuildPublishPlugin с фиксверсией
        """.trimIndent()
        val givenAssembleTask = "assembleDebug"
        val givenClickUpAutomationTask = "clickUpAutomationDebug"
        val git = projectDir.initGit()
        val givenChangelogFile = projectDir.getFile("app/build/changelog-debug.txt")

        val expectedFixVersion = "fix_1.0.2"
        val expectedIssueKey = "86c72yxu3"

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)

        val fixVersionId = clickUpController.getOrCreateCustomFieldId(workspaceName, "Fix version")
        clickUpController.clearCustomField(expectedIssueKey, fixVersionId)
        val beforeAutomationFixVersions = clickUpController.getTaskFields(expectedIssueKey).fields

        assertTrue { beforeAutomationFixVersions.all { it.id == fixVersionId && it.value.isNullOrBlank() } }

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
            • [$expectedIssueKey] Задача для проверки работы BuildPublishPlugin с фиксверсией
            """.trimIndent()

        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )

        val afterAutomationFixVersion = clickUpController.getTaskFields(expectedIssueKey).fields
        assertTrue { afterAutomationFixVersion.all { it.id == fixVersionId && it.value == expectedFixVersion } }
    }

    @Test
    @Throws(IOException::class)
    fun `clickup fix version automation executes with automation config and already added list field without assemble`() {
        val workspaceName = "Ilia Nekrasov\'s Workspace"

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
                    fixVersionPattern = "fix_%1\\\$s.%2\\\$s",
                    fixVersionFieldName = "Fix version",
                    tagPattern = null,
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )
        val givenIssueKey = "86c72yxu3"
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 = """
            86c72yxu3: Add test readme
            
            CHANGELOG: [$givenIssueKey] Задача для проверки работы BuildPublishPlugin с фиксверсией
        """.trimIndent()
        val givenClickUpAutomationTask = "clickUpAutomationDebug"
        val git = projectDir.initGit()
        val givenChangelogFile = projectDir.getFile("app/build/changelog-debug.txt")

        val expectedFixVersion = "fix_1.0.2"
        val expectedIssueKey = "86c72yxu3"

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)

        val fixVersionId = clickUpController.getOrCreateCustomFieldId(workspaceName, "Fix version")
        clickUpController.clearCustomField(expectedIssueKey, fixVersionId)
        val beforeAutomationFixVersions = clickUpController.getTaskFields(expectedIssueKey).fields

        assertTrue { beforeAutomationFixVersions.all { it.id == fixVersionId && it.value.isNullOrBlank() } }

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
            • [$expectedIssueKey] Задача для проверки работы BuildPublishPlugin с фиксверсией
            """.trimIndent()

        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )

        val afterAutomationFixVersion = clickUpController.getTaskFields(expectedIssueKey).fields
        assertTrue { afterAutomationFixVersion.all { it.id == fixVersionId && it.value == expectedFixVersion } }
    }

    @Test
    @Throws(IOException::class)
    fun `clickup fix version automation executes with automation config and already attached and added list field`() {
        val workspaceName = "Ilia Nekrasov\'s Workspace"

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
                    fixVersionPattern = "fix_%1\\\$s.%2\\\$s",
                    fixVersionFieldName = "Fix version",
                    tagPattern = null,
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
            86c72yxu3: Add test readme
            
            CHANGELOG: [86c72yxu3] Задача для проверки работы BuildPublishPlugin с фиксверсией
        """.trimIndent()
        val givenAssembleTask = "assembleDebug"
        val givenClickUpAutomationTask = "clickUpAutomationDebug"
        val git = projectDir.initGit()
        val givenChangelogFile = projectDir.getFile("app/build/changelog-debug.txt")

        val expectedFixVersion = "fix_1.0.2"
        val expectedIssueKey = "86c72yxu3"

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)

        val fixVersionId = clickUpController.getOrCreateCustomFieldId(workspaceName, "Fix version")
        clickUpController.addFieldToTask(expectedIssueKey, fixVersionId, expectedFixVersion)

        val beforeAutomationFixVersions = clickUpController.getTaskFields(expectedIssueKey).fields
        assertTrue { beforeAutomationFixVersions.all { it.id == fixVersionId && it.value == expectedFixVersion } }

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
            • [$expectedIssueKey] Задача для проверки работы BuildPublishPlugin с фиксверсией
            """.trimIndent()

        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )

        val afterAutomationFixVersion = clickUpController.getTaskFields(expectedIssueKey).fields
        assertTrue { afterAutomationFixVersion.all { it.id == fixVersionId && it.value == expectedFixVersion } }
    }

    @Test
    @Throws(IOException::class)
    fun `clickup fix version automation executes with automation config and already attached and added list field without assemble`() {
        val workspaceName = "Ilia Nekrasov\'s Workspace"

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
                    fixVersionPattern = "fix_%1\\\$s.%2\\\$s",
                    fixVersionFieldName = "Fix version",
                    tagPattern = null,
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
            86c72yxu3: Add test readme
            
            CHANGELOG: [86c72yxu3] Задача для проверки работы BuildPublishPlugin с фиксверсией
        """.trimIndent()
        val givenClickUpAutomationTask = "clickUpAutomationDebug"
        val git = projectDir.initGit()
        val givenChangelogFile = projectDir.getFile("app/build/changelog-debug.txt")

        val expectedFixVersion = "fix_1.0.2"
        val expectedIssueKey = "86c72yxu3"

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)

        val fixVersionId = clickUpController.getOrCreateCustomFieldId(workspaceName, "Fix version")
        clickUpController.addFieldToTask(expectedIssueKey, fixVersionId, expectedFixVersion)

        val beforeAutomationFixVersions = clickUpController.getTaskFields(expectedIssueKey).fields
        assertTrue { beforeAutomationFixVersions.all { it.id == fixVersionId && it.value == expectedFixVersion } }

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
            • [$expectedIssueKey] Задача для проверки работы BuildPublishPlugin с фиксверсией
            """.trimIndent()

        assertEquals(
            expectedChangelogFile,
            givenChangelogFile.readText(),
            "Changelogs equality",
        )

        val afterAutomationFixVersion = clickUpController.getTaskFields(expectedIssueKey).fields
        assertTrue { afterAutomationFixVersion.all { it.id == fixVersionId && it.value == expectedFixVersion } }
    }

    @Test
    @Throws(IOException::class)
    fun `clickup fix version automation executes with automation config and when task is not available`() {
        val workspaceName = "Ilia Nekrasov\'s Workspace"

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
                    fixVersionPattern = "fix_%1\\\$s.%2\\\$s",
                    fixVersionFieldName = "Fix version",
                    tagPattern = null,
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )

        val givenIssueKey = "11172yxu1"
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage1 = "Initial commit"
        val givenCommitMessage2 = """
            $givenIssueKey: Add test readme
            
            CHANGELOG: [$givenIssueKey] Задача для проверки работы BuildPublishPlugin с фиксверсией
        """.trimIndent()
        val givenAssembleTask = "assembleDebug"
        val givenClickUpAutomationTask = "clickUpAutomationDebug"
        val git = projectDir.initGit()
        val givenChangelogFile = projectDir.getFile("app/build/changelog-debug.txt")

        val expectedIssueKey = "11172yxu1"

        git.addAllAndCommit(givenCommitMessage1)
        git.tag.addNamed(givenTagName1)
        projectDir.getFile("app/README.md").writeText("This is test project")
        git.addAllAndCommit(givenCommitMessage2)
        git.tag.addNamed(givenTagName2)

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
            • [$expectedIssueKey] Задача для проверки работы BuildPublishPlugin с фиксверсией
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
                        Failed to add field '10c97542-4317-4bea-bb2c-3c59ed057e30' to task '11172yxu1'
                        Unknown(code=401, reason={"err":"Oauth token not found","ECODE":"OAUTH_018"})
                    """.trimIndent()
                )
        }
    }

}
