package ru.kode.android.build.publish.plugin.play.task

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.task.GetLastTagTaskOutput
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.core.util.flatMapByNameOrCommon
import ru.kode.android.build.publish.plugin.play.config.PlayDistributionConfig
import ru.kode.android.build.publish.plugin.play.service.PlayServiceExtension
import ru.kode.android.build.publish.plugin.play.task.distribution.PlayDistributionTask

internal const val PLAY_DISTRIBUTION_UPLOAD_TASK_PREFIX = "playUpload"

/**
 * Utility object for registering Play Store related Gradle tasks.
 *
 * This object provides methods to register different types of Play Store tasks:
 * - Distribution upload tasks for publishing to the Play Store
 * - Task configuration based on build variants and distribution settings
 *
 * It handles task creation and configuration based on the provided parameters and build variants.
 */
internal object PlayTasksRegistrar {
    /**
     * Registers a task for uploading an app bundle to the Google Play Store.
     *
     * @param project The project to register the task in
     * @param distributionConfig Configuration for the Play Store distribution
     * @param params Parameters for the distribution task
     *
     * @return A TaskProvider for the registered PlayDistributionTask
     */
    internal fun registerDistributionTask(
        project: Project,
        distributionConfig: PlayDistributionConfig,
        params: PlayTaskParams,
    ): TaskProvider<PlayDistributionTask> {
        return project.registerPlayDistributionTask(distributionConfig, params)
    }
}

/**
 * Registers a Play Store distribution upload task in the project.
 *
 * @param distributionConfig Configuration for the Play Store distribution
 * @param params Parameters for the distribution task
 *
 * @return A TaskProvider for the registered PlayDistributionTask
 */
private fun Project.registerPlayDistributionTask(
    distributionConfig: PlayDistributionConfig,
    params: PlayTaskParams,
): TaskProvider<PlayDistributionTask> {
    val buildVariant = params.buildVariant

    return tasks.register(
        "$PLAY_DISTRIBUTION_UPLOAD_TASK_PREFIX${buildVariant.capitalizedName()}",
        PlayDistributionTask::class.java,
    ) {
        val networkService =
            extensions
                .getByType(PlayServiceExtension::class.java)
                .networkServices
                .flatMapByNameOrCommon(params.buildVariant.name)

        it.buildTagFile.set(params.lastBuildTagFileProvider.flatMap { it.tagBuildFile })
        it.distributionFile.set(params.bundleOutputFile)
        it.trackId.set(distributionConfig.trackId)
        it.updatePriority.set(distributionConfig.updatePriority)
        it.networkService.set(networkService)

        it.dependsOn(params.lastBuildTagFileProvider)
    }
}

/**
 * Parameters for configuring a Play Store distribution task.
 */
internal data class PlayTaskParams(
    /**
     * The build variant this task is associated with
     */
    val buildVariant: BuildVariant,
    /**
     * The app bundle file to upload to the Play Store
     */
    val bundleOutputFile: Provider<RegularFile>,
    /**
     * File containing the last build tag for change tracking
     */
    val lastBuildTagFileProvider: Provider<out GetLastTagTaskOutput>,
)
