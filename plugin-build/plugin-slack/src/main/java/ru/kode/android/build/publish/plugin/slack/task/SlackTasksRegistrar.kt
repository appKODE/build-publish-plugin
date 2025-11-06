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
import ru.kode.android.build.publish.plugin.slack.config.SlackDistributionConfig
import ru.kode.android.build.publish.plugin.slack.service.SlackServiceExtension
import ru.kode.android.build.publish.plugin.slack.task.distribution.SlackDistributionTask

internal const val SLACK_DISTRIBUTION_UPLOAD_TASK_PREFIX = "slackDistributionUpload"
internal const val SLACK_DISTRIBUTION_UPLOAD_BUNDLE_TASK_PREFIX = "slackDistributionUploadBundle"

/**
 * Utility object for registering Slack-related Gradle tasks.
 *
 * This object provides methods to register file distribution upload tasks
 * that upload build artifacts to Slack with rich text changelog.
 *
 * It handles task creation and configuration based on the provided parameters and build variants.
 */
internal object SlackTasksRegistrar {
    private val logger: Logger = Logging.getLogger(this::class.java)

    /**
     * Checks if the distribution configuration is valid for task registration.
     */
    private fun SlackDistributionConfig.isValidForRegistration(): Boolean {
        return uploadApiTokenFile.isPresent &&
            destinationChannels.isPresent &&
            destinationChannels.get().isNotEmpty()
    }

    /**
     * Registers a task for uploading APK to Slack for distribution.
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
    internal fun registerApkDistributionTask(
        project: Project,
        distributionConfig: SlackDistributionConfig,
        params: SlackApkDistributionTaskParams,
    ): TaskProvider<SlackDistributionTask>? {
        return if (distributionConfig.isValidForRegistration()) {
            project.registerApkSlackDistributionTask(distributionConfig, params)
        } else {
            logger.info(
                "SlackDistributionTask for APK was not created, " +
                    "uploadApiTokenFile and destinationChannels are not present or empty",
            )
            null
        }
    }

    /**
     * Registers a task for uploading Bundle to Slack for distribution.
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
    internal fun registerBundleDistributionTask(
        project: Project,
        distributionConfig: SlackDistributionConfig,
        params: SlackBundleDistributionTaskParams,
    ): TaskProvider<SlackDistributionTask>? {
        return if (distributionConfig.isValidForRegistration()) {
            project.registerBundleSlackDistributionTask(distributionConfig, params)
        } else {
            logger.info(
                "SlackDistributionTask for Bundle was not created, " +
                    "uploadApiTokenFile and destinationChannels are not present or empty",
            )
            null
        }
    }
}

/**
 * Sets up dependency on GenerateChangelogTask if it exists.
 * GenerateChangelogTask is only registered if changelog is configured in foundation plugin.
 */
private fun setupChangelogTaskDependency(
    project: Project,
    taskProvider: TaskProvider<SlackDistributionTask>,
    buildVariant: BuildVariant,
) {
    project.afterEvaluate {
        // GenerateChangelogTask naming convention: "generateChangelog" + buildVariant name
        val changelogTaskName = "generateChangelog${buildVariant.capitalizedName()}"
        project.tasks.findByName(changelogTaskName)?.let { changelogTask ->
            taskProvider.configure { task ->
                task.dependsOn(changelogTask)
            }
        }
    }
}

/**
 * Registers a task for uploading APK to Slack for distribution.
 *
 * @param distributionConfig Configuration for the file distribution
 * @param params Parameters for the distribution task
 *
 * @return A TaskProvider for the registered task
 */
private fun Project.registerApkSlackDistributionTask(
    distributionConfig: SlackDistributionConfig,
    params: SlackApkDistributionTaskParams,
): TaskProvider<SlackDistributionTask> {
    val taskProvider =
        tasks.register(
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
            it.changelogFile.set(params.changelogFile)
            it.userMentions.set(distributionConfig.userMentions)
            it.distributionDescription.set(distributionConfig.distributionDescription)
            it.networkService.set(uploadService)
        }
    setupChangelogTaskDependency(project, taskProvider, params.buildVariant)
    return taskProvider
}

/**
 * Registers a task for uploading Bundle to Slack for distribution.
 *
 * @param distributionConfig Configuration for the file distribution
 * @param params Parameters for the distribution task
 *
 * @return A TaskProvider for the registered task
 */
private fun Project.registerBundleSlackDistributionTask(
    distributionConfig: SlackDistributionConfig,
    params: SlackBundleDistributionTaskParams,
): TaskProvider<SlackDistributionTask>? {
    return if (params.bundleOutputFile.isPresent) {
        val taskProvider =
            tasks.register(
                "$SLACK_DISTRIBUTION_UPLOAD_BUNDLE_TASK_PREFIX${params.buildVariant.capitalizedName()}",
                SlackDistributionTask::class.java,
            ) {
                val uploadService =
                    extensions
                        .getByType(SlackServiceExtension::class.java)
                        .uploadServices
                        .flatMapByNameOrCommon(params.buildVariant.name)

                it.distributionFile.set(params.bundleOutputFile)
                it.destinationChannels.set(distributionConfig.destinationChannels)
                it.buildTagFile.set(params.lastBuildTagFile)
                it.baseOutputFileName.set(params.baseFileName)
                it.changelogFile.set(params.changelogFile)
                it.userMentions.set(distributionConfig.userMentions)
                it.distributionDescription.set(distributionConfig.distributionDescription)
                it.networkService.set(uploadService)
            }
        setupChangelogTaskDependency(project, taskProvider, params.buildVariant)
        return taskProvider
    } else {
        logger.info(
            "SlackDistributionTask for Bundle was not created, bundleOutputFile is not present",
        )
        return null
    }
}

/**
 * Parameters for configuring a Slack APK distribution task.
 */
internal data class SlackApkDistributionTaskParams(
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
    /**
     * File containing changelog content
     */
    val changelogFile: Provider<RegularFile>,
)

/**
 * Parameters for configuring a Slack Bundle distribution task.
 */
internal data class SlackBundleDistributionTaskParams(
    /**
     * The build variant this task is associated with
     */
    val buildVariant: BuildVariant,
    /**
     * The Bundle file to distribute
     */
    val bundleOutputFile: Provider<RegularFile>,
    /**
     * File containing the last build tag for change tracking
     */
    val lastBuildTagFile: Provider<RegularFile>,
    /**
     * Base name to use for the build in notifications
     */
    val baseFileName: Provider<String>,
    /**
     * File containing changelog content
     */
    val changelogFile: Provider<RegularFile>,
)
