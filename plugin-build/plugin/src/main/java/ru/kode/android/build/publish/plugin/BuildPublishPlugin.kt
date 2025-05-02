@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.impl.VariantOutputImpl
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.tasks.PackageAndroidArtifact
import com.android.builder.model.Version.ANDROID_GRADLE_PLUGIN_VERSION
import com.google.firebase.appdistribution.gradle.AppDistributionExtension
import com.google.firebase.appdistribution.gradle.AppDistributionPlugin
import org.ajoberstar.grgit.gradle.GrgitService
import org.ajoberstar.grgit.gradle.GrgitServiceExtension
import org.ajoberstar.grgit.gradle.GrgitServicePlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.internal.VersionNumber
import ru.kode.android.build.publish.plugin.enity.BuildVariant
import ru.kode.android.build.publish.plugin.enity.mapper.fromJson
import ru.kode.android.build.publish.plugin.extension.BuildPublishExtension
import ru.kode.android.build.publish.plugin.extension.EXTENSION_NAME
import ru.kode.android.build.publish.plugin.extension.config.AppCenterDistributionConfig
import ru.kode.android.build.publish.plugin.extension.config.ChangelogConfig
import ru.kode.android.build.publish.plugin.extension.config.ClickUpConfig
import ru.kode.android.build.publish.plugin.extension.config.ConfluenceConfig
import ru.kode.android.build.publish.plugin.extension.config.FirebaseAppDistributionConfig
import ru.kode.android.build.publish.plugin.extension.config.JiraConfig
import ru.kode.android.build.publish.plugin.extension.config.OutputConfig
import ru.kode.android.build.publish.plugin.extension.config.PlayConfig
import ru.kode.android.build.publish.plugin.extension.config.SlackConfig
import ru.kode.android.build.publish.plugin.extension.config.TelegramConfig
import ru.kode.android.build.publish.plugin.task.appcenter.AppCenterDistributionTask
import ru.kode.android.build.publish.plugin.task.changelog.GenerateChangelogTask
import ru.kode.android.build.publish.plugin.task.clickup.ClickUpAutomationTask
import ru.kode.android.build.publish.plugin.task.confluence.ConfluenceDistributionTask
import ru.kode.android.build.publish.plugin.task.jira.JiraAutomationTask
import ru.kode.android.build.publish.plugin.task.play.PlayDistributionTask
import ru.kode.android.build.publish.plugin.task.slack.changelog.SendSlackChangelogTask
import ru.kode.android.build.publish.plugin.task.slack.distribution.SlackDistributionTask
import ru.kode.android.build.publish.plugin.task.tag.GetLastTagTask
import ru.kode.android.build.publish.plugin.task.tag.PrintLastIncreasedTag
import ru.kode.android.build.publish.plugin.task.telegram.changelog.SendTelegramChangelogTask
import ru.kode.android.build.publish.plugin.task.telegram.distribution.TelegramDistributionTask
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
internal const val DEFAULT_VERSION_NAME = "$DEFAULT_BUILD_VERSION-dev"
internal const val DEFAULT_VERSION_CODE = 1
internal const val DEFAULT_BASE_FILE_NAME = "dev-build"
internal const val CHANGELOG_FILENAME = "changelog.txt"
internal const val APP_CENTER_DISTRIBUTION_UPLOAD_TASK_PREFIX = "appCenterDistributionUpload"
internal const val PLAY_DISTRIBUTION_UPLOAD_TASK_PREFIX = "playUpload"
internal const val SLACK_DISTRIBUTION_UPLOAD_TASK_PREFIX = "slackDistributionUpload"
internal const val TELEGRAM_DISTRIBUTION_UPLOAD_TASK_PREFIX = "telegramDistributionUpload"
internal const val CONFLUENCE_DISTRIBUTION_UPLOAD_TASK_PREFIX = "confluenceDistributionUpload"
internal const val JIRA_AUTOMATION_TASK = "jiraAutomation"
internal const val CLICK_UP_AUTOMATION_TASK = "clickUpAutomation"
internal const val DEFAULT_CONTAINER_NAME = "default"

internal object AgpVersions {
    val CURRENT: VersionNumber = VersionNumber.parse(ANDROID_GRADLE_PLUGIN_VERSION).baseVersion
    val VERSION_7_0_4: VersionNumber = VersionNumber.parse("7.0.4")
}

abstract class BuildPublishPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.stopExecutionIfNotSupported()
        project.pluginManager.apply(GrgitServicePlugin::class.java)

        val buildPublishExtension =
            project.extensions
                .create(EXTENSION_NAME, BuildPublishExtension::class.java)
        val androidExtension =
            project.extensions
                .getByType(ApplicationAndroidComponentsExtension::class.java)
        val changelogFile = project.layout.buildDirectory.file(CHANGELOG_FILENAME)
        val grgitService =
            project.extensions
                .getByType(GrgitServiceExtension::class.java)
                .service

        androidExtension.onVariants(
            callback = { variant ->
                val output =
                    variant.outputs
                        .find { it is VariantOutputImpl && it.fullName == variant.name }
                        as? VariantOutputImpl
                if (output != null) {
                    val buildVariant = BuildVariant(variant.name, variant.flavorName, variant.buildType)
                    val apkOutputFileName = output.outputFileName.get()

                    val bundleFile = variant.artifacts.get(SingleArtifact.BUNDLE)
                    val outputProviders =
                        project.registerVariantTasks(
                            buildPublishExtension,
                            buildVariant,
                            changelogFile,
                            apkOutputFileName,
                            bundleFile,
                            grgitService,
                        )
                    if (outputProviders.versionCode != null) {
                        output.versionCode.set(outputProviders.versionCode)
                    }
                    if (outputProviders.versionName != null) {
                        output.versionName.set(outputProviders.versionName)
                    }
                    output.outputFileName.set(outputProviders.apkOutputFileName)
                }
            },
        )
        androidExtension.finalizeDsl {
            val firebaseAppDistributionConfig =
                buildPublishExtension
                    .firebaseDistribution
                    // NOTE: NamedDomainObjectContainer can be resolved only in task on after finalizeDsl,
                    // because it can be defined after plugin application
                    .findByName(DEFAULT_CONTAINER_NAME)

            if (firebaseAppDistributionConfig != null) {
                project.pluginManager.apply(AppDistributionPlugin::class.java)
            }
            val outputConfig =
                with(buildPublishExtension.output) {
                    getByName(DEFAULT_CONTAINER_NAME)
                }
            project.configurePlugins(
                useDefaultVersionsAsFallback =
                    outputConfig
                        .useDefaultVersionsAsFallback
                        .getOrElse(true),
                firebaseAppDistributionConfig = firebaseAppDistributionConfig,
                changelogFile = changelogFile.get().asFile,
            )
        }
    }

    @Suppress("ComplexMethod", "LongMethod") // split to multiple methods
    private fun Project.registerVariantTasks(
        buildPublishExtension: BuildPublishExtension,
        buildVariant: BuildVariant,
        changelogFile: Provider<RegularFile>,
        apkOutputFileName: String,
        bundleFile: Provider<RegularFile>,
        grgitService: Provider<GrgitService>,
    ): OutputProviders {
        val outputConfig =
            with(buildPublishExtension.output) {
                findByName(buildVariant.name) ?: getByName(DEFAULT_CONTAINER_NAME)
            }
        val tagBuildProvider =
            registerGetLastTagTask(
                buildVariant,
                outputConfig.buildTagPattern,
                grgitService,
            )
        val useVersionsFromTag = outputConfig.useVersionsFromTag.getOrElse(true)
        val useDefaultVersionsAsFallback =
            outputConfig.useDefaultVersionsAsFallback.getOrElse(true)
        val versionCodeProvider =
            when {
                useVersionsFromTag -> tagBuildProvider.map(::mapToVersionCode)
                useDefaultVersionsAsFallback -> project.provider { DEFAULT_VERSION_CODE }
                else -> null
            }
        val apkOutputFileNameProvider =
            if (useVersionsFromTag) {
                outputConfig.baseFileName.zip(tagBuildProvider) { baseFileName, tagBuildFile ->
                    mapToOutputApkFileName(tagBuildFile, apkOutputFileName, baseFileName)
                }
            } else {
                outputConfig.baseFileName.map { baseFileName ->
                    createDefaultOutputFileName(baseFileName, apkOutputFileName)
                }
            }
        val versionNameProvider =
            when {
                useVersionsFromTag -> {
                    tagBuildProvider.map { tagBuildFile ->
                        mapToVersionName(tagBuildFile, buildVariant)
                    }
                }
                useDefaultVersionsAsFallback -> project.provider { DEFAULT_VERSION_NAME }
                else -> null
            }
        val apkOutputFileProvider =
            apkOutputFileNameProvider.flatMap { fileName ->
                mapToOutputApkFile(buildVariant, fileName)
            }
        tasks.registerPrintLastIncreasedTagTask(
            buildVariant,
            tagBuildProvider,
        )
        val changelogConfig =
            with(buildPublishExtension.changelog) {
                findByName(buildVariant.name) ?: findByName(DEFAULT_CONTAINER_NAME)
            }
        if (changelogConfig != null) {
            val generateChangelogFileProvider =
                tasks.registerGenerateChangelogTask(
                    changelogConfig.commitMessageKey,
                    outputConfig.buildTagPattern,
                    buildVariant,
                    changelogFile,
                    tagBuildProvider,
                    grgitService,
                )
            val telegramConfig =
                with(buildPublishExtension.telegram) {
                    findByName(buildVariant.name) ?: findByName(DEFAULT_CONTAINER_NAME)
                }
            if (telegramConfig != null) {
                tasks.registerTelegramTasks(
                    outputConfig,
                    changelogConfig,
                    telegramConfig,
                    buildVariant,
                    generateChangelogFileProvider,
                    tagBuildProvider,
                    apkOutputFileProvider,
                )
            }
            val confluenceConfig =
                with(buildPublishExtension.confluence) {
                    findByName(buildVariant.name) ?: findByName(DEFAULT_CONTAINER_NAME)
                }
            if (confluenceConfig != null) {
                tasks.registerConfluenceUploadTask(
                    config = confluenceConfig,
                    buildVariant = buildVariant,
                    apkOutputFileProvider = apkOutputFileProvider,
                )
            }
            val slackConfig =
                with(buildPublishExtension.slack) {
                    findByName(buildVariant.name) ?: findByName(DEFAULT_CONTAINER_NAME)
                }
            if (slackConfig != null) {
                tasks.registerSlackTasks(
                    outputConfig,
                    changelogConfig,
                    slackConfig,
                    buildVariant,
                    generateChangelogFileProvider,
                    tagBuildProvider,
                    apkOutputFileProvider,
                )
            }
            val appCenterDistributionConfig =
                with(buildPublishExtension.appCenterDistribution) {
                    findByName(buildVariant.name) ?: findByName(DEFAULT_CONTAINER_NAME)
                }
            if (appCenterDistributionConfig != null) {
                val params =
                    AppCenterDistributionTaskParams(
                        config = appCenterDistributionConfig,
                        buildVariant = buildVariant,
                        changelogFileProvider = generateChangelogFileProvider,
                        apkOutputFileProvider = apkOutputFileProvider,
                        tagBuildProvider = tagBuildProvider,
                        outputConfig = outputConfig,
                    )
                tasks.registerAppCenterDistributionTask(params)
            }
            val playConfig =
                with(buildPublishExtension.play) {
                    findByName(buildVariant.name) ?: findByName(DEFAULT_CONTAINER_NAME)
                }
            if (playConfig != null) {
                val params =
                    PlayTaskParams(
                        config = playConfig,
                        buildVariant = buildVariant,
                        bundleOutputFileProvider = bundleFile,
                        tagBuildProvider = tagBuildProvider,
                        outputConfig = outputConfig,
                    )
                tasks.registerPlayTask(params)
            }
            val jiraConfig =
                with(buildPublishExtension.jira) {
                    findByName(buildVariant.name) ?: findByName(DEFAULT_CONTAINER_NAME)
                }
            if (jiraConfig != null) {
                tasks.registerJiraTasks(
                    jiraConfig,
                    buildVariant,
                    changelogConfig.issueNumberPattern,
                    changelogFile,
                    tagBuildProvider,
                )
            }

            val clickUpConfig =
                with(buildPublishExtension.clickUp) {
                    findByName(buildVariant.name) ?: findByName(DEFAULT_CONTAINER_NAME)
                }
            if (clickUpConfig != null) {
                tasks.registerClickUpTasks(
                    clickUpConfig,
                    buildVariant,
                    changelogConfig.issueNumberPattern,
                    changelogFile,
                    tagBuildProvider,
                )
            }
        }
        return OutputProviders(
            versionName = versionNameProvider,
            versionCode = versionCodeProvider,
            apkOutputFileName = apkOutputFileNameProvider,
        )
    }

    private fun TaskContainer.registerJiraTasks(
        config: JiraConfig,
        buildVariant: BuildVariant,
        issueNumberPattern: Provider<String>,
        changelogFileProvider: Provider<RegularFile>,
        tagBuildProvider: Provider<RegularFile>,
    ) {
        if (
            config.labelPattern.isPresent ||
            config.fixVersionPattern.isPresent ||
            config.resolvedStatusTransitionId.isPresent
        ) {
            register(
                "$JIRA_AUTOMATION_TASK${buildVariant.capitalizedName()}",
                JiraAutomationTask::class.java,
            ) {
                it.tagBuildFile.set(tagBuildProvider)
                it.changelogFile.set(changelogFileProvider)
                it.issueNumberPattern.set(issueNumberPattern)
                it.baseUrl.set(config.baseUrl)
                it.username.set(config.authUsername)
                it.projectId.set(config.projectId)
                it.password.set(config.authPassword)
                it.labelPattern.set(config.labelPattern)
                it.fixVersionPattern.set(config.fixVersionPattern)
                it.resolvedStatusTransitionId.set(config.resolvedStatusTransitionId)
            }
        }
    }

    private fun TaskContainer.registerClickUpTasks(
        config: ClickUpConfig,
        buildVariant: BuildVariant,
        issueNumberPattern: Provider<String>,
        changelogFileProvider: Provider<RegularFile>,
        tagBuildProvider: Provider<RegularFile>,
    ) {
        val fixVersionIsPresent =
            config.fixVersionPattern.isPresent && config.fixVersionFieldId.isPresent
        val hasMissingFixVersionProperties =
            config.fixVersionPattern.isPresent || config.fixVersionFieldId.isPresent

        if (!fixVersionIsPresent && hasMissingFixVersionProperties) {
            throw GradleException(
                "To use the fixVersion logic, the fixVersionPattern or fixVersionFieldId " +
                    "properties must be specified",
            )
        }

        if (fixVersionIsPresent || config.tagName.isPresent) {
            register(
                "$CLICK_UP_AUTOMATION_TASK${buildVariant.capitalizedName()}",
                ClickUpAutomationTask::class.java,
            ) {
                it.tagBuildFile.set(tagBuildProvider)
                it.changelogFile.set(changelogFileProvider)
                it.issueNumberPattern.set(issueNumberPattern)
                it.apiTokenFile.set(config.apiTokenFile)
                it.fixVersionPattern.set(config.fixVersionPattern)
                it.fixVersionFieldId.set(config.fixVersionFieldId)
                it.taskTag.set(config.tagName)
            }
        }
    }

    @Suppress("LongParameterList") // TODO Get parameters inside
    private fun TaskContainer.registerSlackTasks(
        outputConfig: OutputConfig,
        changelogConfig: ChangelogConfig,
        slackConfig: SlackConfig,
        buildVariant: BuildVariant,
        generateChangelogFileProvider: Provider<RegularFile>,
        tagBuildProvider: Provider<RegularFile>,
        apkOutputFileProvider: Provider<RegularFile>,
    ) {
        registerSendSlackChangelogTask(
            outputConfig,
            changelogConfig,
            slackConfig,
            buildVariant,
            generateChangelogFileProvider,
            tagBuildProvider,
        )
        if (
            slackConfig.uploadApiTokenFile.isPresent &&
            slackConfig.uploadChannels.isPresent
        ) {
            registerSlackUploadTask(
                outputConfig,
                slackConfig.uploadApiTokenFile,
                slackConfig.uploadChannels,
                buildVariant,
                apkOutputFileProvider,
                tagBuildProvider,
            )
        }
    }

    private fun TaskContainer.registerSlackUploadTask(
        outputConfig: OutputConfig,
        apiTokenFile: RegularFileProperty,
        channels: SetProperty<String>,
        buildVariant: BuildVariant,
        apkOutputFileProvider: Provider<RegularFile>,
        tagBuildProvider: Provider<RegularFile>,
    ) {
        register(
            "$SLACK_DISTRIBUTION_UPLOAD_TASK_PREFIX${buildVariant.capitalizedName()}",
            SlackDistributionTask::class.java,
        ) {
            it.buildVariantOutputFile.set(apkOutputFileProvider)
            it.apiTokenFile.set(apiTokenFile)
            it.channels.set(channels)
            it.tagBuildFile.set(tagBuildProvider)
            it.baseOutputFileName.set(outputConfig.baseFileName)
        }
    }

    private fun Project.registerGetLastTagTask(
        buildVariant: BuildVariant,
        buildTagPattern: Provider<String>,
        grgitService: Provider<GrgitService>,
    ): Provider<RegularFile> {
        val tagBuildFile =
            project.layout.buildDirectory
                .file("tag-build-${buildVariant.name}.json")
        return tasks.register(
            "$GET_LAST_TAG_TASK_PREFIX${buildVariant.capitalizedName()}",
            GetLastTagTask::class.java,
        ) { task ->
            task.tagBuildFile.set(tagBuildFile)
            task.buildVariant.set(buildVariant.name)
            task.buildTagPattern.set(buildTagPattern)
            task.getGrgitService().set(grgitService)
        }.flatMap { it.tagBuildFile }
    }

    private fun TaskContainer.registerPrintLastIncreasedTagTask(
        buildVariant: BuildVariant,
        tagBuildProvider: Provider<RegularFile>,
    ) {
        register(
            "$PRINT_LAST_INCREASED_TAG_TASK_PREFIX${buildVariant.capitalizedName()}",
            PrintLastIncreasedTag::class.java,
        ) { task ->
            task.tagBuildFile.set(tagBuildProvider)
        }
    }

    private fun TaskContainer.registerGenerateChangelogTask(
        commitMessageKey: Provider<String>,
        buildTagPattern: Provider<String>,
        buildVariant: BuildVariant,
        changelogFile: Provider<RegularFile>,
        tagBuildProvider: Provider<RegularFile>,
        grgitService: Provider<GrgitService>,
    ): Provider<RegularFile> {
        return register(
            "$GENERATE_CHANGELOG_TASK_PREFIX${buildVariant.capitalizedName()}",
            GenerateChangelogTask::class.java,
        ) {
            it.commitMessageKey.set(commitMessageKey)
            it.buildTagPattern.set(buildTagPattern)
            it.changelogFile.set(changelogFile)
            it.tagBuildFile.set(tagBuildProvider)
            it.getGrgitService().set(grgitService)
        }.flatMap { it.changelogFile }
    }

    @Suppress("LongParameterList") // TODO Get parameters inside
    private fun TaskContainer.registerTelegramTasks(
        outputConfig: OutputConfig,
        changelogConfig: ChangelogConfig,
        telegramConfig: TelegramConfig,
        buildVariant: BuildVariant,
        changelogFileProvider: Provider<RegularFile>,
        tagBuildProvider: Provider<RegularFile>,
        apkOutputFileProvider: Provider<RegularFile>,
    ) {
        register(
            "$SEND_TELEGRAM_CHANGELOG_TASK_PREFIX${buildVariant.capitalizedName()}",
            SendTelegramChangelogTask::class.java,
        ) {
            it.changelogFile.set(changelogFileProvider)
            it.tagBuildFile.set(tagBuildProvider)
            it.issueUrlPrefix.set(changelogConfig.issueUrlPrefix)
            it.issueNumberPattern.set(changelogConfig.issueNumberPattern)
            it.baseOutputFileName.set(outputConfig.baseFileName)
            it.botId.set(telegramConfig.botId)
            it.chatId.set(telegramConfig.chatId)
            it.topicId.set(telegramConfig.topicId)
            it.userMentions.set(telegramConfig.userMentions)
        }
        if (telegramConfig.uploadBuild.orNull == true) {
            registerTelegramUploadTask(
                telegramConfig.botId,
                telegramConfig.chatId,
                telegramConfig.topicId,
                buildVariant,
                apkOutputFileProvider,
            )
        }
    }

    private fun TaskContainer.registerTelegramUploadTask(
        botId: Provider<String>,
        chatId: Provider<String>,
        topicId: Provider<String>,
        buildVariant: BuildVariant,
        apkOutputFileProvider: Provider<RegularFile>,
    ) {
        register(
            "$TELEGRAM_DISTRIBUTION_UPLOAD_TASK_PREFIX${buildVariant.capitalizedName()}",
            TelegramDistributionTask::class.java,
        ) {
            it.buildVariantOutputFile.set(apkOutputFileProvider)
            it.botId.set(botId)
            it.chatId.set(chatId)
            it.topicId.set(topicId)
        }
    }

    private fun TaskContainer.registerConfluenceUploadTask(
        config: ConfluenceConfig,
        buildVariant: BuildVariant,
        apkOutputFileProvider: Provider<RegularFile>,
    ) {
        register(
            "$CONFLUENCE_DISTRIBUTION_UPLOAD_TASK_PREFIX${buildVariant.capitalizedName()}",
            ConfluenceDistributionTask::class.java,
        ) {
            it.buildVariantOutputFile.set(apkOutputFileProvider)
            it.username.set(config.username)
            it.password.set(config.password)
            it.pageId.set(config.pageId)
        }
    }

    @Suppress("LongParameterList") // TODO Get parameters inside
    private fun TaskContainer.registerSendSlackChangelogTask(
        outputConfig: OutputConfig,
        changelogConfig: ChangelogConfig,
        slackConfig: SlackConfig,
        buildVariant: BuildVariant,
        changelogFileProvider: Provider<RegularFile>,
        tagBuildProvider: Provider<RegularFile>,
    ) {
        register(
            "$SEND_SLACK_CHANGELOG_TASK_PREFIX${buildVariant.capitalizedName()}",
            SendSlackChangelogTask::class.java,
        ) {
            it.changelogFile.set(changelogFileProvider)
            it.tagBuildFile.set(tagBuildProvider)
            it.issueUrlPrefix.set(changelogConfig.issueUrlPrefix)
            it.issueNumberPattern.set(changelogConfig.issueNumberPattern)
            it.baseOutputFileName.set(outputConfig.baseFileName)
            it.webhookUrl.set(slackConfig.webhookUrl)
            it.iconUrl.set(slackConfig.iconUrl)
            it.userMentions.set(slackConfig.userMentions)
            it.attachmentColor.set(slackConfig.attachmentColor)
        }
    }

    private fun TaskContainer.registerAppCenterDistributionTask(
        params: AppCenterDistributionTaskParams,
    ): TaskProvider<AppCenterDistributionTask> {
        val buildVariant = params.buildVariant
        val config = params.config

        return register(
            "$APP_CENTER_DISTRIBUTION_UPLOAD_TASK_PREFIX${buildVariant.capitalizedName()}",
            AppCenterDistributionTask::class.java,
        ) {
            it.tagBuildFile.set(params.tagBuildProvider)
            it.buildVariantOutputFile.set(params.apkOutputFileProvider)
            it.changelogFile.set(params.changelogFileProvider)
            it.apiTokenFile.set(config.apiTokenFile)
            it.ownerName.set(config.ownerName)
            it.appName.set(config.appName)
            it.baseFileName.set(params.outputConfig.baseFileName)
            it.testerGroups.set(config.testerGroups)
            it.maxUploadStatusRequestCount.set(config.maxUploadStatusRequestCount)
            it.uploadStatusRequestDelayMs.set(config.uploadStatusRequestDelayMs)
            it.uploadStatusRequestDelayCoefficient.set(config.uploadStatusRequestDelayCoefficient)
        }
    }

    private fun TaskContainer.registerPlayTask(params: PlayTaskParams): TaskProvider<PlayDistributionTask> {
        val buildVariant = params.buildVariant
        val config = params.config

        return register(
            "$PLAY_DISTRIBUTION_UPLOAD_TASK_PREFIX${buildVariant.capitalizedName()}",
            PlayDistributionTask::class.java,
        ) {
            it.tagBuildFile.set(params.tagBuildProvider)
            it.buildVariantOutputFile.set(params.bundleOutputFileProvider)
            it.apiTokenFile.set(config.apiTokenFile)
            it.appId.set(config.appId)
            it.trackId.set(config.trackId)
            it.updatePriority.set(config.updatePriority)
        }
    }
}

private fun Project.configurePlugins(
    useDefaultVersionsAsFallback: Boolean,
    firebaseAppDistributionConfig: FirebaseAppDistributionConfig?,
    changelogFile: File,
) {
    plugins.all { plugin ->
        when (plugin) {
            is AppPlugin -> {
                if (useDefaultVersionsAsFallback) {
                    val appExtension = extensions.getByType(AppExtension::class.java)
                    appExtension.configure()
                }
            }

            is AppDistributionPlugin -> {
                if (firebaseAppDistributionConfig != null) {
                    val appDistributionExtension =
                        extensions
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
            "Must only be used with with Android Gradle Plugin >= 7.4 ",
        )
    }
    if (!plugins.hasPlugin(AppPlugin::class.java)) {
        throw StopExecutionException(
            "Must only be used with Android application projects." +
                " Please apply the 'com.android.application' plugin.",
        )
    }
}

private fun AppExtension.configure() {
    defaultConfig {
        it.versionCode = DEFAULT_VERSION_CODE
        it.versionName = DEFAULT_VERSION_NAME
    }
}

private fun AppDistributionExtension.configure(
    config: FirebaseAppDistributionConfig,
    changelogFile: File,
) {
    val serviceCredentialsFilePath =
        config
            .serviceCredentialsFilePath
            .orNull
            ?.takeIf { it.isNotBlank() }
    val applicationId =
        config
            .appId
            .orNull
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
    val versionName: Provider<String>?,
    val versionCode: Provider<Int>?,
    val apkOutputFileName: Provider<String>,
)

private data class AppCenterDistributionTaskParams(
    val config: AppCenterDistributionConfig,
    val buildVariant: BuildVariant,
    val changelogFileProvider: Provider<RegularFile>,
    val apkOutputFileProvider: Provider<RegularFile>,
    val tagBuildProvider: Provider<RegularFile>,
    val outputConfig: OutputConfig,
)

private data class PlayTaskParams(
    val config: PlayConfig,
    val buildVariant: BuildVariant,
    val bundleOutputFileProvider: Provider<RegularFile>,
    val tagBuildProvider: Provider<RegularFile>,
    val outputConfig: OutputConfig,
)

private fun mapToVersionCode(tagBuildFile: RegularFile): Int {
    val file = tagBuildFile.asFile
    return if (file.exists()) {
        fromJson(file).buildNumber
    } else {
        DEFAULT_VERSION_CODE
    }
}

private fun mapToOutputApkFileName(
    tagBuildFile: RegularFile,
    outputFileName: String,
    baseFileName: String?,
): String {
    val file = tagBuildFile.asFile
    val formattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy"))
    return if (file.exists() && outputFileName.endsWith(".apk")) {
        val tagBuild = fromJson(file)
        val versionName = tagBuild.buildVariant
        val versionCode = tagBuild.buildNumber
        "$baseFileName-$versionName-vc$versionCode-$formattedDate.apk"
    } else if (!file.exists() && outputFileName.endsWith(".apk")) {
        "$baseFileName-$formattedDate.apk"
    } else {
        createDefaultOutputFileName(baseFileName, outputFileName)
    }
}

private fun createDefaultOutputFileName(
    baseFileName: String?,
    outputFileName: String,
): String {
    return (baseFileName ?: DEFAULT_BASE_FILE_NAME)
        .let { "$it.${outputFileName.split(".").last()}" }
}

private fun mapToVersionName(
    tagBuildFile: RegularFile,
    buildVariant: BuildVariant,
): String {
    val file = tagBuildFile.asFile
    return if (file.exists()) {
        fromJson(tagBuildFile.asFile).name
    } else {
        "$DEFAULT_BUILD_VERSION-${buildVariant.name}"
    }
}

private fun Project.mapToOutputApkFile(
    buildVariant: BuildVariant,
    fileName: String,
): Provider<RegularFile> {
    return project.tasks.withType(PackageAndroidArtifact::class.java)
        .firstOrNull { it.variantName == buildVariant.name }
        ?.outputDirectory
        ?.map { directory -> directory.file(fileName) }
        ?: throw GradleException("no output for variant ${buildVariant.name}")
}
