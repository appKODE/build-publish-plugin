package ru.kode.android.build.publish.plugin.yandexdisk

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.core.task.TaskNames
import ru.kode.android.build.publish.plugin.test.utils.createNonAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.outputShouldContain
import ru.kode.android.build.publish.plugin.test.utils.runTask
import java.io.File

class YandexDiskStandaloneTasksTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `uploadToYandexDisk task registered when plugin applied without Foundation or AGP`() {
        val projectDir = File(tempDir, "test-project")
        projectDir.createNonAndroidProject(
            pluginId = "ru.kode.android.build-publish-novo.yandexdisk",
            pluginConfigBlock =
                """
                buildPublishYandexDisk {
                    auth {
                        common {
                            oauthTokenFile.set(file("yandex-token.txt"))
                        }
                    }
                }
                """.trimIndent(),
        )

        val result = projectDir.runTask("tasks")

        result.outputShouldContain("BUILD SUCCESSFUL")
        result.outputShouldContain(TaskNames.YandexDisk.UPLOAD)
    }
}
