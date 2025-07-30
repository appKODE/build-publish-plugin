package ru.kode.android.build.publish.plugin.play.core

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.play.task.PlayDistributionTask

internal const val PLAY_DISTRIBUTION_UPLOAD_TASK_PREFIX = "playUpload"

interface PlayConfig {
    val name: String

    /**
     * The path to file with token for Google Play project
     */
    @get:InputFile
    val apiTokenFile: RegularFileProperty

    /**
     * appId in Google Play
     */
    @get:Input
    val appId: Property<String>

    /**
     * Track name of target app. Defaults to "internal"
     */
    @get:Input
    val trackId: Property<String>

    /**
     * Test groups for app distribution
     *
     * For example: [android-testers]
     */
    @get:Input
    val updatePriority: Property<Int>

    fun registerDistributionTask(
        project: Project,
        params: PlayTaskParams
    ): TaskProvider<PlayDistributionTask> {
        return project.tasks.registerPlayDistributionTask(this, params)
    }
}

private fun TaskContainer.registerPlayDistributionTask(
    config: PlayConfig,
    params: PlayTaskParams
): TaskProvider<PlayDistributionTask> {
    val buildVariant = params.buildVariant

    return register(
        "$PLAY_DISTRIBUTION_UPLOAD_TASK_PREFIX${buildVariant.capitalizedName()}",
        PlayDistributionTask::class.java,
    ) {
        it.tagBuildFile.set(params.tagBuildProvider)
        it.buildVariantOutputFile.set(params.bundleOutputFileProvider)
        it.apiTokenFile.set(config.apiTokenFile)
        it.appId.set(config.appId)
        it.trackId.set(config.trackId)
        it.updatePriority.set(config.updatePriority)
    }
}

data class PlayTaskParams(
    val buildVariant: BuildVariant,
    val bundleOutputFileProvider: Provider<RegularFile>,
    val tagBuildProvider: Provider<RegularFile>,
)
