package ru.kode.android.build.publish.plugin.confluence

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.core.task.TaskNames
import ru.kode.android.build.publish.plugin.test.utils.createNonAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.outputShouldContain
import ru.kode.android.build.publish.plugin.test.utils.runTask
import java.io.File

class ConfluenceStandaloneTasksTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `uploadToConfluence task registered when plugin applied without Foundation or AGP`() {
        val projectDir = File(tempDir, "test-project")
        projectDir.createNonAndroidProject(
            pluginId = "ru.kode.android.build-publish-novo.confluence",
            pluginConfigBlock =
                """
                buildPublishConfluence {
                    auth {
                        common {
                            baseUrl.set("https://confluence.example.com")
                            credentials.username.set("user")
                            credentials.password.set("pass")
                        }
                    }
                }
                """.trimIndent(),
        )

        val result = projectDir.runTask("tasks")

        result.outputShouldContain("BUILD SUCCESSFUL")
        result.outputShouldContain(TaskNames.Confluence.UPLOAD)
    }
}
