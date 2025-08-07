package ru.kode.android.build.publish.plugin.appcenter.task

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.appcenter.config.AppCenterDistributionConfig
import ru.kode.android.build.publish.plugin.appcenter.service.AppCenterServiceExtension
import ru.kode.android.build.publish.plugin.appcenter.task.distribution.AppCenterDistributionTask
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.core.util.flatMapByNameOrDefault

internal const val APP_CENTER_DISTRIBUTION_UPLOAD_TASK_PREFIX = "appCenterDistributionUpload"

object AppCenterTasksRegistrar {
    fun registerDistributionTask(
        project: Project,
        distributionConfig: AppCenterDistributionConfig,
        params: AppCenterDistributionTaskParams,
    ): TaskProvider<AppCenterDistributionTask> {
        return project.registerAppCenterDistributionTask(distributionConfig, params)
    }
}

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
                .flatMapByNameOrDefault(params.buildVariant.name)

        it.tagBuildFile.set(params.tagBuildProvider)
        it.buildVariantOutputFile.set(params.apkOutputFileProvider)
        it.changelogFile.set(params.changelogFileProvider)
        it.appName.set(distributionConfig.appName)
        it.baseFileName.set(params.baseFileName)
        it.testerGroups.set(distributionConfig.testerGroups)
        it.maxUploadStatusRequestCount.set(distributionConfig.maxUploadStatusRequestCount)
        it.uploadStatusRequestDelayMs.set(distributionConfig.uploadStatusRequestDelayMs)
        it.uploadStatusRequestDelayCoefficient.set(distributionConfig.uploadStatusRequestDelayCoefficient)
        it.networkService.set(networkService)
    }
}

data class AppCenterDistributionTaskParams(
    val buildVariant: BuildVariant,
    val changelogFileProvider: Provider<RegularFile>,
    val apkOutputFileProvider: Provider<RegularFile>,
    val tagBuildProvider: Provider<RegularFile>,
    val baseFileName: Property<String>,
)
