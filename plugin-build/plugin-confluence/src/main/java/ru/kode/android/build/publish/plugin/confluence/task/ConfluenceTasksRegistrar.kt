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

internal object ConfluenceTasksRegistrar {
    internal fun registerDistributionTask(
        project: Project,
        distributionConfig: ConfluenceDistributionConfig,
        params: ConfluenceDistributionTaskParams,
    ): TaskProvider<ConfluenceDistributionTask> {
        return project.registerConfluenceDistributionTask(distributionConfig, params)
    }
}

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

internal data class ConfluenceDistributionTaskParams(
    val buildVariant: BuildVariant,
    val apkOutputFile: Provider<RegularFile>,
)
