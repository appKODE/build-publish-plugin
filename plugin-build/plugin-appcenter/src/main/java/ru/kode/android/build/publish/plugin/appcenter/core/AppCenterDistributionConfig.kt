package ru.kode.android.build.publish.plugin.appcenter.core

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.appcenter.task.AppCenterDistributionTask
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName

internal const val APP_CENTER_DISTRIBUTION_UPLOAD_TASK_PREFIX = "appCenterDistributionUpload"

internal const val MAX_REQUEST_COUNT = 20
internal const val MAX_REQUEST_DELAY_MS = 2000L

interface AppCenterDistributionConfig {
    val name: String

    /**
     * The path to JSON file with token for App Center project
     */
    @get:InputFile
    val apiTokenFile: RegularFileProperty

    /**
     * Owner name of the App Center project
     */
    @get:Input
    val ownerName: Property<String>

    /**
     * "Application name in AppCenter. If appName isn't set plugin uses <baseFileName>-<variantName>,
     * for example example-base-project-android-debug, example-base-project-android-internal"
     */
    @get:Input
    val appName: Property<String>

    /**
     * Test groups for app distribution
     *
     * For example: [android-testers]
     */
    @get:Input
    val testerGroups: SetProperty<String>

    /**
     * Max request count to check upload status. Default = [MAX_REQUEST_COUNT]
     */
    @get:Input
    @get:Optional
    val maxUploadStatusRequestCount: Property<Int>

    /**
     * Request delay in ms for each request. Default = [MAX_REQUEST_DELAY_MS] ms
     */
    @get:Input
    @get:Optional
    val uploadStatusRequestDelayMs: Property<Long>

    /**
     * Coefficient K for dynamic upload status request delay calculation:
     *
     * delaySecs = apkSizeMb / K
     *
     * If this isn't specified or 0, [uploadStatusRequestDelayMs] will be used.
     *
     * Default value is null.
     */
    @get:Input
    @get:Optional
    val uploadStatusRequestDelayCoefficient: Property<Long>

    fun registerDistributionTask(
        project: TaskContainer,
        params: AppCenterDistributionTaskParams
    ): TaskProvider<AppCenterDistributionTask> {
        return project.registerAppCenterDistributionTask(this, params)
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
