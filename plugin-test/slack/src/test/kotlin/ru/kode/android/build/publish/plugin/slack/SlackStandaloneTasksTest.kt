package ru.kode.android.build.publish.plugin.slack

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.core.task.TaskNames
import ru.kode.android.build.publish.plugin.test.utils.createNonAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.outputShouldContain
import ru.kode.android.build.publish.plugin.test.utils.runTask
import java.io.File

class SlackStandaloneTasksTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `sendSlackMessage task registered when plugin applied without Foundation or AGP`() {
        val projectDir = File(tempDir, "test-project")
        projectDir.createNonAndroidProject(
            pluginId = "ru.kode.android.build-publish-novo.slack",
            pluginConfigBlock =
                """
                buildPublishSlack {
                    bot {
                        common {
                            webhookUrl.set("https://hooks.slack.com/services/test")
                        }
                    }
                }
                """.trimIndent(),
        )

        val result = projectDir.runTask("tasks")

        result.outputShouldContain("BUILD SUCCESSFUL")
        result.outputShouldContain(TaskNames.Slack.SEND_MESSAGE)
    }

    @Test
    fun `sendSlackFile task registered when plugin applied without Foundation or AGP`() {
        val projectDir = File(tempDir, "test-project2")
        projectDir.createNonAndroidProject(
            pluginId = "ru.kode.android.build-publish-novo.slack",
            pluginConfigBlock =
                """
                buildPublishSlack {
                    bot {
                        common {
                            webhookUrl.set("https://hooks.slack.com/services/test")
                        }
                    }
                }
                """.trimIndent(),
        )

        val result = projectDir.runTask("tasks")

        result.outputShouldContain("BUILD SUCCESSFUL")
        result.outputShouldContain(TaskNames.Slack.SEND_FILE)
    }
}
