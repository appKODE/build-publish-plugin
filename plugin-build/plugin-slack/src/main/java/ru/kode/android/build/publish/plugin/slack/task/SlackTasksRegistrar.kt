package ru.kode.android.build.publish.plugin.slack.task

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.core.util.flatMapByNameOrDefault
import ru.kode.android.build.publish.plugin.slack.config.SlackBotConfig
import ru.kode.android.build.publish.plugin.slack.config.SlackChangelogConfig
import ru.kode.android.build.publish.plugin.slack.config.SlackDistributionConfig
import ru.kode.android.build.publish.plugin.slack.service.SlackServiceExtension
import ru.kode.android.build.publish.plugin.slack.task.changelog.SendSlackChangelogTask
import ru.kode.android.build.publish.plugin.slack.task.distribution.SlackDistributionTask

internal const val SEND_SLACK_CHANGELOG_TASK_PREFIX = "sendSlackChangelog"
internal const val SLACK_DISTRIBUTION_UPLOAD_TASK_PREFIX = "slackDistributionUpload"

object SlackTasksRegistrar {
    private val logger: Logger = Logging.getLogger(this::class.java)

    fun registerChangelogTask(
        project: Project,
        botConfig: SlackBotConfig,
        changelogConfig: SlackChangelogConfig,
        params: SlackChangelogTaskParams,
    ): TaskProvider<SendSlackChangelogTask> {
        return project.registerSendSlackChangelogTask(botConfig, changelogConfig, params)
    }

    fun registerDistributionTask(
        project: Project,
        distributionConfig: SlackDistributionConfig,
        params: SlackDistributionTaskParams,
    ): TaskProvider<SlackDistributionTask>? {
        return if (
            distributionConfig.uploadApiTokenFile.isPresent &&
            distributionConfig.uploadChannels.isPresent
        ) {
            project.registerSlackDistributionTask(distributionConfig, params)
        } else {
            logger.info(
                "SlackDistributionTask was not created, " +
                    "uploadApiTokenFile and uploadChannels are not present",
            )
            null
        }
    }
}

private fun Project.registerSendSlackChangelogTask(
    botConfig: SlackBotConfig,
    changelogConfig: SlackChangelogConfig,
    params: SlackChangelogTaskParams,
): TaskProvider<SendSlackChangelogTask> {
    return tasks.register(
        "$SEND_SLACK_CHANGELOG_TASK_PREFIX${params.buildVariant.capitalizedName()}",
        SendSlackChangelogTask::class.java,
    ) {
        val webhookService =
            extensions
                .getByType(SlackServiceExtension::class.java)
                .webhookServices
                .flatMapByNameOrDefault(params.buildVariant.name)

        it.changelogFile.set(params.generateChangelogFileProvider)
        it.tagBuildFile.set(params.tagBuildProvider)
        it.issueUrlPrefix.set(params.issueUrlPrefix)
        it.issueNumberPattern.set(params.issueNumberPattern)
        it.baseOutputFileName.set(params.baseFileName)
        it.iconUrl.set(botConfig.iconUrl)
        it.userMentions.set(changelogConfig.userMentions)
        it.attachmentColor.set(changelogConfig.attachmentColor)
        it.networkService.set(webhookService)
    }
}

private fun Project.registerSlackDistributionTask(
    distributionConfig: SlackDistributionConfig,
    params: SlackDistributionTaskParams,
): TaskProvider<SlackDistributionTask> {
    return tasks.register(
        "$SLACK_DISTRIBUTION_UPLOAD_TASK_PREFIX${params.buildVariant.capitalizedName()}",
        SlackDistributionTask::class.java,
    ) {
        val uploadService =
            extensions
                .getByType(SlackServiceExtension::class.java)
                .uploadServices
                .flatMapByNameOrDefault(params.buildVariant.name)

        it.buildVariantOutputFile.set(params.apkOutputFileProvider)
        it.channels.set(distributionConfig.uploadChannels)
        it.tagBuildFile.set(params.tagBuildProvider)
        it.baseOutputFileName.set(params.baseFileName)
        it.networkService.set(uploadService)
    }
}

data class SlackChangelogTaskParams(
    val baseFileName: Property<String>,
    val issueNumberPattern: Provider<String>,
    val issueUrlPrefix: Property<String>,
    val buildVariant: BuildVariant,
    val generateChangelogFileProvider: Provider<RegularFile>,
    val tagBuildProvider: Provider<RegularFile>,
)

data class SlackDistributionTaskParams(
    val baseFileName: Property<String>,
    val buildVariant: BuildVariant,
    val tagBuildProvider: Provider<RegularFile>,
    val apkOutputFileProvider: Provider<RegularFile>,
)
