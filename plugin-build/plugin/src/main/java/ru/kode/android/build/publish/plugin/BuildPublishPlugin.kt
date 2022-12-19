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
import ru.kode.android.build.publish.plugin.extension.config.FirebaseAppDistributionConfig
import ru.kode.android.build.publish.plugin.extension.config.JiraConfig
import ru.kode.android.build.publish.plugin.extension.config.OutputConfig
import ru.kode.android.build.publish.plugin.extension.config.SlackConfig
import ru.kode.android.build.publish.plugin.extension.config.TelegramConfig
import ru.kode.android.build.publish.plugin.task.appcenter.AppCenterDistributionTask
import ru.kode.android.build.publish.plugin.task.changelog.GenerateChangelogTask
import ru.kode.android.build.publish.plugin.task.jira.JiraAutomationTask
import ru.kode.android.build.publish.plugin.task.slack.changelog.SendSlackChangelogTask
import ru.kode.android.build.publish.plugin.task.slack.distribution.SlackDistributionTask
import ru.kode.android.build.publish.plugin.task.tag.GetLastTagTask
import ru.kode.android.build.publish.plugin.task.tag.PrintLastIncreasedTag
import ru.kode.android.build.publish.plugin.task.telegram.SendTelegramChangelogTask
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
internal const val SLACK_DISTRIBUTION_UPLOAD_TASK_PREFIX = "slackDistributionUpload"
internal const val JIRA_AUTOMATION_TASK = "jiraAutomation"

internal object AgpVersions {
    val CURRENT: VersionNumber = VersionNumber.parse(ANDROID_GRADLE_PLUGIN_VERSION).baseVersion
    val VERSION_7_0_4: VersionNumber = VersionNumber.parse("7.0.4")
}

@Suppress("LongMethod", "TooManyFunctions") // TODO Split into small methods
abstract class BuildPublishPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.stopExecutionIfNotSupported()
        project.pluginManager.apply(GrgitServicePlugin::class.java)

        val buildPublishExtension = project.extensions
            .create(EXTENSION_NAME, BuildPublishExtension::class.java)
        val androidExtension = project.extensions
            .getByType(ApplicationAndroidComponentsExtension::class.java)
        val changelogFile = project.layout.buildDirectory.file(CHANGELOG_FILENAME)
        val grgitService = project.extensions
            .getByType(GrgitServiceExtension::class.java)
            .service
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
                        outputFileName,
                        grgitService,
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

    @Suppress("ComplexMethod", "LongMethod") // split to multiple methods
    private fun Project.registerVariantTasks(
        buildPublishExtension: BuildPublishExtension,
        buildVariant: BuildVariant,
        changelogFile: Provider<RegularFile>,
        outputFileName: String,
        grgitService: Provider<GrgitService>,
    ): OutputProviders {
        val outputConfig = buildPublishExtension.output.getByName("default")
        val tagBuildProvider = registerGetLastTagTask(buildVariant, grgitService)
        val versionCodeProvider = tagBuildProvider.map(::mapToVersionCode)
        val outputFileNameProvider = outputConfig.baseFileName.zip(tagBuildProvider) { baseFileName, tagBuildFile ->
            mapToOutputFileName(tagBuildFile, outputFileName, baseFileName)
        }
        val versionNameProvider = tagBuildProvider.map { tagBuildFile ->
            mapToVersionName(tagBuildFile, buildVariant)
        }
        val outputFileProvider = outputFileNameProvider.flatMap { fileName ->
            mapToOutputFile(buildVariant, fileName)
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
                changelogConfig.commitMessageKey,
                buildVariant,
                changelogFile,
                tagBuildProvider,
                grgitService,
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
                tasks.registerSlackTasks(
                    outputConfig,
                    changelogConfig,
                    slackConfig,
                    buildVariant,
                    generateChangelogFileProvider,
                    tagBuildProvider,
                    outputFileProvider
                )
            }
            val appCenterDistributionConfig = with(buildPublishExtension.appCenterDistribution) {
                findByName(buildVariant.name) ?: findByName("default")
            }
            if (appCenterDistributionConfig != null) {
                val params = AppCenterDistributionTaskParams(
                    config = appCenterDistributionConfig,
                    buildVariant = buildVariant,
                    changelogFileProvider = generateChangelogFileProvider,
                    buildVariantOutputFileProvider = outputFileProvider,
                    tagBuildProvider = tagBuildProvider,
                    outputConfig = outputConfig,
                )
                tasks.registerAppCenterDistributionTask(params)
            }
            val jiraConfig = with(buildPublishExtension.jira) {
                findByName(buildVariant.name) ?: findByName("default")
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
        }
        return OutputProviders(
            versionName = versionNameProvider,
            versionCode = versionCodeProvider,
            outputFileName = outputFileNameProvider
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

    @Suppress("LongParameterList") // TODO Get parameters inside
    private fun TaskContainer.registerSlackTasks(
        outputConfig: OutputConfig,
        changelogConfig: ChangelogConfig,
        slackConfig: SlackConfig,
        buildVariant: BuildVariant,
        generateChangelogFileProvider: Provider<RegularFile>,
        tagBuildProvider: Provider<RegularFile>,
        outputFileProvider: Provider<RegularFile>
    ) {
        registerSendSlackChangelogTask(
            outputConfig,
            changelogConfig,
            slackConfig,
            buildVariant,
            generateChangelogFileProvider,
            tagBuildProvider
        )
        if (
            slackConfig.uploadApiTokenFile.isPresent &&
            slackConfig.uploadChannels.isPresent
        ) {
            registerSlackUploadTask(
                slackConfig.uploadApiTokenFile,
                slackConfig.uploadChannels,
                buildVariant,
                outputFileProvider
            )
        }
    }

    private fun TaskContainer.registerSlackUploadTask(
        apiTokenFile: RegularFileProperty,
        channels: SetProperty<String>,
        buildVariant: BuildVariant,
        outputFileProvider: Provider<RegularFile>
    ) {
        register(
            "$SLACK_DISTRIBUTION_UPLOAD_TASK_PREFIX${buildVariant.capitalizedName()}",
            SlackDistributionTask::class.java
        ) {
            it.buildVariantOutputFile.set(outputFileProvider)
            it.apiTokenFile.set(apiTokenFile)
            it.channels.set(channels)
        }
    }

    private fun Project.registerGetLastTagTask(
        buildVariant: BuildVariant,
        grgitService: Provider<GrgitService>,
    ): Provider<RegularFile> {
        val tagBuildFile = project.layout.buildDirectory
            .file("tag-build-${buildVariant.name}.json")
        return tasks.register(
            "$GET_LAST_TAG_TASK_PREFIX${buildVariant.capitalizedName()}",
            GetLastTagTask::class.java
        ) { task ->
            task.tagBuildFile.set(tagBuildFile)
            task.buildVariant.set(buildVariant.name)
            task.getGrgitService().set(grgitService)
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
        commitMessageKey: Provider<String>,
        buildVariant: BuildVariant,
        changelogFile: Provider<RegularFile>,
        tagBuildProvider: Provider<RegularFile>,
        grgitService: Provider<GrgitService>,
    ): Provider<RegularFile> {
        return register(
            "$GENERATE_CHANGELOG_TASK_PREFIX${buildVariant.capitalizedName()}",
            GenerateChangelogTask::class.java
        ) {
            it.commitMessageKey.set(commitMessageKey)
            it.buildVariant.set(buildVariant.name)
            it.changelogFile.set(changelogFile)
            it.tagBuildFile.set(tagBuildProvider)
            it.getGrgitService().set(grgitService)
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
        params: AppCenterDistributionTaskParams,
    ): TaskProvider<AppCenterDistributionTask> {
        val buildVariant = params.buildVariant
        val config = params.config

        return register(
            "$APP_CENTER_DISTRIBUTION_UPLOAD_TASK_PREFIX${buildVariant.capitalizedName()}",
            AppCenterDistributionTask::class.java,
        ) {
            it.tagBuildFile.set(params.tagBuildProvider)
            it.buildVariantOutputFile.set(params.buildVariantOutputFileProvider)
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

private data class AppCenterDistributionTaskParams(
    val config: AppCenterDistributionConfig,
    val buildVariant: BuildVariant,
    val changelogFileProvider: Provider<RegularFile>,
    val buildVariantOutputFileProvider: Provider<RegularFile>,
    val tagBuildProvider: Provider<RegularFile>,
    val outputConfig: OutputConfig,
)

private fun mapToVersionCode(tagBuildFile: RegularFile): Int {
    val file = tagBuildFile.asFile
    return if (file.exists()) {
        fromJson(file).buildNumber
    } else {
        1
    }
}

private fun mapToOutputFileName(tagBuildFile: RegularFile, outputFileName: String, baseFileName: String?): String {
    val file = tagBuildFile.asFile
    val formattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy"))
    return if (file.exists() && outputFileName.endsWith(".apk")) {
        val tagBuild = fromJson(file)
        val versionName = tagBuild.buildVariant
        val versionCode = tagBuild.buildNumber
        "$baseFileName-$versionName-vc$versionCode-$formattedDate.apk"
    } else if (!file.exists() && outputFileName.endsWith(".apk")) {
        "$baseFileName-$formattedDate.apk"
    } else "$baseFileName.${outputFileName.split(".").last()}"
}

private fun mapToVersionName(tagBuildFile: RegularFile, buildVariant: BuildVariant): String {
    val file = tagBuildFile.asFile
    return if (file.exists()) {
        fromJson(tagBuildFile.asFile).name
    } else {
        "$DEFAULT_BUILD_VERSION-${buildVariant.name}"
    }
}

private fun Project.mapToOutputFile(
    buildVariant: BuildVariant,
    fileName: String
): Provider<RegularFile> {
    return project.tasks.withType(PackageAndroidArtifact::class.java)
        .firstOrNull { it.variantName == buildVariant.name }
        ?.outputDirectory
        ?.map { directory -> directory.file(fileName) }
        ?: throw GradleException("no output for variant ${buildVariant.name}")
}
