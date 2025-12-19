package ru.kode.android.build.publish.plugin.play

import org.gradle.api.logging.Logger
import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.test.utils.AlwaysInfoLogger
import ru.kode.android.build.publish.plugin.test.utils.BuildType
import ru.kode.android.build.publish.plugin.test.utils.FoundationConfig
import ru.kode.android.build.publish.plugin.test.utils.TelegramConfig
import ru.kode.android.build.publish.plugin.test.utils.TelegramConfig.Chat
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

class PlayDistributionTest {

    private val logger: Logger = AlwaysInfoLogger()

    @TempDir
    lateinit var tempDir: File
    private lateinit var projectDir: File

    @BeforeEach
    fun setup() {
        projectDir = File(tempDir, "test-project")
    }

    @Test
    @Throws(IOException::class)
    fun `firebase build distribution available with distribution config without proxy and custom server`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"),
                BuildType("release")
            ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog = FoundationConfig.Changelog(
                        issueNumberPattern = "CEB-\\\\d+",
                        issueUrlPrefix = "${System.getProperty("JIRA_BASE_URL")}/browse/"
                    )
                ),
            telegramConfig = TelegramConfig(
                bots = TelegramConfig.Bots(
                    listOf(
                        TelegramConfig.Bot(
                            botName = "DistributionBot",
                            botId = System.getProperty("TELEGRAM_BOT_ID"),
                            botServerBaseUrl = System.getProperty("TELEGRAM_BOT_SERVER_BASE_URL"),
                            botServerUsername = System.getProperty("TELEGRAM_BOT_SERVER_USERNAME"),
                            botServerPassword = System.getProperty("TELEGRAM_BOT_SERVER_PASSWORD"),
                            chats = listOf(
                                Chat(
                                    chatName = "DistributionTest",
                                    chatId = System.getProperty("TELEGRAM_CHAT_ID"),
                                    topicId = null,
                                )
                            ),
                        )
                    )
                ),
                changelog = null,
                distribution = TelegramConfig.Distribution(
                    destinationBots = listOf(
                        TelegramConfig.DestinationBot(
                            botName = "DistributionBot",
                            chatNames = listOf("DistributionTest")
                        )
                    )
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
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDebug"
        val givenTelegramDistributionTask = "telegramDistributionUploadDebug"
        val git = projectDir.initGit()
        val givenOutputFile = projectDir.getFile("app/build/outputs/apk/debug/autotest-debug-vc2-$currentDate.apk")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)

        getChangelog()
            .split("\n")
            .forEachIndexed { index, changelogLine ->
                val givenCommitMessageN = """
                Add $index change in codebase
                
                CHANGELOG: $changelogLine
                """.trimIndent()
                projectDir.getFile("app/README${index}.md").writeText("This is test project")
                git.addAllAndCommit(givenCommitMessageN)
            }
        git.tag.addNamed(givenTagName2)


        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val distributionResult: BuildResult = projectDir.runTask(givenTelegramDistributionTask)

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
            distributionResult.output.contains("BUILD SUCCESSFUL"),
            "Telegram distribution successful"
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
    }
}

private fun getChangelog(): String {
    return """
[CEB-3243] [And] Mickey tried to fix the loader on Goofy’s form after settings got tangled, and navigation went bonkers
[CEB-3277] [Android] Donald’s transaction history exploded when he peeked into Daisy’s credit card details
    """.trimIndent()
}
