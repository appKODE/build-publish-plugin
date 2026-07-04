package ru.kode.android.build.publish.plugin.telegram

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.core.task.TaskNames
import ru.kode.android.build.publish.plugin.test.utils.createNonAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.outputShouldContain
import ru.kode.android.build.publish.plugin.test.utils.runTask
import java.io.File

class TelegramStandaloneTasksTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `sendTelegramMessage task registered when plugin applied without Foundation or AGP`() {
        val projectDir = File(tempDir, "test-project")
        projectDir.createNonAndroidProject(
            pluginId = "ru.kode.android.build-publish-novo.telegram",
            pluginConfigBlock =
                """
                buildPublishTelegram {
                    bots {
                        common {
                            bot("main") {
                                botId.set("123456:FAKE_TOKEN")
                                chat("builds") {
                                    chatId = "-100123456789"
                                }
                            }
                        }
                    }
                }
                """.trimIndent(),
        )

        val result = projectDir.runTask("tasks")

        result.outputShouldContain("BUILD SUCCESSFUL")
        result.outputShouldContain(TaskNames.Telegram.SEND_MESSAGE)
    }

    @Test
    fun `sendTelegramFile task registered when plugin applied without Foundation or AGP`() {
        val projectDir = File(tempDir, "test-project2")
        projectDir.createNonAndroidProject(
            pluginId = "ru.kode.android.build-publish-novo.telegram",
            pluginConfigBlock =
                """
                buildPublishTelegram {
                    bots {
                        common {
                            bot("main") {
                                botId.set("123456:FAKE_TOKEN")
                                chat("builds") {
                                    chatId = "-100123456789"
                                }
                            }
                        }
                    }
                }
                """.trimIndent(),
        )

        val result = projectDir.runTask("tasks")

        result.outputShouldContain("BUILD SUCCESSFUL")
        result.outputShouldContain(TaskNames.Telegram.SEND_FILE)
    }
}
