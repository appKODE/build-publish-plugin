package ru.kode.android.build.publish.plugin.foundation.utils

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

internal fun File.createAndroidProject(
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
                google()
                mavenCentral()
            }
        }
        rootProject.name = "My Application"
        include ':app'
        """.trimIndent()
    writeFile(topSettingsFile, topSettingsFileContent)
    val buildTypesBlock = buildTypes
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
    val flavorDimensionsBlock = productFlavors
        .mapTo(mutableSetOf()) { it.dimension }
        .joinToString { "\"$it\"" }
        .takeIf { it.isNotEmpty() }
        ?.let {
            "flavorDimensions += [$it]"
        }
        .orEmpty()
    val productFlavorsBlock = productFlavors
        .joinToString(separator = "\n") {
            """
                create("${it.name}") {
                    dimension = "${it.dimension}"
                }
            """
        }.let {
            """
            productFlavors {
            $it
            }
            """
        }

    val defaultConfigBlock = defaultConfig?.let { config ->
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
    val buildTypeOutputBlock = foundationConfig.buildTypeOutput?.let { (name, config) ->
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
            }
        }
    """
    val appCenterConfigBlock = appCenterConfig?.let { config ->
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
                    ${config.distribution.maxUploadStatusRequestCount?.let { "maxUploadStatusRequestCount.set(${it})" }}
                    ${config.distribution.uploadStatusRequestDelayMs?.let { "uploadStatusRequestDelayMs.set(${it})" }}
                    ${config.distribution.uploadStatusRequestDelayCoefficient?.let { "uploadStatusRequestDelayCoefficient.set(${it})" }}
                }
            }
        }
    """.trimIndent()
    }.orEmpty()

    val clickUpConfigBlock = clickUpConfig?.let { config ->
        """
        buildPublishClickUp {
            auth {
                common {
                    apiTokenFile.set(File("${config.auth.apiTokenFilePath}"))
                }
            }
            automation {
                common {
                    ${config.automation.fixVersionPattern?.let { """fixVersionPattern.set("$it")""" }}
                    ${config.automation.fixVersionFieldId?.let { """fixVersionFieldId.set("$it")""" }}
                    ${config.automation.tagName?.let { """tagName.set("$it")""" }}
                }
            }
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
        
        $appCenterConfigBlock
        
        $clickUpConfigBlock
        """.trimIndent()
            .removeEmptyLines()
            .also {
                println("--- BUILD.GRADLE START ---")
                println(it)
                println("--- BUILD.GRADLE END ---")
            }
    writeFile(appBuildFile, appBuildFileContent)
    val androidManifestFileContent = """
        <?xml version="1.0" encoding="utf-8"?>
        <manifest />
    """.trimIndent()
    writeFile(androidManifestFile, androidManifestFileContent)
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

internal fun File.printFilesRecursively(prefix: String = "") {
    println("--- FILES START ---")
    printFilesRecursivelyInternal(
        prefix,
        filterFile = {
            val ext = it.extension
            ext.contains("apk") || ext.contains("json") || ext.contains("aab")
        },
        filterDirectory = { it.endsWith("build") || it.path.contains("outputs") }
    )
    println("--- FILES END ---")
}

private fun File.printFilesRecursivelyInternal(
    prefix: String,
    filterFile: (File) -> Boolean,
    filterDirectory: (File) -> Boolean
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

internal fun File.getFile(path: String): File {
    val file = File(this, path)
    file.parentFile.mkdirs()
    return file
}

internal fun File.runTask(task: String): BuildResult {
    return GradleRunner.create()
        .withProjectDir(this)
        .withArguments(task)
        .withPluginClasspath()
        .forwardOutput()
        .build()
}

internal fun File.runTaskWithFail(task: String): BuildResult {
    return GradleRunner.create()
        .withProjectDir(this)
        .withArguments(task)
        .withPluginClasspath()
        .forwardOutput()
        .buildAndFail()
}

internal data class BuildType(
    val name: String
)

internal data class ProductFlavor(
    val name: String,
    val dimension: String
)

internal data class FoundationConfig(
    val output: Output = Output(),
    val buildTypeOutput: Pair<String, Output>? = null,
    val changelog: Changelog = Changelog(),
) {
    data class Output(
        val baseFileName: String = "test-app",
        val useVersionsFromTag: Boolean? = null,
        val useStubsForTagAsFallback: Boolean? = null,
        val useDefaultsForVersionsAsFallback: Boolean? = null,
        val buildTagPatternBuilderFunctions: List<String>? = null
    )

    data class Changelog(
        val issueNumberPattern: String = "TICKET-\\\\d+",
        val issueUrlPrefix: String = "https://jira.example.com/browse/",
        val commitMessageKey: String = "[CHANGELOG]",
    )
}

internal data class AppCenterConfig(
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
        val uploadStatusRequestDelayCoefficient: Int?
    )
}

internal data class ClickUpConfig(
    val auth: Auth,
    val automation: Automation,
) {
    data class Auth(
        val apiTokenFilePath: String,
    )

    data class Automation(
        val fixVersionPattern: String?,
        val fixVersionFieldId: String?,
        val tagName: String?
    )
}

internal data class ConfluenceConfig(
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

internal data class FirebaseConfig(
    val distribution: Distribution,
) {
    data class Distribution(
        val serviceCredentialsFilePath: String,
        val artifactType: String,
        val appId: String,
        val testerGroups: List<String>,
    )
}

internal data class JiraConfig(
    val auth: Auth,
    val automation: Automation,
) {
    data class Auth(
        val baseUrl: String,
        val username: String,
        val password: String,
    )

    data class Automation(
        val projectId: String,
        val labelPattern: String?,
        val fixVersionPattern: String?,
        val resolvedStatusTransitionId: String?
    )
}

internal data class PlayConfig(
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

internal data class SlackConfig(
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

internal data class DefaultConfig(
    val applicationId: String = "com.example.build.types.android",
    val versionCode: Int? = 1,
    val versionName: String? = "1.0",
    val minSdk: Int = 31,
    val targetSdk: Int = 34,
)

internal data class TelegramConfig(
    val bots: Bots,
    val changelog: Changelog,
    val distribution: Distribution,
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
        val botId: String,
        val botServerBaseUrl: String?,
        val username: String?,
        val password: String?,
        val chats: List<Chat>
    )

    data class DestinationBot(
        val botName: String,
        val chatNames: String
    )

    data class Chat(
        val chatId: String,
        val topicId: String?,
    )
}

private fun String.removeEmptyLines(): String {
    return this.lines()
        .filter { it.trim().isNotEmpty() }
        .joinToString("\n")
}
