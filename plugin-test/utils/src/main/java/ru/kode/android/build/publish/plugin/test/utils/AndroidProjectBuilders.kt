package ru.kode.android.build.publish.plugin.test.utils

import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.PluginUnderTestMetadataReading
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

private val IS_CI get() = System.getenv("CI") == "true"

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

    val fullApplicationId = "\${fullApplicationId}"
    val authority = "\${authority}"
    val buildTypeBuilder = { type: BuildType ->
        val appId = type.appId?.let { appId -> "\"$appId\"" }
            ?: """android.defaultConfig.applicationId + ".${type.name}""""
        val suffix = type.applicationIdSuffix?.let { suffix -> "applicationIdSuffix \"$suffix\"" } ?: ""
        """
                ${type.name} {
                    def fullApplicationId = $appId
                    def authority = "$fullApplicationId.provider"
        
                    debuggable true
                    $suffix
                    buildConfigField "String", "FILE_PROVIDER_AUTHORITY", "\"$authority\""
                    manifestPlaceholders = [
                          APPLICATION_ID          : fullApplicationId,
                          FILE_PROVIDER_AUTHORITY : authority
                    ]
                }
        """.takeIf { configureApplicationVariants } ?: type.name
    }
    val buildTypesBlock =
        buildTypes
            .joinToString(separator = "\n") {
                """
                ${buildTypeBuilder(it)}
            """
            }
            .let { buildType ->
                """
            buildTypes {
            $buildType 
            }
            """
            }
    val flavorDimensionsBlock =
        productFlavors
            .takeIf { flavor -> flavor.isNotEmpty() }
            ?.mapTo(mutableSetOf()) { flavor -> flavor.dimension }
            ?.joinToString { dimension -> "\"$dimension\"" }
            ?.let { dimension ->
                "flavorDimensions += [$dimension]"
            }
            .orEmpty()
    val productFlavorsBlock =
        productFlavors
            .takeIf { flavor -> flavor.isNotEmpty() }
            ?.joinToString(separator = "\n") { flavor ->
                """
                create("${flavor.name}") {
                    dimension = "${flavor.dimension}"
                }
            """
            }?.let { flavor ->
                """
            productFlavors {
            $flavor
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
                
                ${config.versionCode?.let { vsCode -> "versionCode $vsCode" }.orEmpty()}
                ${config.versionName?.let { vsName -> "versionName \"$vsName\"" }.orEmpty()}
            }
        """
        }
    val buildTypeOutputBlock1 =
        foundationConfig.buildTypeOutput?.let { (name, config) ->
            val useVersionsFromTag = config.useVersionsFromTag?.let { use ->
                "useVersionsFromTag.set($use)"
            }.orEmpty()
            val useStabs = config.useStubsForTagAsFallback?.let { use ->
                "useStubsForTagAsFallback.set($use)"
            }.orEmpty()
            val useDefaults = config.useDefaultsForVersionsAsFallback?.let { use ->
                "useDefaultsForVersionsAsFallback.set($use)"
            }.orEmpty()
            val pattern = config.buildTagPatternBuilderFunctions?.let { pattern ->
                buildTagPatternBlock(pattern)
            }.orEmpty()
            val versionName = config.versionNameStrategy?.let { strategy ->
                """versionNameStrategy { $strategy }"""
            }.orEmpty()
            val versionTag = config.versionCodeStrategy?.let { strategy ->
                """versionCodeStrategy { $strategy }"""
            }.orEmpty()

            """
                buildVariant("$name") {
                    baseFileName.set("${config.baseFileName}")
                    $useVersionsFromTag
                    $useStabs
                    $useDefaults
                    $pattern
                    $versionName
                    $versionTag
                }
        """
        }
    val buildTypeOutputBlock2 =
        foundationConfig.buildTypeOutput2?.let { (name, config) ->
            val useVersionsFromTag = config.useVersionsFromTag?.let { use ->
                "useVersionsFromTag.set($use)"
            }.orEmpty()
            val useStubs = config.useStubsForTagAsFallback?.let { use ->
                "useStubsForTagAsFallback.set($use)"
            }.orEmpty()
            val yseDefaults = config.useDefaultsForVersionsAsFallback?.let { use ->
                "useDefaultsForVersionsAsFallback.set($use)"
            }.orEmpty()
            val pattern = config.buildTagPatternBuilderFunctions?.let { pattern ->
                buildTagPatternBlock(pattern)
            }.orEmpty()
            val versionName = config.versionNameStrategy?.let { strategy ->
                """versionNameStrategy { $strategy }"""
            }.orEmpty()
            val versionCode = config.versionCodeStrategy?.let { strategy ->
                """versionCodeStrategy { $strategy }"""
            }.orEmpty()
            """
                buildVariant("$name") {
                    baseFileName.set("${config.baseFileName}")
                    $useVersionsFromTag
                    $useStubs
                    $yseDefaults
                    $pattern
                    $versionName
                    $versionCode
                }
        """
        }
    val buildTypeOutputBlock3 =
        foundationConfig.buildTypeOutput3?.let { (name, config) ->
            val useVersions = config.useVersionsFromTag?.let { use ->
                "useVersionsFromTag.set($use)"
            }.orEmpty()
            val useStubs = config.useStubsForTagAsFallback?.let { use ->
                "useStubsForTagAsFallback.set($use)"
            }.orEmpty()
            val useDefaults = config.useDefaultsForVersionsAsFallback?.let { use ->
                "useDefaultsForVersionsAsFallback.set($use)"
            }.orEmpty()
            val pattern = config.buildTagPatternBuilderFunctions?.let { pattern ->
                buildTagPatternBlock(pattern)
            }.orEmpty()
            val versionName = config.versionNameStrategy?.let { strategy ->
                """versionNameStrategy { $strategy }"""
            }.orEmpty()
            val versionCode = config.versionCodeStrategy?.let { strategy ->
                """versionCodeStrategy { $strategy }"""
            }.orEmpty()
            """
                buildVariant("$name") {
                    baseFileName.set("${config.baseFileName}")
                    $useVersions
                    $useStubs
                    $useDefaults
                    $pattern
                    $versionName
                    $versionCode
                }
        """
        }
    val useVersions = foundationConfig.output.useVersionsFromTag?.let { use ->
        "useVersionsFromTag.set($use)"
    }.orEmpty()
    val useStubs = foundationConfig.output.useStubsForTagAsFallback?.let { use ->
        "useStubsForTagAsFallback.set($use)"
    }.orEmpty()
    val useDefaults = foundationConfig.output.useDefaultsForVersionsAsFallback?.let { use ->
        "useDefaultsForVersionsAsFallback.set($use)"
    }
    val pattern = foundationConfig.output.buildTagPatternBuilderFunctions?.let { pattern ->
        buildTagPatternBlock(pattern)
    }.orEmpty()
    val changelogStrategy = foundationConfig.changelog.changelogMessageStrategy?.let { strategy ->
        "changelogMessageStrategy { $strategy }"
    }.orEmpty()
    val foundationConfigBlock = """
        buildPublishFoundation {
            bodyLogging.set(${foundationConfig.bodyLogging})
            verboseLogging.set(${foundationConfig.verboseLogging})
            
            output {
                common {
                    baseFileName.set("${foundationConfig.output.baseFileName}")
                    $useVersions
                    $useStubs
                    ${useDefaults.orEmpty()}
                    $pattern
                }
        
                ${buildTypeOutputBlock1.orEmpty()}
                ${buildTypeOutputBlock2.orEmpty()}
                ${buildTypeOutputBlock3.orEmpty()}
            }
            
            changelogCommon {
                issueNumberPattern.set("${foundationConfig.changelog.issueNumberPattern}")
                issueUrlPrefix.set("${foundationConfig.changelog.issueUrlPrefix}")
                commitMessageKey.set("${foundationConfig.changelog.commitMessageKey}")
                $changelogStrategy
            }
        }
    """

    val clickUpConfigBlock =
        clickUpConfig?.let { config ->
            val automation = config.automation?.let { automation ->
                clickUpAutomationBlock(automation)
            }.orEmpty()
            """
        buildPublishClickUp {
            auth {
                common {
                    apiTokenFile = project.file("${config.auth.apiTokenFilePath}")
                }
            }
            
            $automation
        }
            """
        }.orEmpty()

    val confluenceConfigBlock =
        confluenceConfig?.let { config ->
            val distribution = config.distribution?.let { distribution ->
                confluenceDistributionBlock(distribution)
            }.orEmpty()
            """
        buildPublishConfluence {
            auth {
                common {
                    baseUrl.set("${config.auth.baseUrl}")
                    credentials.username.set("${config.auth.username}")
                    credentials.password.set("${config.auth.password}")
                }
            }
                
            $distribution
        }
            """
        }.orEmpty()

    val firebaseConfigBlock =
        firebaseConfig?.let { config ->
            val buildTypeFirebaseBlock =
                config.distributionBuildType?.let { (name, config) ->
                    val testerGroups = config.testerGroups?.let { testerGroups ->
                        """testerGroups(${testerGroups.joinToString { group -> "\"$group\"" }})"""
                    }.orEmpty()
                    """
                buildVariant("$name") {
                    serviceCredentialsFile = project.file("${config.serviceCredentialsFilePath}")
                    appId.set("${config.appId}")
                    artifactType.set(${config.artifactType})
                    $testerGroups
                }
                    """
                }
            val testerGroups = config.distributionCommon.testerGroups?.let { testerGroups ->
                """testerGroups(${testerGroups.joinToString { group -> "\"$group\"" }})"""
            }
            """
        buildPublishFirebase {
            distribution {
                common {
                    serviceCredentialsFile = project.file("${config.distributionCommon.serviceCredentialsFilePath}")
                    appId.set("${config.distributionCommon.appId}")
                    artifactType.set(${config.distributionCommon.artifactType})
                    ${testerGroups.orEmpty()}
                }
                ${buildTypeFirebaseBlock.orEmpty()}
            }
        }
            """
        }.orEmpty()

    val jiraConfigBlock =
        jiraConfig?.let { config ->
            val automation = config.automation?.let { automation ->
                jiraAutomationBlock(automation)
            }.orEmpty()
            """
        buildPublishJira {
            auth {
                common {
                    baseUrl.set("${config.auth.baseUrl}")
                    credentials.username.set("${config.auth.username}")
                    credentials.password.set("${config.auth.password}")
                }
            }
                
            $automation
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
            val uploadPath = config.bot.uploadApiTokenFilePath?.let { path ->
                """uploadApiTokenFile = project.file("$path")"""
            }.orEmpty()
            val changelog = config.changelog?.let { changelog ->
                slackChangelogBlock(changelog)
            }.orEmpty()
            val distribution = config.distribution?.let { distribution ->
                slackDistributionBlock(distribution)
            }.orEmpty()
            """
        buildPublishSlack {
            bot {
                common {
                    webhookUrl.set("${config.bot.webhookUrl}")
                    $uploadPath
                    iconUrl.set("${config.bot.iconUrl}")
                }
            }
            $changelog
            $distribution
        }
            """
        }.orEmpty()

    val telegramConfigBlock =
        telegramConfig?.let { config ->
            val bots = config.bots.bots.takeIf { bots -> bots.isNotEmpty() }?.let { bots -> telegramBotsBlock(bots) }.orEmpty()
            val lookup = config.lookup?.let { lookup -> telegramLookupBlock(lookup) }.orEmpty()
            val changelog = config.changelog?.let { changelog -> telegramChangelogBlock(changelog) }.orEmpty()
            val distribution = config.distribution?.let { distribution -> telegramDistributionBlock(distribution) }.orEmpty()
            """
            buildPublishTelegram {
                $bots
                $lookup
                $changelog
                $distribution
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
            buildFeatures.buildConfig = true
            
            compileSdk $compileSdk
        
            $defaultConfigBlock
            
            $buildTypesBlock
            
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
    val pattern = automation.fixVersionPattern?.let { pattern -> """fixVersionPattern.set("$pattern")""" }.orEmpty()
    val label = automation.labelPattern?.let { label -> """labelPattern.set("$label")""" }.orEmpty()
    val statusName = automation.targetStatusName?.let { statusName -> """targetStatusName.set("$statusName")""" }.orEmpty()
    return """
            automation {
                common {
                    projectKey.set("${automation.projectKey}")
                    $pattern
                    $label
                    $statusName
                }
            }
    """
}

private fun clickUpAutomationBlock(automation: ClickUpConfig.Automation): String {
    val workspaceName = automation.workspaceName.let { name -> """workspaceName.set("$name")""" }
    val fixVersionPattern = automation.fixVersionPattern?.let { pattern ->"""fixVersionPattern.set("$pattern")""" }.orEmpty()
    val fixVersionFieldName = automation.fixVersionFieldName?.let { name ->"""fixVersionFieldName.set("$name")""" }.orEmpty()
    val tagPattern = automation.tagPattern?.let { pattern -> """tagPattern.set("$pattern")""" }.orEmpty()
    return """
            automation {
                common {
                    $workspaceName
                    $fixVersionPattern
                    $fixVersionFieldName
                    $tagPattern
                }
            }
    """
}

private fun confluenceDistributionBlock(distribution: ConfluenceConfig.Distribution): String {
    return """
           distribution {
                common {
                    compressed.set(${distribution.compressed})
                    pageId.set("${distribution.pageId}")
                }
           }
    """
}

private fun telegramChangelogBlock(changelog: TelegramConfig.Changelog): String {
    val userMentions = changelog.userMentions.joinToString { mention -> "\"$mention\"" }
    val destinationBots = changelog.destinationBots.joinToString(separator = "\n") { bot -> telegramDestinationBotBlock(bot) }
    return """
                        changelog {
                            common {
                                userMentions($userMentions)
                                
                                $destinationBots
                            }
                        }
    """
}

private fun telegramLookupBlock(changelog: TelegramConfig.Lookup): String {
    val topicName = changelog.topicName?.let { name -> "topicName.set(\"${name}\")" }.orEmpty()
    return """
                        lookup {
                            botName.set("${changelog.botName}")
                            chatName.set("${changelog.chatName}")
                            $topicName
                        }
    """
}

private fun slackChangelogBlock(changelog: SlackConfig.Changelog): String {
    val userMentions = changelog.userMentions.joinToString { userMention -> "\"$userMention\"" }
    return """
            changelog {
                common {
                    userMentions($userMentions)
                    attachmentColor.set("${changelog.attachmentColor}")
                }
            }
    """
}

private fun telegramBotsBlock(bots: List<TelegramConfig.Bot>): String {
    val bots = bots.joinToString("\n") { bot -> telegramBotBlock(bot) }
    return """
                        bots {
                            common {
                                $bots
                            }
                        }
    """
}

private fun telegramDistributionBlock(distribution: TelegramConfig.Distribution): String {
    val bots = distribution.destinationBots.joinToString("\n") { bot -> telegramDestinationBotBlock(bot) }
    return """
                        distribution {
                           common {
                               compressed.set(${distribution.compressed})
                               $bots
                           } 
                        }
    """
}

private fun slackDistributionBlock(distribution: SlackConfig.Distribution): String {
    val channels = distribution.destinationChannels.joinToString { channel -> "\"$channel\"" }
    return """
            distribution {
               common {
                   destinationChannels($channels)
               } 
            }
    """
}

private fun telegramBotBlock(bot: TelegramConfig.Bot): String {
    val baseUrk = bot.botServerBaseUrl?.let { url -> """botServerBaseUrl.set("$url")""" }.orEmpty()
    val userName = bot.botServerUsername?.let { username -> """botServerAuth.username.set("$username")""" }.orEmpty()
    val userPassword = bot.botServerPassword?.let { password -> """botServerAuth.password.set("$password")""" }.orEmpty()
    val chat = bot.chats.joinToString(separator = "\n") { chat -> telegramBotChatBlock(chat) }
    return """     
                                bot("${bot.botName}") {
                                    botId.set("${bot.botId}")
                                    $baseUrk
                                    $userName
                                    $userPassword
                                    
                                    $chat
                                }
    """
}

private fun telegramBotChatBlock(chat: TelegramConfig.Chat): String {
    val chatId = chat.chatId?.let { id -> "chatId = \"$id\"" }.orEmpty()
    val topicId = chat.topicId?.let { id -> "topicId = \"$id\"" }.orEmpty()
    return """
                                    chat("${chat.chatName}") {
                                        $chatId
                                        $topicId
                                    }
    """
}

private fun telegramDestinationBotBlock(destinationBot: TelegramConfig.DestinationBot): String {
    val chatNames = destinationBot.chatNames.joinToString { name -> "\"$name\"" }
    return """
                                destinationBot {
                                    botName = "${destinationBot.botName}"
                                    chatNames($chatNames)
                                }
    """
}

private fun buildTagPatternBlock(items: List<String>): String {
    val pattern = items.joinToString(separator = "\n") { item -> "                      $item" }
    return """
                    buildTagPattern {
$pattern
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
        .filter { line -> line.trim().isNotEmpty() }
        .joinToString("\n")
}

fun File.printFilesRecursively(prefix: String = "") {
    println("--- FILES START ---")
    printFilesRecursivelyInternal(
        prefix,
        filterFile = { file ->
            val ext = file.extension
            ext.contains("apk") || ext.contains("json") || ext.contains("aab") || ext.contains("txt")
        },
        filterDirectory = { directory -> directory.endsWith("build") || directory.path.contains("outputs") || directory.path.contains("renamed") || directory.path.contains("intermediates") },
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
        if (IS_CI) add("--info")
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
        if (IS_CI) add("--info")
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
    val args = mutableListOf(task, "--stacktrace").apply {
        if (IS_CI) add("--info")
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
    val appId: String? = null,
    val applicationIdSuffix: String? = ".${name}",
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
        val changelogMessageStrategy: String? = null,
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
