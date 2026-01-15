package ru.kode.android.build.publish.plugin.foundation.task

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.task.GenerateChangelogTaskOutput
import ru.kode.android.build.publish.plugin.core.task.GetLastTagSnapshotTaskOutput
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.core.util.changelogFileProvider
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
    ): TaskProvider<out GenerateChangelogTaskOutput> {
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
@Suppress("MaxLineLength") // One parameter function
private fun Project.registerGenerateChangelogTask(params: GenerateChangelogTaskParams): TaskProvider<out GenerateChangelogTaskOutput> {
    val buildVariant = params.buildVariant
    val changelogFile = project.changelogFileProvider(buildVariant.name)
    return tasks.register(
        "$GENERATE_CHANGELOG_TASK_PREFIX${buildVariant.capitalizedName()}",
        GenerateChangelogTask::class.java,
    ) {
        it.commitMessageKey.set(params.commitMessageKey)
        it.excludeMessageKey.set(params.excludeMessageKey)
        it.buildTagPattern.set(params.buildTagPattern)
        it.changelogFile.set(changelogFile)
        it.buildTagSnapshotFile.set(params.buildTagSnapshotProvider.flatMap { it.buildTagSnapshotFile })

        it.dependsOn(params.buildTagSnapshotProvider)
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
     * Provider indicating whether the [commitMessageKey] should be removed from commit messages
     * in the generated changelog.
     */
    val excludeMessageKey: Provider<Boolean>,
    /**
     * Provider for the pattern to match build tags against
     */
    val buildTagPattern: Provider<String>,
    /**
     * The build variant to generate the changelog for
     */
    val buildVariant: BuildVariant,
    /**
     * Provider for the file containing the last tag information
     */
    val buildTagSnapshotProvider: Provider<out GetLastTagSnapshotTaskOutput>,
)
