package ru.kode.android.build.publish.plugin.vk.task

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.entity.BuildVariant
import ru.kode.android.build.publish.plugin.core.task.GetLastTagSnapshotTaskOutput
import ru.kode.android.build.publish.plugin.core.task.TaskNames
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.vk.config.VkDistributionConfig
import ru.kode.android.build.publish.plugin.vk.service.VkServiceExtension
import ru.kode.android.build.publish.plugin.vk.task.distribution.VkDistributionTask
import ru.kode.android.gradle.commons.logger.LoggerServiceExtension
import ru.kode.android.gradle.commons.util.getByNameOrCommon

internal object VkTasksRegistrar {
    internal fun registerApkDistributionTask(
        project: Project,
        distributionConfig: VkDistributionConfig,
        params: VkApkDistributionTaskParams,
    ): TaskProvider<VkDistributionTask> = project.registerApkVkDistributionTask(distributionConfig, params)

    internal fun registerBundleDistributionTask(
        project: Project,
        distributionConfig: VkDistributionConfig,
        params: VkBundleDistributionTaskParams,
    ): TaskProvider<VkDistributionTask>? = project.registerBundleVkDistributionTask(distributionConfig, params)
}

private fun Project.registerApkVkDistributionTask(
    distributionConfig: VkDistributionConfig,
    params: VkApkDistributionTaskParams,
): TaskProvider<VkDistributionTask> =
    tasks.register(
        "${TaskNames.Vk.DISTRIBUTION_UPLOAD_PREFIX}${params.buildVariant.capitalizedName()}",
        VkDistributionTask::class.java,
    ) {
        val service =
            project.extensions
                .getByType(VkServiceExtension::class.java)
                .services
                .get()
                .getByNameOrCommon(params.buildVariant.name)
        val loggerService =
            project.extensions
                .getByType(LoggerServiceExtension::class.java)
                .service

        it.distributionFile.set(params.apkOutputFile)
        it.buildTagSnapshotFile.set(
            params.buildTagSnapshotProvider.flatMap { provider -> provider.buildTagSnapshotFile },
        )
        it.baseOutputFileName.set(params.baseFileName)
        it.buildVariantName.set(params.buildVariant.name)
        it.buildVariantDefaultVersionName.set(params.buildVariantDefaultVersionName)
        it.peerId.set(distributionConfig.peerId)
        it.compressed.set(distributionConfig.compressed)
        it.message.set(distributionConfig.message)
        it.title.set(distributionConfig.title)
        it.service.set(service)
        it.loggerService.set(loggerService)

        it.usesService(service)
        it.usesService(loggerService)
        it.dependsOn(params.buildTagSnapshotProvider)
    }

private fun Project.registerBundleVkDistributionTask(
    distributionConfig: VkDistributionConfig,
    params: VkBundleDistributionTaskParams,
): TaskProvider<VkDistributionTask>? =
    tasks.register(
        "${TaskNames.Vk.DISTRIBUTION_UPLOAD_BUNDLE_PREFIX}${params.buildVariant.capitalizedName()}",
        VkDistributionTask::class.java,
    ) {
        val service =
            project.extensions
                .getByType(VkServiceExtension::class.java)
                .services
                .get()
                .getByNameOrCommon(params.buildVariant.name)
        val loggerService =
            project.extensions
                .getByType(LoggerServiceExtension::class.java)
                .service

        it.distributionFile.set(params.bundleOutputFile)
        it.buildTagSnapshotFile.set(
            params.buildTagSnapshotProvider.flatMap { provider -> provider.buildTagSnapshotFile },
        )
        it.baseOutputFileName.set(params.baseFileName)
        it.buildVariantName.set(params.buildVariant.name)
        it.buildVariantDefaultVersionName.set(params.buildVariantDefaultVersionName)
        it.peerId.set(distributionConfig.peerId)
        it.compressed.set(distributionConfig.compressed)
        it.message.set(distributionConfig.message)
        it.title.set(distributionConfig.title)
        it.service.set(service)
        it.loggerService.set(loggerService)

        it.usesService(service)
        it.usesService(loggerService)
        it.dependsOn(params.buildTagSnapshotProvider)
    }

internal data class VkApkDistributionTaskParams(
    val buildVariant: BuildVariant,
    val apkOutputFile: Provider<RegularFile>,
    val buildTagSnapshotProvider: Provider<out GetLastTagSnapshotTaskOutput>,
    val baseFileName: Provider<String>,
    val buildVariantDefaultVersionName: String?,
)

internal data class VkBundleDistributionTaskParams(
    val buildVariant: BuildVariant,
    val bundleOutputFile: Provider<RegularFile>,
    val buildTagSnapshotProvider: Provider<out GetLastTagSnapshotTaskOutput>,
    val baseFileName: Provider<String>,
    val buildVariantDefaultVersionName: String?,
)
