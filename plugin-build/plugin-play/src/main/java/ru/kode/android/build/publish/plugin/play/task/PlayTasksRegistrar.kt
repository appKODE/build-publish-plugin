package ru.kode.android.build.publish.plugin.play.task

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.core.util.flatMapByNameOrDefault
import ru.kode.android.build.publish.plugin.play.core.PlayDistribution
import ru.kode.android.build.publish.plugin.play.service.PlayNetworkServiceExtension
import ru.kode.android.build.publish.plugin.play.task.distribution.PlayDistributionTask

internal const val PLAY_DISTRIBUTION_UPLOAD_TASK_PREFIX = "playUpload"

object PlayTasksRegistrar {
    fun registerDistributionTask(
        project: Project,
        distributionConfig: PlayDistribution,
        params: PlayTaskParams,
    ): TaskProvider<PlayDistributionTask> {
        return project.registerPlayDistributionTask(distributionConfig, params)
    }
}

private fun Project.registerPlayDistributionTask(
    distributionConfig: PlayDistribution,
    params: PlayTaskParams,
): TaskProvider<PlayDistributionTask> {
    val buildVariant = params.buildVariant

    return tasks.register(
        "$PLAY_DISTRIBUTION_UPLOAD_TASK_PREFIX${buildVariant.capitalizedName()}",
        PlayDistributionTask::class.java,
    ) {
        val networkService =
            extensions
                .getByType(PlayNetworkServiceExtension::class.java)
                .services
                .flatMapByNameOrDefault(params.buildVariant.name)

        it.tagBuildFile.set(params.tagBuildProvider)
        it.buildVariantOutputFile.set(params.bundleOutputFileProvider)
        it.trackId.set(distributionConfig.trackId)
        it.updatePriority.set(distributionConfig.updatePriority)
        it.networkService.set(networkService)
    }
}

data class PlayTaskParams(
    val buildVariant: BuildVariant,
    val bundleOutputFileProvider: Provider<RegularFile>,
    val tagBuildProvider: Provider<RegularFile>,
)
