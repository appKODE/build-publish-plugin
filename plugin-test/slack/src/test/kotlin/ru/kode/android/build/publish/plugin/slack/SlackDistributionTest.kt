package ru.kode.android.build.publish.plugin.slack

import org.gradle.api.logging.Logger
import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.core.logger.PluginLogger
import ru.kode.android.build.publish.plugin.slack.controller.SlackController
import ru.kode.android.build.publish.plugin.slack.controller.SlackControllerFactory
import ru.kode.android.build.publish.plugin.test.utils.AlwaysInfoLogger
import ru.kode.android.build.publish.plugin.test.utils.BuildType
import ru.kode.android.build.publish.plugin.test.utils.FoundationConfig
import ru.kode.android.build.publish.plugin.test.utils.SlackConfig
import ru.kode.android.build.publish.plugin.test.utils.addAllAndCommit
import ru.kode.android.build.publish.plugin.test.utils.addNamed
import ru.kode.android.build.publish.plugin.test.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.getFile
import ru.kode.android.build.publish.plugin.test.utils.initGit
import ru.kode.android.build.publish.plugin.test.utils.printFilesRecursively
import ru.kode.android.build.publish.plugin.test.utils.runTask
import ru.kode.android.build.publish.plugin.test.utils.runTaskWithFail
import ru.kode.android.build.publish.plugin.test.utils.runTasks
import java.io.File
import java.io.IOException

class SlackDistributionTest {

    private val logger: Logger = AlwaysInfoLogger()

    @TempDir
    lateinit var tempDir: File
    private lateinit var projectDir: File
    private lateinit var slackController: SlackController

    @BeforeEach
    fun setup() {
        projectDir = File(tempDir, "test-project")
        slackController = SlackControllerFactory.build(
            object : PluginLogger {
                override fun info(message: String, exception: Throwable?) {
                    logger.info(message)
                }

                override fun warn(message: String) {
                    logger.warn(message)
                }

                override fun error(message: String, exception: Throwable?) {
                    logger.error(message, exception)
                }

                override fun quiet(message: String) {
                    logger.quiet(message)
                }
            }
        )
    }

    @Test
    @Throws(IOException::class)
    fun `slack build distribution available with distribution config without proxy`() {
        val clickUpTokenFile = projectDir.getFile("app/slack_token.txt").apply {
            writeText(System.getProperty("SLACK_UPLOAD_API_TOKEN"))
        }
        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"),
                BuildType("release")
            ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog = FoundationConfig.Changelog(
                        issueNumberPattern = "CEB-\\\\d+",
                        issueUrlPrefix = "${System.getProperty("JIRA_BASE_URL")}/browse/"
                    )
                ),
            slackConfig = SlackConfig(
                bot = SlackConfig.Bot(
                    webhookUrl = System.getProperty("SLACK_WEBHOOK_URL"),
                    iconUrl = System.getProperty("SLACK_ICON_URL"),
                    uploadApiTokenFilePath = clickUpTokenFile.name,
                ),
                changelog = null,
                distribution = SlackConfig.Distribution(
                    destinationChannels = listOf(System.getProperty("SLACK_DISTRIBUTION_CHANNEL"))
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDebug"
        val givenSlackDistributionTask = "slackDistributionUploadDebug"
        val git = projectDir.initGit()

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)

        getChangelog()
            .split("\n")
            .forEachIndexed { index, changelogLine ->
                val givenCommitMessageN = """
                Add $index change in codebase
                
                CHANGELOG: $changelogLine
                """.trimIndent()
                projectDir.getFile("app/README${index}.md").writeText("This is test project")
                git.addAllAndCommit(givenCommitMessageN)
            }
        git.tag.addNamed(givenTagName2)


        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val distributionResult: BuildResult = projectDir.runTask(givenSlackDistributionTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false


        assertTrue(
            !assembleResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build successful",
        )
        assertTrue(
            distributionResult.output.contains("BUILD SUCCESSFUL"),
            "Slack distribution successful"
        )
        assertTrue(givenOutputFileExists, "Output file exists")
    }

    @Test
    @Throws(IOException::class)
    fun `slack build distribution available with distribution and changelog configs without proxy`() {
        val clickUpTokenFile = projectDir.getFile("app/slack_token.txt").apply {
            writeText(System.getProperty("SLACK_UPLOAD_API_TOKEN"))
        }
        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"),
                BuildType("release")
            ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog = FoundationConfig.Changelog(
                        issueNumberPattern = "CEB-\\\\d+",
                        issueUrlPrefix = "${System.getProperty("JIRA_BASE_URL")}/browse/"
                    )
                ),
            slackConfig = SlackConfig(
                bot = SlackConfig.Bot(
                    webhookUrl = System.getProperty("SLACK_WEBHOOK_URL"),
                    iconUrl = System.getProperty("SLACK_ICON_URL"),
                    uploadApiTokenFilePath = clickUpTokenFile.name,
                ),
                changelog = SlackConfig.Changelog(
                    userMentions = listOf(
                        "@melora_silvian_ar",
                        "@renalt_meridun_rt",
                        "@theronvale_miro_xt",
                        "@corvann_elidra_qm",
                        "@Marvilo7"
                    ),
                    attachmentColor = "#fff000"
                ),
                distribution = SlackConfig.Distribution(
                    destinationChannels = listOf(System.getProperty("SLACK_DISTRIBUTION_CHANNEL"))
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage = "Initial commit"
        val givenSlackChangelogTask = "sendSlackChangelogDebug"
        val givenSlackDistributionTask = "slackDistributionUploadDebug"
        val git = projectDir.initGit()

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)

        getChangelog()
            .split("\n")
            .forEachIndexed { index, changelogLine ->
                val givenCommitMessageN = """
                Add $index change in codebase
                
                CHANGELOG: $changelogLine
                """.trimIndent()
                projectDir.getFile("app/README${index}.md").writeText("This is test project")
                git.addAllAndCommit(givenCommitMessageN)
            }
        git.tag.addNamed(givenTagName2)


        val changelogResult: BuildResult = projectDir.runTask(givenSlackChangelogTask)
        val distributionResult: BuildResult = projectDir.runTask(givenSlackDistributionTask)

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            !changelogResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Slack changelog successful",
        )
        assertTrue(
            distributionResult.output.contains("BUILD SUCCESSFUL"),
            "Slack distribution successful"
        )
        assertTrue(givenOutputFileExists, "Output file exists")
    }

    @Test
    @Throws(IOException::class)
    fun `slack build distribution available with simultaneous distribution and changelog configs`() {
        val clickUpTokenFile = projectDir.getFile("app/slack_token.txt").apply {
            writeText(System.getProperty("SLACK_UPLOAD_API_TOKEN"))
        }
        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"),
                BuildType("release")
            ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog = FoundationConfig.Changelog(
                        issueNumberPattern = "CEB-\\\\d+",
                        issueUrlPrefix = "${System.getProperty("JIRA_BASE_URL")}/browse/"
                    )
                ),
            slackConfig = SlackConfig(
                bot = SlackConfig.Bot(
                    webhookUrl = System.getProperty("SLACK_WEBHOOK_URL"),
                    iconUrl = System.getProperty("SLACK_ICON_URL"),
                    uploadApiTokenFilePath = clickUpTokenFile.name,
                ),
                changelog = SlackConfig.Changelog(
                    userMentions = listOf(
                        "@melora_silvian_ar",
                        "@renalt_meridun_rt",
                        "@theronvale_miro_xt",
                        "@corvann_elidra_qm",
                        "@Marvilo7"
                    ),
                    attachmentColor = "#fff000"
                ),
                distribution = SlackConfig.Distribution(
                    destinationChannels = listOf(System.getProperty("SLACK_DISTRIBUTION_CHANNEL"))
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage = "Initial commit"
        val givenSlackChangelogTask = "sendSlackChangelogDebug"
        val givenSlackDistributionTask = "slackDistributionUploadDebug"
        val git = projectDir.initGit()

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)

        getChangelog()
            .split("\n")
            .forEachIndexed { index, changelogLine ->
                val givenCommitMessageN = """
                Add $index change in codebase
                
                CHANGELOG: $changelogLine
                """.trimIndent()
                projectDir.getFile("app/README${index}.md").writeText("This is test project")
                git.addAllAndCommit(givenCommitMessageN)
            }
        git.tag.addNamed(givenTagName2)


        val result: BuildResult = projectDir.runTasks(givenSlackChangelogTask, givenSlackDistributionTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !result.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            result.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            result.output.contains("BUILD SUCCESSFUL"),
            "Slack changelog and distribution successful",
        )
        assertTrue(
            !result.output.contains("BUILD FAILED"),
            "Slack changelog and distribution not failed"
        )
        assertTrue(givenOutputFileExists, "Output file exists")
    }

    @Test
    @Throws(IOException::class)
    fun `slack build distribution available with distribution config without proxy and assemble`() {
        val clickUpTokenFile = projectDir.getFile("app/slack_token.txt").apply {
            writeText(System.getProperty("SLACK_UPLOAD_API_TOKEN"))
        }
        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"),
                BuildType("release")
            ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog = FoundationConfig.Changelog(
                        issueNumberPattern = "CEB-\\\\d+",
                        issueUrlPrefix = "${System.getProperty("JIRA_BASE_URL")}/browse/"
                    )
                ),
            slackConfig = SlackConfig(
                bot = SlackConfig.Bot(
                    webhookUrl = System.getProperty("SLACK_WEBHOOK_URL"),
                    iconUrl = System.getProperty("SLACK_ICON_URL"),
                    uploadApiTokenFilePath = clickUpTokenFile.name,
                ),
                changelog = SlackConfig.Changelog(
                    userMentions = listOf(
                        "@melora_silvian_ar",
                        "@renalt_meridun_rt",
                        "@theronvale_miro_xt",
                        "@corvann_elidra_qm",
                        "@Marvilo7"
                    ),
                    attachmentColor = "#fff000"
                ),
                distribution = SlackConfig.Distribution(
                    destinationChannels = listOf(System.getProperty("SLACK_DISTRIBUTION_CHANNEL"))
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage = "Initial commit"
        val givenSlackDistributionTask = "slackDistributionUploadDebug"
        val git = projectDir.initGit()

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)

        getChangelog()
            .split("\n")
            .forEachIndexed { index, changelogLine ->
                val givenCommitMessageN = """
                Add $index change in codebase
                
                CHANGELOG: $changelogLine
                """.trimIndent()
                projectDir.getFile("app/README${index}.md").writeText("This is test project")
                git.addAllAndCommit(givenCommitMessageN)
            }
        git.tag.addNamed(givenTagName2)

        val distributionResult: BuildResult = projectDir.runTask(givenSlackDistributionTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !distributionResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            distributionResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            distributionResult.output.contains("BUILD SUCCESSFUL"),
            "Slack distribution successful"
        )
        assertTrue(givenOutputFileExists, "Output file exists")
    }

    @Test
    @Throws(IOException::class)
    fun `slack build bundle distribution available with distribution config without proxy`() {
        val clickUpTokenFile = projectDir.getFile("app/slack_token.txt").apply {
            writeText(System.getProperty("SLACK_UPLOAD_API_TOKEN"))
        }
        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"),
                BuildType("release")
            ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                    changelog = FoundationConfig.Changelog(
                        issueNumberPattern = "CEB-\\\\d+",
                        issueUrlPrefix = "${System.getProperty("JIRA_BASE_URL")}/browse/"
                    )
                ),
            slackConfig = SlackConfig(
                bot = SlackConfig.Bot(
                    webhookUrl = System.getProperty("SLACK_WEBHOOK_URL"),
                    iconUrl = System.getProperty("SLACK_ICON_URL"),
                    uploadApiTokenFilePath = clickUpTokenFile.name,
                ),
                changelog = null,
                distribution = SlackConfig.Distribution(
                    destinationChannels = listOf(System.getProperty("SLACK_DISTRIBUTION_CHANNEL"))
                )
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )
        val givenTagName1 = "v1.0.1-debug"
        val givenTagName2 = "v1.0.2-debug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "bundleDebug"
        val givenSlackDistributionTask = "slackDistributionUploadBundleDebug"
        val git = projectDir.initGit()
        val givenOutputFile = projectDir.getFile("app/build/outputs/bundle/debug/app-debug.aab")

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)

        getChangelog()
            .split("\n")
            .forEachIndexed { index, changelogLine ->
                val givenCommitMessageN = """
                Add $index change in codebase
                
                CHANGELOG: $changelogLine
                """.trimIndent()
                projectDir.getFile("app/README${index}.md").writeText("This is test project")
                git.addAllAndCommit(givenCommitMessageN)
            }
        git.tag.addNamed(givenTagName2)

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask)
        val distributionResult: BuildResult = projectDir.runTask(givenSlackDistributionTask)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            !assembleResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build successful",
        )
        assertTrue(
            distributionResult.output.contains("BUILD SUCCESSFUL"),
            "Slack distribution successful"
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")
    }

    @Test
    @Throws(IOException::class)
    fun `slack build distribution not available without distribution config`() {
        val clickUpTokenFile = projectDir.getFile("app/slack_token.txt").apply {
            writeText(System.getProperty("SLACK_UPLOAD_API_TOKEN"))
        }
        projectDir.createAndroidProject(
            buildTypes = listOf(BuildType("debug"), BuildType("release")),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "autotest",
                        ),
                ),
            slackConfig = SlackConfig(
                bot = SlackConfig.Bot(
                    webhookUrl = System.getProperty("SLACK_WEBHOOK_URL"),
                    iconUrl = System.getProperty("SLACK_ICON_URL"),
                    uploadApiTokenFilePath = clickUpTokenFile.name,
                ),
                changelog = null,
                distribution = null
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )
        val givenTagName = "v1.0.1-debug"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleDebug"
        val givenSlackDistributionTask = "slackDistributionUploadDebug"
        val git = projectDir.initGit()

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val assembleResult: BuildResult = projectDir.runTaskWithFail(givenAssembleTask)
        val automationResult: BuildResult = projectDir.runTaskWithFail(givenSlackDistributionTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc1-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !assembleResult.output.contains("Task :app:getLastTagRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            !assembleResult.output.contains("Task :app:getLastTagDebug"),
            "Task getLastTagDebug not executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD FAILED"),
            "Build failed",
        )
        assertTrue(
            automationResult.output.contains("BUILD FAILED"),
            "Slack distribution failed"
        )
        assertTrue(
            !givenOutputFileExists,
            "Output file not exists"
        )
    }
}

private fun getChangelog(): String {
    return """
[CEB-3243] [And] Mickey tried to fix the loader on Goofy’s form after settings got tangled, and navigation went bonkers
[CEB-3277] [Android] Donald’s transaction history exploded when he peeked into Daisy’s credit card details
    """.trimIndent()
}
