package ru.kode.android.build.publish.plugin.vk

import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.test.utils.BuildType
import ru.kode.android.build.publish.plugin.test.utils.FoundationConfig
import ru.kode.android.build.publish.plugin.test.utils.VkConfig
import ru.kode.android.build.publish.plugin.test.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.outputShouldContain
import ru.kode.android.build.publish.plugin.test.utils.runTaskWithFail
import ru.kode.android.build.publish.plugin.test.utils.runTasks
import java.io.File

class VkDistributionTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `vk distribution task fails without auth config`() {
        val projectDir = File(tempDir, "test-project")
        projectDir.createProject(
            VkConfig(
                auth = null,
                distribution = VkConfig.Distribution(peerId = 2000000001L),
            ),
        )

        val result: BuildResult = projectDir.runTaskWithFail("vkDistributionUploadDebug")

        result.outputShouldContain("MISSING AUTHENTICATION CONFIGURATION")
        result.outputShouldContain("BUILD FAILED")
    }

    @Test
    fun `vk distribution task fails without distribution config`() {
        val projectDir = File(tempDir, "test-project")
        projectDir.createProject(
            VkConfig(
                auth = VkConfig.Auth(accessTokenFilePath = "vk-token.txt"),
                distribution = null,
            ),
        )

        val result: BuildResult = projectDir.runTaskWithFail("vkDistributionUploadDebug")

        result.outputShouldContain("MISSING DISTRIBUTION CONFIGURATION")
        result.outputShouldContain("BUILD FAILED")
    }

    @Test
    fun `vk tasks are registered when distribution is configured`() {
        val projectDir = File(tempDir, "test-project")
        projectDir.createProject(
            VkConfig(
                auth = VkConfig.Auth(accessTokenFilePath = "vk-token.txt"),
                distribution = VkConfig.Distribution(peerId = 2000000001L, compressed = true),
            ),
        )

        val result: BuildResult = projectDir.runTasks("tasks", "--all")

        result.outputShouldContain("vkDistributionUploadDebug")
        result.outputShouldContain("vkDistributionUploadBundleDebug")
        result.outputShouldContain("uploadToVk")
    }

    private fun File.createProject(config: VkConfig) {
        createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output = FoundationConfig.Output(baseFileName = "autotest"),
                ),
            vkConfig = config,
            topBuildFileContent =
                """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
                """.trimIndent(),
        )
    }
}
