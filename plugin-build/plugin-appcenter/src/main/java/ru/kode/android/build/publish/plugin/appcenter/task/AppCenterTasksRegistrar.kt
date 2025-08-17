package ru.kode.android.build.publish.plugin.appcenter.task

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.appcenter.config.AppCenterDistributionConfig
import ru.kode.android.build.publish.plugin.appcenter.service.AppCenterServiceExtension
import ru.kode.android.build.publish.plugin.appcenter.task.distribution.AppCenterDistributionTask
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.core.util.flatMapByNameOrCommon

internal const val APP_CENTER_DISTRIBUTION_UPLOAD_TASK_PREFIX = "appCenterDistributionUpload"

/**
 * Utility responsible for registering AppCenter distribution upload tasks for Android build variants.
 *
 * This class handles the creation and configuration of [AppCenterDistributionTask] instances
 * for different build variants with their respective configurations.
 *
 * @see AppCenterDistributionTask
 * @see AppCenterDistributionConfig
 */
internal object AppCenterTasksRegistrar {
    /**
     * Registers a new AppCenter distribution upload task for the given configuration and parameters.
     *
     * @param project The Gradle project to register the task in
     * @param distributionConfig The AppCenter distribution configuration
     * @param params Parameters required for the distribution task
     *
     * @return A [TaskProvider] for the registered [AppCenterDistributionTask]
     */
    internal fun registerDistributionTask(
        project: Project,
        distributionConfig: AppCenterDistributionConfig,
        params: AppCenterDistributionTaskParams,
    ): TaskProvider<AppCenterDistributionTask> {
        return project.registerAppCenterDistributionTask(distributionConfig, params)
    }
}

/**
 * Extension function to register an AppCenter distribution task on a project.
 *
 * This function configures the task with all necessary inputs including:
 * - Build variant information
 * - Distribution configuration
 * - Network service for AppCenter API communication
 * - File paths for APK, changelog, and build tags
 *
 * @param distributionConfig Configuration for the AppCenter distribution
 * @param params Task parameters including file paths and build variant
 *
 * @return A [TaskProvider] for the configured [AppCenterDistributionTask]
 */
private fun Project.registerAppCenterDistributionTask(
    distributionConfig: AppCenterDistributionConfig,
    params: AppCenterDistributionTaskParams,
): TaskProvider<AppCenterDistributionTask> {
    val buildVariant = params.buildVariant

    return tasks.register(
        "$APP_CENTER_DISTRIBUTION_UPLOAD_TASK_PREFIX${buildVariant.capitalizedName()}",
        AppCenterDistributionTask::class.java,
    ) {
        val networkService =
            project.extensions
                .getByType(AppCenterServiceExtension::class.java)
                .networkServices
                .flatMapByNameOrCommon(params.buildVariant.name)

        it.buildTagFile.set(params.lastBuildTagFile)
        it.distributionFile.set(params.apkOutputFile)
        it.changelogFile.set(params.changelogFile)
        it.appName.set(distributionConfig.appName)
        it.baseFileName.set(params.baseFileName)
        it.testerGroups.set(distributionConfig.testerGroups)
        it.maxUploadStatusRequestCount.set(distributionConfig.maxUploadStatusRequestCount)
        it.uploadStatusRequestDelayMs.set(distributionConfig.uploadStatusRequestDelayMs)
        it.uploadStatusRequestDelayCoefficient.set(distributionConfig.uploadStatusRequestDelayCoefficient)
        it.networkService.set(networkService)
    }
}

/**
 * Data class holding parameters required to configure an AppCenter distribution task.
 */
internal data class AppCenterDistributionTaskParams(
    /**
     * The build variant this task is for
     */
    val buildVariant: BuildVariant,
    /**
     * Provider for the changelog file to include with the distribution
     */
    val changelogFile: Provider<RegularFile>,
    /**
     * Provider for the APK file to upload
     */
    val apkOutputFile: Provider<RegularFile>,
    /**
     * Provider for the file containing the last build tag
     */
    val lastBuildTagFile: Provider<RegularFile>,
    /**
     * Base name used for generating output files
     */
    val baseFileName: Provider<String>,
)
