package ru.kode.android.build.publish.plugin.vk

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.core.task.TaskNames
import ru.kode.android.build.publish.plugin.test.utils.createNonAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.outputShouldContain
import ru.kode.android.build.publish.plugin.test.utils.runTask
import java.io.File

class VkStandaloneTasksTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `uploadToVk task registered when plugin applied without Foundation or AGP`() {
        val projectDir = File(tempDir, "test-project")
        projectDir.createNonAndroidProject(
            pluginId = "ru.kode.android.build-publish-novo.vk",
            pluginConfigBlock =
                """
                buildPublishVk {
                    auth {
                        common {
                            accessTokenFile.set(file("vk-token.txt"))
                        }
                    }
                }
                """.trimIndent(),
        )

        val result = projectDir.runTask("tasks")

        result.outputShouldContain("BUILD SUCCESSFUL")
        result.outputShouldContain(TaskNames.Vk.UPLOAD)
    }
}
