package ru.kode.android.build.publish.plugin.sender

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.test.utils.createNonAndroidProject
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
}
