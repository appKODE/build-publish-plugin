package ru.kode.android.build.publish.plugin.appcenter.task

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.appcenter.core.AppCenterDistributionConfig
import ru.kode.android.build.publish.plugin.appcenter.task.distribution.AppCenterDistributionTask
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName

internal const val APP_CENTER_DISTRIBUTION_UPLOAD_TASK_PREFIX = "appCenterDistributionUpload"

object AppCenterTasksRegistrar {

    fun registerDistributionTask(
        project: TaskContainer,
        config: AppCenterDistributionConfig,
        params: AppCenterDistributionTaskParams
    ): TaskProvider<AppCenterDistributionTask> {
        return project.registerAppCenterDistributionTask(config, params)
    }
}


private fun TaskContainer.registerAppCenterDistributionTask(
    config: AppCenterDistributionConfig,
    params: AppCenterDistributionTaskParams,
): TaskProvider<AppCenterDistributionTask> {
    val buildVariant = params.buildVariant

    return register(
        "$APP_CENTER_DISTRIBUTION_UPLOAD_TASK_PREFIX${buildVariant.capitalizedName()}",
        AppCenterDistributionTask::class.java,
    ) {
        it.tagBuildFile.set(params.tagBuildProvider)
        it.buildVariantOutputFile.set(params.apkOutputFileProvider)
        it.changelogFile.set(params.changelogFileProvider)
        it.apiTokenFile.set(config.apiTokenFile)
        it.ownerName.set(config.ownerName)
        it.appName.set(config.appName)
        it.baseFileName.set(params.baseFileName)
        it.testerGroups.set(config.testerGroups)
        it.maxUploadStatusRequestCount.set(config.maxUploadStatusRequestCount)
        it.uploadStatusRequestDelayMs.set(config.uploadStatusRequestDelayMs)
        it.uploadStatusRequestDelayCoefficient.set(config.uploadStatusRequestDelayCoefficient)
    }
}

data class AppCenterDistributionTaskParams(
    val buildVariant: BuildVariant,
    val changelogFileProvider: Provider<RegularFile>,
    val apkOutputFileProvider: Provider<RegularFile>,
    val tagBuildProvider: Provider<RegularFile>,
    val baseFileName: Property<String>,
)
