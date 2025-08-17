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

/**
 * Utility object for registering Slack-related Gradle tasks.
 *
 * This object provides methods to register different types of Slack tasks:
 * - Changelog notification tasks
 * - File distribution upload tasks
 *
 * It handles task creation and configuration based on the provided parameters and build variants.
 */
internal object SlackTasksRegistrar {
    private val logger: Logger = Logging.getLogger(this::class.java)

    /**
     * Registers a task for sending changelog notifications to Slack.
     *
     * @param project The project to register the task in
     * @param botConfig Configuration for the Slack bot
     * @param changelogConfig Configuration for the changelog message
     * @param params Parameters for the changelog task
     *
     * @return A TaskProvider for the registered task
     */
    internal fun registerChangelogTask(
        project: Project,
        botConfig: SlackBotConfig,
        changelogConfig: SlackChangelogConfig,
        params: SlackChangelogTaskParams,
    ): TaskProvider<SendSlackChangelogTask> {
        return project.registerSendSlackChangelogTask(botConfig, changelogConfig, params)
    }

    /**
     * Registers a task for uploading files to Slack for distribution.
     *
     * The task will only be registered if both `uploadApiTokenFile` and `destinationChannels`
     * are present in the distribution configuration.
     *
     * @param project The project to register the task in
     * @param distributionConfig Configuration for the file distribution
     * @param params Parameters for the distribution task
     *
     * @return A TaskProvider for the registered task, or null if requirements aren't met
     */
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

/**
 * Registers a task for sending changelog notifications to Slack.
 *
 * @param botConfig Configuration for the Slack bot
 * @param changelogConfig Configuration for the changelog message
 * @param params Parameters for the changelog task
 *
 * @return A TaskProvider for the registered task
 */
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

/**
 * Registers a task for uploading files to Slack for distribution.
 *
 * @param distributionConfig Configuration for the file distribution
 * @param params Parameters for the distribution task
 *
 * @return A TaskProvider for the registered task
 */
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

/**
 * Parameters for configuring a Slack changelog notification task.
 */
internal data class SlackChangelogTaskParams(
    /**
     * The build variant this task is associated with
     */
    val buildVariant: BuildVariant,
    /**
     * The file containing the changelog to send
     */
    val changelogFile: Provider<RegularFile>,
    /**
     * File containing the last build tag for change tracking
     */
    val lastBuildTagFile: Provider<RegularFile>,
    /**
     * Base URL for issue links
     */
    val issueUrlPrefix: Provider<String>,
    /**
     * Regex pattern for matching issue numbers in commit messages
     */
    val issueNumberPattern: Provider<String>,
    /**
     * Base name to use for the build in notifications
     */
    val baseFileName: Provider<String>,
)

/**
 * Parameters for configuring a Slack file distribution task.
 */
internal data class SlackDistributionTaskParams(
    /**
     * The build variant this task is associated with
     */
    val buildVariant: BuildVariant,
    /**
     * The APK file to distribute
     */
    val apkOutputFile: Provider<RegularFile>,
    /**
     * File containing the last build tag for change tracking
     */
    val lastBuildTagFile: Provider<RegularFile>,
    /**
     * Base name to use for the build in notifications
     */
    val baseFileName: Provider<String>,
)
