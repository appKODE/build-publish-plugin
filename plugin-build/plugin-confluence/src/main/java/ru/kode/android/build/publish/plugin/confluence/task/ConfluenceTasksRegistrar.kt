package ru.kode.android.build.publish.plugin.confluence.task

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.confluence.core.ConfluenceDistributionConfig
import ru.kode.android.build.publish.plugin.confluence.task.distribution.ConfluenceDistributionTask
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.core.util.flatMapByNameOrDefault
import ru.kode.android.build.publish.plugin.jira.service.ConfluenceNetworkServiceExtension

internal const val CONFLUENCE_DISTRIBUTION_UPLOAD_TASK_PREFIX = "confluenceDistributionUpload"

object ConfluenceTasksRegistrar {

    fun registerDistributionTask(
        project: Project,
        distributionConfig: ConfluenceDistributionConfig,
        params: ConfluenceDistributionTaskParams
    ): TaskProvider<ConfluenceDistributionTask> {
        return project.registerConfluenceDistributionTask(distributionConfig, params)
    }
}

private fun Project.registerConfluenceDistributionTask(
    distributionConfig: ConfluenceDistributionConfig,
    params: ConfluenceDistributionTaskParams
): TaskProvider<ConfluenceDistributionTask> {
    return tasks.register(
        "$CONFLUENCE_DISTRIBUTION_UPLOAD_TASK_PREFIX${params.buildVariant.capitalizedName()}",
        ConfluenceDistributionTask::class.java,
    ) {
        val networkService = project.extensions
            .getByType(ConfluenceNetworkServiceExtension::class.java)
            .services
            .flatMapByNameOrDefault(params.buildVariant.name)

        it.buildVariantOutputFile.set(params.apkOutputFileProvider)
        it.pageId.set(distributionConfig.pageId)
        it.networkService.set(networkService)
    }
}

data class ConfluenceDistributionTaskParams(
    val buildVariant: BuildVariant,
    val apkOutputFileProvider: Provider<RegularFile>,
)
