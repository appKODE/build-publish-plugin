package ru.kode.android.build.publish.plugin.test.utils

import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.PluginUnderTestMetadataReading
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

@Suppress("LongMethod", "CyclomaticComplexMethod", "CascadingCallWrapping")
fun File.createAndroidProject(
    compileSdk: Int = 36,
    buildTypes: List<BuildType>,
    productFlavors: List<ProductFlavor> = listOf(),
    defaultConfig: DefaultConfig? = DefaultConfig(),
    foundationConfig: FoundationConfig = FoundationConfig(),
    clickUpConfig: ClickUpConfig? = null,
    confluenceConfig: ConfluenceConfig? = null,
    firebaseConfig: FirebaseConfig? = null,
    jiraConfig: JiraConfig? = null,
    playConfig: PlayConfig? = null,
    slackConfig: SlackConfig? = null,
    telegramConfig: TelegramConfig? = null,
    topBuildFileContent: String? = null,
    import: String? = null,
    configureApplicationVariants: Boolean = false,
) {
    val topSettingsFile = this.getFile("settings.gradle")
    val topBuildFile = this.getFile("build.gradle")
    val appBuildFile = this.getFile("app/build.gradle")
    val androidManifestFile = this.getFile("app/src/main/AndroidManifest.xml")

    if (topBuildFileContent != null) {
        println("--- ${topBuildFile.path} START ---")
        println(topBuildFileContent)
        println("--- ${topBuildFile.path} END ---")
        topBuildFile.writeText(topBuildFileContent)
    }

    val topSettingsFileContent =
        """
        pluginManagement {
            repositories {
                google()
                mavenCentral()
                gradlePluginPortal()
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
            .also {
                println("--- ${topSettingsFile.path} START ---")
                println(it)
                println("--- ${topBuildFile.path} END ---")
            }
    writeFile(topSettingsFile, topSettingsFileContent)
    val buildTypesBlock =
        buildTypes
            .joinToString(separator = "\n") {
                """
                ${it.name} { debuggable = true } 
            """
            }
            .let {
                """
            buildTypes {
            $it 
            }
            """
            }
    val fullApplicationId = "\${fullApplicationId}"
    val authority = "\${authority}"
    val configureVariantsBlock =
        """
            applicationVariants.configureEach { variant ->
                def buildTypeSuffix = variant.buildType.applicationIdSuffix ?: ""
                def fullApplicationId = android.defaultConfig.applicationId + buildTypeSuffix
            
                def authority = "$fullApplicationId.provider"
                variant.buildConfigField(
                        "String",
                        "FILE_PROVIDER_AUTHORITY",
                        "\"$authority\""
                )
            
                def mergedVariant = variant.getMergedFlavor()
                mergedVariant.manifestPlaceholders.putAll([
                        APPLICATION_ID          : fullApplicationId,
                        FILE_PROVIDER_AUTHORITY : authority
                ])
            }
        """
            .takeIf { configureApplicationVariants }
            .orEmpty()
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
    val buildTypeOutputBlock1 =
        foundationConfig.buildTypeOutput?.let { (name, config) ->
            """
                buildVariant("$name") {
                    it.baseFileName.set("${config.baseFileName}")
                    ${config.useVersionsFromTag?.let { "it.useVersionsFromTag.set($it)" }.orEmpty()}
                    ${config.useStubsForTagAsFallback?.let { "it.useStubsForTagAsFallback.set($it)" }.orEmpty()}
                    ${config.useDefaultsForVersionsAsFallback?.let { "it.useDefaultsForVersionsAsFallback.set($it)" }.orEmpty()}
                    ${config.buildTagPatternBuilderFunctions?.let { buildTagPatternBlock(it) }.orEmpty()}
                    ${config.versionNameStrategy?.let { """it.versionNameStrategy { $it }""" }.orEmpty()}
                    ${config.versionCodeStrategy?.let { """it.versionCodeStrategy { $it }""" }.orEmpty()}
                }
        """
        }
    val buildTypeOutputBlock2 =
        foundationConfig.buildTypeOutput2?.let { (name, config) ->
            """
                buildVariant("$name") {
                    it.baseFileName.set("${config.baseFileName}")
                    ${config.useVersionsFromTag?.let { "it.useVersionsFromTag.set($it)" }.orEmpty()}
                    ${config.useStubsForTagAsFallback?.let { "it.useStubsForTagAsFallback.set($it)" }.orEmpty()}
                    ${config.useDefaultsForVersionsAsFallback?.let { "it.useDefaultsForVersionsAsFallback.set($it)" }.orEmpty()}
                    ${config.buildTagPatternBuilderFunctions?.let { buildTagPatternBlock(it) }.orEmpty()}
                    ${config.versionNameStrategy?.let { """it.versionNameStrategy { $it }""" }.orEmpty()}
                    ${config.versionCodeStrategy?.let { """it.versionCodeStrategy { $it }""" }.orEmpty()}
                }
        """
        }
    val buildTypeOutputBlock3 =
        foundationConfig.buildTypeOutput3?.let { (name, config) ->
            """
                buildVariant("$name") {
                    it.baseFileName.set("${config.baseFileName}")
                    ${config.useVersionsFromTag?.let { "it.useVersionsFromTag.set($it)" }.orEmpty()}
                    ${config.useStubsForTagAsFallback?.let { "it.useStubsForTagAsFallback.set($it)" }.orEmpty()}
                    ${config.useDefaultsForVersionsAsFallback?.let { "it.useDefaultsForVersionsAsFallback.set($it)" }.orEmpty()}
                    ${config.buildTagPatternBuilderFunctions?.let { buildTagPatternBlock(it) }.orEmpty()}
                    ${config.versionNameStrategy?.let { """it.versionNameStrategy { $it }""" }.orEmpty()}
                    ${config.versionCodeStrategy?.let { """it.versionCodeStrategy { $it }""" }.orEmpty()}
                }
        """
        }
    val foundationConfigBlock = """
        buildPublishFoundation {
            bodyLogging.set(${foundationConfig.bodyLogging})
            verboseLogging.set(${foundationConfig.verboseLogging})
            
            output {
                common {
                    it.baseFileName.set("${foundationConfig.output.baseFileName}")
                    ${foundationConfig.output.useVersionsFromTag?.let { "it.useVersionsFromTag.set($it)" }.orEmpty()}
                    ${foundationConfig.output.useStubsForTagAsFallback?.let { "it.useStubsForTagAsFallback.set($it)" }.orEmpty()}
                    ${foundationConfig.output.useDefaultsForVersionsAsFallback?.let { "it.useDefaultsForVersionsAsFallback.set($it)" }.orEmpty()}
                    ${foundationConfig.output.buildTagPatternBuilderFunctions?.let { buildTagPatternBlock(it) }.orEmpty()}
                }
        
                ${buildTypeOutputBlock1.orEmpty()}
                ${buildTypeOutputBlock2.orEmpty()}
                ${buildTypeOutputBlock3.orEmpty()}
            }
            
            changelogCommon {
                issueNumberPattern.set("${foundationConfig.changelog.issueNumberPattern}")
                issueUrlPrefix.set("${foundationConfig.changelog.issueUrlPrefix}")
                commitMessageKey.set("${foundationConfig.changelog.commitMessageKey}")
                excludeMessageKey.set(${foundationConfig.changelog.excludeMessageKey})
            }
        }
    """

    val clickUpConfigBlock =
        clickUpConfig?.let { config ->
            """
        buildPublishClickUp {
            auth {
                common {
                    it.apiTokenFile = project.file("${config.auth.apiTokenFilePath}")
                }
            }
            
            ${config.automation?.let { clickUpAutomationBlock(it) }.orEmpty()}
        }
            """
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
                
            ${config.distribution?.let { confluenceDistributionBlock(it) }.orEmpty()}
        }
            """
        }.orEmpty()

    val firebaseConfigBlock =
        firebaseConfig?.let { config ->
            val buildTypeFirebaseBlock =
                config.distributionBuildType?.let { (name, config) ->
                    """
                buildVariant("$name") {
                    it.serviceCredentialsFile = project.file("${config.serviceCredentialsFilePath}")
                    it.appId.set("${config.appId}")
                    it.artifactType.set(${config.artifactType})
                    ${config.testerGroups?.let { """it.testerGroups(${it.joinToString { "\"$it\"" }})""" }.orEmpty()}
                }
                    """
                }
            """
        buildPublishFirebase {
            distribution {
                common {
                    it.serviceCredentialsFile = project.file("${config.distributionCommon.serviceCredentialsFilePath}")
                    it.appId.set("${config.distributionCommon.appId}")
                    it.artifactType.set(${config.distributionCommon.artifactType})
                    ${config.distributionCommon.testerGroups?.let { """it.testerGroups(${it.joinToString { "\"$it\"" }})""" }.orEmpty()}
                }
                ${buildTypeFirebaseBlock.orEmpty()}
            }
        }
            """
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
                        it.apiTokenFile.set(File("${config.auth.apiTokenFilePath}"))
                        it.appId.set("${config.auth.appId}")
                    }
                }
            
                distribution {
                    common {
                        it.trackId.set("${config.distribution.trackId}")
                        it.updatePriority.set(${config.distribution.updatePriority})
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
                    it.webhookUrl.set("${config.bot.webhookUrl}")
                    ${config.bot.uploadApiTokenFilePath?.let { """it.uploadApiTokenFile = project.file("$it")""" }.orEmpty()}
                    it.iconUrl.set("${config.bot.iconUrl}")
                }
            }
            ${config.changelog?.let { slackChangelogBlock(it) }.orEmpty()}
            ${config.distribution?.let { slackDistributionBlock(it) }.orEmpty()}
        }
            """
        }.orEmpty()

    val telegramConfigBlock =
        telegramConfig?.let { config ->
            """
            buildPublishTelegram {
                ${config.bots.bots.takeIf { it.isNotEmpty() }?.let { telegramBotsBlock(it) }.orEmpty()}
                ${config.lookup?.let { telegramLookupBlock(it) }.orEmpty()}
                ${config.changelog?.let { telegramChangelogBlock(it) }.orEmpty()}
                ${config.distribution?.let { telegramDistributionBlock(it) }.orEmpty()}
            }
            """.trimIndent()
        }.orEmpty()

    val appBuildFileContent =
        """
        ${import.orEmpty()}
        
        plugins {
            id 'com.android.application'
            id 'ru.kode.android.build-publish-novo.foundation'
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
            $configureVariantsBlock
            
            $flavorDimensionsBlock
            
            $productFlavorsBlock
        }
                
        $foundationConfigBlock
        
        $jiraConfigBlock
        
        $clickUpConfigBlock
        
        $confluenceConfigBlock
        
        $firebaseConfigBlock
                
        $playConfigBlock
        
        $slackConfigBlock
        
        $telegramConfigBlock
                
        """.trimIndent()
            .removeEmptyLines()
            .also {
                println("--- ${appBuildFile.path} START ---")
                println(it)
                println("--- ${appBuildFile.path} END ---")
            }
    writeFile(appBuildFile, appBuildFileContent)
    val androidManifestFileContent =
        """
        <?xml version="1.0" encoding="utf-8"?>
        
        <manifest 
                xmlns:android="http://schemas.android.com/apk/res/android">
            <application
                android:label="Test App" />
        </manifest>
        """.trimIndent().apply {
            println("--- ${androidManifestFile.path} START ---")
            println(this)
            println("--- ${androidManifestFile.path} END ---")
        }
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

private fun clickUpAutomationBlock(automation: ClickUpConfig.Automation): String {
    return """
            automation {
                common {
                    ${automation.workspaceName.let { """it.workspaceName.set("$it")""" }}
                    ${automation.fixVersionPattern?.let { """it.fixVersionPattern.set("$it")""" }.orEmpty()}
                    ${automation.fixVersionFieldName?.let { """it.fixVersionFieldName.set("$it")""" }.orEmpty()}
                    ${automation.tagPattern?.let { """it.tagPattern.set("$it")""" }.orEmpty()}
                }
            }
    """
}

private fun confluenceDistributionBlock(distribution: ConfluenceConfig.Distribution): String {
    return """
           distribution {
                common {
                    it.compressed.set(${distribution.compressed})
                    it.pageId.set("${distribution.pageId}")
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

private fun telegramLookupBlock(changelog: TelegramConfig.Lookup): String {
    return """
                        lookup {
                            it.botName.set("${changelog.botName}")
                            it.chatName.set("${changelog.chatName}")
                            ${changelog.topicName?.let { "it.topicName.set(\"${it}\")" }.orEmpty()}
                        }
    """
}

private fun slackChangelogBlock(changelog: SlackConfig.Changelog): String {
    return """
            changelog {
                common {
                    it.userMentions(${changelog.userMentions.joinToString { "\"$it\"" }})
                    it.attachmentColor.set("${changelog.attachmentColor}")
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
                               it.compressed.set(${distribution.compressed})
                               ${distribution.destinationBots.joinToString("\n") { telegramDestinationBotBlock(it) }}
                           } 
                        }
    """
}

private fun slackDistributionBlock(distribution: SlackConfig.Distribution): String {
    return """
            distribution {
               common {
                   it.destinationChannels(${distribution.destinationChannels.joinToString { "\"$it\"" }})
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
                                        ${chat.chatId?.let { "chatId = \"$it\"" }.orEmpty()}
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
        filterDirectory = { it.endsWith("build") || it.path.contains("outputs") || it.path.contains("renamed")|| it.path.contains("intermediates") },
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
    systemProperties: Map<String, String> = emptyMap(),
    agpClasspath: List<File> = emptyList(),
    gradleVersion: String = "9.2.1"
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
        .apply {
            if (agpClasspath.isNotEmpty()) {
                withPluginClasspath(prepareClasspath(agpClasspath))
            } else {
                withPluginClasspath()
            }
        }
        .withGradleVersion(gradleVersion)
        .forwardOutput()
        .build()
}

fun File.runTasks(
    vararg tasks: String,
    systemProperties: Map<String, String> = emptyMap(),
    agpClasspath: List<File> = emptyList(),
    gradleVersion: String = "9.2.1"
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
        .apply {
            if (agpClasspath.isNotEmpty()) {
                withPluginClasspath(prepareClasspath(agpClasspath))
            } else {
                withPluginClasspath()
            }
        }
        .withGradleVersion(gradleVersion)
        .forwardOutput()
        .build()
}

fun File.runTaskWithFail(
    task: String,
    systemProperties: Map<String, String> = emptyMap(),
    agpClasspath: List<File> = emptyList(),
    gradleVersion: String = "9.2.1"
): BuildResult {
    val args = mutableListOf(task, "--info").apply {
        systemProperties.forEach { (key, value) ->
            add("-D$key=$value")
        }
    }
    return GradleRunner.create()
        .withProjectDir(this)
        .withArguments(args)
        .apply {
            if (agpClasspath.isNotEmpty()) {
                withPluginClasspath(prepareClasspath(agpClasspath))
            } else {
                withPluginClasspath()
            }
        }
        .withGradleVersion(gradleVersion)
        .forwardOutput()
        .buildAndFail()
}

private fun prepareClasspath(agpClassPath: List<File>): List<File> {
    val pluginClasspath: List<File> = PluginUnderTestMetadataReading.readImplementationClasspath()
    val filteredClasspath = pluginClasspath.filter { file ->
        val name = file.name.lowercase()
        !name.startsWith("gradle") ||
            !name.contains("android") ||
            !name.contains("agp")
    }
    println("Filtered ${filteredClasspath.size} classpath items, adding ${agpClassPath.size} AGP JARs")
    return filteredClasspath + agpClassPath
}

fun resolveRequiredAgpJars(agpVersion: String): List<File> {
    val project = ProjectBuilder.builder()
        .withName("temp-resolver")
        .build()

    project.buildscript.repositories.apply {
        google()
        mavenCentral()
    }

    val pluginClasspath = project.buildscript.configurations.getByName("classpath").apply {
        dependencies.clear()
        dependencies.add(project.dependencies.create("com.android.tools.build:gradle:$agpVersion"))
        dependencies.add(project.dependencies.create("com.android.application:com.android.application.gradle.plugin:$agpVersion"))
    }.resolve()

    return pluginClasspath.toList()
}


data class BuildType(
    val name: String,
)

data class ProductFlavor(
    val name: String,
    val dimension: String,
)

data class FoundationConfig(
    val bodyLogging: Boolean = false,
    val verboseLogging: Boolean = true,
    val output: Output = Output(),
    val buildTypeOutput: Pair<String, Output>? = null,
    val buildTypeOutput2: Pair<String, Output>? = null,
    val buildTypeOutput3: Pair<String, Output>? = null,
    val changelog: Changelog = Changelog(),
) {
    data class Output(
        val baseFileName: String = "test-app",
        val useVersionsFromTag: Boolean? = null,
        val useStubsForTagAsFallback: Boolean? = null,
        val useDefaultsForVersionsAsFallback: Boolean? = null,
        val buildTagPatternBuilderFunctions: List<String>? = null,
        val versionNameStrategy: String? = null,
        val versionCodeStrategy: String? = null,
    )

    data class Changelog(
        val issueNumberPattern: String = "TICKET-\\\\d+",
        val issueUrlPrefix: String = "https://jira.example.com/browse/",
        val commitMessageKey: String = "CHANGELOG",
        val excludeMessageKey: Boolean = true,
        val versionNameStrategy: String? = null,
        val versionCodeStrategy: String? = null,
    )
}

data class ClickUpConfig(
    val auth: Auth,
    val automation: Automation?,
) {
    data class Auth(
        val apiTokenFilePath: String,
    )

    data class Automation(
        val workspaceName: String,
        val fixVersionPattern: String?,
        val fixVersionFieldName: String?,
        val tagPattern: String?,
    )
}

data class ConfluenceConfig(
    val auth: Auth,
    val distribution: Distribution?,
) {
    data class Auth(
        val baseUrl: String,
        val username: String,
        val password: String,
    )

    data class Distribution(
        val compressed: Boolean = false,
        val pageId: String,
    )
}

data class FirebaseConfig(
    val distributionCommon: Distribution,
    val distributionBuildType: Pair<String, Distribution>? = null,
) {
    data class Distribution(
        val serviceCredentialsFilePath: String,
        val artifactType: String,
        val appId: String,
        val testerGroups: List<String>?,
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
    val changelog: Changelog?,
    val distribution: Distribution?,
) {
    data class Bot(
        val webhookUrl: String,
        val uploadApiTokenFilePath: String?,
        val iconUrl: String,
    )

    data class Changelog(
        val userMentions: List<String>,
        val attachmentColor: String,
    )

    data class Distribution(
        val destinationChannels: List<String>,
    )
}

data class DefaultConfig(
    val applicationId: String = "com.example.build.types.android",
    val versionCode: Int? = 1,
    val versionName: String? = "1.0",
    val minSdk: Int = 26,
    val targetSdk: Int = 36,
)

data class TelegramConfig(
    val bots: Bots,
    val lookup: Lookup? = null,
    val changelog: Changelog?,
    val distribution: Distribution?,
) {
    data class Lookup(
        val botName: String,
        val chatName: String,
        val topicName: String?
    )

    data class Bots(
        val bots: List<Bot>,
    )

    data class Changelog(
        val userMentions: List<String>,
        val destinationBots: List<DestinationBot>,
    )

    data class Distribution(
        val compressed: Boolean = false,
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
        val chatId: String?,
        val topicId: String?,
    )
}
