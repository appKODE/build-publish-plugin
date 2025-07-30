package ru.kode.android.build.publish.plugin.slack.core

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.slack.task.changelog.SendSlackChangelogTask
import ru.kode.android.build.publish.plugin.slack.task.distribution.SlackDistributionTask

internal const val SEND_SLACK_CHANGELOG_TASK_PREFIX = "sendSlackChangelog"
internal const val SLACK_DISTRIBUTION_UPLOAD_TASK_PREFIX = "slackDistributionUpload"

interface SlackConfig {
    val name: String

    /**
     * Slack bot webhook url
     * For example: https://hooks.slack.com/services/111111111/AAAAAAA/DDDDDDD
     */
    @get:Input
    val webhookUrl: Property<String>

    /**
     * Slack bot icon url
     * For example: https://i.imgur.com/HQTF5FK.png
     */
    @get:Input
    val iconUrl: Property<String>

    /**
     * List of mentioning users for Slack, can be empty or null
     * For example: ["@aa", "@bb", "@ccc"]
     */
    @get:Input
    val userMentions: SetProperty<String>

    /**
     * Attachment's vertical line color in hex format
     * For example: #ffffff
     */
    @get:Input
    val attachmentColor: Property<String>

    /**
     * Api token file to upload files in slack
     */
    @get:Optional
    @get:InputFile
    val uploadApiTokenFile: RegularFileProperty

    /**
     * Public channels where file will be uploaded
     */
    @get:Optional
    @get:Input
    val uploadChannels: SetProperty<String>

    fun registerChangelogTask(
        project: Project,
        params: SlackChangelogTaskParams
    ): TaskProvider<SendSlackChangelogTask> {
        return project.tasks.registerSendSlackChangelogTask(this, params)
    }

    fun registerDistributionTask(
        project: Project,
        params: SlackDistributionTasksParams
    ): TaskProvider<SlackDistributionTask>? {
        return if (
            this.uploadApiTokenFile.isPresent &&
            this.uploadChannels.isPresent
        ) {
            project.tasks.registerSlackDistributionTask(this, params)
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
