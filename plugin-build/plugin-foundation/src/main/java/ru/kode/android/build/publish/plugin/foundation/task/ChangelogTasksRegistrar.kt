package ru.kode.android.build.publish.plugin.foundation.task

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.foundation.task.changelog.GenerateChangelogTask

internal const val GENERATE_CHANGELOG_TASK_PREFIX = "generateChangelog"

/**
 * Utility object for registering changelog-related tasks in the build process.
 *
 * This registrar provides methods to register tasks that handle changelog generation
 * based on Git commit history between tags.
 *
 * ## Features
 * - Registers tasks to generate changelogs for specific build variants
 * - Supports custom commit message keys for filtering commits
 * - Integrates with the versioning system through build tags
 *
 * @see GenerateChangelogTask
 */
internal object ChangelogTasksRegistrar {
    /**
     * Registers a task to generate a changelog for a specific build variant.

     * @param project The Gradle project
     * @param params Configuration parameters for the task
     *
     * @return A [Provider] that will contain the generated changelog file
     */
    internal fun registerGenerateChangelogTask(
        project: Project,
        params: GenerateChangelogTaskParams,
    ): Provider<RegularFile> {
        return project.registerGenerateChangelogTask(params)
    }
}

/**
 * Registers a task to generate a changelog for a specific build variant.
 *
 * This function creates and configures a [GenerateChangelogTask] with the provided parameters
 * and sets up its dependencies. The task will be named according to the build variant
 * (e.g., `generateChangelogRelease`).
 *
 * @param params Configuration parameters for the task
 *
 * @return A [Provider] that will contain the generated changelog file
 */
private fun Project.registerGenerateChangelogTask(params: GenerateChangelogTaskParams): Provider<RegularFile> {
    return tasks.register(
        "$GENERATE_CHANGELOG_TASK_PREFIX${params.buildVariant.capitalizedName()}",
        GenerateChangelogTask::class.java,
    ) {
        it.commitMessageKey.set(params.commitMessageKey)
        it.buildTagPattern.set(params.buildTagPattern)
        it.changelogFile.set(params.changelogFile)
        it.buildTagFile.set(params.lastTagFile)
    }.map {
        project.layout.projectDirectory.file(it.outputs.files.singleFile.path)
    }
}

/**
 * Configuration parameters for the generate changelog task.
 */
internal data class GenerateChangelogTaskParams(
    /**
     * Provider for the key used to filter relevant commit messages
     */
    val commitMessageKey: Provider<String>,
    /**
     * Provider for the pattern to match build tags against
     */
    val buildTagPattern: Provider<String>,
    /**
     * The build variant to generate the changelog for
     */
    val buildVariant: BuildVariant,
    /**
     * Provider for the output file where the changelog was written
     */
    val changelogFile: Provider<RegularFile>,
    /**
     * Provider for the file containing the last tag information
     */
    val lastTagFile: Provider<RegularFile>,
)
