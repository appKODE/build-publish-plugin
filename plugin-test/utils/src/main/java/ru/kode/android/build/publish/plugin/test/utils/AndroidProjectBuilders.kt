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

/**
 * System properties that carry real credentials into the test JVM (wired per module via
 * `systemProperty(name, getEnvOrProperty(name))`). Their literal values must never be rendered into a
 * generated build script — otherwise they end up in the captured build output that CI uploads as
 * artifacts. See [redactScriptSecrets] / [withSecretEnvironment].
 */
private val SENSITIVE_SYSTEM_PROPERTIES =
    listOf(
        "JIRA_USER_PASSWORD",
        "CLICKUP_TOKEN",
        "SLACK_WEBHOOK_URL",
        "SLACK_UPLOAD_API_TOKEN",
        "TELEGRAM_BOT_ID",
        "TELEGRAM_BOT_SERVER_USERNAME",
        "TELEGRAM_BOT_SERVER_PASSWORD",
        "CONFLUENCE_USER_PASSWORD",
        "NEXTCLOUD_USER_PASSWORD",
        "PROXY_USER",
        "PROXY_PASSWORD",
    )

// Guards against corrupting a script by substituting a short, ambiguous value (e.g. a 2-char user name).
private const val MIN_REDACTABLE_SECRET_LENGTH = 8

/** name -> value for every sensitive system property that is set and long enough to redact safely. */
private fun redactableSecrets(): List<Pair<String, String>> =
    SENSITIVE_SYSTEM_PROPERTIES.mapNotNull { name ->
        System.getProperty(name)?.takeIf { it.length >= MIN_REDACTABLE_SECRET_LENGTH }?.let { name to it }
    }

/**
 * Replaces any literal secret rendered into a generated build script with a `${System.getenv('NAME')}`
 * reference, so secrets never appear in the script on disk or in the captured/printed build output. The
 * daemon resolves the reference from the environment forwarded by the runners (see [withSecretEnvironment]).
 */
internal fun String.redactScriptSecrets(): String =
    redactableSecrets()
        .sortedByDescending { it.second.length }
        .fold(this) { acc, (name, value) -> acc.replace(value, "\${System.getenv('$name')}") }

/** Adds the sensitive secret values to a daemon environment map so redacted `System.getenv` references resolve. */
private fun MutableMap<String, String>.withSecretEnvironment(): MutableMap<String, String> {
    redactableSecrets().forEach { (name, value) -> put(name, value) }
    return this
}

@Suppress("LongMethod", "CyclomaticComplexMethod", "CascadingCallWrapping", "LongParameterList")
fun File.createAndroidProject(
    compileSdk: Int = 36,
    buildTypes: List<BuildType>,
    productFlavors: List<ProductFlavor> = listOf(),
    defaultConfig: DefaultConfig? = DefaultConfig(),
    foundationConfig: FoundationConfig = FoundationConfig(),
    clickUpConfig: ClickUpConfig? = null,
    confluenceConfig: ConfluenceConfig? = null,
    nextcloudConfig: NextcloudConfig? = null,
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
        val safeTopBuildFileContent = topBuildFileContent.redactScriptSecrets()
        println("--- ${topBuildFile.path} START ---")
        println(safeTopBuildFileContent)
        println("--- ${topBuildFile.path} END ---")
        topBuildFile.writeText(safeTopBuildFileContent)
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
        val appId =
            type.appId?.let { appId -> "\"$appId\"" }
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
        """.takeIf {
            configureApplicationVariants
        } ?: type.name
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
            val useVersionsFromTag =
                config.useVersionsFromTag?.let { use ->
                    "useVersionsFromTag.set($use)"
                }.orEmpty()
            val useStabs =
                config.useStubsForTagAsFallback?.let { use ->
                    "useStubsForTagAsFallback.set($use)"
                }.orEmpty()
            val useDefaults =
                config.useDefaultsForVersionsAsFallback?.let { use ->
                    "useDefaultsForVersionsAsFallback.set($use)"
                }.orEmpty()
            val pattern =
                config.buildTagPatternBuilderFunctions?.let { pattern ->
                    buildTagPatternBlock(pattern)
                }.orEmpty()
            val versionName =
                config.versionNameStrategy?.let { strategy ->
                    """versionNameStrategy { $strategy }"""
                }.orEmpty()
            val versionTag =
                config.versionCodeStrategy?.let { strategy ->
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
            val useVersionsFromTag =
                config.useVersionsFromTag?.let { use ->
                    "useVersionsFromTag.set($use)"
                }.orEmpty()
            val useStubs =
                config.useStubsForTagAsFallback?.let { use ->
                    "useStubsForTagAsFallback.set($use)"
                }.orEmpty()
            val yseDefaults =
                config.useDefaultsForVersionsAsFallback?.let { use ->
                    "useDefaultsForVersionsAsFallback.set($use)"
                }.orEmpty()
            val pattern =
                config.buildTagPatternBuilderFunctions?.let { pattern ->
                    buildTagPatternBlock(pattern)
                }.orEmpty()
            val versionName =
                config.versionNameStrategy?.let { strategy ->
                    """versionNameStrategy { $strategy }"""
                }.orEmpty()
            val versionCode =
                config.versionCodeStrategy?.let { strategy ->
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
            val useVersions =
                config.useVersionsFromTag?.let { use ->
                    "useVersionsFromTag.set($use)"
                }.orEmpty()
            val useStubs =
                config.useStubsForTagAsFallback?.let { use ->
                    "useStubsForTagAsFallback.set($use)"
                }.orEmpty()
            val useDefaults =
                config.useDefaultsForVersionsAsFallback?.let { use ->
                    "useDefaultsForVersionsAsFallback.set($use)"
                }.orEmpty()
            val pattern =
                config.buildTagPatternBuilderFunctions?.let { pattern ->
                    buildTagPatternBlock(pattern)
                }.orEmpty()
            val versionName =
                config.versionNameStrategy?.let { strategy ->
                    """versionNameStrategy { $strategy }"""
                }.orEmpty()
            val versionCode =
                config.versionCodeStrategy?.let { strategy ->
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
    val useVersions =
        foundationConfig.output.useVersionsFromTag?.let { use ->
            "useVersionsFromTag.set($use)"
        }.orEmpty()
    val useStubs =
        foundationConfig.output.useStubsForTagAsFallback?.let { use ->
            "useStubsForTagAsFallback.set($use)"
        }.orEmpty()
    val useDefaults =
        foundationConfig.output.useDefaultsForVersionsAsFallback?.let { use ->
            "useDefaultsForVersionsAsFallback.set($use)"
        }
    val pattern =
        foundationConfig.output.buildTagPatternBuilderFunctions?.let { pattern ->
            buildTagPatternBlock(pattern)
        }.orEmpty()
    val commonVersionName =
        foundationConfig.output.versionNameStrategy?.let { strategy ->
            """versionNameStrategy { $strategy }"""
        }.orEmpty()
    val commonVersionCode =
        foundationConfig.output.versionCodeStrategy?.let { strategy ->
            """versionCodeStrategy { $strategy }"""
        }.orEmpty()
    val changelogStrategy =
        foundationConfig.changelog.changelogMessageStrategy?.let { strategy ->
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
                    $commonVersionName
                    $commonVersionCode
                }
        
                ${buildTypeOutputBlock1.orEmpty()}
                ${buildTypeOutputBlock2.orEmpty()}
                ${buildTypeOutputBlock3.orEmpty()}
            }
            
            changelogCommon {
                ${changelogIssueSourcesBlock(foundationConfig.changelog)}
                commitMessageKey.set("${foundationConfig.changelog.commitMessageKey}")
                $changelogStrategy
            }
        }
    """

    val clickUpConfigBlock =
        clickUpConfig?.let { config ->
            val automation =
                config.automation?.let { automation ->
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
            val distribution =
                config.distribution?.let { distribution ->
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

    val nextcloudConfigBlock =
        nextcloudConfig?.let { config ->
            val auth =
                config.auth?.let { auth ->
                    """
            auth {
                common {
                    baseUrl.set("${auth.baseUrl}")
                    credentials.username.set("${auth.username}")
                    credentials.password.set("${auth.password}")
                }
            }
                    """
                }.orEmpty()
            val distribution =
                config.distribution?.let { distribution ->
                    nextcloudDistributionBlock(distribution)
                }.orEmpty()
            """
        buildPublishNextcloud {
            $auth

            $distribution
        }
            """
        }.orEmpty()

    val firebaseConfigBlock =
        firebaseConfig?.let { config ->
            val buildTypeFirebaseBlock =
                config.distributionBuildType?.let { (name, config) ->
                    val testerGroups =
                        config.testerGroups?.let { testerGroups ->
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
            val testerGroups =
                config.distributionCommon.testerGroups?.let { testerGroups ->
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
            val automation =
                config.automation?.let { automation ->
                    jiraAutomationBlock(automation)
                }.orEmpty()
            val secondaryInstanceBlocks =
                config.secondaryAuth.entries.joinToString("\n") { (name, auth) ->
                    """
                    instance("$name") {
                        baseUrl.set("${auth.baseUrl}")
                        credentials.username.set("${auth.username}")
                        credentials.password.set("${auth.password}")
                    }
                    """.trimIndent()
                }
            """
        buildPublishJira {
            auth {
                common {
                    instance("default") {
                        baseUrl.set("${config.auth.baseUrl}")
                        credentials.username.set("${config.auth.username}")
                        credentials.password.set("${config.auth.password}")
                    }
                    $secondaryInstanceBlocks
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
            val uploadPath =
                config.bot.uploadApiTokenFilePath?.let { path ->
                    """uploadApiTokenFile = project.file("$path")"""
                }.orEmpty()
            val changelog =
                config.changelog?.let { changelog ->
                    slackChangelogBlock(changelog)
                }.orEmpty()
            val distribution =
                config.distribution?.let { distribution ->
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
            val bots =
                config.bots.bots.takeIf { it.isNotEmpty() }?.let { telegramBotsBlock(it) }.orEmpty()
            val lookup = config.lookup?.let { lookup -> telegramLookupBlock(lookup) }.orEmpty()
            val changelog = config.changelog?.let { changelog -> telegramChangelogBlock(changelog) }.orEmpty()
            val distribution =
                config.distribution?.let { telegramDistributionBlock(it) }.orEmpty()
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
            ${nextcloudConfig?.let { """id 'ru.kode.android.build-publish-novo.nextcloud'""" }.orEmpty()}
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

        $nextcloudConfigBlock

        $firebaseConfigBlock
                
        $playConfigBlock
        
        $slackConfigBlock
        
        $telegramConfigBlock
                
        """.trimIndent()
            .removeEmptyLines()
            .redactScriptSecrets()
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

private fun changelogIssueSourcesBlock(changelog: FoundationConfig.Changelog): String {
    val sources =
        changelog.issueSources.ifEmpty {
            listOf(
                FoundationConfig.Changelog.IssueSource(
                    name = "primary",
                    numberPattern = changelog.issueNumberPattern,
                    urlPrefix = changelog.issueUrlPrefix,
                ),
            )
        }
    val entries =
        sources.joinToString("\n") { source ->
            val url = source.urlPrefix?.let { prefix -> """urlPrefix.set("$prefix")""" }.orEmpty()
            """
            issueSource("${source.name}") {
                numberPattern.set("${source.numberPattern}")
                $url
            }
            """.trimIndent()
        }
    // A single source uses the issueSource(...) shorthand; multiple sources use the issueSources { } block.
    return if (sources.size == 1) {
        entries
    } else {
        """
        issueSources {
            $entries
        }
        """.trimIndent()
    }
}

private fun jiraAutomationBlock(automation: JiraConfig.Automation): String {
    val entries = automation.projects.joinToString("\n") { jiraProjectBlock(it) }
    return """
            automation {
                common {
                    projects {
                        $entries
                    }
                }
            }
    """
}

private fun jiraProjectBlock(project: JiraConfig.Project): String {
    val instanceName = project.instanceName?.let { name -> """instanceName.set("$name")""" }.orEmpty()
    val label = project.labelPattern?.let { pattern -> """labelPattern.set("$pattern")""" }.orEmpty()
    val fixVersion = project.fixVersionPattern?.let { pattern -> """fixVersionPattern.set("$pattern")""" }.orEmpty()
    val statusName = project.targetStatusName?.let { name -> """targetStatusName.set("$name")""" }.orEmpty()
    return """
        project("${project.name}") {
            projectKey.set("${project.projectKey}")
            $instanceName
            $label
            $fixVersion
            $statusName
        }
        """.trimIndent()
}

private fun clickUpAutomationBlock(automation: ClickUpConfig.Automation): String {
    val workspaceName = automation.workspaceName.let { name -> """workspaceName.set("$name")""" }
    val fixVersionPattern =
        automation.fixVersionPattern?.let { """fixVersionPattern.set("$it")""" }.orEmpty()
    val fixVersionFieldName =
        automation.fixVersionFieldName?.let { """fixVersionFieldName.set("$it")""" }.orEmpty()
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

private fun nextcloudDistributionBlock(distribution: NextcloudConfig.Distribution): String {
    val shareMode =
        distribution.shareMode?.let { mode ->
            "shareMode.set(ru.kode.android.build.publish.plugin.nextcloud.config.NextcloudShareMode.$mode)"
        }.orEmpty()
    val userRecipients =
        if (distribution.userRecipients.isNotEmpty()) {
            "userRecipients(${distribution.userRecipients.joinToString { value -> "\"$value\"" }})"
        } else {
            ""
        }
    val groupRecipients =
        if (distribution.groupRecipients.isNotEmpty()) {
            "groupRecipients(${distribution.groupRecipients.joinToString { value -> "\"$value\"" }})"
        } else {
            ""
        }
    val remoteFileName = distribution.remoteFileName?.let { value -> """remoteFileName.set("$value")""" }.orEmpty()
    return """
           distribution {
                common {
                    compressed.set(${distribution.compressed})
                    remotePath.set("${distribution.remotePath}")
                    $shareMode
                    $userRecipients
                    $groupRecipients
                    $remoteFileName
                }
           }
    """
}

private fun telegramChangelogBlock(changelog: TelegramConfig.Changelog): String {
    val userMentions = changelog.userMentions.joinToString { mention -> "\"$mention\"" }
    val destinationBots =
        changelog.destinationBots.joinToString(separator = "\n") { telegramDestinationBotBlock(it) }
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
    val userPassword = bot.botServerPassword?.let { """botServerAuth.password.set("$it")""" }.orEmpty()
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
        filterDirectory = { directory ->
            directory.endsWith("build") ||
                directory.path.contains("outputs") ||
                directory.path.contains("renamed") ||
                directory.path.contains("intermediates")
        },
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
    taskArguments: Map<String, String> = emptyMap(),
    agpClasspath: List<File> = emptyList(),
    gradleVersion: String = "9.6.1",
    gradleJvmArgs: List<String> = emptyList(),
    environment: Map<String, String> = emptyMap(),
): BuildResult {
    val args =
        mutableListOf(task).apply {
            if (!IS_CI) add("--info")
            add("--stacktrace")
            taskArguments.forEach { (key, value) ->
                add("-D$key=$value")
            }
        }
    val env =
        System.getenv().toMutableMap().apply {
            if (gradleJvmArgs.isNotEmpty()) {
                this["GRADLE_OPTS"] = gradleJvmArgs.joinToString(" ")
            }
            putAll(environment)
        }.withSecretEnvironment()
    return GradleRunner.create()
        .withProjectDir(this)
        .withArguments(args)
        .withEnvironment(env)
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
    taskArguments: Map<String, String> = emptyMap(),
    agpClasspath: List<File> = emptyList(),
    gradleVersion: String = "9.6.1",
    gradleJvmArgs: List<String> = emptyList(),
): BuildResult {
    val args =
        tasks.toMutableList().apply {
            if (!IS_CI) add("--info")
            add("--stacktrace")
            taskArguments.forEach { (key, value) ->
                add("-D$key=$value")
            }
        }
    val env =
        System.getenv().toMutableMap().apply {
            if (gradleJvmArgs.isNotEmpty()) {
                this["GRADLE_OPTS"] = gradleJvmArgs.joinToString(" ")
            }
        }.withSecretEnvironment()
    return GradleRunner.create()
        .withProjectDir(this)
        .withArguments(args)
        .withEnvironment(env)
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
    taskArguments: Map<String, String> = emptyMap(),
    agpClasspath: List<File> = emptyList(),
    gradleVersion: String = "9.6.1",
    gradleJvmArgs: List<String> = emptyList(),
): BuildResult {
    val args =
        mutableListOf(task, "--stacktrace").apply {
            if (!IS_CI) add("--info")
            taskArguments.forEach { (key, value) ->
                add("-D$key=$value")
            }
        }
    val env =
        System.getenv().toMutableMap().apply {
            if (gradleJvmArgs.isNotEmpty()) {
                this["GRADLE_OPTS"] = gradleJvmArgs.joinToString(" ")
            }
        }.withSecretEnvironment()
    return GradleRunner.create()
        .withProjectDir(this)
        .withArguments(args)
        .withEnvironment(env)
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
    val filteredClasspath =
        pluginClasspath.filter { file ->
            val name = file.name.lowercase()
            !name.startsWith("gradle") ||
                !name.contains("android") ||
                !name.contains("agp")
        }
    println("Filtered ${filteredClasspath.size} classpath items, adding ${agpClassPath.size} AGP JARs")
    return filteredClasspath + agpClassPath
}

fun resolveRequiredAgpJars(agpVersion: String): List<File> {
    val project =
        ProjectBuilder.builder()
            .withName("temp-resolver")
            .build()

    project.buildscript.repositories.apply {
        google()
        mavenCentral()
    }

    val pluginClasspath =
        project.buildscript.configurations.getByName("classpath").apply {
            dependencies.clear()
            dependencies.add(project.dependencies.create("com.android.tools.build:gradle:$agpVersion"))
            dependencies.add(
                project.dependencies.create(
                    "com.android.application:com.android.application.gradle.plugin:$agpVersion",
                ),
            )
        }.resolve()

    return pluginClasspath.toList()
}

fun File.createNonAndroidProject(
    pluginId: String,
    pluginConfigBlock: String = "",
): File {
    val settingsFile = this.getFile("settings.gradle")
    val buildFile = this.getFile("build.gradle")

    val settingsContent =
        """
        pluginManagement {
            repositories {
                mavenLocal()
                gradlePluginPortal()
                mavenCentral()
                google()
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
        rootProject.name = "test-non-android"
        """.trimIndent()

    val buildContent =
        """
        plugins {
            id '$pluginId'
        }
        $pluginConfigBlock
        """.trimIndent()

    settingsFile.writeText(settingsContent)
    buildFile.writeText(buildContent)
    return this
}

data class BuildType(
    val name: String,
    val appId: String? = null,
    val applicationIdSuffix: String? = ".$name",
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
        /**
         * Explicit changelog issue sources. When empty, a single "primary" source is rendered from
         * [issueNumberPattern] / [issueUrlPrefix] (backward-compatible with single-source tests).
         */
        val issueSources: List<IssueSource> = emptyList(),
    ) {
        data class IssueSource(
            val name: String,
            val numberPattern: String,
            val urlPrefix: String? = null,
        )
    }
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

data class NextcloudConfig(
    val auth: Auth?,
    val distribution: Distribution?,
) {
    data class Auth(
        val baseUrl: String,
        val username: String,
        val password: String,
    )

    data class Distribution(
        val compressed: Boolean = false,
        val remotePath: String,
        val shareMode: String? = null,
        val userRecipients: List<String> = emptyList(),
        val groupRecipients: List<String> = emptyList(),
        val remoteFileName: String? = null,
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
    val secondaryAuth: Map<String, Auth> = emptyMap(),
) {
    data class Auth(
        val baseUrl: String,
        val username: String,
        val password: String,
    )

    data class Automation(
        val projects: List<Project> = emptyList(),
    ) {
        companion object {
            /**
             * Test-only convenience for the common single-project case: wraps one [Project] named
             * "default". Keeps automation test call sites concise without reintroducing any
             * production single-project API — the generated DSL always uses `projects { project(...) }`.
             */
            fun singleProject(
                projectKey: String,
                instanceName: String? = null,
                labelPattern: String? = null,
                fixVersionPattern: String? = null,
                targetStatusName: String? = null,
            ) = Automation(
                projects =
                    listOf(
                        Project(
                            name = "default",
                            projectKey = projectKey,
                            instanceName = instanceName,
                            labelPattern = labelPattern,
                            fixVersionPattern = fixVersionPattern,
                            targetStatusName = targetStatusName,
                        ),
                    ),
            )
        }
    }

    data class Project(
        val name: String,
        val projectKey: String,
        val instanceName: String? = null,
        val labelPattern: String? = null,
        val fixVersionPattern: String? = null,
        val targetStatusName: String? = null,
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
        val topicName: String?,
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
