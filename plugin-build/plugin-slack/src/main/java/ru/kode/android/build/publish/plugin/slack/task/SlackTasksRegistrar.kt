package ru.kode.android.build.publish.plugin.slack.task

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.slack.core.SlackConfig
import ru.kode.android.build.publish.plugin.slack.task.changelog.SendSlackChangelogTask
import ru.kode.android.build.publish.plugin.slack.task.distribution.SlackDistributionTask

internal const val SEND_SLACK_CHANGELOG_TASK_PREFIX = "sendSlackChangelog"
internal const val SLACK_DISTRIBUTION_UPLOAD_TASK_PREFIX = "slackDistributionUpload"

object SlackTasksRegistrar {

    fun registerChangelogTask(
        project: TaskContainer,
        config: SlackConfig,
        params: SlackChangelogTaskParams
    ): TaskProvider<SendSlackChangelogTask> {
        return project.registerSendSlackChangelogTask(config, params)
    }

    fun registerDistributionTask(
        project: TaskContainer,
        config: SlackConfig,
        params: SlackDistributionTasksParams
    ): TaskProvider<SlackDistributionTask>? {
        return if (
            config.uploadApiTokenFile.isPresent &&
            config.uploadChannels.isPresent
        ) {
            project.registerSlackDistributionTask(config, params)
        } else {
            // TODO: Add logs
            null
        }
    }
}

private fun TaskContainer.registerSendSlackChangelogTask(
    config: SlackConfig,
    params: SlackChangelogTaskParams,
): TaskProvider<SendSlackChangelogTask> {
    return register(
        "$SEND_SLACK_CHANGELOG_TASK_PREFIX${params.buildVariant.capitalizedName()}",
        SendSlackChangelogTask::class.java,
    ) {
        it.changelogFile.set(params.generateChangelogFileProvider)
        it.tagBuildFile.set(params.tagBuildProvider)
        it.issueUrlPrefix.set(params.issueUrlPrefix)
        it.issueNumberPattern.set(params.issueNumberPattern)
        it.baseOutputFileName.set(params.baseFileName)
        it.webhookUrl.set(config.webhookUrl)
        it.iconUrl.set(config.iconUrl)
        it.userMentions.set(config.userMentions)
        it.attachmentColor.set(config.attachmentColor)
    }
}

private fun TaskContainer.registerSlackDistributionTask(
    config: SlackConfig,
    params: SlackDistributionTasksParams,
): TaskProvider<SlackDistributionTask> {
    return register(
        "$SLACK_DISTRIBUTION_UPLOAD_TASK_PREFIX${params.buildVariant.capitalizedName()}",
        SlackDistributionTask::class.java,
    ) {
        it.buildVariantOutputFile.set(params.apkOutputFileProvider)
        it.apiTokenFile.set(config.uploadApiTokenFile)
        it.channels.set(config.uploadChannels)
        it.tagBuildFile.set(params.tagBuildProvider)
        it.baseOutputFileName.set(params.baseFileName)
    }
}

data class SlackChangelogTaskParams(
    val baseFileName: Property<String>,
    val issueNumberPattern: Property<String>,
    val issueUrlPrefix: Property<String>,
    val buildVariant: BuildVariant,
    val generateChangelogFileProvider: Provider<RegularFile>,
    val tagBuildProvider: Provider<RegularFile>,
)

data class SlackDistributionTasksParams(
    val baseFileName: Property<String>,
    val buildVariant: BuildVariant,
    val tagBuildProvider: Provider<RegularFile>,
    val apkOutputFileProvider: Provider<RegularFile>,
)
