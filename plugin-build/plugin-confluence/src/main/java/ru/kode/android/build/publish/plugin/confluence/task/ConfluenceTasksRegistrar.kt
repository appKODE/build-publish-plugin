package ru.kode.android.build.publish.plugin.confluence.task

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.confluence.config.ConfluenceDistributionConfig
import ru.kode.android.build.publish.plugin.confluence.service.ConfluenceServiceExtension
import ru.kode.android.build.publish.plugin.confluence.task.distribution.ConfluenceDistributionTask
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.logger.LoggerServiceExtension
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.core.util.getByNameOrCommon

internal const val CONFLUENCE_DISTRIBUTION_UPLOAD_TASK_PREFIX = "confluenceDistributionUpload"
internal const val CONFLUENCE_DISTRIBUTION_UPLOAD_BUNDLE_TASK_PREFIX = "confluenceDistributionUploadBundle"

/**
 * Registers and configures Confluence-related tasks for the build.
 */
internal object ConfluenceTasksRegistrar {
    /**
     * Registers a Confluence APK distribution upload task for the given configuration.
     *
     * @param project The Gradle project to register the task in
     * @param distributionConfig Configuration for the Confluence distribution
     * @param params Parameters for the distribution task
     *
     * @return A [TaskProvider] for the registered task
     */
    internal fun registerApkDistributionTask(
        project: Project,
        distributionConfig: ConfluenceDistributionConfig,
        params: ConfluenceApkDistributionTaskParams,
    ): TaskProvider<ConfluenceDistributionTask> {
        return project.registerApkConfluenceDistributionTask(distributionConfig, params)
    }

    /**
     * Registers a Confluence Bundle distribution upload task for the given configuration.
     *
     * @param project The Gradle project to register the task in
     * @param distributionConfig Configuration for the Confluence distribution
     * @param params Parameters for the distribution task
     *
     * @return A [TaskProvider] for the registered task
     */
    internal fun registerBundleDistributionTask(
        project: Project,
        distributionConfig: ConfluenceDistributionConfig,
        params: ConfluenceBundleDistributionTaskParams,
    ): TaskProvider<ConfluenceDistributionTask>? {
        return project.registerBundleConfluenceDistributionTask(distributionConfig, params)
    }
}

/**
 * Registers a APK Confluence distribution task for the current project.
 *
 * @receiver The project to register the task in
 * @param distributionConfig Configuration for the Confluence distribution
 * @param params Parameters for the distribution task
 *
 * @return A [TaskProvider] for the registered task
 */
private fun Project.registerApkConfluenceDistributionTask(
    distributionConfig: ConfluenceDistributionConfig,
    params: ConfluenceApkDistributionTaskParams,
): TaskProvider<ConfluenceDistributionTask> {
    return tasks.register(
        "$CONFLUENCE_DISTRIBUTION_UPLOAD_TASK_PREFIX${params.buildVariant.capitalizedName()}",
        ConfluenceDistributionTask::class.java,
    ) {
        val service =
            project.extensions
                .getByType(ConfluenceServiceExtension::class.java)
                .services
                .get()
                .getByNameOrCommon(params.buildVariant.name)
        val loggerService =
            project.extensions
                .getByType(LoggerServiceExtension::class.java)
                .service

        it.distributionFile.set(params.apkOutputFile)
        it.pageId.set(distributionConfig.pageId)
        it.compressed.set(distributionConfig.compressed)
        it.service.set(service)
        it.loggerService.set(loggerService)

        it.usesService(service)
        it.usesService(loggerService)
    }
}

/**
 * Registers a Bundle Confluence distribution task for the current project.
 *
 * @receiver The project to register the task in
 * @param distributionConfig Configuration for the Confluence distribution
 * @param params Parameters for the distribution task
 *
 * @return A [TaskProvider] for the registered task
 */
private fun Project.registerBundleConfluenceDistributionTask(
    distributionConfig: ConfluenceDistributionConfig,
    params: ConfluenceBundleDistributionTaskParams,
): TaskProvider<ConfluenceDistributionTask>? {
    return tasks.register(
        "$CONFLUENCE_DISTRIBUTION_UPLOAD_BUNDLE_TASK_PREFIX${params.buildVariant.capitalizedName()}",
        ConfluenceDistributionTask::class.java,
    ) {
        val service =
            project.extensions
                .getByType(ConfluenceServiceExtension::class.java)
                .services
                .get()
                .getByNameOrCommon(params.buildVariant.name)
        val loggerService =
            project.extensions
                .getByType(LoggerServiceExtension::class.java)
                .service

        it.distributionFile.set(params.bundleOutputFile)
        it.pageId.set(distributionConfig.pageId)
        it.compressed.set(distributionConfig.compressed)
        it.service.set(service)
        it.loggerService.set(loggerService)

        it.usesService(service)
        it.usesService(loggerService)
    }
}

/**
 * Parameters required to create a APK Confluence distribution task.
 */
internal data class ConfluenceApkDistributionTaskParams(
    /**
     * The build variant this task is associated with
     */
    val buildVariant: BuildVariant,
    /**
     * Provider for the APK output file to upload
     */
    val apkOutputFile: Provider<RegularFile>,
)

/**
 * Parameters required to create a Bundle Confluence distribution task.
 */
internal data class ConfluenceBundleDistributionTaskParams(
    /**
     * The build variant this task is associated with
     */
    val buildVariant: BuildVariant,
    /**
     * Provider for the Bundle output file to upload
     */
    val bundleOutputFile: Provider<RegularFile>,
)
