package ru.kode.android.build.publish.plugin.firebase

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.test.utils.BuildType
import ru.kode.android.build.publish.plugin.test.utils.FirebaseConfig
import ru.kode.android.build.publish.plugin.test.utils.FoundationConfig
import ru.kode.android.build.publish.plugin.test.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.outputShouldContain
import ru.kode.android.build.publish.plugin.test.utils.runTask
import ru.kode.android.build.publish.plugin.test.utils.runTaskWithFail
import java.io.File

/**
 * Configuration-phase tests for the Firebase plugin. These exercise the `finalizeDsl` gate in
 * [BuildPublishFirebasePlugin] — Google's `AppDistributionPlugin` (and therefore the
 * `appDistributionUpload*` tasks) must be applied only when a distribution config is declared.
 * They run the configuration phase via the `tasks` task, so they need an Android SDK but no
 * Firebase credentials and no network.
 */
class FirebaseConfigurationTest {
    @TempDir
    lateinit var tempDir: File
    private lateinit var projectDir: File

    @BeforeEach
    fun setup() {
        projectDir = File(tempDir, "test-project")
    }

    @Test
    fun `appDistributionUpload task registered when distribution configured`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig = FoundationConfig(output = FoundationConfig.Output(baseFileName = "autotest")),
            firebaseConfig =
                FirebaseConfig(
                    distributionCommon =
                        FirebaseConfig.Distribution(
                            serviceCredentialsFilePath = "service-credentials.json",
                            appId = "1:1234567890:android:abcdef",
                            testerGroups = listOf("testers"),
                            artifactType = "ArtifactType.Apk",
                        ),
                ),
            topBuildFileContent =
                """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
                """.trimIndent(),
            import = "import ru.kode.android.build.publish.plugin.firebase.config.ArtifactType",
            configureApplicationVariants = true,
        )

        val result = projectDir.runTask("tasks", cliArguments = listOf("--all"))

        result.outputShouldContain("BUILD SUCCESSFUL")
        assertTrue(
            result.output.contains("appDistributionUploadDebug"),
            "AppDistributionPlugin should be applied and register the upload task when distribution is configured",
        )
    }

    @Test
    fun `configuration fails fast with a helpful message when firebase applied without distribution`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig = FoundationConfig(output = FoundationConfig.Output(baseFileName = "autotest")),
            // Firebase plugin applied but no distribution declared: the per-variant guard must fail fast
            // with a helpful message instead of silently producing a no-op upload task.
            firebaseConfig = FirebaseConfig(),
            topBuildFileContent =
                """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
                """.trimIndent(),
            import = "import ru.kode.android.build.publish.plugin.firebase.config.ArtifactType",
            configureApplicationVariants = true,
        )

        val result = projectDir.runTaskWithFail("tasks")

        assertTrue(
            result.output.contains("MISSING FIREBASE DISTRIBUTION CONFIGURATION"),
            "Configuration should fail with the missing-distribution guidance, output was:\n${result.output}",
        )
    }
}
