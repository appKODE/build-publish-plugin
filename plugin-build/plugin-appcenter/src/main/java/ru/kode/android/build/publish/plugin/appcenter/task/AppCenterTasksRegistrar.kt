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
import ru.kode.android.build.publish.plugin.core.util.flatMapByNameOrCommon

internal const val APP_CENTER_DISTRIBUTION_UPLOAD_TASK_PREFIX = "appCenterDistributionUpload"

/**
 * Utility responsible for registering App Center distribution upload tasks for given build variants.
 *
 * **Purpose**:
 *  - Dynamically create a dedicated `AppCenterDistributionTask` for each configured [AppCenterDistributionConfig]
 *    and Android build variant.
 *  - Bind all required inputs (APK path, changelog, tester groups, etc.) and the correct [AppCenterNetworkService].
 *
 * **Flow**:
 *
 * 1. **Entry point** – [AppCenterTasksRegistrar.registerDistributionTask]:
 *    - Called by the plugin or build logic when a new App Center distribution upload task needs to be created.
 *    - Delegates to `Project.registerAppCenterDistributionTask` to perform actual task registration.
 *
 * 2. **Task registration** – `Project.registerAppCenterDistributionTask`:
 *    - Derives a unique task name:
 *      ```
 *      "appCenterDistributionUpload<CapitalizedVariantName>"
 *      ```
 *      Example: `appCenterDistributionUploadDebug` or `appCenterDistributionUploadRelease`.
 *
 *    - Uses Gradle's lazy `tasks.register(...)` to:
 *      - Avoid creating the task until it’s actually needed in the build graph.
 *      - Ensure configuration is incremental and cache-friendly.
 *
 * 3. **Network service resolution**:
 *    - Retrieves the [AppCenterServiceExtension] created earlier by the plugin.
 *    - Calls `flatMapByNameOrCommon` to select the correct [AppCenterNetworkService] for the given variant name,
 *      or falls back to a "common" service if variant-specific one is not found.
 *
 * 4. **Task input wiring**:
 *    - Binds all required inputs from:
 *      - [distributionConfig] → `appName`, `testerGroups`, upload delay settings.
 *      - [params] → APK file, changelog file, build tag, base file name.
 *      - `networkService` → For making authenticated App Center API calls.
 *
 * **Result**:
 *  - A `TaskProvider<AppCenterDistributionTask>` that, when executed, will:
 *    1. Upload the build artifact (APK) to App Center.
 *    2. Apply changelog and tester groups.
 *    3. Poll the upload status using configured delay/count rules.
 *
 * **Example task names**:
 *  - `appCenterDistributionUploadDebug`
 *  - `appCenterDistributionUploadInternal`
 *  - `appCenterDistributionUploadRelease`
 */
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
                .flatMapByNameOrCommon(params.buildVariant.name)

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
