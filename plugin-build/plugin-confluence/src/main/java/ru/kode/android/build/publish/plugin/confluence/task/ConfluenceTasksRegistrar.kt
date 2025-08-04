package ru.kode.android.build.publish.plugin.confluence.task

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.confluence.core.ConfluenceAuthConfig
import ru.kode.android.build.publish.plugin.confluence.core.ConfluenceDistributionConfig
import ru.kode.android.build.publish.plugin.confluence.task.distribution.ConfluenceDistributionTask
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName

internal const val CONFLUENCE_DISTRIBUTION_UPLOAD_TASK_PREFIX = "confluenceDistributionUpload"

object ConfluenceTasksRegistrar {

    fun registerDistributionTask(
        project: TaskContainer,
        authConfig: ConfluenceAuthConfig,
        distributionConfig: ConfluenceDistributionConfig,
        params: ConfluenceDistributionTaskParams
    ): TaskProvider<ConfluenceDistributionTask> {
        return project.registerConfluenceDistributionTask(authConfig, distributionConfig, params)
    }
}

private fun TaskContainer.registerConfluenceDistributionTask(
    authConfig: ConfluenceAuthConfig,
    distributionConfig: ConfluenceDistributionConfig,
    params: ConfluenceDistributionTaskParams
): TaskProvider<ConfluenceDistributionTask> {
    return register(
        "$CONFLUENCE_DISTRIBUTION_UPLOAD_TASK_PREFIX${params.buildVariant.capitalizedName()}",
        ConfluenceDistributionTask::class.java,
    ) {
        it.buildVariantOutputFile.set(params.apkOutputFileProvider)
        it.username.set(authConfig.username)
        it.password.set(authConfig.password)
        it.pageId.set(distributionConfig.pageId)
    }
}

data class ConfluenceDistributionTaskParams(
    val buildVariant: BuildVariant,
    val apkOutputFileProvider: Provider<RegularFile>,
)
