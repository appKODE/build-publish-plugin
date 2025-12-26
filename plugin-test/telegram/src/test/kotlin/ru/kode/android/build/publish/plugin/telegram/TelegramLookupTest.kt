package ru.kode.android.build.publish.plugin.telegram

import org.gradle.api.logging.Logger
import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.Disabled
import ru.kode.android.build.publish.plugin.core.logger.PluginLogger
import ru.kode.android.build.publish.plugin.telegram.controller.TelegramController
import ru.kode.android.build.publish.plugin.telegram.controller.TelegramControllerFactory
import ru.kode.android.build.publish.plugin.telegram.controller.entity.TelegramLastMessage
import ru.kode.android.build.publish.plugin.telegram.messages.botWithoutChatMessage
import ru.kode.android.build.publish.plugin.telegram.messages.lookupSuccessMessage
import ru.kode.android.build.publish.plugin.test.utils.AlwaysInfoLogger
import ru.kode.android.build.publish.plugin.test.utils.BuildType
import ru.kode.android.build.publish.plugin.test.utils.FoundationConfig
import ru.kode.android.build.publish.plugin.test.utils.TelegramConfig
import ru.kode.android.build.publish.plugin.test.utils.TelegramConfig.Chat
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

@Disabled // It is disabled because it requires to write in chats each time
class TelegramLookupTest {

    private val logger: Logger = AlwaysInfoLogger()

    @TempDir
    lateinit var tempDir: File
    private lateinit var projectDir: File
    private lateinit var telegramController: TelegramController

    @BeforeEach
    fun setup() {
        projectDir = File(tempDir, "test-project")
        telegramController = TelegramControllerFactory.build(
            object : PluginLogger {
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
    fun `lookup chat id by chat name returns correctly if chat exists and message exists`() {
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
                            botName = "ChangelogBot",
                            botId = System.getProperty("TELEGRAM_BOT_ID"),
                            botServerBaseUrl = null,
                            botServerUsername = null,
                            botServerPassword = null,
                            chats = listOf(
                                Chat(
                                    chatName = "ChangelogTest",
                                    chatId = System.getProperty("TELEGRAM_LOOKUP_CHAT_ID"),
                                    topicId = null,
                                )
                            ),
                        )
                    )
                ),
                lookup = TelegramConfig.Lookup(
                    botName = "ChangelogBot",
                    chatName = "Changelog Test Lookup",
                    topicName = null,
                ),
                changelog = null,
                distribution = null

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
        val telegramLookupTask = "telegramLookupDebug"
        val git = projectDir.initGit()

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

        val changelogResult: BuildResult = projectDir.runTask(telegramLookupTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !changelogResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            !changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug not executed",
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Lookup successful",
        )
        assertTrue(!givenOutputFileExists, "Output file not exists")
        assertTrue(
            changelogResult.output.contains(
                lookupSuccessMessage(
                    botName = "ChangelogBot",
                    message = TelegramLastMessage(
                        chatId = System.getProperty("TELEGRAM_LOOKUP_CHAT_ID"),
                        chatName = "Changelog Test Lookup",
                        topicId = null,
                        topicName = null,
                        text = "dada",
                    )
                )
            ),
            "Config error message found",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `lookup chat id by chat name returns correctly if chat exists and message exists, chat not configured`() {
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
                            botName = "ChangelogBot",
                            botId = System.getProperty("TELEGRAM_BOT_ID"),
                            botServerBaseUrl = null,
                            botServerUsername = null,
                            botServerPassword = null,
                            chats = listOf(),
                        )
                    )
                ),
                lookup = TelegramConfig.Lookup(
                    botName = "ChangelogBot",
                    chatName = "Changelog Test Lookup",
                    topicName = null,
                ),
                changelog = null,
                distribution = null

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
        val telegramLookupTask = "telegramLookupDebug"
        val git = projectDir.initGit()

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

        val changelogResult: BuildResult = projectDir.runTask(telegramLookupTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !changelogResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            !changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug not executed",
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Lookup successful",
        )
        assertTrue(!givenOutputFileExists, "Output file not exists")
        assertTrue(
            changelogResult.output.contains(
                lookupSuccessMessage(
                    botName = "ChangelogBot",
                    message = TelegramLastMessage(
                        chatId = System.getProperty("TELEGRAM_LOOKUP_CHAT_ID"),
                        chatName = "Changelog Test Lookup",
                        topicId = null,
                        topicName = null,
                        text = "dada",
                    )
                )
            ),
            "Config error message found",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `lookup fails if chat exists and message exists, no chat id in config`() {
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
                            botName = "ChangelogBot",
                            botId = System.getProperty("TELEGRAM_BOT_ID"),
                            botServerBaseUrl = null,
                            botServerUsername = null,
                            botServerPassword = null,
                            chats = listOf(
                                Chat(
                                    chatName = "ChangelogTest",
                                    chatId = null,
                                    topicId = null,
                                )
                            ),
                        )
                    )
                ),
                lookup = TelegramConfig.Lookup(
                    botName = "ChangelogBot",
                    chatName = "Changelog Test Lookup",
                    topicName = null,
                ),
                changelog = null,
                distribution = null

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
        val telegramLookupTask = "telegramLookupDebug"
        val git = projectDir.initGit()

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

        val changelogResult: BuildResult = projectDir.runTaskWithFail(telegramLookupTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !changelogResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            !changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug not executed",
        )
        assertTrue(
            changelogResult.output.contains("BUILD FAILED"),
            "Lookup failed",
        )
        assertTrue(!givenOutputFileExists, "Output file not exists")
        val expectedMessage = botWithoutChatMessage("ChangelogBot")
        println(expectedMessage)
        assertTrue(
            changelogResult.output.contains(
                expectedMessage.replace("|", "  |"),
            ),
            "Config error message found",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `lookup channel id by chat name returns correctly if chat exists and message exists`() {
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
                            botName = "ChangelogBot",
                            botId = System.getProperty("TELEGRAM_BOT_ID"),
                            botServerBaseUrl = null,
                            botServerUsername = null,
                            botServerPassword = null,
                            chats = listOf(
                                Chat(
                                    chatName = "ChangelogTest",
                                    chatId = System.getProperty("TELEGRAM_LOOKUP_CHANNEL_ID"),
                                    topicId = null,
                                )
                            ),
                        )
                    )
                ),
                lookup = TelegramConfig.Lookup(
                    botName = "ChangelogBot",
                    chatName = "Changlog Test Channel Lookup",
                    topicName = null,
                ),
                changelog = null,
                distribution = null
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
        val telegramLookupTask = "telegramLookupDebug"
        val git = projectDir.initGit()

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

        val changelogResult: BuildResult = projectDir.runTask(telegramLookupTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !changelogResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            !changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug not executed",
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Lookup successful",
        )
        assertTrue(!givenOutputFileExists, "Output file not exists")
        assertTrue(
            changelogResult.output.contains(
                lookupSuccessMessage(
                    botName = "ChangelogBot",
                    message = TelegramLastMessage(
                        chatId = System.getProperty("TELEGRAM_LOOKUP_CHANNEL_ID"),
                        chatName = "Changlog Test Channel Lookup",
                        topicId = null,
                        topicName = null,
                        text = "haha",
                    )
                )
            ),
            "Config error message found",
        )
    }

    @Test
    @Throws(IOException::class)
    fun `lookup forum id by chat name returns correctly if chat exists and message exists`() {
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
                            botName = "ChangelogBot",
                            botId = System.getProperty("TELEGRAM_BOT_ID"),
                            botServerBaseUrl = null,
                            botServerUsername = null,
                            botServerPassword = null,
                            chats = listOf(
                                Chat(
                                    chatName = "ChangelogTest",
                                    chatId = System.getProperty("TELEGRAM_LOOKUP_CHAT_FORUM_ID"),
                                    topicId = System.getProperty("TELEGRAM_LOOKUP_CHAT_FORUM_TOPIC_ID"),
                                )
                            ),
                        )
                    )
                ),
                lookup = TelegramConfig.Lookup(
                    botName = "ChangelogBot",
                    chatName = "Changelog Test with Topic Lookup",
                    topicName = "TestTopic",
                ),
                changelog = null,
                distribution = null
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
        val telegramLookupTask = "telegramLookupDebug"
        val git = projectDir.initGit()

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

        val changelogResult: BuildResult = projectDir.runTask(telegramLookupTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !changelogResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            !changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug not executed",
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Lookup successful",
        )
        assertTrue(!givenOutputFileExists, "Output file not exists")
        val expectedLastMessage = TelegramLastMessage(
            chatId = System.getProperty("TELEGRAM_LOOKUP_CHAT_FORUM_ID"),
            chatName = "Changelog Test with Topic Lookup",
            topicId = System.getProperty("TELEGRAM_LOOKUP_CHAT_FORUM_TOPIC_ID"),
            topicName = "TestTopic",
            text = "topic",
        )
        assertTrue(
            changelogResult.output.contains(
                lookupSuccessMessage(
                    botName = "ChangelogBot",
                    message = expectedLastMessage
                )
            ),
            "Config error message found",
        )
    }
}

private fun getChangelog(): String {
    return """
[CEB-3243] [And] Mickey tried to fix the loader on Goofy’s form after settings got tangled, and navigation went bonkers
[CEB-3277] [Android] Donald’s transaction history exploded when he peeked into Daisy’s credit card details
[CEB-3158] [Android] Goofy’s final SBP screen shows all the goofy statuses after he paid his cookie subscription
    """.trimIndent()
}
