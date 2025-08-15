package ru.kode.android.build.publish.plugin.play.task

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.core.util.flatMapByNameOrCommon
import ru.kode.android.build.publish.plugin.play.config.PlayDistribution
import ru.kode.android.build.publish.plugin.play.service.PlayServiceExtension
import ru.kode.android.build.publish.plugin.play.task.distribution.PlayDistributionTask

internal const val PLAY_DISTRIBUTION_UPLOAD_TASK_PREFIX = "playUpload"

internal object PlayTasksRegistrar {
    internal fun registerDistributionTask(
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
                .getByType(PlayServiceExtension::class.java)
                .networkServices
                .flatMapByNameOrCommon(params.buildVariant.name)

        it.buildTagFile.set(params.lastBuildTagFile)
        it.distributionFile.set(params.bundleOutputFile)
        it.trackId.set(distributionConfig.trackId)
        it.updatePriority.set(distributionConfig.updatePriority)
        it.networkService.set(networkService)
    }
}

internal data class PlayTaskParams(
    val buildVariant: BuildVariant,
    val bundleOutputFile: Provider<RegularFile>,
    val lastBuildTagFile: Provider<RegularFile>,
)
