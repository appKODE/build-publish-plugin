package ru.kode.android.build.publish.plugin.confluence

import org.gradle.api.logging.Logger
import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.kode.android.build.publish.plugin.confluence.controller.ConfluenceController
import ru.kode.android.build.publish.plugin.confluence.controller.factory.ConfluenceControllerFactory
import ru.kode.android.build.publish.plugin.core.logger.PluginLogger
import ru.kode.android.build.publish.plugin.core.util.NetworkProxy
import ru.kode.android.build.publish.plugin.test.utils.AlwaysInfoLogger
import ru.kode.android.build.publish.plugin.test.utils.BuildType
import ru.kode.android.build.publish.plugin.test.utils.ConfluenceConfig
import ru.kode.android.build.publish.plugin.test.utils.FoundationConfig
import ru.kode.android.build.publish.plugin.test.utils.addAllAndCommit
import ru.kode.android.build.publish.plugin.test.utils.addNamed
import ru.kode.android.build.publish.plugin.test.utils.createAndroidProject
import ru.kode.android.build.publish.plugin.test.utils.currentDate
import ru.kode.android.build.publish.plugin.test.utils.getFile
import ru.kode.android.build.publish.plugin.test.utils.initGit
import ru.kode.android.build.publish.plugin.test.utils.printFilesRecursively
import ru.kode.android.build.publish.plugin.test.utils.runTask
import ru.kode.android.build.publish.plugin.test.utils.runTaskWithFail
import java.io.File
import java.io.IOException

class ConfluenceDistributionTest {

    private val logger: Logger = AlwaysInfoLogger()

    @TempDir
    lateinit var tempDir: File
    private lateinit var projectDir: File
    private lateinit var confluenceController: ConfluenceController

    @BeforeEach
    fun setup() {
        projectDir = File(tempDir, "test-project")
        confluenceController = ConfluenceControllerFactory.build(
            baseUrl = System.getProperty("CONFLUENCE_BASE_URL"),
            username = System.getProperty("CONFLUENCE_USER_NAME"),
            password = System.getProperty("CONFLUENCE_USER_PASSWORD"),
            logger = object : PluginLogger {
                override val bodyLogging: Boolean get() = false

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
            },
            proxy = {
                NetworkProxy(
                    host = System.getProperty("PROXY_HOST"),
                    port = System.getProperty("PROXY_PORT"),
                    user = System.getProperty("PROXY_USER"),
                    password = System.getProperty("PROXY_PASSWORD"),
                )
            }
        )
    }

    @Test
    @Throws(IOException::class)
    fun `confluence build distribution not available without distribution config`() {
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
            confluenceConfig = ConfluenceConfig(
                auth = ConfluenceConfig.Auth(
                    baseUrl = System.getProperty("CONFLUENCE_BASE_URL"),
                    username = System.getProperty("CONFLUENCE_USER_NAME"),
                    password = System.getProperty("CONFLUENCE_USER_PASSWORD")
                ),
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
        val givenConfluenceDistributionTask = "confluenceDistributionUploadDebug"
        val git = projectDir.initGit()

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val assembleResult: BuildResult = projectDir.runTaskWithFail(givenAssembleTask)
        val automationResult: BuildResult = projectDir.runTaskWithFail(givenConfluenceDistributionTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc1-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !assembleResult.output.contains("Task :app:getLastTagSnapshotRelease"),
            "Task getLastTagSnapshotRelease not executed",
        )
        assertTrue(
            !assembleResult.output.contains("Task :app:getLastTagSnapshotDebug"),
            "Task getLastTagSnapshotDebug not executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD FAILED"),
            "Build failed",
        )
        assertTrue(
            automationResult.output.contains("BUILD FAILED"),
            "Confluence distribution failed"
        )
        assertTrue(!givenOutputFileExists, "Output file not exists")
    }

    @Test
    @Throws(IOException::class)
    fun `confluence apk build distribution available with distribution config with proxy`() {
        val pageId = System.getProperty("CONFLUENCE_PAGE_ID")

        val beforeAutomationAttachments = confluenceController.getAttachments(pageId)
        val beforeAutomationComments = confluenceController.getComments(pageId)

        beforeAutomationComments.forEach {
            confluenceController.removeComment(it.id)
        }

        beforeAutomationAttachments.forEach {
            confluenceController.removeAttachment(it.id)
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
            confluenceConfig = ConfluenceConfig(
                auth = ConfluenceConfig.Auth(
                    baseUrl = System.getProperty("CONFLUENCE_BASE_URL"),
                    username = System.getProperty("CONFLUENCE_USER_NAME"),
                    password = System.getProperty("CONFLUENCE_USER_PASSWORD")
                ),
                distribution = ConfluenceConfig.Distribution(
                    pageId = pageId
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
        val givenConfluenceDistributionTask = "confluenceDistributionUploadDebug"
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

        val proxyProps = mapOf(
            "https.proxyUser" to System.getProperty("PROXY_USER"),
            "https.proxyPassword" to System.getProperty("PROXY_PASSWORD"),
            "https.proxyHost" to System.getProperty("PROXY_HOST"),
            "https.proxyPort" to System.getProperty("PROXY_PORT")
        )

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask, proxyProps)
        val distributionResult: BuildResult = projectDir.runTask(givenConfluenceDistributionTask, proxyProps)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !assembleResult.output.contains("Task :app:getLastTagSnapshotRelease"),
            "Task getLastTagSnapshotRelease not executed",
        )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagSnapshotDebug"),
            "Task getLastTagSnapshotDebug executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build successful",
        )
        assertTrue(
            distributionResult.output.contains("BUILD SUCCESSFUL"),
            "Confluence distribution successful"
        )
        assertTrue(givenOutputFileExists, "Output file exists")

        val afterAutomationAttachments = confluenceController.getAttachments(pageId)
        val afterAutomationComments = confluenceController.getComments(pageId)

        assertTrue {
            afterAutomationAttachments.last().fileName.contains("autotest-debug-vc2-$currentDate.apk")
        }
        assertTrue {
            afterAutomationComments.last().html.contains("autotest-debug-vc2-$currentDate.apk")
        }
    }

    @Test
    @Throws(IOException::class)
    fun `confluence apk build distribution available with distribution config with proxy without assemble`() {
        val pageId = System.getProperty("CONFLUENCE_PAGE_ID")

        val beforeAutomationAttachments = confluenceController.getAttachments(pageId)
        val beforeAutomationComments = confluenceController.getComments(pageId)

        beforeAutomationComments.forEach {
            confluenceController.removeComment(it.id)
        }

        beforeAutomationAttachments.forEach {
            confluenceController.removeAttachment(it.id)
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
            confluenceConfig = ConfluenceConfig(
                auth = ConfluenceConfig.Auth(
                    baseUrl = System.getProperty("CONFLUENCE_BASE_URL"),
                    username = System.getProperty("CONFLUENCE_USER_NAME"),
                    password = System.getProperty("CONFLUENCE_USER_PASSWORD")
                ),
                distribution = ConfluenceConfig.Distribution(
                    pageId = pageId
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
        val givenConfluenceDistributionTask = "confluenceDistributionUploadDebug"
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

        val proxyProps = mapOf(
            "https.proxyUser" to System.getProperty("PROXY_USER"),
            "https.proxyPassword" to System.getProperty("PROXY_PASSWORD"),
            "https.proxyHost" to System.getProperty("PROXY_HOST"),
            "https.proxyPort" to System.getProperty("PROXY_PORT")
        )

        val distributionResult: BuildResult = projectDir.runTask(givenConfluenceDistributionTask, proxyProps)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !distributionResult.output.contains("Task :app:getLastTagSnapshotRelease"),
            "Task getLastTagSnapshotRelease not executed",
        )
        assertTrue(
            distributionResult.output.contains("Task :app:getLastTagSnapshotDebug"),
            "Task getLastTagSnapshotDebug executed",
        )
        assertTrue(
            distributionResult.output.contains("BUILD SUCCESSFUL"),
            "Confluence distribution successful"
        )
        assertTrue(givenOutputFileExists, "Output file exists")

        val afterAutomationAttachments = confluenceController.getAttachments(pageId)
        val afterAutomationComments = confluenceController.getComments(pageId)

        assertTrue {
            afterAutomationAttachments.last().fileName.contains("autotest-debug-vc2-$currentDate.apk")
        }
        assertTrue {
            afterAutomationComments.last().html.contains("autotest-debug-vc2-$currentDate.apk")
        }
    }

    @Test
    @Throws(IOException::class)
    fun `confluence bundle build distribution available with distribution config with proxy`() {
        val pageId = System.getProperty("CONFLUENCE_PAGE_ID")

        val beforeAutomationAttachments = confluenceController.getAttachments(pageId)
        val beforeAutomationComments = confluenceController.getComments(pageId)

        beforeAutomationComments.forEach {
            confluenceController.removeComment(it.id)
        }

        beforeAutomationAttachments.forEach {
            confluenceController.removeAttachment(it.id)
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
            confluenceConfig = ConfluenceConfig(
                auth = ConfluenceConfig.Auth(
                    baseUrl = System.getProperty("CONFLUENCE_BASE_URL"),
                    username = System.getProperty("CONFLUENCE_USER_NAME"),
                    password = System.getProperty("CONFLUENCE_USER_PASSWORD")
                ),
                distribution = ConfluenceConfig.Distribution(
                    pageId = pageId
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
        val givenConfluenceDistributionTask = "confluenceDistributionUploadBundleDebug"
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


        val proxyProps = mapOf(
            "https.proxyUser" to System.getProperty("PROXY_USER"),
            "https.proxyPassword" to System.getProperty("PROXY_PASSWORD"),
            "https.proxyHost" to System.getProperty("PROXY_HOST"),
            "https.proxyPort" to System.getProperty("PROXY_PORT")
        )

        val assembleResult: BuildResult = projectDir.runTask(givenAssembleTask, proxyProps)
        val distributionResult: BuildResult = projectDir.runTask(givenConfluenceDistributionTask, proxyProps)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            !assembleResult.output.contains("Task :app:getLastTagSnapshotRelease"),
            "Task getLastTagSnapshotRelease not executed",
        )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagSnapshotDebug"),
            "Task getLastTagSnapshotDebug executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build successful",
        )
        assertTrue(
            distributionResult.output.contains("BUILD SUCCESSFUL"),
            "Confluence distribution successful"
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")

        val afterAutomationAttachments = confluenceController.getAttachments(pageId)
        val afterAutomationComments = confluenceController.getComments(pageId)

        assertTrue {
            afterAutomationAttachments.last().fileName.contains("app-debug.aab")
        }
        assertTrue {
            afterAutomationComments.last().html.contains("app-debug.aab")
        }
    }

    @Test
    @Throws(IOException::class)
    fun `confluence bundle build distribution available with distribution config with proxy without assemble`() {
        val pageId = System.getProperty("CONFLUENCE_PAGE_ID")


        val beforeAutomationAttachments = confluenceController.getAttachments(pageId)
        val beforeAutomationComments = confluenceController.getComments(pageId)

        beforeAutomationComments.forEach {
            confluenceController.removeComment(it.id)
        }

        beforeAutomationAttachments.forEach {
            confluenceController.removeAttachment(it.id)
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
            confluenceConfig = ConfluenceConfig(
                auth = ConfluenceConfig.Auth(
                    baseUrl = System.getProperty("CONFLUENCE_BASE_URL"),
                    username = System.getProperty("CONFLUENCE_USER_NAME"),
                    password = System.getProperty("CONFLUENCE_USER_PASSWORD")
                ),
                distribution = ConfluenceConfig.Distribution(
                    pageId = pageId
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
        val givenConfluenceDistributionTask = "confluenceDistributionUploadBundleDebug"
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

        val proxyProps = mapOf(
            "https.proxyUser" to System.getProperty("PROXY_USER"),
            "https.proxyPassword" to System.getProperty("PROXY_PASSWORD"),
            "https.proxyHost" to System.getProperty("PROXY_HOST"),
            "https.proxyPort" to System.getProperty("PROXY_PORT")
        )

        val distributionResult: BuildResult = projectDir.runTask(givenConfluenceDistributionTask, proxyProps)

        projectDir.getFile("app").printFilesRecursively()

        assertTrue(
            !distributionResult.output.contains("Task :app:getLastTagSnapshotRelease"),
            "Task getLastTagSnapshotRelease not executed",
        )
        assertTrue(
            distributionResult.output.contains("Task :app:getLastTagSnapshotDebug"),
            "Task getLastTagSnapshotDebug executed",
        )
        assertTrue(
            distributionResult.output.contains("BUILD SUCCESSFUL"),
            "Confluence distribution successful"
        )
        assertTrue(givenOutputFile.exists(), "Output file exists")

        val afterAutomationAttachments = confluenceController.getAttachments(pageId)
        val afterAutomationComments = confluenceController.getComments(pageId)

        assertTrue {
            afterAutomationAttachments.last().fileName.contains("app-debug.aab")
        }
        assertTrue {
            afterAutomationComments.last().html.contains("app-debug.aab")
        }
    }

}

private fun getChangelog(): String {
    return """
[CEB-3243] [And] Mickey tried to fix the loader on Goofy’s form after settings got tangled, and navigation went bonkers
[CEB-3277] [Android] Donald’s transaction history exploded when he peeked into Daisy’s credit card details
    """.trimIndent()
}
