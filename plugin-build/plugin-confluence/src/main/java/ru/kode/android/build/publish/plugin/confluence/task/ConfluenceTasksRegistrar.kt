package ru.kode.android.build.publish.plugin.confluence.task

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.confluence.config.ConfluenceDistributionConfig
import ru.kode.android.build.publish.plugin.confluence.service.ConfluenceServiceExtension
import ru.kode.android.build.publish.plugin.confluence.task.distribution.ConfluenceDistributionTask
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.core.util.flatMapByNameOrCommon

internal const val CONFLUENCE_DISTRIBUTION_UPLOAD_TASK_PREFIX = "confluenceDistributionUpload"

/**
 * Registers and configures Confluence-related tasks for the build.
 */
internal object ConfluenceTasksRegistrar {
    /**
     * Registers a Confluence distribution upload task for the given configuration.
     *
     * @param project The Gradle project to register the task in
     * @param distributionConfig Configuration for the Confluence distribution
     * @param params Parameters for the distribution task
     *
     * @return A [TaskProvider] for the registered task
     */
    internal fun registerDistributionTask(
        project: Project,
        distributionConfig: ConfluenceDistributionConfig,
        params: ConfluenceDistributionTaskParams,
    ): TaskProvider<ConfluenceDistributionTask> {
        return project.registerConfluenceDistributionTask(distributionConfig, params)
    }
}

/**
 * Registers a Confluence distribution task for the current project.
 *
 * @receiver The project to register the task in
 * @param distributionConfig Configuration for the Confluence distribution
 * @param params Parameters for the distribution task
 *
 * @return A [TaskProvider] for the registered task
 */
private fun Project.registerConfluenceDistributionTask(
    distributionConfig: ConfluenceDistributionConfig,
    params: ConfluenceDistributionTaskParams,
): TaskProvider<ConfluenceDistributionTask> {
    return tasks.register(
        "$CONFLUENCE_DISTRIBUTION_UPLOAD_TASK_PREFIX${params.buildVariant.capitalizedName()}",
        ConfluenceDistributionTask::class.java,
    ) {
        val networkService =
            project.extensions
                .getByType(ConfluenceServiceExtension::class.java)
                .networkServices
                .flatMapByNameOrCommon(params.buildVariant.name)

        it.distributionFile.set(params.apkOutputFile)
        it.pageId.set(distributionConfig.pageId)
        it.networkService.set(networkService)
    }
}

/**
 * Parameters required to create a Confluence distribution task.
 */
internal data class ConfluenceDistributionTaskParams(
    /**
     * The build variant this task is associated with
     */
    val buildVariant: BuildVariant,
    /**
     * Provider for the APK output file to upload
     */
    val apkOutputFile: Provider<RegularFile>,
)
