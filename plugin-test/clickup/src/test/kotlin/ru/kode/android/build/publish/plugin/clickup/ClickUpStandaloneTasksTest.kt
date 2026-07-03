package ru.kode.android.build.publish.plugin.clickup

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.core.task.TaskNames
import ru.kode.android.build.publish.plugin.test.utils.createNonAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.getFile
import ru.kode.android.build.publish.plugin.test.utils.outputShouldContain
import ru.kode.android.build.publish.plugin.test.utils.runTask
import java.io.File

class ClickUpStandaloneTasksTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `addClickUpTag task registered when plugin applied without Foundation or AGP`() {
        val projectDir = File(tempDir, "test-project")
        // Create the token file inside the project directory so project.file() can resolve it
        val tokenFile = projectDir.getFile("clickup_token.txt").also { it.writeText("fake-token") }

        projectDir.createNonAndroidProject(
            pluginId = "ru.kode.android.build-publish-novo.clickup",
            pluginConfigBlock =
                """
                buildPublishClickUp {
                    auth {
                        common {
                            apiTokenFile = project.file("${tokenFile.name}")
                        }
                    }
                    automation {
                        common {
                            workspaceName.set("my-workspace")
                        }
                    }
                }
                """.trimIndent(),
        )

        val result = projectDir.runTask("tasks")

        result.outputShouldContain("BUILD SUCCESSFUL")
        result.outputShouldContain(TaskNames.ClickUp.ADD_TAG)
    }
}
