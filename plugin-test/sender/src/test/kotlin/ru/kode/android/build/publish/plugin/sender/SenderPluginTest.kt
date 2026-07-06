package ru.kode.android.build.publish.plugin.sender

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.core.task.TaskNames
import ru.kode.android.build.publish.plugin.test.utils.createNonAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.getFile
import ru.kode.android.build.publish.plugin.test.utils.outputShouldContain
import ru.kode.android.build.publish.plugin.test.utils.runTask
import java.io.File

class SenderPluginTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `sender plugin applies without Foundation or AGP`() {
        val projectDir = File(tempDir, "test-project")
        projectDir.createNonAndroidProject(
            pluginId = "ru.kode.android.build-publish-novo.sender",
            pluginConfigBlock = "",
        )

        val result = projectDir.runTask("tasks")

        result.outputShouldContain("BUILD SUCCESSFUL")
    }

    @Test
    fun `slack tasks registered in sender plugin when slack block configured`() {
        val projectDir = File(tempDir, "test-slack")
        projectDir.createNonAndroidProject(
            pluginId = "ru.kode.android.build-publish-novo.sender",
            pluginConfigBlock =
                """
                buildPublishSender {
                    slack {
                        webhookUrl.set("https://hooks.slack.com/services/test")
                    }
                }
                """.trimIndent(),
        )

        val result = projectDir.runTask("tasks")

        result.outputShouldContain("BUILD SUCCESSFUL")
        result.outputShouldContain("sendSlackMessage")
    }

    @Test
    fun `telegram tasks registered in sender plugin when telegram block configured`() {
        val projectDir = File(tempDir, "test-telegram")
        projectDir.createNonAndroidProject(
            pluginId = "ru.kode.android.build-publish-novo.sender",
            pluginConfigBlock =
                """
                buildPublishSender {
                    telegram {
                        botId.set("123456:TOKEN")
                        chatId.set("-100123456789")
                    }
                }
                """.trimIndent(),
        )

        val result = projectDir.runTask("tasks")

        result.outputShouldContain("BUILD SUCCESSFUL")
        result.outputShouldContain("sendTelegramMessage")
    }

    @Test
    fun `nextcloud tasks registered in sender plugin when nextcloud block configured`() {
        val projectDir = File(tempDir, "test-nextcloud")
        projectDir.createNonAndroidProject(
            pluginId = "ru.kode.android.build-publish-novo.sender",
            pluginConfigBlock =
                """
                buildPublishSender {
                    nextcloud {
                        baseUrl.set("https://cloud.example.com")
                        username.set("mobile-bot")
                        password.set("secret")
                    }
                }
                """.trimIndent(),
        )

        val result = projectDir.runTask("tasks")

        result.outputShouldContain("BUILD SUCCESSFUL")
        result.outputShouldContain(TaskNames.Nextcloud.UPLOAD)
    }

    @Test
    fun `jira tasks registered in sender plugin when jira block configured`() {
        val projectDir = File(tempDir, "test-jira")
        projectDir.createNonAndroidProject(
            pluginId = "ru.kode.android.build-publish-novo.sender",
            pluginConfigBlock =
                """
                buildPublishSender {
                    jira {
                        baseUrl.set("https://jira.example.com")
                        username.set("bot")
                        apiToken.set("token")
                    }
                }
                """.trimIndent(),
        )

        val result = projectDir.runTask("tasks")

        result.outputShouldContain("BUILD SUCCESSFUL")
        result.outputShouldContain(TaskNames.Jira.ADD_FIX_VERSION)
        result.outputShouldContain(TaskNames.Jira.ADD_LABEL)
        result.outputShouldContain(TaskNames.Jira.TRANSITION_ISSUE)
    }

    @Test
    fun `confluence tasks registered in sender plugin when confluence block configured`() {
        val projectDir = File(tempDir, "test-confluence")
        projectDir.createNonAndroidProject(
            pluginId = "ru.kode.android.build-publish-novo.sender",
            pluginConfigBlock =
                """
                buildPublishSender {
                    confluence {
                        baseUrl.set("https://confluence.example.com")
                        username.set("bot")
                        apiToken.set("token")
                    }
                }
                """.trimIndent(),
        )

        val result = projectDir.runTask("tasks")

        result.outputShouldContain("BUILD SUCCESSFUL")
        result.outputShouldContain(TaskNames.Confluence.UPLOAD)
        result.outputShouldContain(TaskNames.Confluence.ADD_COMMENT)
    }

    @Test
    fun `clickup tasks registered in sender plugin when clickup block configured`() {
        val projectDir = File(tempDir, "test-clickup")
        // Create the token file inside the project directory so project.file() can resolve it
        val tokenFile = projectDir.getFile("clickup_token.txt").also { it.writeText("fake-token") }
        projectDir.createNonAndroidProject(
            pluginId = "ru.kode.android.build-publish-novo.sender",
            pluginConfigBlock =
                """
                buildPublishSender {
                    clickUp {
                        apiTokenFile = project.file("${tokenFile.name}")
                    }
                }
                """.trimIndent(),
        )

        val result = projectDir.runTask("tasks")

        result.outputShouldContain("BUILD SUCCESSFUL")
        result.outputShouldContain(TaskNames.ClickUp.ADD_TAG)
        result.outputShouldContain(TaskNames.ClickUp.ADD_FIX_VERSION)
    }
}
