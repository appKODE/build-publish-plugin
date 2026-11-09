package ru.kode.android.build.publish.plugin.yandexdisk

import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.test.utils.BuildType
import ru.kode.android.build.publish.plugin.test.utils.FoundationConfig
import ru.kode.android.build.publish.plugin.test.utils.YandexDiskConfig
import ru.kode.android.build.publish.plugin.test.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.outputShouldContain
import ru.kode.android.build.publish.plugin.test.utils.runTaskWithFail
import ru.kode.android.build.publish.plugin.test.utils.runTasks
import java.io.File

class YandexDiskDistributionTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `yandex disk distribution task fails without auth config`() {
        val projectDir = File(tempDir, "test-project")
        projectDir.createProject(
            YandexDiskConfig(
                auth = null,
                distribution = YandexDiskConfig.Distribution(remotePath = "builds/android"),
            ),
        )

        val result: BuildResult = projectDir.runTaskWithFail("yandexDiskDistributionUploadDebug")

        result.outputShouldContain("MISSING AUTHENTICATION CONFIGURATION")
        result.outputShouldContain("BUILD FAILED")
    }

    @Test
    fun `yandex disk distribution task fails without distribution config`() {
        val projectDir = File(tempDir, "test-project")
        projectDir.createProject(
            YandexDiskConfig(
                auth = YandexDiskConfig.Auth(oauthTokenFilePath = "yandex-token.txt"),
                distribution = null,
            ),
        )

        val result: BuildResult = projectDir.runTaskWithFail("yandexDiskDistributionUploadDebug")

        result.outputShouldContain("MISSING DISTRIBUTION CONFIGURATION")
        result.outputShouldContain("BUILD FAILED")
    }

    @Test
    fun `yandex disk tasks are registered when distribution is configured`() {
        val projectDir = File(tempDir, "test-project")
        projectDir.createProject(
            YandexDiskConfig(
                auth = YandexDiskConfig.Auth(oauthTokenFilePath = "yandex-token.txt"),
                distribution = YandexDiskConfig.Distribution(remotePath = "builds/android", publicLink = true),
            ),
        )

        val result: BuildResult = projectDir.runTasks("tasks", "--all")

        result.outputShouldContain("yandexDiskDistributionUploadDebug")
        result.outputShouldContain("yandexDiskDistributionUploadBundleDebug")
        result.outputShouldContain("yandexDiskChangelogUploadDebug")
        result.outputShouldContain("uploadToYandexDisk")
    }

    private fun File.createProject(config: YandexDiskConfig) {
        createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output = FoundationConfig.Output(baseFileName = "autotest"),
                ),
            yandexDiskConfig = config,
            topBuildFileContent =
                """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
                """.trimIndent(),
        )
    }
}
