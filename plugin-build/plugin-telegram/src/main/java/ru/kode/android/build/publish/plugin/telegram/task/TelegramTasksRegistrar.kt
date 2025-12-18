package ru.kode.android.build.publish.plugin.telegram.task

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.core.util.getByNameOrCommon
import ru.kode.android.build.publish.plugin.telegram.config.TelegramChangelogConfig
import ru.kode.android.build.publish.plugin.telegram.config.TelegramDistributionConfig
import ru.kode.android.build.publish.plugin.telegram.config.TelegramLookupConfig
import ru.kode.android.build.publish.plugin.telegram.controller.mappers.mapToEntity
import ru.kode.android.build.publish.plugin.telegram.controller.mappers.toJson
import ru.kode.android.build.publish.plugin.telegram.messages.distributionBundleTaskNotCreatedMessage
import ru.kode.android.build.publish.plugin.telegram.messages.distributionTaskNotCreatedMessage
import ru.kode.android.build.publish.plugin.telegram.service.TelegramServiceExtension
import ru.kode.android.build.publish.plugin.telegram.task.changelog.SendTelegramChangelogTask
import ru.kode.android.build.publish.plugin.telegram.task.distribution.TelegramDistributionTask
import ru.kode.android.build.publish.plugin.telegram.task.lookup.TelegramLookupTask

internal const val SEND_TELEGRAM_CHANGELOG_TASK_PREFIX = "sendTelegramChangelog"
// TODO: Replace with telegramDistributionUploadApk
internal const val TELEGRAM_DISTRIBUTION_UPLOAD_APK_TASK_PREFIX = "telegramDistributionUpload"
internal const val TELEGRAM_DISTRIBUTION_UPLOAD_BUNDLE_TASK_PREFIX = "telegramDistributionUploadBundle"
internal const val TELEGRAM_LOOKUP_TASK_PREFIX = "telegramLookup"

/**
 * Utility object for registering Telegram-related Gradle tasks.
 *
 * This object provides methods to register different types of Telegram notification tasks:
 * - Changelog notification tasks: For sending release notes and changelog information
 * - Distribution upload notification tasks: For notifying about new app distribution uploads
 *
 * The registrar handles task creation, configuration, and dependency setup based on the
 * provided parameters and build variants. It ensures that tasks are properly named and
 * configured according to the build variant they're associated with.
 *
 * @see SendTelegramChangelogTask For the changelog notification task implementation
 * @see TelegramDistributionTask For the distribution notification task implementation
 */
internal object TelegramTasksRegistrar {
    private val logger: Logger = Logging.getLogger(this::class.java)

    /**
     * Registers a task for sending changelog notifications to Telegram.
     *
     * This method creates and configures a task that will send a formatted changelog
     * message to the configured Telegram chats when executed.
     *
     * @param project The Gradle project to register the task in
     * @param config Configuration for the changelog notification, including
     *                       destination bots and message formatting options
     * @param params Parameters for the changelog task, including build variant and
     *              changelog file location
     * @return A TaskProvider for the registered SendTelegramChangelogTask
     * @throws IllegalStateException If required configuration is missing
     *
     * @see TelegramChangelogTaskParams For available task parameters
     * @see TelegramChangelogConfig For available configuration options
     */
    internal fun registerChangelogTask(
        project: Project,
        config: TelegramChangelogConfig,
        params: TelegramChangelogTaskParams,
    ): TaskProvider<SendTelegramChangelogTask> {
        return project.registerSendTelegramChangelogTask(config, params)
    }

    /**
     * Registers a task for sending distribution notifications to Telegram.
     *
     * This method creates and configures a task that will send a notification about
     * a new app distribution to the configured Telegram chats when executed.
     *
     * @param project The Gradle project to register the task in
     * @param config Configuration for the distribution notification,
     *                          including destination bots and message formatting options
     * @param params Parameters for the distribution task, including build variant
     *              and APK file location
     * @return A TaskProvider for the registered TelegramDistributionTask, or null if
     *         no destination bots are configured
     *
     * @throws IllegalStateException If required configuration is missing
     *
     * @see TelegramApkDistributionTaskParams For available task parameters
     * @see TelegramDistributionConfig For available configuration options
     */
    internal fun registerApkDistributionTask(
        project: Project,
        config: TelegramDistributionConfig,
        params: TelegramApkDistributionTaskParams,
    ): TaskProvider<TelegramDistributionTask>? {
        return if (config.destinationBots.isPresent) {
            project.registerTelegramUploadAokTask(config, params)
        } else {
            logger.info(distributionTaskNotCreatedMessage())
            null
        }
    }

    /**
     * Registers a task for sending distribution notifications to Telegram.
     *
     * This method creates and configures a task that will send a notification about
     * a new app distribution to the configured Telegram chats when executed.
     *
     * @param project The Gradle project to register the task in
     * @param config Configuration for the distribution notification,
     *                          including destination bots and message formatting options
     * @param params Parameters for the distribution task, including build variant
     *              and Bundle file location
     * @return A TaskProvider for the registered TelegramDistributionTask, or null if
     *         no destination bots are configured
     *
     * @throws IllegalStateException If required configuration is missing
     *
     * @see TelegramBundleDistributionTaskParams For available task parameters
     * @see TelegramDistributionConfig For available configuration options
     */
    internal fun registerBundleDistributionTask(
        project: Project,
        config: TelegramDistributionConfig,
        params: TelegramBundleDistributionTaskParams,
    ): TaskProvider<TelegramDistributionTask>? {
        return if (config.destinationBots.isPresent) {
            project.registerTelegramBundleUploadTask(config, params)
        } else {
            logger.info(distributionBundleTaskNotCreatedMessage())
            null
        }
    }

    /**
     * Registers a Telegram lookup notification task in the project.
     *
     * This method creates and configures a task that will look up the specified
     * chatId and topicId when executed.
     *
     * @param project The Gradle project to register the task in
     * @param config Configuration for the lookup notification, including
     *                       destination bots and message formatting options
     * @param params Parameters for the lookup task, including build variant
     * @return A TaskProvider for the registered TelegramLookupTask, or null if
     *         no destination bots are configured
     *
     * @see TelegramLookupTask For the task implementation
     * @see TelegramLookupTaskParams For available task parameters
     */
    internal fun registerLookupTask(
        project: Project,
        config: TelegramLookupConfig,
        params: TelegramLookupTaskParams,
    ): TaskProvider<TelegramLookupTask>? {
        return project.registerTelegramLookupTask(config, params)
    }
}

/**
 * Registers a Telegram changelog notification task in the project.
 *
 * This extension function configures a [SendTelegramChangelogTask] with the provided
 * parameters and sets up its dependencies.
 *
 * The task will be configured to:
 * - Use the specified changelog file
 * - Format the message according to the provided configuration
 * - Send the notification to the configured Telegram chats
 *
 * @receiver The Gradle project to register the task in
 * @param changelogConfig Configuration for the changelog notification
 * @param params Parameters for the changelog task
 * @return A TaskProvider for the registered SendTelegramChangelogTask
 *
 * @see SendTelegramChangelogTask For the task implementation
 * @see TelegramChangelogTaskParams For available task parameters
 */
private fun Project.registerSendTelegramChangelogTask(
    changelogConfig: TelegramChangelogConfig,
    params: TelegramChangelogTaskParams,
): TaskProvider<SendTelegramChangelogTask> {
    return tasks.register(
        "$SEND_TELEGRAM_CHANGELOG_TASK_PREFIX${params.buildVariant.capitalizedName()}",
        SendTelegramChangelogTask::class.java,
    ) {
        val service =
            project.extensions
                .getByType(TelegramServiceExtension::class.java)
                .services
                .get()
                .getByNameOrCommon(params.buildVariant.name)

        it.changelogFile.set(params.changelogFile)
        it.buildTagFile.set(params.lastBuildTagFile)
        it.issueUrlPrefix.set(params.issueUrlPrefix)
        it.issueNumberPattern.set(params.issueNumberPattern)
        it.baseOutputFileName.set(params.baseFileName)
        it.userMentions.set(changelogConfig.userMentions)
        it.destinationBots.set(changelogConfig.destinationBots.map { it.mapToEntity().toJson() })
        it.service.set(service)

        it.dependsOn(params.changelogFile)
        it.usesService(service)
    }
}

/**
 * Registers a Telegram distribution upload notification task in the project.
 *
 * This extension function configures a [TelegramDistributionTask] with the provided
 * parameters and sets up its dependencies.
 *
 * The task will be configured to:
 * - Upload the specified APK file
 * - Format the notification message according to the provided configuration
 * - Send the notification to the configured Telegram chats
 *
 * @receiver The Gradle project to register the task in
 * @param distributionConfig Configuration for the distribution notification
 * @param params Parameters for the distribution task
 * @return A TaskProvider for the registered TelegramDistributionTask
 *
 * @see TelegramDistributionTask For the task implementation
 * @see TelegramApkDistributionTaskParams For available task parameters
 */
@Suppress("MaxLineLength") // One parameter function
private fun Project.registerTelegramUploadAokTask(
    distributionConfig: TelegramDistributionConfig,
    params: TelegramApkDistributionTaskParams,
): TaskProvider<TelegramDistributionTask> {
    return tasks.register(
        "$TELEGRAM_DISTRIBUTION_UPLOAD_APK_TASK_PREFIX${params.buildVariant.capitalizedName()}",
        TelegramDistributionTask::class.java,
    ) {
        val service =
            project.extensions
                .getByType(TelegramServiceExtension::class.java)
                .services
                .get()
                .getByNameOrCommon(params.buildVariant.name)

        it.distributionFile.set(params.apkOutputFile)
        it.destinationBots.set(distributionConfig.destinationBots.map { it.mapToEntity().toJson() })
        it.service.set(service)

        it.usesService(service)
    }
}

/**
 * Registers a Telegram distribution upload notification task in the project.
 *
 * This extension function configures a [TelegramDistributionTask] with the provided
 * parameters and sets up its dependencies.
 *
 * The task will be configured to:
 * - Upload the specified Bundle file
 * - Format the notification message according to the provided configuration
 * - Send the notification to the configured Telegram chats
 *
 * @receiver The Gradle project to register the task in
 * @param distributionConfig Configuration for the distribution notification
 * @param params Parameters for the distribution task
 * @return A TaskProvider for the registered TelegramDistributionTask
 *
 * @see TelegramDistributionTask For the task implementation
 * @see TelegramBundleDistributionTaskParams For available task parameters
 */
private fun Project.registerTelegramBundleUploadTask(
    distributionConfig: TelegramDistributionConfig,
    params: TelegramBundleDistributionTaskParams,
): TaskProvider<TelegramDistributionTask>? {
    return tasks.register(
        "$TELEGRAM_DISTRIBUTION_UPLOAD_BUNDLE_TASK_PREFIX${params.buildVariant.capitalizedName()}",
        TelegramDistributionTask::class.java,
    ) {
        val service =
            project.extensions
                .getByType(TelegramServiceExtension::class.java)
                .services
                .get()
                .getByNameOrCommon(params.buildVariant.name)

        it.distributionFile.set(params.bundleOutputFile)
        it.destinationBots.set(distributionConfig.destinationBots.map { it.mapToEntity().toJson() })
        it.service.set(service)

        it.usesService(service)
    }
}

/**
 * Registers a Telegram lookup notification task in the project.
 *
 * This extension function configures a [TelegramLookupTask] with the provided
 * parameters and sets up its dependencies.
 *
 * The task will be configured to:
 * - Lookup the specified chatId and topicId
 * - Format the notification message according to the provided configuration
 * - Send the notification to the configured Telegram chats
 *
 * @receiver The Gradle project to register the task in
 * @param distributionConfig Configuration for the lookup notification
 * @param params Parameters for the lookup task
 * @return A TaskProvider for the registered TelegramLookupTask
 *
 * @see TelegramLookupTask For the task implementation
 * @see TelegramLookupTaskParams For available task parameters
 */
private fun Project.registerTelegramLookupTask(
    distributionConfig: TelegramLookupConfig,
    params: TelegramLookupTaskParams,
): TaskProvider<TelegramLookupTask>? {
    return tasks.register(
        "$TELEGRAM_LOOKUP_TASK_PREFIX${params.buildVariant.capitalizedName()}",
        TelegramLookupTask::class.java,
    ) {
        val service =
            project.extensions
                .getByType(TelegramServiceExtension::class.java)
                .services
                .get()
                .getByNameOrCommon(params.buildVariant.name)

        it.botName.set(distributionConfig.botName)
        it.chatName.set(distributionConfig.chatName)
        it.topicName.set(distributionConfig.topicName)
        it.service.set(service)

        it.usesService(service)
    }
}

/**
 * Parameters for configuring a Telegram changelog notification task.
 *
 * This data class holds all the necessary parameters to configure a [SendTelegramChangelogTask].
 * It includes information about the build variant, changelog content, and issue tracking.
 *
 * @see SendTelegramChangelogTask For how these parameters are used
 */
internal data class TelegramChangelogTaskParams(
    /**
     * Base name for the build output.
     */
    val baseFileName: Provider<String>,
    /**
     * Pattern for matching issue numbers in the changelog.
     */
    val issueNumberPattern: Provider<String>,
    /**
     * Base URL for issue tracker links.
     */
    val issueUrlPrefix: Provider<String>,
    /**
     * The build variant this task is associated with.
     */
    val buildVariant: BuildVariant,
    /**
     * File containing the changelog text to send.
     */
    val changelogFile: Provider<RegularFile>,
    /**
     * File containing the last build tag information.
     */
    val lastBuildTagFile: Provider<RegularFile>,
)

/**
 * Parameters for configuring a Telegram lookup notification task.
 *
 * This data class holds all the necessary parameters to configure a [TelegramLookupTask].
 * It includes information about the build variant and the destination bot configuration.
 */
internal data class TelegramLookupTaskParams(
    /**
     * The build variant this task is associated with.
     */
    val buildVariant: BuildVariant,
)

/**
 * Parameters for configuring a Telegram distribution notification task.
 *
 * This data class holds all the necessary parameters to configure a [TelegramDistributionTask].
 * It includes information about the build variant and the APK file that was distributed.
 *
 * @see TelegramDistributionTask For how these parameters are used
 */
internal data class TelegramApkDistributionTaskParams(
    /**
     * Base name for the build output.
     *
     * This value is used to generate the final file name for the APK artifact.
     * It is concatenated with the build variant name to form the full file name.
     *
     * Example: If `baseFileName` is set to "MyApp", and the build variant is "debug",
     * the final file name will be "MyApp-debug.apk".
     */
    val baseFileName: Provider<String>,
    /**
     * The build variant this task is associated with.
     */
    val buildVariant: BuildVariant,
    /**
     * File containing the last build tag information.
     *
     * This file is typically generated by the [ru.kode.android.build.publish.plugin.foundation.task.tag.GetLastTagTask] task.
     */
    val lastBuildTag: Provider<RegularFile>,
    /**
     * The APK file that was distributed.
     */
    val apkOutputFile: Provider<RegularFile>,
)

/**
 * Parameters for configuring a Telegram distribution notification task.
 *
 * This data class holds all the necessary parameters to configure a [TelegramDistributionTask].
 * It includes information about the build variant and the Bundle file that was distributed.
 *
 * @see TelegramDistributionTask For how these parameters are used
 */
internal data class TelegramBundleDistributionTaskParams(
    /**
     * Base name for the build output.
     *
     * This value is used to generate the final file name for the Bundle artifact.
     * It is concatenated with the build variant name to form the full file name.
     *
     * Example: If `baseFileName` is set to "MyApp", and the build variant is "debug",
     * the final file name will be "MyApp-debug.bundle".
     */
    val baseFileName: Provider<String>,
    /**
     * The build variant this task is associated with.
     */
    val buildVariant: BuildVariant,
    /**
     * File containing the last build tag information.
     *
     * This file is typically generated by the [ru.kode.android.build.publish.plugin.foundation.task.tag.GetLastTagTask] task.
     */
    val lastBuildTag: Provider<RegularFile>,
    /**
     * The Bundle file that was distributed.
     */
    val bundleOutputFile: Provider<RegularFile>,
)
