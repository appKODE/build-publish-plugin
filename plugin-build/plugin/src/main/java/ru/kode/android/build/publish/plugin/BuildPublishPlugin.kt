@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.impl.VariantOutputImpl
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.tasks.PackageAndroidArtifact
import com.android.builder.model.Version.ANDROID_GRADLE_PLUGIN_VERSION
import com.google.firebase.appdistribution.gradle.AppDistributionExtension
import com.google.firebase.appdistribution.gradle.AppDistributionPlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.internal.VersionNumber
import ru.kode.android.build.publish.plugin.enity.BuildVariant
import ru.kode.android.build.publish.plugin.extension.BuildPublishExtension
import ru.kode.android.build.publish.plugin.extension.EXTENSION_NAME
import ru.kode.android.build.publish.plugin.extension.config.AppCenterDistributionConfig
import ru.kode.android.build.publish.plugin.extension.config.ChangelogConfig
import ru.kode.android.build.publish.plugin.extension.config.FirebaseAppDistributionConfig
import ru.kode.android.build.publish.plugin.extension.config.OutputConfig
import ru.kode.android.build.publish.plugin.extension.config.SlackConfig
import ru.kode.android.build.publish.plugin.extension.config.TelegramConfig
import ru.kode.android.build.publish.plugin.git.mapper.fromJson
import ru.kode.android.build.publish.plugin.task.GenerateChangelogTask
import ru.kode.android.build.publish.plugin.task.GetLastTagTask
import ru.kode.android.build.publish.plugin.task.PrintLastIncreasedTag
import ru.kode.android.build.publish.plugin.task.SendSlackChangelogTask
import ru.kode.android.build.publish.plugin.task.SendTelegramChangelogTask
import ru.kode.android.build.publish.plugin.task.appcenter.AppCenterDistributionTask
import ru.kode.android.build.publish.plugin.util.capitalizedName
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal const val SEND_SLACK_CHANGELOG_TASK_PREFIX = "sendSlackChangelog"
internal const val SEND_TELEGRAM_CHANGELOG_TASK_PREFIX = "sendTelegramChangelog"
internal const val GENERATE_CHANGELOG_TASK_PREFIX = "generateChangelog"
internal const val PRINT_LAST_INCREASED_TAG_TASK_PREFIX = "printLastIncreasedTag"
internal const val GET_LAST_TAG_TASK_PREFIX = "getLastTag"
internal const val DEFAULT_BUILD_VERSION = "v0.0.1"
internal const val CHANGELOG_FILENAME = "changelog.txt"
internal const val APP_CENTER_DISTRIBUTION_UPLOAD_TASK_PREFIX = "appCenterDistributionUpload"

internal object AgpVersions {
    val CURRENT: VersionNumber = VersionNumber.parse(ANDROID_GRADLE_PLUGIN_VERSION).baseVersion
    val VERSION_7_0_4: VersionNumber = VersionNumber.parse("7.0.4")
}

@Suppress("LongMethod") // TODO Split into small methods
abstract class BuildPublishPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.stopExecutionIfNotSupported()

        val buildPublishExtension = project.extensions
            .create(EXTENSION_NAME, BuildPublishExtension::class.java)
        val androidExtension = project.extensions
            .getByType(ApplicationAndroidComponentsExtension::class.java)
        val changelogFile = project.layout.buildDirectory.file(CHANGELOG_FILENAME)
        androidExtension.onVariants(
            callback = { variant ->
                val output = variant.outputs
                    .find { it is VariantOutputImpl && it.fullName == variant.name }
                    as? VariantOutputImpl
                if (output != null) {
                    val buildVariant = BuildVariant(variant.name, variant.flavorName, variant.buildType)
                    val outputFileName = output.outputFileName.get()
                    val outputProviders = project.registerVariantTasks(
                        buildPublishExtension,
                        buildVariant,
                        changelogFile,
                        outputFileName
                    )
                    output.versionCode.set(outputProviders.versionCode)
                    output.outputFileName.set(outputProviders.outputFileName)
                    output.versionName.set(outputProviders.versionName)
                }
            }
        )
        androidExtension.finalizeDsl {
            project.configurePlugins(buildPublishExtension, changelogFile.get().asFile)
        }
    }

    private fun Project.registerVariantTasks(
        buildPublishExtension: BuildPublishExtension,
        buildVariant: BuildVariant,
        changelogFile: Provider<RegularFile>,
        outputFileName: String,
    ): OutputProviders {
        val outputConfig = buildPublishExtension.output.getByName("default")
        val tagBuildProvider = registerGetLastTagTask(buildVariant)
        val versionCodeProvider = tagBuildProvider.map { tagBuildFile ->
            val file = tagBuildFile.asFile
            if (file.exists()) {
                fromJson(file).buildNumber
            } else {
                1
            }
        }
        val fileNameProvider = outputConfig.baseFileName.zip(tagBuildProvider) { baseFileName, tagBuildFile ->
            val file = tagBuildFile.asFile
            val formattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy"))
            if (file.exists() && outputFileName.endsWith(".apk")) {
                val tagBuild = fromJson(file)
                val versionName = tagBuild.buildVariant
                val versionCode = tagBuild.buildNumber
                "$baseFileName-$versionName-vc$versionCode-$formattedDate.apk"
            } else if (!file.exists() && outputFileName.endsWith(".apk")) {
                "$baseFileName-$formattedDate.apk"
            } else "$baseFileName.${outputFileName.split(".").last()}"
        }
        val versionNameProvider = tagBuildProvider.map { tagBuildFile ->
            val file = tagBuildFile.asFile
            if (file.exists()) {
                fromJson(tagBuildFile.asFile).name
            } else {
                "$DEFAULT_BUILD_VERSION-${buildVariant.name}"
            }
        }
        tasks.registerPrintLastIncreasedTagTask(
            buildVariant,
            tagBuildProvider
        )
        val changelogConfig = with(buildPublishExtension.changelog) {
            findByName(buildVariant.name) ?: findByName("default")
        }
        if (changelogConfig != null) {
            val generateChangelogFileProvider = tasks.registerGenerateChangelogTask(
                changelogConfig,
                buildVariant,
                changelogFile,
                tagBuildProvider
            )
            val telegramConfig = with(buildPublishExtension.telegram) {
                findByName(buildVariant.name) ?: findByName("default")
            }
            if (telegramConfig != null) {
                tasks.registerSendTelegramChangelogTask(
                    outputConfig,
                    changelogConfig,
                    telegramConfig,
                    buildVariant,
                    generateChangelogFileProvider,
                    tagBuildProvider
                )
            }
            val slackConfig = with(buildPublishExtension.slack) {
                findByName(buildVariant.name) ?: findByName("default")
            }
            if (slackConfig != null) {
                tasks.registerSendSlackChangelogTask(
                    outputConfig,
                    changelogConfig,
                    slackConfig,
                    buildVariant,
                    generateChangelogFileProvider,
                    tagBuildProvider
                )
            }
            val appCenterDistributionConfig = with(buildPublishExtension.appCenterDistribution) {
                findByName(buildVariant.name) ?: findByName("default")
            }
            if (appCenterDistributionConfig != null) {
                val outputFileProvider = fileNameProvider.flatMap { fileName ->
                    project.tasks.withType(PackageAndroidArtifact::class.java)
                        .firstOrNull { it.variantName == buildVariant.name }
                        ?.outputDirectory
                        ?.map { directory -> directory.file(fileName) }
                        ?: throw GradleException("no output for variant ${buildVariant.name}")
                }
                tasks.registerAppCenterDistributionTask(
                    appCenterDistributionConfig,
                    buildVariant,
                    generateChangelogFileProvider,
                    outputFileProvider,
                    tagBuildProvider,
                )
            }
        }
        return OutputProviders(
            versionName = versionNameProvider,
            versionCode = versionCodeProvider,
            outputFileName = fileNameProvider
        )
    }

    private fun Project.registerGetLastTagTask(
        buildVariant: BuildVariant,
    ): Provider<RegularFile> {
        val tagBuildFile = project.layout.buildDirectory
            .file("tag-build-${buildVariant.name}.json")
        return tasks.register(
            "$GET_LAST_TAG_TASK_PREFIX${buildVariant.capitalizedName()}",
            GetLastTagTask::class.java
        ) { task ->
            task.tagBuildFile.set(tagBuildFile)
            task.buildVariant.set(buildVariant.name)
        }.flatMap { it.tagBuildFile }
    }

    private fun TaskContainer.registerPrintLastIncreasedTagTask(
        buildVariant: BuildVariant,
        tagBuildProvider: Provider<RegularFile>
    ) {
        register(
            "$PRINT_LAST_INCREASED_TAG_TASK_PREFIX${buildVariant.capitalizedName()}",
            PrintLastIncreasedTag::class.java
        ) { task ->
            task.tagBuildFile.set(tagBuildProvider)
        }
    }

    private fun TaskContainer.registerGenerateChangelogTask(
        changelogConfig: ChangelogConfig,
        buildVariant: BuildVariant,
        changelogFile: Provider<RegularFile>,
        tagBuildProvider: Provider<RegularFile>
    ): Provider<RegularFile> {
        return register(
            "$GENERATE_CHANGELOG_TASK_PREFIX${buildVariant.capitalizedName()}",
            GenerateChangelogTask::class.java
        ) {
            it.commitMessageKey.set(changelogConfig.commitMessageKey)
            it.buildVariant.set(buildVariant.name)
            it.changelogFile.set(changelogFile)
            it.tagBuildFile.set(tagBuildProvider)
        }.flatMap { it.changelogFile }
    }

    @Suppress("LongParameterList") // TODO Get parameters inside
    private fun TaskContainer.registerSendTelegramChangelogTask(
        outputConfig: OutputConfig,
        changelogConfig: ChangelogConfig,
        telegramConfig: TelegramConfig,
        buildVariant: BuildVariant,
        changelogFileProvider: Provider<RegularFile>,
        tagBuildProvider: Provider<RegularFile>
    ) {
        register(
            "$SEND_TELEGRAM_CHANGELOG_TASK_PREFIX${buildVariant.capitalizedName()}",
            SendTelegramChangelogTask::class.java
        ) {
            it.changelogFile.set(changelogFileProvider)
            it.tagBuildFile.set(tagBuildProvider)
            it.issueUrlPrefix.set(changelogConfig.issueUrlPrefix)
            it.issueNumberPattern.set(changelogConfig.issueNumberPattern)
            it.baseOutputFileName.set(outputConfig.baseFileName)
            it.webhookUrl.set(telegramConfig.webhookUrl)
            it.botId.set(telegramConfig.botId)
            it.chatId.set(telegramConfig.chatId)
            it.userMentions.set(telegramConfig.userMentions)
        }
    }

    @Suppress("LongParameterList") // TODO Get parameters inside
    private fun TaskContainer.registerSendSlackChangelogTask(
        outputConfig: OutputConfig,
        changelogConfig: ChangelogConfig,
        slackConfig: SlackConfig,
        buildVariant: BuildVariant,
        changelogFileProvider: Provider<RegularFile>,
        tagBuildProvider: Provider<RegularFile>
    ) {
        register(
            "$SEND_SLACK_CHANGELOG_TASK_PREFIX${buildVariant.capitalizedName()}",
            SendSlackChangelogTask::class.java
        ) {
            it.changelogFile.set(changelogFileProvider)
            it.tagBuildFile.set(tagBuildProvider)
            it.issueUrlPrefix.set(changelogConfig.issueUrlPrefix)
            it.issueNumberPattern.set(changelogConfig.issueNumberPattern)
            it.baseOutputFileName.set(outputConfig.baseFileName)
            it.webhookUrl.set(slackConfig.webhookUrl)
            it.iconUrl.set(slackConfig.iconUrl)
            it.userMentions.set(slackConfig.userMentions)
        }
    }

    private fun TaskContainer.registerAppCenterDistributionTask(
        config: AppCenterDistributionConfig,
        buildVariant: BuildVariant,
        changelogFileProvider: Provider<RegularFile>,
        buildVariantOutputFileProvider: Provider<RegularFile>,
        tagBuildProvider: Provider<RegularFile>,
    ): TaskProvider<AppCenterDistributionTask> {
        return register(
            "$APP_CENTER_DISTRIBUTION_UPLOAD_TASK_PREFIX${buildVariant.capitalizedName()}",
            AppCenterDistributionTask::class.java,
        ) {
            it.tagBuildFile.set(tagBuildProvider)
            it.buildVariantOutputFile.set(buildVariantOutputFileProvider)
            it.changelogFile.set(changelogFileProvider)
            it.apiTokenFile.set(config.apiTokenFile)
            it.ownerName.set(config.ownerName)
            it.appNamePrefix.set(config.appNamePrefix)
            it.testerGroups.set(config.testerGroups)
            it.maxRequestCount.set(config.maxRequestCount)
            it.requestDelayMs.set(config.requestDelayMs)
        }
    }

    private fun Project.configurePlugins(
        buildPublishExtension: BuildPublishExtension,
        changelogFile: File,
    ) {
        val firebaseAppDistributionConfig = buildPublishExtension.firebaseDistribution.findByName("default")
        plugins.all { plugin ->
            when (plugin) {
                is AppPlugin -> {
                    val appExtension = extensions.getByType(AppExtension::class.java)
                    appExtension.configure()
                }
                is AppDistributionPlugin -> if (firebaseAppDistributionConfig != null) {
                    val appDistributionExtension = extensions
                        .getByType(AppDistributionExtension::class.java)
                    appDistributionExtension.configure(
                        config = firebaseAppDistributionConfig,
                        changelogFile = changelogFile,
                    )
                }
            }
        }
    }
}

@Suppress("ThrowsCount") // block to throws exceptions on apply
private fun Project.stopExecutionIfNotSupported() {
    if (AgpVersions.CURRENT < AgpVersions.VERSION_7_0_4) {
        throw StopExecutionException(
            "Must only be used with with Android Gradle Plugin >= 7.4 "
        )
    }
    if (!plugins.hasPlugin(AppPlugin::class.java)) {
        throw StopExecutionException(
            "Must only be used with Android application projects." +
                " Please apply the 'com.android.application' plugin."
        )
    }

    if (!plugins.hasPlugin(AppDistributionPlugin::class.java)) {
        throw StopExecutionException(
            "Must only be used with Firebase App Distribution." +
                " Please apply the 'com.google.firebase.appdistribution' plugin."
        )
    }
}

private fun AppExtension.configure() {
    defaultConfig {
        it.versionCode = 1
        it.versionName = "$DEFAULT_BUILD_VERSION-dev"
    }
}

private fun AppDistributionExtension.configure(
    config: FirebaseAppDistributionConfig,
    changelogFile: File,
) {
    val serviceCredentialsFilePath = config
        .serviceCredentialsFilePath.orNull
        ?.takeIf { it.isNotBlank() }
    val applicationId = config
        .appId.orNull
        ?.takeIf { it.isNotBlank() }
    val testerGroups = config.testerGroups.get()
    val artifactType = config.artifactType.orNull ?: "APK"

    if (applicationId != null) {
        appId = applicationId
    }
    serviceCredentialsFile = serviceCredentialsFilePath.orEmpty()
    releaseNotesFile = changelogFile.path
    this.artifactType = artifactType
    this.groups = testerGroups.joinToString(",")
}

private data class OutputProviders(
    val versionName: Provider<String>,
    val versionCode: Provider<Int>,
    val outputFileName: Provider<String>,
)
