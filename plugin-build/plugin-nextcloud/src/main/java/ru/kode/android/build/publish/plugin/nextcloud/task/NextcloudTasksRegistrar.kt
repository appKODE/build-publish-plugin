package ru.kode.android.build.publish.plugin.nextcloud.task

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.entity.BuildVariant
import ru.kode.android.build.publish.plugin.core.logger.LoggerServiceExtension
import ru.kode.android.build.publish.plugin.core.task.GenerateChangelogTaskOutput
import ru.kode.android.build.publish.plugin.core.task.GetLastTagSnapshotTaskOutput
import ru.kode.android.build.publish.plugin.core.task.TaskNames
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.core.util.getByNameOrCommon
import ru.kode.android.build.publish.plugin.nextcloud.config.NextcloudDistributionConfig
import ru.kode.android.build.publish.plugin.nextcloud.service.NextcloudServiceExtension
import ru.kode.android.build.publish.plugin.nextcloud.task.changelog.NextcloudChangelogTask
import ru.kode.android.build.publish.plugin.nextcloud.task.distribution.NextcloudDistributionTask

internal object NextcloudTasksRegistrar {
    internal fun registerApkDistributionTask(
        project: Project,
        distributionConfig: NextcloudDistributionConfig,
        params: NextcloudApkDistributionTaskParams,
    ): TaskProvider<NextcloudDistributionTask> {
        return project.registerApkNextcloudDistributionTask(distributionConfig, params)
    }

    internal fun registerBundleDistributionTask(
        project: Project,
        distributionConfig: NextcloudDistributionConfig,
        params: NextcloudBundleDistributionTaskParams,
    ): TaskProvider<NextcloudDistributionTask>? {
        return project.registerBundleNextcloudDistributionTask(distributionConfig, params)
    }

    internal fun registerChangelogTask(
        project: Project,
        distributionConfig: NextcloudDistributionConfig,
        params: NextcloudChangelogTaskParams,
    ): TaskProvider<NextcloudChangelogTask> {
        return project.registerNextcloudChangelogTask(distributionConfig, params)
    }
}

private fun Project.registerApkNextcloudDistributionTask(
    distributionConfig: NextcloudDistributionConfig,
    params: NextcloudApkDistributionTaskParams,
): TaskProvider<NextcloudDistributionTask> {
    return tasks.register(
        "${TaskNames.Nextcloud.DISTRIBUTION_UPLOAD_PREFIX}${params.buildVariant.capitalizedName()}",
        NextcloudDistributionTask::class.java,
    ) {
        val service =
            project.extensions
                .getByType(NextcloudServiceExtension::class.java)
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
        it.shareMode.set(distributionConfig.shareMode)
        it.userRecipients.addAll(distributionConfig.userRecipients)
        it.groupRecipients.addAll(distributionConfig.groupRecipients)
        it.remoteFileName.set(distributionConfig.remoteFileName)
        it.service.set(service)
        it.loggerService.set(loggerService)

        it.usesService(service)
        it.usesService(loggerService)
        it.dependsOn(params.buildTagSnapshotProvider)
    }
}

private fun Project.registerBundleNextcloudDistributionTask(
    distributionConfig: NextcloudDistributionConfig,
    params: NextcloudBundleDistributionTaskParams,
): TaskProvider<NextcloudDistributionTask>? {
    return tasks.register(
        "${TaskNames.Nextcloud.DISTRIBUTION_UPLOAD_BUNDLE_PREFIX}${params.buildVariant.capitalizedName()}",
        NextcloudDistributionTask::class.java,
    ) {
        val service =
            project.extensions
                .getByType(NextcloudServiceExtension::class.java)
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
        it.shareMode.set(distributionConfig.shareMode)
        it.userRecipients.addAll(distributionConfig.userRecipients)
        it.groupRecipients.addAll(distributionConfig.groupRecipients)
        it.remoteFileName.set(distributionConfig.remoteFileName)
        it.service.set(service)
        it.loggerService.set(loggerService)

        it.usesService(service)
        it.usesService(loggerService)
        it.dependsOn(params.buildTagSnapshotProvider)
    }
}

private fun Project.registerNextcloudChangelogTask(
    distributionConfig: NextcloudDistributionConfig,
    params: NextcloudChangelogTaskParams,
): TaskProvider<NextcloudChangelogTask> {
    return tasks.register(
        "${TaskNames.Nextcloud.CHANGELOG_UPLOAD_PREFIX}${params.buildVariant.capitalizedName()}",
        NextcloudChangelogTask::class.java,
    ) {
        val service =
            project.extensions
                .getByType(NextcloudServiceExtension::class.java)
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
        it.shareMode.set(distributionConfig.shareMode)
        it.userRecipients.addAll(distributionConfig.userRecipients)
        it.groupRecipients.addAll(distributionConfig.groupRecipients)
        it.remoteFileName.set(distributionConfig.remoteFileName)
        it.service.set(service)
        it.loggerService.set(loggerService)

        it.usesService(service)
        it.usesService(loggerService)
        it.dependsOn(params.buildTagSnapshotProvider, params.changelogFileProvider)
    }
}

internal data class NextcloudApkDistributionTaskParams(
    val buildVariant: BuildVariant,
    val apkOutputFile: Provider<RegularFile>,
    val buildTagSnapshotProvider: Provider<out GetLastTagSnapshotTaskOutput>,
    val baseFileName: Provider<String>,
    val buildVariantDefaultVersionName: String?,
)

internal data class NextcloudBundleDistributionTaskParams(
    val buildVariant: BuildVariant,
    val bundleOutputFile: Provider<RegularFile>,
    val buildTagSnapshotProvider: Provider<out GetLastTagSnapshotTaskOutput>,
    val baseFileName: Provider<String>,
    val buildVariantDefaultVersionName: String?,
)

internal data class NextcloudChangelogTaskParams(
    val buildVariant: BuildVariant,
    val changelogFileProvider: TaskProvider<out GenerateChangelogTaskOutput>,
    val buildTagSnapshotProvider: Provider<out GetLastTagSnapshotTaskOutput>,
    val baseFileName: Provider<String>,
    val buildVariantDefaultVersionName: String?,
)
