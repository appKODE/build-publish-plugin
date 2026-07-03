package ru.kode.android.build.publish.plugin.nextcloud

import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.test.utils.BuildType
import ru.kode.android.build.publish.plugin.test.utils.FoundationConfig
import ru.kode.android.build.publish.plugin.test.utils.NextcloudConfig
import ru.kode.android.build.publish.plugin.test.utils.addAllAndCommit
import ru.kode.android.build.publish.plugin.test.utils.addNamed
import ru.kode.android.build.publish.plugin.test.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.initGit
import ru.kode.android.build.publish.plugin.test.utils.outputShouldContain
import ru.kode.android.build.publish.plugin.test.utils.runTask
import ru.kode.android.build.publish.plugin.test.utils.runTaskWithFail
import ru.kode.android.build.publish.plugin.test.utils.runTasks
import java.io.File

class NextcloudDistributionTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `nextcloud distribution task fails without auth config`() {
        val projectDir = File(tempDir, "test-project")
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output = FoundationConfig.Output(baseFileName = "autotest"),
                ),
            nextcloudConfig =
                NextcloudConfig(
                    auth = null,
                    distribution =
                        NextcloudConfig.Distribution(
                            remotePath = "mobile/tests/autotest",
                        ),
                ),
            topBuildFileContent =
                """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
                """.trimIndent(),
        )

        val result: BuildResult = projectDir.runTaskWithFail("nextcloudDistributionUploadDebug")

        result.outputShouldContain("MISSING AUTHENTICATION CONFIGURATION")
        result.outputShouldContain("BUILD FAILED")
    }

    @Test
    fun `nextcloud distribution task fails without distribution config`() {
        val projectDir = File(tempDir, "test-project")
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output = FoundationConfig.Output(baseFileName = "autotest"),
                ),
            nextcloudConfig =
                NextcloudConfig(
                    auth =
                        NextcloudConfig.Auth(
                            baseUrl = System.getProperty("NEXTCLOUD_BASE_URL"),
                            username = System.getProperty("NEXTCLOUD_USER_NAME"),
                            password = System.getProperty("NEXTCLOUD_USER_PASSWORD"),
                        ),
                    distribution = null,
                ),
            topBuildFileContent =
                """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
                """.trimIndent(),
        )

        val result: BuildResult = projectDir.runTaskWithFail("nextcloudDistributionUploadDebug")

        result.outputShouldContain("MISSING DISTRIBUTION CONFIGURATION")
        result.outputShouldContain("BUILD FAILED")
    }

    @Test
    fun `nextcloud tasks are registered when distribution is configured`() {
        val projectDir = File(tempDir, "test-project")
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output = FoundationConfig.Output(baseFileName = "autotest"),
                ),
            nextcloudConfig =
                NextcloudConfig(
                    auth =
                        NextcloudConfig.Auth(
                            baseUrl = System.getProperty("NEXTCLOUD_BASE_URL"),
                            username = System.getProperty("NEXTCLOUD_USER_NAME"),
                            password = System.getProperty("NEXTCLOUD_USER_PASSWORD"),
                        ),
                    distribution =
                        NextcloudConfig.Distribution(
                            remotePath = "mobile/tests/autotest",
                        ),
                ),
            topBuildFileContent =
                """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
                """.trimIndent(),
        )

        val result: BuildResult = projectDir.runTasks("tasks", "--all")

        result.outputShouldContain("nextcloudDistributionUploadDebug")
        result.outputShouldContain("nextcloudDistributionUploadBundleDebug")
        result.outputShouldContain("nextcloudChangelogUploadDebug")
    }

    @Test
    fun `nextcloud distribution task fails in internal mode without recipients`() {
        val projectDir = File(tempDir, "test-project")
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output = FoundationConfig.Output(baseFileName = "autotest"),
                ),
            nextcloudConfig =
                NextcloudConfig(
                    auth =
                        NextcloudConfig.Auth(
                            baseUrl = System.getProperty("NEXTCLOUD_BASE_URL"),
                            username = System.getProperty("NEXTCLOUD_USER_NAME"),
                            password = System.getProperty("NEXTCLOUD_USER_PASSWORD"),
                        ),
                    distribution =
                        NextcloudConfig.Distribution(
                            remotePath = "mobile/tests/autotest",
                        ),
                ),
            topBuildFileContent =
                """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
                """.trimIndent(),
        )

        val git = projectDir.initGit()
        git.addAllAndCommit("initial")
        git.tag.addNamed("v1.0.1-debug")

        val result: BuildResult = projectDir.runTaskWithFail("nextcloudDistributionUploadDebug")

        result.outputShouldContain("MISSING INTERNAL RECIPIENTS ERROR")
        result.outputShouldContain("userRecipients/groupRecipients entry")
        result.outputShouldContain("BUILD FAILED")
    }

    @Test
    fun `nextcloud file and changelog tasks can be invoked together`() {
        val projectDir = File(tempDir, "test-project")
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output = FoundationConfig.Output(baseFileName = "autotest"),
                ),
            nextcloudConfig =
                NextcloudConfig(
                    auth =
                        NextcloudConfig.Auth(
                            baseUrl = System.getProperty("NEXTCLOUD_BASE_URL"),
                            username = System.getProperty("NEXTCLOUD_USER_NAME"),
                            password = System.getProperty("NEXTCLOUD_USER_PASSWORD"),
                        ),
                    distribution =
                        NextcloudConfig.Distribution(
                            remotePath = "mobile/tests/autotest",
                            shareMode = "PUBLIC_LINK",
                        ),
                ),
            topBuildFileContent =
                """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
                """.trimIndent(),
        )

        val result: BuildResult =
            projectDir.runTasks(
                "nextcloudDistributionUploadDebug",
                "nextcloudChangelogUploadDebug",
                "--dry-run",
            )

        result.outputShouldContain("nextcloudDistributionUploadDebug")
        result.outputShouldContain("nextcloudChangelogUploadDebug")
    }

    @Test
    fun `nextcloud distribution uses buildVariant auth over common`() {
        val projectDir = File(tempDir, "test-project")
        val remotePath = System.getProperty("NEXTCLOUD_REMOTE_PATH")
        val baseUrl = System.getProperty("NEXTCLOUD_BASE_URL")
        val userName = System.getProperty("NEXTCLOUD_USER_NAME")
        val userPassword = System.getProperty("NEXTCLOUD_USER_PASSWORD")

        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output = FoundationConfig.Output(baseFileName = "autotest"),
                ),
            nextcloudConfig =
                NextcloudConfig(
                    auth =
                        NextcloudConfig.Auth(
                            baseUrl = "https://invalid.example.invalid/",
                            username = "invalid",
                            password = "invalid",
                        ),
                    distribution =
                        NextcloudConfig.Distribution(
                            remotePath = remotePath,
                            shareMode = "PUBLIC_LINK",
                        ),
                ),
            topBuildFileContent =
                """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
                """.trimIndent(),
        )

        File(projectDir, "app/build.gradle").appendText(
            """
            
            buildPublishNextcloud {
                auth {
                    buildVariant("debug") {
                        baseUrl.set("$baseUrl")
                        credentials.username.set("$userName")
                        credentials.password.set("$userPassword")
                    }
                }
            }
            """.trimIndent(),
        )

        val git = projectDir.initGit()
        git.addAllAndCommit("initial")
        git.tag.addNamed("v1.0.1-debug")

        val result = projectDir.runTask("nextcloudDistributionUploadDebug")

        result.outputShouldContain("Nextcloud public share")
        result.outputShouldContain("autotest-1.0-debug.apk")
    }

    @Test
    fun `nextcloud distribution uses buildVariant distribution over common`() {
        val projectDir = File(tempDir, "test-project")
        val remotePath = System.getProperty("NEXTCLOUD_REMOTE_PATH")

        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output = FoundationConfig.Output(baseFileName = "autotest"),
                ),
            nextcloudConfig =
                NextcloudConfig(
                    auth =
                        NextcloudConfig.Auth(
                            baseUrl = System.getProperty("NEXTCLOUD_BASE_URL"),
                            username = System.getProperty("NEXTCLOUD_USER_NAME"),
                            password = System.getProperty("NEXTCLOUD_USER_PASSWORD"),
                        ),
                    distribution =
                        NextcloudConfig.Distribution(
                            remotePath = "mobile/tests/common-path-should-not-be-used",
                        ),
                ),
            topBuildFileContent =
                """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
                """.trimIndent(),
        )

        File(projectDir, "app/build.gradle").appendText(
            """
            
            buildPublishNextcloud {
                distribution {
                    buildVariant("debug") {
                        remotePath.set("$remotePath")
                        shareMode.set(ru.kode.android.build.publish.plugin.nextcloud.config.NextcloudShareMode.PUBLIC_LINK)
                        remoteFileName.set("variant-override-debug.apk")
                    }
                }
            }
            """.trimIndent(),
        )

        val git = projectDir.initGit()
        git.addAllAndCommit("initial")
        git.tag.addNamed("v1.0.1-debug")

        val result = projectDir.runTask("nextcloudDistributionUploadDebug")

        result.outputShouldContain("variant-override-debug.apk")
        result.outputShouldContain("Nextcloud public share")
    }

    @Test
    fun `nextcloud internal recipients with user config uploads and logs internal link`() {
        val projectDir = File(tempDir, "test-project")
        val remotePath = System.getProperty("NEXTCLOUD_REMOTE_PATH")
        val userRecipient = configuredRecipient("NEXTCLOUD_USER_RECIPIENT") ?: "user-a"

        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output = FoundationConfig.Output(baseFileName = "autotest"),
                ),
            nextcloudConfig =
                NextcloudConfig(
                    auth =
                        NextcloudConfig.Auth(
                            baseUrl = System.getProperty("NEXTCLOUD_BASE_URL"),
                            username = System.getProperty("NEXTCLOUD_USER_NAME"),
                            password = System.getProperty("NEXTCLOUD_USER_PASSWORD"),
                        ),
                    distribution =
                        NextcloudConfig.Distribution(
                            remotePath = remotePath,
                            userRecipients = listOf(userRecipient),
                        ),
                ),
            topBuildFileContent =
                """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
                """.trimIndent(),
        )

        if (configuredRecipient("NEXTCLOUD_USER_RECIPIENT") == null) {
            val dryRun =
                projectDir.runTasks(
                    "nextcloudDistributionUploadDebug",
                    "--dry-run",
                )
            dryRun.outputShouldContain("nextcloudDistributionUploadDebug")
            return
        }

        val git = projectDir.initGit()
        git.addAllAndCommit("initial")
        git.tag.addNamed("v1.0.1-debug")

        val result = projectDir.runTask("nextcloudDistributionUploadDebug")

        result.outputShouldContain("Nextcloud internal share ready")
        result.outputShouldContain("/f/")
    }

    @Test
    fun `nextcloud internal recipients with group config uploads and logs internal link`() {
        val projectDir = File(tempDir, "test-project")
        val remotePath = System.getProperty("NEXTCLOUD_REMOTE_PATH")
        val groupRecipient = configuredRecipient("NEXTCLOUD_GROUP_RECIPIENT") ?: "qa"

        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output = FoundationConfig.Output(baseFileName = "autotest"),
                ),
            nextcloudConfig =
                NextcloudConfig(
                    auth =
                        NextcloudConfig.Auth(
                            baseUrl = System.getProperty("NEXTCLOUD_BASE_URL"),
                            username = System.getProperty("NEXTCLOUD_USER_NAME"),
                            password = System.getProperty("NEXTCLOUD_USER_PASSWORD"),
                        ),
                    distribution =
                        NextcloudConfig.Distribution(
                            remotePath = remotePath,
                            groupRecipients = listOf(groupRecipient),
                        ),
                ),
            topBuildFileContent =
                """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
                """.trimIndent(),
        )

        if (configuredRecipient("NEXTCLOUD_GROUP_RECIPIENT") == null) {
            val dryRun =
                projectDir.runTasks(
                    "nextcloudDistributionUploadDebug",
                    "--dry-run",
                )
            dryRun.outputShouldContain("nextcloudDistributionUploadDebug")
            return
        }

        val git = projectDir.initGit()
        git.addAllAndCommit("initial")
        git.tag.addNamed("v1.0.1-debug")

        val result = projectDir.runTask("nextcloudDistributionUploadDebug")

        result.outputShouldContain("Nextcloud internal share ready")
        result.outputShouldContain("/f/")
    }

    @Test
    fun `nextcloud distribution uploads and reuses public share on rerun`() {
        val projectDir = File(tempDir, "test-project")
        val remotePath = System.getProperty("NEXTCLOUD_REMOTE_PATH")

        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output = FoundationConfig.Output(baseFileName = "autotest"),
                ),
            nextcloudConfig =
                NextcloudConfig(
                    auth =
                        NextcloudConfig.Auth(
                            baseUrl = System.getProperty("NEXTCLOUD_BASE_URL"),
                            username = System.getProperty("NEXTCLOUD_USER_NAME"),
                            password = System.getProperty("NEXTCLOUD_USER_PASSWORD"),
                        ),
                    distribution =
                        NextcloudConfig.Distribution(
                            remotePath = remotePath,
                            shareMode = "PUBLIC_LINK",
                        ),
                ),
            topBuildFileContent =
                """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
                """.trimIndent(),
        )

        val git = projectDir.initGit()
        git.addAllAndCommit("initial")
        git.tag.addNamed("v1.0.1-debug")

        val firstRun = projectDir.runTask("nextcloudDistributionUploadDebug")
        val secondRun = projectDir.runTask("nextcloudDistributionUploadDebug")

        firstRun.outputShouldContain("autotest-1.0-debug.apk")
        secondRun.outputShouldContain("autotest-1.0-debug.apk")
        firstRun.outputShouldContain("Nextcloud public share")
        secondRun.outputShouldContain("Nextcloud public share reused")
    }

    @Test
    fun `nextcloud distribution upload supports remote file name override`() {
        val projectDir = File(tempDir, "test-project")
        val remotePath = System.getProperty("NEXTCLOUD_REMOTE_PATH")

        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output = FoundationConfig.Output(baseFileName = "autotest"),
                ),
            nextcloudConfig =
                NextcloudConfig(
                    auth =
                        NextcloudConfig.Auth(
                            baseUrl = System.getProperty("NEXTCLOUD_BASE_URL"),
                            username = System.getProperty("NEXTCLOUD_USER_NAME"),
                            password = System.getProperty("NEXTCLOUD_USER_PASSWORD"),
                        ),
                    distribution =
                        NextcloudConfig.Distribution(
                            remotePath = remotePath,
                            shareMode = "PUBLIC_LINK",
                            remoteFileName = "override-upload-debug.apk",
                        ),
                ),
            topBuildFileContent =
                """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
                """.trimIndent(),
        )

        val git = projectDir.initGit()
        git.addAllAndCommit("initial")
        git.tag.addNamed("v1.0.1-debug")

        val result = projectDir.runTask("nextcloudDistributionUploadDebug")

        result.outputShouldContain("override-upload-debug.apk")
        result.outputShouldContain("Nextcloud public share")
    }

    @Test
    fun `nextcloud changelog upload supports remote file name override`() {
        val projectDir = File(tempDir, "test-project")
        val remotePath = System.getProperty("NEXTCLOUD_REMOTE_PATH")

        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output = FoundationConfig.Output(baseFileName = "autotest"),
                ),
            nextcloudConfig =
                NextcloudConfig(
                    auth =
                        NextcloudConfig.Auth(
                            baseUrl = System.getProperty("NEXTCLOUD_BASE_URL"),
                            username = System.getProperty("NEXTCLOUD_USER_NAME"),
                            password = System.getProperty("NEXTCLOUD_USER_PASSWORD"),
                        ),
                    distribution =
                        NextcloudConfig.Distribution(
                            remotePath = remotePath,
                            shareMode = "PUBLIC_LINK",
                            remoteFileName = "override-upload-debug-changelog.md",
                        ),
                ),
            topBuildFileContent =
                """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
                """.trimIndent(),
        )

        val git = projectDir.initGit()
        git.addAllAndCommit("initial")
        git.tag.addNamed("v1.0.1-debug")

        val result = projectDir.runTask("nextcloudChangelogUploadDebug")

        result.outputShouldContain("override-upload-debug-changelog.md")
        result.outputShouldContain("Nextcloud public share")
    }

    @Test
    fun `nextcloud deterministic naming changes when build version changes`() {
        val projectDir = File(tempDir, "test-project")
        val remotePath = System.getProperty("NEXTCLOUD_REMOTE_PATH")

        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output = FoundationConfig.Output(baseFileName = "autotest"),
                ),
            nextcloudConfig =
                NextcloudConfig(
                    auth =
                        NextcloudConfig.Auth(
                            baseUrl = System.getProperty("NEXTCLOUD_BASE_URL"),
                            username = System.getProperty("NEXTCLOUD_USER_NAME"),
                            password = System.getProperty("NEXTCLOUD_USER_PASSWORD"),
                        ),
                    distribution =
                        NextcloudConfig.Distribution(
                            remotePath = remotePath,
                            shareMode = "PUBLIC_LINK",
                        ),
                ),
            topBuildFileContent =
                """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
                """.trimIndent(),
        )

        val git = projectDir.initGit()
        git.addAllAndCommit("initial")
        git.tag.addNamed("v1.0.1-debug")
        val firstRun = projectDir.runTask("nextcloudDistributionUploadDebug")

        val markerFile = File(projectDir, "version-change-marker.txt")
        markerFile.writeText("version 1.1 marker")
        git.addAllAndCommit("next version")
        git.tag.addNamed("v1.1.2-debug")
        val secondRun = projectDir.runTask("nextcloudDistributionUploadDebug")

        firstRun.outputShouldContain("autotest-1.0-debug.apk")
        secondRun.outputShouldContain("autotest-1.1-debug.apk")
    }

    @Test
    fun `nextcloud bundle distribution supports compressed upload`() {
        val projectDir = File(tempDir, "test-project")
        val remotePath = System.getProperty("NEXTCLOUD_REMOTE_PATH")

        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output = FoundationConfig.Output(baseFileName = "autotest"),
                ),
            nextcloudConfig =
                NextcloudConfig(
                    auth =
                        NextcloudConfig.Auth(
                            baseUrl = System.getProperty("NEXTCLOUD_BASE_URL"),
                            username = System.getProperty("NEXTCLOUD_USER_NAME"),
                            password = System.getProperty("NEXTCLOUD_USER_PASSWORD"),
                        ),
                    distribution =
                        NextcloudConfig.Distribution(
                            remotePath = remotePath,
                            compressed = true,
                            shareMode = "PUBLIC_LINK",
                        ),
                ),
            topBuildFileContent =
                """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
                """.trimIndent(),
        )

        val git = projectDir.initGit()
        git.addAllAndCommit("initial")
        git.tag.addNamed("v1.0.1-debug")

        val result = projectDir.runTask("nextcloudDistributionUploadBundleDebug")

        result.outputShouldContain("autotest-1.0-debug.zip")
        result.outputShouldContain("Nextcloud public share")
    }

    @Test
    fun `nextcloud changelog upload task is executable`() {
        val projectDir = File(tempDir, "test-project")
        val remotePath = System.getProperty("NEXTCLOUD_REMOTE_PATH")

        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output = FoundationConfig.Output(baseFileName = "autotest"),
                ),
            nextcloudConfig =
                NextcloudConfig(
                    auth =
                        NextcloudConfig.Auth(
                            baseUrl = System.getProperty("NEXTCLOUD_BASE_URL"),
                            username = System.getProperty("NEXTCLOUD_USER_NAME"),
                            password = System.getProperty("NEXTCLOUD_USER_PASSWORD"),
                        ),
                    distribution =
                        NextcloudConfig.Distribution(
                            remotePath = remotePath,
                            shareMode = "PUBLIC_LINK",
                        ),
                ),
            topBuildFileContent =
                """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
                """.trimIndent(),
        )

        val git = projectDir.initGit()
        git.addAllAndCommit("initial")
        git.tag.addNamed("v1.0.1-debug")

        val result = projectDir.runTask("nextcloudChangelogUploadDebug")

        result.outputShouldContain("autotest-1.0-debug-changelog.txt")
    }

    private fun configuredRecipient(propertyName: String): String? {
        val value = System.getProperty(propertyName)?.trim().orEmpty()
        return value.takeIf { it.isNotBlank() && it != "not_defined_stub" }
    }
}
