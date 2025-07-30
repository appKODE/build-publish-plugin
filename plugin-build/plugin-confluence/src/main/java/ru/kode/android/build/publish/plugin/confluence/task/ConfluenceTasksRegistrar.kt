package ru.kode.android.build.publish.plugin.confluence.task

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.confluence.core.ConfluenceConfig
import ru.kode.android.build.publish.plugin.confluence.task.distribution.ConfluenceDistributionTask
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName

internal const val CONFLUENCE_DISTRIBUTION_UPLOAD_TASK_PREFIX = "confluenceDistributionUpload"

object ConfluenceTasksRegistrar {

    fun registerDistributionTask(
        project: TaskContainer,
        config: ConfluenceConfig,
        params: ConfluenceDistributionTaskParams
    ): TaskProvider<ConfluenceDistributionTask> {
        return project.registerConfluenceDistributionTask(config, params)
    }
}

private fun TaskContainer.registerConfluenceDistributionTask(
    config: ConfluenceConfig,
    params: ConfluenceDistributionTaskParams
): TaskProvider<ConfluenceDistributionTask> {
    return register(
        "$CONFLUENCE_DISTRIBUTION_UPLOAD_TASK_PREFIX${params.buildVariant.capitalizedName()}",
        ConfluenceDistributionTask::class.java,
    ) {
        it.buildVariantOutputFile.set(params.apkOutputFileProvider)
        it.username.set(config.username)
        it.password.set(config.password)
        it.pageId.set(config.pageId)
    }
}

data class ConfluenceDistributionTaskParams(
    val buildVariant: BuildVariant,
    val apkOutputFileProvider: Provider<RegularFile>,
)
