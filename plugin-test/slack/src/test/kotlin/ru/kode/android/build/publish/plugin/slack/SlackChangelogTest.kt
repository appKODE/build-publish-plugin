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
import java.io.File
import java.io.IOException

class SlackChangelogTest {

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
            }
        )
    }

    @Test
    @Throws(IOException::class)
    fun `slack changelog sending available with changelog config without proxy`() {
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
                        issueNumberPattern = "TEST-\\\\d+",
                        issueUrlPrefix = "${System.getProperty("JIRA_BASE_URL")}/browse/"
                    )
                ),
            slackConfig = SlackConfig(
                bot = SlackConfig.Bot(
                    webhookUrl = System.getProperty("SLACK_WEBHOOK_URL"),
                    uploadApiTokenFilePath = null,
                    iconUrl = System.getProperty("SLACK_ICON_URL")
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
                distribution = null
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
        val givenSlackChangelogTask = "sendSlackChangelogDebug"
        val git = projectDir.initGit()

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)

        getLongChangelog()
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
        val changelogResult: BuildResult = projectDir.runTask(givenSlackChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !assembleResult.output.contains("Task :app:getLastTagSnapshotRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagSnapshotDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build successful",
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Slack changelog successful"
        )
        assertTrue(givenOutputFileExists, "Output file exists")
    }

    @Test
    @Throws(IOException::class)
    fun `slack changelog sending available with changelog config without proxy and assemble`() {
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
                        issueNumberPattern = "TEST-\\\\d+",
                        issueUrlPrefix = "${System.getProperty("JIRA_BASE_URL")}/browse/"
                    )
                ),
            slackConfig = SlackConfig(
                bot = SlackConfig.Bot(
                    webhookUrl = System.getProperty("SLACK_WEBHOOK_URL"),
                    iconUrl = System.getProperty("SLACK_ICON_URL"),
                    uploadApiTokenFilePath = null
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
                distribution = null
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
        val git = projectDir.initGit()

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)

        getLongChangelog()
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

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc2-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !changelogResult.output.contains("Task :app:getLastTagSnapshotRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            changelogResult.output.contains("Task :app:getLastTagSnapshotDebug"),
            "Task getLastTagDebug executed",
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Slack changelog successful"
        )
        assertTrue(!givenOutputFileExists, "Output file not exists")
    }

    @Test
    @Throws(IOException::class)
    fun `slack changelog sending available with changelog config with proxy`() {
        projectDir.createAndroidProject(
            buildTypes = listOf(
                BuildType("debug"),
                BuildType("release")
            ),
            foundationConfig =
                FoundationConfig(
                    output =
                        FoundationConfig.Output(
                            baseFileName = "test-android",
                        ),
                    changelog = FoundationConfig.Changelog(
                        issueNumberPattern = "TEST-\\\\d+",
                        issueUrlPrefix = "${System.getProperty("JIRA_BASE_URL")}/browse/"
                    )
                ),
            slackConfig = SlackConfig(
                bot = SlackConfig.Bot(
                    webhookUrl = System.getProperty("SLACK_WEBHOOK_URL"),
                    iconUrl = System.getProperty("SLACK_ICON_URL"),
                    uploadApiTokenFilePath = null
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
                distribution = null
            ),
            topBuildFileContent = """
                plugins {
                    id 'ru.kode.android.build-publish-novo.foundation' apply false
                }
            """.trimIndent()
        )
        val givenTagName1 = "v0.0.338-release"
        val givenTagName2 = "v0.0.339-release"
        val givenCommitMessage = "Initial commit"
        val givenAssembleTask = "assembleRelease"
        val givenSlackChangelogTask = "sendSlackChangelogRelease"
        val git = projectDir.initGit()

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName1)

        getLongChangelog()
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

        val assembleResult: BuildResult = projectDir.runTask(
            givenAssembleTask,
            systemProperties = proxyProps
        )
        val changelogResult: BuildResult = projectDir.runTask(
            givenSlackChangelogTask,
            systemProperties = proxyProps
        )

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/release")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("test-android-release-vc339-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            assembleResult.output.contains("Task :app:getLastTagSnapshotRelease"),
            "Task getLastTagRelease executed",
        )
        assertTrue(
            !assembleResult.output.contains("Task :app:getLastTagSnapshotDebug"),
            "Task getLastTagDebug not executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD SUCCESSFUL"),
            "Build successful",
        )
        assertTrue(
            changelogResult.output.contains("BUILD SUCCESSFUL"),
            "Slack changelog successful"
        )
        assertTrue(givenOutputFileExists, "Output file exists")
    }

    @Test
    @Throws(IOException::class)
    fun `slack changelog sending not available without changelog config`() {
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
                    uploadApiTokenFilePath = null
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
        val givenSlackChangelogTask = "slackChangelogDebug"
        val git = projectDir.initGit()

        git.addAllAndCommit(givenCommitMessage)
        git.tag.addNamed(givenTagName)

        val assembleResult: BuildResult = projectDir.runTaskWithFail(givenAssembleTask)
        val automationResult: BuildResult = projectDir.runTaskWithFail(givenSlackChangelogTask)

        projectDir.getFile("app").printFilesRecursively()

        val apkDir = projectDir.getFile("app/build/outputs/apk/debug")
        val givenOutputFileExists = apkDir.listFiles()
            ?.any { it.name.matches(Regex("autotest-debug-vc1-\\d{8}\\.apk")) }
            ?: false

        assertTrue(
            !assembleResult.output.contains("Task :app:getLastTagSnapshotRelease"),
            "Task getLastTagRelease not executed",
        )
        assertTrue(
            !assembleResult.output.contains("Task :app:getLastTagSnapshotDebug"),
            "Task getLastTagDebug not executed",
        )
        assertTrue(
            assembleResult.output.contains("BUILD FAILED"),
            "Build failed",
        )
        assertTrue(
            automationResult.output.contains("BUILD FAILED"),
            "Slack changelog failed"
        )
        assertTrue(!givenOutputFileExists, "Output file not exists")
    }
}

private fun getLongChangelog(): String {
    return """
[TEST-3243] [And] Mickey tried to fix the loader on Goofy’s form after settings got tangled, and navigation went bonkers
[TEST-3277] [Android] Donald’s transaction history exploded when he peeked into Daisy’s credit card details
[TEST-3158] [Android] Goofy’s final SBP screen shows all the goofy statuses after he paid his cookie subscription
[TEST-3230] [And] Pluto’s auto-payments change color depending on whether he’s happy, sad, or chasing a ball
[TEST-3104] [And] Minnie swapped coins while checking her personal rate, but the exchange screen got all wobbly
[TEST-3208] [Android] Chip and Dale counted the tiny coins that kept jumping off the currency chart
[TEST-3176] [Android] Huey, Dewey, and Louie couldn’t find the icon for their adventure map
[TEST-3179] [Android] Goofy tried to go back to the main tab, but the app slammed the door and locked him out
[TEST-3198] [Android] Donald crashed the app while testing his investment rocket after the backend hiccuped
[TEST-3177] [And] Mickey painted the transaction header in rainbow colors, but some stripes ran off the screen
[TEST-3195] [Android] Daisy’s form constructor went haywire when the backend sent funny answers
[TEST-3015] [Android] Goofy tried to send his phone number, but some digits went missing and played hide-and-seek
[TEST-3088] [Android] Pluto’s numbers kept dancing in the flow instead of staying still for the magic spell
[TEST-3125] [Android] Minnie searched for her friend’s contact, but the list kept spinning like a merry-go-round
[TEST-3128] [Android] Donald shared a card through the magic portal, and the app froze in surprise
[TEST-2742] [Android] Mickey scanned a QR code, but the camera refused to sleep and blinked endlessly
[TEST-3063] [Android] Daisy tried uploading a picture, but the image got all squished and caused chaos
[TEST-3074] [And] Goofy opened files from the gallery, and the system sheet just wouldn’t hide, sticking like glue
[TEST-1573] [And] Minnie created error templates that looked like funny comic strips
[TEST-2683] [And] Pluto arranged all the credit cards in the product block like a deck of silly cards
[TEST-3011] [And] Goofy’s history scroll didn’t match the design, hopping like kangaroos
[TEST-3038] [And] Daisy’s SBB icon refused to appear while she tried topping up her magical account
[TEST-3068] [And] Mickey’s main investment tab turned colors like a chameleon in the enchanted forest
[TEST-3080] [And] Pluto counted his final-fin operations, but splitting 100 coins was trickier than catching butterflies
[TEST-3181] [Android] Huey tracked the “Completion Date,” but the SLA didn’t recognize his cartoon logic
[TEST-2996] [And] Goofy connected AppMetrica while juggling pies in the kitchen
[TEST-3141] [And] Minnie sent the time through a rocket, and the screen winked in response
[TEST-2871] [And] Donald faced errors 400, 401, 403 while racing through the authorization maze
[TEST-3028] [And] Pluto’s zone tried to select a coin, but it bounced like a magic ball
[TEST-2991] [And] Mickey’s calendar auto-payments glowed in colors that only cartoons could understand
[TEST-3107] [And] Goofy checked the transaction icons while hopping on rainbow tiles for the installment plan
[TEST-2972] [iOS] Huey clicked all credit buttons, but they squeaked like tiny rubber ducks
[TEST-3070] [iOS] Daisy’s credit card updated magically, but only after a pull-refresh spell
[TEST-3073] [iOS] Mickey’s Pikachu card refused to show its blocked status, hiding behind clouds
[iOS] [TEST-3031] Pluto tried to close the MP elements, but autoblock made them dance like elves
[TEST-2912] [And] Goofy erased the “Enter sum” text from the filters, leaving sparkles behind
[TEST-3033] [Android] Minnie logged out, but the response code tumbled like bouncing jelly
[TEST-3055] [Android] Daisy’s credit status stayed frozen while she spun in a cartoon loop
[TEST-3058] [And] Pluto’s form button changed colors every step, leading him to the next adventure
[TEST-3061] [And] Mickey’s RADIO_BUTTON glowed, and validation errors turned into tiny cartoon clouds
[TEST-2975] [TEST-2977] [And] [iOS] Daisy juggled coins while the cross-rate spell added magical values in the input field
[TEST-3082] [And] Goofy jumped through transaction details, seeing cartoon numbers appear in the header
[TEST-3086] [And] Mickey drew a payment chart, but each bar danced with a life of its own
[TEST-3077] [IOS] Pluto’s main screen blocked products, and cards glimmered like tiny treasures [TEST-3078] [IOS] The savings tab sparkled while new products remained hidden in fairy dust
[TEST-3069] [And] Minnie tapped the date fields, and a magical calendar appeared on top of the passport screen
[TEST-3076] [And] Mickey’s main screen blocked cards and accounts, with sparkles indicating hidden treasures
[TEST-3079] [And] Pluto’s savings tab glimmered, showing hidden coins behind the magical curtain
[TEST-3046] [And] Goofy opened the installment plan, and coins floated magically onto the next screen
[TEST-2978] [Android] Donald logged out, and bottom sheets disappeared like vanishing cartoons
[TEST-2994] [Android] Mickey’s main screen opened a new product, but the magic requirements were tricky
[TEST-3030] [And] Pluto tried to close MP elements, but they hopped away like cartoon rabbits
[TEST-3041] [Android] Daisy tapped her virtual card, and it flew magically to the chosen screen
[TEST-3048] [And] Mickey’s credit text wrapped into two lines, like a bouncing comic balloon
[TEST-3060] [And] Goofy closed the MP sheet, but after three months, it magically reappeared
[TEST-2985] [TEST-2986] [And] [iOS] Pluto’s profile validation failed, making files dance like mischievous elves
[TEST-321] [common] Huey sent header data through the network, leaving sparkles in the requests
[TEST-3015] [And] [Android] Goofy tried sending his number, but digits went missing and played hide-and-seek
[TEST-3021] [And] Daisy’s phone translations reset magically as if a wizard waved a wand
[TEST-3024] [Android] Chip tried to load the banner, but it floated away before appearing
[TEST-3004] [TEST-3006] [And] [iOS] Mickey’s profile couldn’t scroll automatically, so tiny elves helped him
[TEST-2509] [And] Goofy opened the settings, and animations popped like fireworks across the screen
[TEST-2830] [And] Pluto adjusted the currency arrows, making them twirl in rainbow patterns
[TEST-3016] [And] Daisy’s cross-rate button glowed green, letting her spend magical coins
[TEST-3017] [And] Mickey tried entering zero, but the button refused to sleep like a stubborn cat
[TEST-3036] [And] Huey’s notification ribbon glimmered, but didn’t match cartoon design rules
[TEST-3038] [And] Daisy’s SBB icon disappeared while topping up, hopping like a playful rabbit
[TEST-2984] [Android] Goofy’s forms wouldn’t scroll automatically, leaving tiny clouds behind
    """.trimIndent()
}
