package ru.kode.android.build.publish.plugin.yandexdisk.task

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.entity.BuildVariant
import ru.kode.android.build.publish.plugin.core.task.GenerateChangelogTaskOutput
import ru.kode.android.build.publish.plugin.core.task.GetLastTagSnapshotTaskOutput
import ru.kode.android.build.publish.plugin.core.task.TaskNames
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.yandexdisk.config.YandexDiskDistributionConfig
import ru.kode.android.build.publish.plugin.yandexdisk.service.YandexDiskServiceExtension
import ru.kode.android.build.publish.plugin.yandexdisk.task.changelog.YandexDiskChangelogTask
import ru.kode.android.build.publish.plugin.yandexdisk.task.distribution.YandexDiskDistributionTask
import ru.kode.android.gradle.commons.logger.LoggerServiceExtension
import ru.kode.android.gradle.commons.util.getByNameOrCommon

internal object YandexDiskTasksRegistrar {
    internal fun registerApkDistributionTask(
        project: Project,
        distributionConfig: YandexDiskDistributionConfig,
        params: YandexDiskApkDistributionTaskParams,
    ): TaskProvider<YandexDiskDistributionTask> =
        project.registerApkYandexDiskDistributionTask(distributionConfig, params)

    internal fun registerBundleDistributionTask(
        project: Project,
        distributionConfig: YandexDiskDistributionConfig,
        params: YandexDiskBundleDistributionTaskParams,
    ): TaskProvider<YandexDiskDistributionTask>? =
        project.registerBundleYandexDiskDistributionTask(distributionConfig, params)

    internal fun registerChangelogTask(
        project: Project,
        distributionConfig: YandexDiskDistributionConfig,
        params: YandexDiskChangelogTaskParams,
    ): TaskProvider<YandexDiskChangelogTask> = project.registerYandexDiskChangelogTask(distributionConfig, params)
}

private fun Project.registerApkYandexDiskDistributionTask(
    distributionConfig: YandexDiskDistributionConfig,
    params: YandexDiskApkDistributionTaskParams,
): TaskProvider<YandexDiskDistributionTask> =
    tasks.register(
        "${TaskNames.YandexDisk.DISTRIBUTION_UPLOAD_PREFIX}${params.buildVariant.capitalizedName()}",
        YandexDiskDistributionTask::class.java,
    ) {
        val service =
            project.extensions
                .getByType(YandexDiskServiceExtension::class.java)
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
        it.remotePath.set(distributionConfig.remotePath)
        it.compressed.set(distributionConfig.compressed)
        it.publicLink.set(distributionConfig.publicLink)
        it.remoteFileName.set(distributionConfig.remoteFileName)
        it.service.set(service)
        it.loggerService.set(loggerService)

        it.usesService(service)
        it.usesService(loggerService)
        it.dependsOn(params.buildTagSnapshotProvider)
    }

private fun Project.registerBundleYandexDiskDistributionTask(
    distributionConfig: YandexDiskDistributionConfig,
    params: YandexDiskBundleDistributionTaskParams,
): TaskProvider<YandexDiskDistributionTask>? =
    tasks.register(
        "${TaskNames.YandexDisk.DISTRIBUTION_UPLOAD_BUNDLE_PREFIX}${params.buildVariant.capitalizedName()}",
        YandexDiskDistributionTask::class.java,
    ) {
        val service =
            project.extensions
                .getByType(YandexDiskServiceExtension::class.java)
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
        it.remotePath.set(distributionConfig.remotePath)
        it.compressed.set(distributionConfig.compressed)
        it.publicLink.set(distributionConfig.publicLink)
        it.remoteFileName.set(distributionConfig.remoteFileName)
        it.service.set(service)
        it.loggerService.set(loggerService)

        it.usesService(service)
        it.usesService(loggerService)
        it.dependsOn(params.buildTagSnapshotProvider)
    }

private fun Project.registerYandexDiskChangelogTask(
    distributionConfig: YandexDiskDistributionConfig,
    params: YandexDiskChangelogTaskParams,
): TaskProvider<YandexDiskChangelogTask> =
    tasks.register(
        "${TaskNames.YandexDisk.CHANGELOG_UPLOAD_PREFIX}${params.buildVariant.capitalizedName()}",
        YandexDiskChangelogTask::class.java,
    ) {
        val service =
            project.extensions
                .getByType(YandexDiskServiceExtension::class.java)
                .services
                .get()
                .getByNameOrCommon(params.buildVariant.name)
        val loggerService =
            project.extensions
                .getByType(LoggerServiceExtension::class.java)
                .service

        it.changelogFile.set(params.changelogFileProvider.flatMap { provider -> provider.changelogFile })
        it.buildTagSnapshotFile.set(
            params.buildTagSnapshotProvider.flatMap { provider -> provider.buildTagSnapshotFile },
        )
        it.baseOutputFileName.set(params.baseFileName)
        it.buildVariantName.set(params.buildVariant.name)
        it.buildVariantDefaultVersionName.set(params.buildVariantDefaultVersionName)
        it.remotePath.set(distributionConfig.remotePath)
        it.publicLink.set(distributionConfig.publicLink)
        it.remoteFileName.set(distributionConfig.remoteFileName)
        it.service.set(service)
        it.loggerService.set(loggerService)

        it.usesService(service)
        it.usesService(loggerService)
        it.dependsOn(params.buildTagSnapshotProvider, params.changelogFileProvider)
    }

internal data class YandexDiskApkDistributionTaskParams(
    val buildVariant: BuildVariant,
    val apkOutputFile: Provider<RegularFile>,
    val buildTagSnapshotProvider: Provider<out GetLastTagSnapshotTaskOutput>,
    val baseFileName: Provider<String>,
    val buildVariantDefaultVersionName: String?,
)

internal data class YandexDiskBundleDistributionTaskParams(
    val buildVariant: BuildVariant,
    val bundleOutputFile: Provider<RegularFile>,
    val buildTagSnapshotProvider: Provider<out GetLastTagSnapshotTaskOutput>,
    val baseFileName: Provider<String>,
    val buildVariantDefaultVersionName: String?,
)

internal data class YandexDiskChangelogTaskParams(
    val buildVariant: BuildVariant,
    val changelogFileProvider: TaskProvider<out GenerateChangelogTaskOutput>,
    val buildTagSnapshotProvider: Provider<out GetLastTagSnapshotTaskOutput>,
    val baseFileName: Provider<String>,
    val buildVariantDefaultVersionName: String?,
)
