package ru.kode.android.build.publish.plugin.nextcloud

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.core.task.TaskNames
import ru.kode.android.build.publish.plugin.test.utils.createNonAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.outputShouldContain
import ru.kode.android.build.publish.plugin.test.utils.runTask
import java.io.File

class NextcloudStandaloneTasksTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `uploadToNextcloud task registered when plugin applied without Foundation or AGP`() {
        val projectDir = File(tempDir, "test-project")
        projectDir.createNonAndroidProject(
            pluginId = "ru.kode.android.build-publish-novo.nextcloud",
            pluginConfigBlock =
                """
                buildPublishNextcloud {
                    auth {
                        common {
                            baseUrl.set("https://cloud.example.com")
                            credentials.username.set("user")
                            credentials.password.set("pass")
                        }
                    }
                }
                """.trimIndent(),
        )

        val result = projectDir.runTask("tasks")

        result.outputShouldContain("BUILD SUCCESSFUL")
        result.outputShouldContain(TaskNames.Nextcloud.UPLOAD)
    }
}
