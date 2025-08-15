package ru.kode.android.build.publish.plugin.slack.task

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.core.util.flatMapByNameOrCommon
import ru.kode.android.build.publish.plugin.slack.config.SlackBotConfig
import ru.kode.android.build.publish.plugin.slack.config.SlackChangelogConfig
import ru.kode.android.build.publish.plugin.slack.config.SlackDistributionConfig
import ru.kode.android.build.publish.plugin.slack.service.SlackServiceExtension
import ru.kode.android.build.publish.plugin.slack.task.changelog.SendSlackChangelogTask
import ru.kode.android.build.publish.plugin.slack.task.distribution.SlackDistributionTask

internal const val SEND_SLACK_CHANGELOG_TASK_PREFIX = "sendSlackChangelog"
internal const val SLACK_DISTRIBUTION_UPLOAD_TASK_PREFIX = "slackDistributionUpload"

internal object SlackTasksRegistrar {
    private val logger: Logger = Logging.getLogger(this::class.java)

    internal fun registerChangelogTask(
        project: Project,
        botConfig: SlackBotConfig,
        changelogConfig: SlackChangelogConfig,
        params: SlackChangelogTaskParams,
    ): TaskProvider<SendSlackChangelogTask> {
        return project.registerSendSlackChangelogTask(botConfig, changelogConfig, params)
    }

    internal fun registerDistributionTask(
        project: Project,
        distributionConfig: SlackDistributionConfig,
        params: SlackDistributionTaskParams,
    ): TaskProvider<SlackDistributionTask>? {
        return if (
            distributionConfig.uploadApiTokenFile.isPresent &&
            distributionConfig.destinationChannels.isPresent
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
                .flatMapByNameOrCommon(params.buildVariant.name)

        it.changelogFile.set(params.changelogFile)
        it.buildTagFile.set(params.lastBuildTagFile)
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
                .flatMapByNameOrCommon(params.buildVariant.name)

        it.distributionFile.set(params.apkOutputFile)
        it.destinationChannels.set(distributionConfig.destinationChannels)
        it.buildTagFile.set(params.lastBuildTagFile)
        it.baseOutputFileName.set(params.baseFileName)
        it.networkService.set(uploadService)
    }
}

internal data class SlackChangelogTaskParams(
    val baseFileName: Provider<String>,
    val issueNumberPattern: Provider<String>,
    val issueUrlPrefix: Provider<String>,
    val buildVariant: BuildVariant,
    val changelogFile: Provider<RegularFile>,
    val lastBuildTagFile: Provider<RegularFile>,
)

internal data class SlackDistributionTaskParams(
    val baseFileName: Provider<String>,
    val buildVariant: BuildVariant,
    val lastBuildTagFile: Provider<RegularFile>,
    val apkOutputFile: Provider<RegularFile>,
)
