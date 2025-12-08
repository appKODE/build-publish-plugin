package ru.kode.android.build.publish.plugin.test.utils

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

@Suppress("LongMethod", "CyclomaticComplexMethod", "CascadingCallWrapping")
fun File.createAndroidProject(
    compileSdk: Int = 34,
    buildTypes: List<BuildType>,
    productFlavors: List<ProductFlavor> = listOf(),
    defaultConfig: DefaultConfig? = DefaultConfig(),
    foundationConfig: FoundationConfig = FoundationConfig(),
    appCenterConfig: AppCenterConfig? = null,
    clickUpConfig: ClickUpConfig? = null,
    confluenceConfig: ConfluenceConfig? = null,
    firebaseConfig: FirebaseConfig? = null,
    jiraConfig: JiraConfig? = null,
    playConfig: PlayConfig? = null,
    slackConfig: SlackConfig? = null,
    telegramConfig: TelegramConfig? = null,
    topBuildFileContent: String? = null,
) {
    val topSettingsFile = this.getFile("settings.gradle")
    val topBuildFile = this.getFile("build.gradle")
    val appBuildFile = this.getFile("app/build.gradle")
    val androidManifestFile = this.getFile("app/src/main/AndroidManifest.xml")

    if (topBuildFileContent != null) {
        println("--- CORE BUILD.GRADLE START ---")
        println(topBuildFileContent)
        println("--- CORE BUILD.GRADLE END ---")
        topBuildFile.writeText(topBuildFileContent)
    }

    val topSettingsFileContent =
        """
        pluginManagement {
            repositories {
                mavenLocal()
                google()
                mavenCentral()
                gradlePluginPortal()
            }
            plugins {
                id("com.android.application") version "8.11.1"
            }
        }
    
        dependencyResolutionManagement {
            repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
            repositories {
                mavenLocal()
                google()
                mavenCentral()
            }
        }
    
        rootProject.name = "My Application"
        include(":app")
        """.trimIndent()
    writeFile(topSettingsFile, topSettingsFileContent)
    val buildTypesBlock =
        buildTypes
            .joinToString(separator = "\n") {
                """
                ${it.name} 
            """
            }
            .let {
                """
            buildTypes {
            $it
            }
            """
            }
    val flavorDimensionsBlock =
        productFlavors
            .takeIf { it.isNotEmpty() }
            ?.mapTo(mutableSetOf()) { it.dimension }
            ?.joinToString { "\"$it\"" }
            ?.let {
                "flavorDimensions += [$it]"
            }
            .orEmpty()
    val productFlavorsBlock =
        productFlavors
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = "\n") {
                """
                create("${it.name}") {
                    dimension = "${it.dimension}"
                }
            """
            }?.let {
                """
            productFlavors {
            $it
            }
            """
            }.orEmpty()

    val defaultConfigBlock =
        defaultConfig?.let { config ->
            """
            defaultConfig {
                applicationId "${config.applicationId}"
                minSdk ${config.minSdk}
                targetSdk ${config.targetSdk}
                
                ${config.versionCode?.let { "versionCode $it" }.orEmpty()}
                ${config.versionName?.let { "versionName \"$it\"" }.orEmpty()}
            }
        """
        }
    val buildTypeOutputBlock =
        foundationConfig.buildTypeOutput?.let { (name, config) ->
            """
                buildVariant("$name") {
                    it.baseFileName.set("${foundationConfig.output.baseFileName}")
                    ${config.useVersionsFromTag?.let { "it.useVersionsFromTag.set($it)" }.orEmpty()}
                    ${config.useStubsForTagAsFallback?.let { "it.useStubsForTagAsFallback.set($it)" }.orEmpty()}
                    ${config.useDefaultsForVersionsAsFallback?.let { "it.useDefaultsForVersionsAsFallback.set($it)" }.orEmpty()}
                    ${config.buildTagPatternBuilderFunctions?.let { buildTagPatternBlock(it) }.orEmpty()}
                }
        """
        }
    val foundationConfigBlock = """
        buildPublishFoundation {
            output {
                common {
                    it.baseFileName.set("${foundationConfig.output.baseFileName}")
                    ${foundationConfig.output.useVersionsFromTag?.let { "it.useVersionsFromTag.set($it)" }.orEmpty()}
                    ${foundationConfig.output.useStubsForTagAsFallback?.let { "it.useStubsForTagAsFallback.set($it)" }.orEmpty()}
                    ${foundationConfig.output.useDefaultsForVersionsAsFallback?.let { "it.useDefaultsForVersionsAsFallback.set($it)" }.orEmpty()}
                    ${foundationConfig.output.buildTagPatternBuilderFunctions?.let { buildTagPatternBlock(it) }.orEmpty()}
                }
        
                ${buildTypeOutputBlock.orEmpty()}
            }
            
            changelogCommon {
                issueNumberPattern.set("${foundationConfig.changelog.issueNumberPattern}")
                issueUrlPrefix.set("${foundationConfig.changelog.issueUrlPrefix}")
                commitMessageKey.set("${foundationConfig.changelog.commitMessageKey}")
                excludeMessageKey.set(${foundationConfig.changelog.excludeMessageKey})
            }
        }
    """
    val appCenterConfigBlock =
        appCenterConfig?.let { config ->
            """
            buildPublishAppCenter {
                auth {
                    common {
                        ownerName.set("${config.auth.ownerName}")
                        apiTokenFile.set(File("${config.auth.apiTokenFilePath}"))
                    }
                }
            
                distribution {
                    common {
                        appName.set("${config.distribution.appName}")
                        testerGroups(${config.distribution.testerGroups.joinToString { "\"$it\"" }})
                        ${config.distribution.maxUploadStatusRequestCount?.let { "maxUploadStatusRequestCount.set($it)" }.orEmpty()}
                        ${config.distribution.uploadStatusRequestDelayMs?.let { "uploadStatusRequestDelayMs.set($it)" }.orEmpty()}
                        ${config.distribution.uploadStatusRequestDelayCoefficient?.let { "uploadStatusRequestDelayCoefficient.set($it)" }.orEmpty()}
                    }
                }
            }
            """.trimIndent()
        }.orEmpty()

    val clickUpConfigBlock =
        clickUpConfig?.let { config ->
            """
            buildPublishClickUp {
                auth {
                    common {
                        it.apiTokenFile.set(File("${config.auth.apiTokenFilePath}"))
                    }
                }
                
                automation {
                    common {
                        ${config.automation.fixVersionPattern?.let { """fixVersionPattern.set("$it")""" }.orEmpty()}
                        ${config.automation.fixVersionFieldId?.let { """fixVersionFieldId.set("$it")""" }.orEmpty()}
                        ${config.automation.tagName?.let { """tagName.set("$it")""" }.orEmpty()}
                    }
                }
            }
            """.trimIndent()
        }.orEmpty()

    val confluenceConfigBlock =
        confluenceConfig?.let { config ->
            """
            buildPublishConfluence {
                auth {
                    common {
                        it.baseUrl.set("${config.auth.baseUrl}")
                        it.credentials.username.set("${config.auth.username}")
                        it.credentials.password.set("${config.auth.password}")
                    }
                }
                
                distribution {
                    buildVariant("default") {
                        pageId.set("${config.distribution.pageId}")
                    }
                }
            }
            """.trimIndent()
        }.orEmpty()

    val firebaseConfigBlock =
        firebaseConfig?.let { config ->
            """
            buildPublishFirebase {
                distribution {
                    common {
                        serviceCredentialsFile.set(File("${config.distribution.serviceCredentialsFilePath}"))
                        appId.set("${config.distribution.appId}")
                        artifactType.set("${config.distribution.artifactType}")
                        testerGroups(${config.distribution.testerGroups.joinToString { "\"$it\"" }})
                    }
                }
            }
            """.trimIndent()
        }.orEmpty()

    val jiraConfigBlock =
        jiraConfig?.let { config ->
            """
        buildPublishJira {
            auth {
                common {
                    it.baseUrl.set("${config.auth.baseUrl}")
                    it.credentials.username.set("${config.auth.username}")
                    it.credentials.password.set("${config.auth.password}")
                }
            }
                
            ${config.automation?.let { jiraAutomationBlock(it) }.orEmpty()}
        }
            """
        }.orEmpty()

    val playConfigBlock =
        playConfig?.let { config ->
            """
            buildPublishPlay {
                auth {
                    common {
                        apiTokenFile.set(File("${config.auth.apiTokenFilePath}"))
                        appId.set("${config.auth.appId}")
                    }
                }
            
                distribution {
                    common {
                        trackId.set("${config.distribution.trackId}")
                        updatePriority.set(${config.distribution.updatePriority})
                    }
                }
            }
            """.trimIndent()
        }.orEmpty()

    val slackConfigBlock =
        slackConfig?.let { config ->
            """
            buildPublishSlack {
                bot {
                    common {
                        webhookUrl.set("${config.bot.webhookUrl}")
                        iconUrl.set("${config.bot.iconUrl}")
                    }
                }
                
                changelog {
                    common {
                        userMentions(${config.changelog.userMentions.joinToString { "\"$it\"" }})
                        attachmentColor.set("${config.changelog.attachmentColor}")
                    }
                }
                
                distributionCommon {
                    uploadApiTokenFile.set(File("${config.distribution.uploadApiTokenFilePath}"))
                    destinationChannels(${config.distribution.destinationChannels.joinToString { "\"$it\"" }})
                }
            }
            """.trimIndent()
        }.orEmpty()

    val telegramConfigBlock =
        telegramConfig?.let { config ->
            """
            buildPublishTelegram {
                ${config.bots.bots.takeIf { it.isNotEmpty() }?.let { telegramBotsBlock(it) }.orEmpty()}
                ${config.changelog?.let { telegramChangelogBlock(it) }.orEmpty()}
                ${config.distribution?.let { telegramDistributionBlock(it) }.orEmpty()}
            }
            """.trimIndent()
        }.orEmpty()

    val appBuildFileContent =
        """
        plugins {
            id 'com.android.application'
            id 'ru.kode.android.build-publish-novo.foundation'
            ${appCenterConfig?.let { """id 'ru.kode.android.build-publish-novo.appcenter'""" }.orEmpty()}
            ${clickUpConfig?.let { """id 'ru.kode.android.build-publish-novo.clickup'""" }.orEmpty()}
            ${confluenceConfig?.let { """id 'ru.kode.android.build-publish-novo.confluence'""" }.orEmpty()}
            ${firebaseConfig?.let { """id 'ru.kode.android.build-publish-novo.firebase'""" }.orEmpty()}
            ${jiraConfig?.let { """id 'ru.kode.android.build-publish-novo.jira'""" }.orEmpty()}
            ${playConfig?.let { """id 'ru.kode.android.build-publish-novo.play'""" }.orEmpty()}
            ${slackConfig?.let { """id 'ru.kode.android.build-publish-novo.slack'""" }.orEmpty()}
            ${telegramConfig?.let { """id 'ru.kode.android.build-publish-novo.telegram'""" }.orEmpty()}
        }
        
        android {
            namespace = "ru.kode.test"

            compileSdk $compileSdk
        
            $defaultConfigBlock
            
            $buildTypesBlock
            
            $flavorDimensionsBlock
            
            $productFlavorsBlock
        }
                
        $foundationConfigBlock
        
        $jiraConfigBlock
        
        $appCenterConfigBlock
        
        $clickUpConfigBlock
        
        $confluenceConfigBlock
        
        $firebaseConfigBlock
                
        $playConfigBlock
        
        $slackConfigBlock
        
        $telegramConfigBlock
        
        """.trimIndent()
            .removeEmptyLines()
            .also {
                println("--- APP BUILD.GRADLE START ---")
                println(it)
                println("--- APP BUILD.GRADLE END ---")
            }
    writeFile(appBuildFile, appBuildFileContent)
    val androidManifestFileContent =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <manifest />
        """.trimIndent()
    writeFile(androidManifestFile, androidManifestFileContent)
}

private fun jiraAutomationBlock(automation: JiraConfig.Automation): String {
    return """
            automation {
                common {
                    it.projectKey.set("${automation.projectKey}")
                    ${automation.fixVersionPattern?.let { """it.fixVersionPattern.set("$it")""" }.orEmpty()}
                    ${automation.labelPattern?.let { """it.labelPattern.set("$it")""" }.orEmpty()}
                    ${automation.targetStatusName?.let { """it.targetStatusName.set("$it")""" }.orEmpty()}
                }
            }
    """
}

private fun telegramChangelogBlock(changelog: TelegramConfig.Changelog): String {
    return """
                        changelog {
                            common {
                                it.userMentions(${changelog.userMentions.joinToString { "\"$it\"" }})
                                
                                ${changelog.destinationBots.joinToString(separator = "\n") { telegramDestinationBotBlock(it) }}
                            }
                        }
    """
}

private fun telegramBotsBlock(bots: List<TelegramConfig.Bot>): String {
    return """
                        bots {
                            common {
                                ${bots.joinToString("\n") { telegramBotBlock(it) }}
                            }
                        }
    """
}

private fun telegramDistributionBlock(distribution: TelegramConfig.Distribution): String {
    return """
                        distribution {
                           common {
                               ${distribution.destinationBots.joinToString("\n") { telegramDestinationBotBlock(it) }}
                           } 
                        }
    """
}

private fun telegramBotBlock(bot: TelegramConfig.Bot): String {
    return """     
                                it.bot("${bot.botName}") {
                                    botId.set("${bot.botId}")
                                    ${bot.botServerBaseUrl?.let { """botServerBaseUrl.set("$it")""" }.orEmpty()}
                                    ${bot.botServerUsername?.let { """botServerAuth.username.set("$it")""" }.orEmpty()}
                                    ${bot.botServerPassword?.let { """botServerAuth.password.set("$it")""" }.orEmpty()}
                                    
                                    ${bot.chats.joinToString(separator = "\n") { telegramBotChatBlock(it) }}
                                }
    """
}

private fun telegramBotChatBlock(chat: TelegramConfig.Chat): String {
    return """
                                    chat("${chat.chatName}") {
                                        chatId = "${chat.chatId}"
                                        ${chat.topicId?.let { "topicId = \"$it\"" }.orEmpty()}
                                    }
    """
}

private fun telegramDestinationBotBlock(destinationBot: TelegramConfig.DestinationBot): String {
    return """
                                it.destinationBot {
                                    botName = "${destinationBot.botName}"
                                    chatNames(${destinationBot.chatNames.joinToString { "\"$it\"" }})
                                }
    """
}

private fun buildTagPatternBlock(items: List<String>): String {
    return """
                    it.buildTagPattern {
${items.joinToString(separator = "\n") { "                      $it" }}
                    }
    """
}

@Throws(IOException::class)
private fun writeFile(
    destination: File,
    content: String,
) {
    var output: BufferedWriter? = null
    try {
        output = BufferedWriter(FileWriter(destination))
        output.write(content)
    } finally {
        output?.close()
    }
}

private fun File.printFilesRecursivelyInternal(
    prefix: String,
    filterFile: (File) -> Boolean,
    filterDirectory: (File) -> Boolean,
): Boolean {
    if (!this.isDirectory) {
        println("Not a directory: ${this.path}")
        return true
    }
    this.listFiles()?.forEach { file ->
        if (file.isFile && filterFile(file)) {
            println("$prefix${file.path}")
        } else if (file.isDirectory && filterDirectory(file)) {
            file.printFilesRecursivelyInternal(prefix, filterFile, filterDirectory)
        }
    }
    return false
}

private fun String.removeEmptyLines(): String {
    return this.lines()
        .filter { it.trim().isNotEmpty() }
        .joinToString("\n")
}

fun File.printFilesRecursively(prefix: String = "") {
    println("--- FILES START ---")
    printFilesRecursivelyInternal(
        prefix,
        filterFile = {
            val ext = it.extension
            ext.contains("apk") || ext.contains("json") || ext.contains("aab") || ext.contains("txt")
        },
        filterDirectory = { it.endsWith("build") || it.path.contains("outputs") || it.path.contains("renamed") },
    )
    println("--- FILES END ---")
}

fun File.getFile(path: String): File {
    val file = File(this, path)
    file.parentFile.mkdirs()
    return file
}

fun File.runTask(
    task: String,
    systemProperties: Map<String, String> = emptyMap()
): BuildResult {
    val args = mutableListOf(task).apply {
        add("--info")
        add("--stacktrace")
        systemProperties.forEach { (key, value) ->
            add("-D$key=$value")
        }
    }
    return GradleRunner.create()
        .withProjectDir(this)
        .withArguments(args)
        .withPluginClasspath()
        .forwardOutput()
        .build()
}

fun File.runTasks(
    vararg tasks: String,
    systemProperties: Map<String, String> = emptyMap()
): BuildResult {
    val args = tasks.toMutableList().apply {
        add("--info")
        add("--stacktrace")
        systemProperties.forEach { (key, value) ->
            add("-D$key=$value")
        }
    }
    return GradleRunner.create()
        .withProjectDir(this)
        .withArguments(args)
        .withPluginClasspath()
        .forwardOutput()
        .build()
}

fun File.runTaskWithFail(
    task: String,
    systemProperties: Map<String, String> = emptyMap()
): BuildResult {
    val args = mutableListOf(task, "--info").apply {
        systemProperties.forEach { (key, value) ->
            add("-D$key=$value")
        }
    }
    return GradleRunner.create()
        .withProjectDir(this)
        .withArguments(args)
        .withPluginClasspath()
        .forwardOutput()
        .buildAndFail()
}

data class BuildType(
    val name: String,
)

data class ProductFlavor(
    val name: String,
    val dimension: String,
)

data class FoundationConfig(
    val output: Output = Output(),
    val buildTypeOutput: Pair<String, Output>? = null,
    val changelog: Changelog = Changelog(),
) {
    data class Output(
        val baseFileName: String = "test-app",
        val useVersionsFromTag: Boolean? = null,
        val useStubsForTagAsFallback: Boolean? = null,
        val useDefaultsForVersionsAsFallback: Boolean? = null,
        val buildTagPatternBuilderFunctions: List<String>? = null,
    )

    data class Changelog(
        val issueNumberPattern: String = "TICKET-\\\\d+",
        val issueUrlPrefix: String = "https://jira.example.com/browse/",
        val commitMessageKey: String = "CHANGELOG",
        val excludeMessageKey: Boolean = true,
    )
}

data class AppCenterConfig(
    val auth: Auth,
    val distribution: Distribution,
) {
    data class Auth(
        val apiTokenFilePath: String,
        val ownerName: String,
    )

    data class Distribution(
        val appName: String,
        val testerGroups: List<String>,
        val maxUploadStatusRequestCount: Int?,
        val uploadStatusRequestDelayMs: Int?,
        val uploadStatusRequestDelayCoefficient: Int?,
    )
}

data class ClickUpConfig(
    val auth: Auth,
    val automation: Automation,
) {
    data class Auth(
        val apiTokenFilePath: String,
    )

    data class Automation(
        val fixVersionPattern: String?,
        val fixVersionFieldId: String?,
        val tagName: String?,
    )
}

data class ConfluenceConfig(
    val auth: Auth,
    val distribution: Distribution,
) {
    data class Auth(
        val baseUrl: String,
        val username: String,
        val password: String,
    )

    data class Distribution(
        val pageId: String,
    )
}

data class FirebaseConfig(
    val distribution: Distribution,
) {
    data class Distribution(
        val serviceCredentialsFilePath: String,
        val artifactType: String,
        val appId: String,
        val testerGroups: List<String>,
    )
}

data class JiraConfig(
    val auth: Auth,
    val automation: Automation?,
) {
    data class Auth(
        val baseUrl: String,
        val username: String,
        val password: String,
    )

    data class Automation(
        val projectKey: String,
        val labelPattern: String?,
        val fixVersionPattern: String?,
        val targetStatusName: String?,
    )
}

data class PlayConfig(
    val auth: Auth,
    val distribution: Distribution,
) {
    data class Auth(
        val apiTokenFilePath: String,
        val appId: String,
    )

    data class Distribution(
        val trackId: String,
        val updatePriority: Int,
    )
}

data class SlackConfig(
    val bot: Bot,
    val changelog: Changelog,
    val distribution: Distribution,
) {
    data class Bot(
        val webhookUrl: String,
        val iconUrl: String,
    )

    data class Changelog(
        val userMentions: List<String>,
        val attachmentColor: String,
    )

    data class Distribution(
        val uploadApiTokenFilePath: String,
        val destinationChannels: List<String>,
    )
}

data class DefaultConfig(
    val applicationId: String = "com.example.build.types.android",
    val versionCode: Int? = 1,
    val versionName: String? = "1.0",
    val minSdk: Int = 31,
    val targetSdk: Int = 34,
)

data class TelegramConfig(
    val bots: Bots,
    val changelog: Changelog?,
    val distribution: Distribution?,
) {
    data class Bots(
        val bots: List<Bot>,
    )

    data class Changelog(
        val userMentions: List<String>,
        val destinationBots: List<DestinationBot>,
    )

    data class Distribution(
        val destinationBots: List<DestinationBot>,
    )

    data class Bot(
        val botName: String,
        val botId: String,
        val botServerBaseUrl: String?,
        val botServerUsername: String?,
        val botServerPassword: String?,
        val chats: List<Chat>,
    )

    data class DestinationBot(
        val botName: String,
        val chatNames: List<String>,
    )

    data class Chat(
        val chatName: String,
        val chatId: String,
        val topicId: String?,
    )
}
