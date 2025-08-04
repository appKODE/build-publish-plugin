package ru.kode.android.build.publish.plugin.play.task

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.play.core.PlayDistribution
import ru.kode.android.build.publish.plugin.play.task.distribution.PlayDistributionTask

internal const val PLAY_DISTRIBUTION_UPLOAD_TASK_PREFIX = "playUpload"

object PlayTasksRegistrar {

    fun registerDistributionTask(
        project: TaskContainer,
        distributionConfig: PlayDistribution,
        params: PlayTaskParams
    ): TaskProvider<PlayDistributionTask> {
        return project.registerPlayDistributionTask(distributionConfig, params)
    }
}

private fun TaskContainer.registerPlayDistributionTask(
    distributionConfig: PlayDistribution,
    params: PlayTaskParams
): TaskProvider<PlayDistributionTask> {
    val buildVariant = params.buildVariant

    return register(
        "$PLAY_DISTRIBUTION_UPLOAD_TASK_PREFIX${buildVariant.capitalizedName()}",
        PlayDistributionTask::class.java,
    ) {
        it.tagBuildFile.set(params.tagBuildProvider)
        it.buildVariantOutputFile.set(params.bundleOutputFileProvider)
        it.apiTokenFile.set(distributionConfig.apiTokenFile)
        it.appId.set(distributionConfig.appId)
        it.trackId.set(distributionConfig.trackId)
        it.updatePriority.set(distributionConfig.updatePriority)
    }
}

data class PlayTaskParams(
    val buildVariant: BuildVariant,
    val bundleOutputFileProvider: Provider<RegularFile>,
    val tagBuildProvider: Provider<RegularFile>,
)
