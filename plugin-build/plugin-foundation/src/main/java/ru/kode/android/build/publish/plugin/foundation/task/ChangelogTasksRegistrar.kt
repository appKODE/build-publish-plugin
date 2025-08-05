package ru.kode.android.build.publish.plugin.foundation.task

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.foundation.task.changelog.GenerateChangelogTask

internal const val GENERATE_CHANGELOG_TASK_PREFIX = "generateChangelog"

object ChangelogTasksRegistrar {

    fun registerGenerateChangelogTask(
        project: Project,
        params: GenerateChangelogTaskParams,
    ): Provider<RegularFile> {
        return project.tasks.registerGenerateChangelogTask(params)
    }
}

private fun TaskContainer.registerGenerateChangelogTask(
    params: GenerateChangelogTaskParams,
): Provider<RegularFile> {
    return register(
        "$GENERATE_CHANGELOG_TASK_PREFIX${params.buildVariant.capitalizedName()}",
        GenerateChangelogTask::class.java,
    ) {
        it.commitMessageKey.set(params.commitMessageKey)
        it.buildTagPattern.set(params.buildTagPattern)
        it.changelogFile.set(params.changelogFile)
        it.tagBuildFile.set(params.tagBuildProvider)
    }.flatMap { it.changelogFile }
}

data class GenerateChangelogTaskParams(
    val commitMessageKey: Provider<String>,
    val buildTagPattern: Provider<String>,
    val buildVariant: BuildVariant,
    val changelogFile: Provider<RegularFile>,
    val tagBuildProvider: Provider<RegularFile>,
)
