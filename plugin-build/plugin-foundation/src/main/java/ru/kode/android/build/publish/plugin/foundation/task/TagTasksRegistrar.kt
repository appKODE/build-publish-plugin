package ru.kode.android.build.publish.plugin.foundation.task

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.strategy.DEFAULT_BUILD_VERSION
import ru.kode.android.build.publish.plugin.core.strategy.OutputApkNameStrategy
import ru.kode.android.build.publish.plugin.core.strategy.VersionCodeStrategy
import ru.kode.android.build.publish.plugin.core.strategy.VersionNameStrategy
import ru.kode.android.build.publish.plugin.core.task.GetLastTagSnapshotTaskOutput
import ru.kode.android.build.publish.plugin.core.util.apkOutputFileNameProvider
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.core.util.tagBuildSnapshotFileProvider
import ru.kode.android.build.publish.plugin.core.util.versionCodeFileProvider
import ru.kode.android.build.publish.plugin.core.util.versionNameProvider
import ru.kode.android.build.publish.plugin.foundation.task.rename.RenameApkTask
import ru.kode.android.build.publish.plugin.foundation.task.tag.ComputeApkOutputFileNameTask
import ru.kode.android.build.publish.plugin.foundation.task.tag.ComputeVersionCodeTask
import ru.kode.android.build.publish.plugin.foundation.task.tag.ComputeVersionNameTask
import ru.kode.android.build.publish.plugin.foundation.task.tag.GetLastTagSnapshotTask
import ru.kode.android.build.publish.plugin.foundation.task.tag.PrintLastIncreasedTag

internal const val PRINT_LAST_INCREASED_TAG_TASK_PREFIX = "printLastIncreasedTag"
internal const val RENAME_APK_TASK_PREFIX = "renameApk"
internal const val GET_LAST_TAG_SNAPSHOT_TASK_PREFIX = "getLastTagSnapshot"
internal const val COMPUTE_VERSION_CODE_TASK_PREFIX = "computeVersionCode"
internal const val COMPUTE_APK_OUTPUT_FILENAME_TASK_PREFIX = "computeApkOutputFileName"
internal const val COMPUTE_VERSION_NAME_TASK_PREFIX = "computeVersionName"

const val DEFAULT_VERSION_NAME = DEFAULT_BUILD_VERSION

/**
 * Utility object for registering tasks related to Git tag management in the build process.
 *
 * This registrar provides methods to register tasks that handle versioning based on Git tags,
 * including retrieving the last tag, generating version names and codes, and formatting output file names.
 *
 * ## Features
 * - Registers tasks to get the last Git tag for a build variant
 * - Generates version names and codes based on Git tags
 * - Formats output APK file names with version information
 * - Supports fallback to default values when tags are not found
 *
 * @see GetLastTagSnapshotTask
 * @see PrintLastIncreasedTag
 */
internal object TagTasksRegistrar {
    /**
     * Registers a task to get the last Git tag and processes version information.
     *
     * This method registers a [GetLastTagSnapshotTask] and processes its output to generate:
     * - Version name (based on the tag or fallback)
     * - Version code (based on the build number from the tag or fallback)
     * - Output APK file name (formatted with version information)
     *
     * @param project The Gradle project
     * @param params Configuration parameters for the task
     *
     * @return [LastTagTaskOutput] containing version information and the last build tag file
     */
    internal fun registerGetLastTagSnapshotTask(
        project: Project,
        params: LastTagSnapshotTaskParams,
    ): TaskProvider<GetLastTagSnapshotTask> {
        return project.registerGetLastTagSnapshotTask(params)
    }

    internal fun registerComputeVersionCodeTask(
        project: Project,
        params: ComputeVersionCodeParams,
    ): TaskProvider<ComputeVersionCodeTask> {
        return project.registerComputeVersionCodeTask(params)
    }

    internal fun registerComputeApkOutputFileNameTask(
        project: Project,
        params: ComputeApkOutputFileNameParams,
    ): TaskProvider<ComputeApkOutputFileNameTask> {
        return project.registerComputeApkOutputFileNameTask(params)
    }

    internal fun registerComputeVersionNameTask(
        project: Project,
        params: ComputeVersionNameParams,
    ): TaskProvider<ComputeVersionNameTask> {
        return project.registerComputeVersionNameTask(params)
    }

    /**
     * Registers a task to print the last increased tag information.
     *
     * @param project The Gradle project
     * @param params Configuration parameters for the task
     *
     * @return A [TaskProvider] for the registered [PrintLastIncreasedTag] task
     */
    internal fun registerPrintLastIncreasedTagTask(
        project: Project,
        params: PrintLastIncreasedTagTaskParams,
    ): TaskProvider<PrintLastIncreasedTag> {
        return project.registerPrintLastIncreasedTagTask(params)
    }

    internal fun registerRenameApkTask(
        project: Project,
        params: RenameApkTaskParams,
    ): TaskProvider<RenameApkTask> {
        return project.registerRenameApkTask(params)
    }
}

@Suppress("MaxLineLength")
private fun Project.registerRenameApkTask(params: RenameApkTaskParams): TaskProvider<RenameApkTask> {
    val variant = params.buildVariant
    val taskName = "$RENAME_APK_TASK_PREFIX${variant.capitalizedName()}"
    return tasks.register(taskName, RenameApkTask::class.java) { task ->
        task.outputFileName.set(params.outputFileName)
        task.inputDir.set(params.inputDir)
    }
}

/**
 * Registers a [GetLastTagSnapshotTask] task for the given [params].
 *
 * This task retrieves the last Git tag for a build variant and generates a JSON file containing the
 * tag information. The generated file is located in the build directory and has the name
 * "tag-build-snapshot-${params.buildVariant.name}.json".
 *
 * @param params Configuration parameters for the task
 * @return A [Provider] for the generated JSON file containing the last tag information
 */
@Suppress("MaxLineLength") // One parameter function
private fun Project.registerGetLastTagSnapshotTask(params: LastTagSnapshotTaskParams): TaskProvider<GetLastTagSnapshotTask> {
    val variant = params.buildVariant
    val tagBuildSnapshotFile = project.tagBuildSnapshotFileProvider(variant.name)
    val taskName = "$GET_LAST_TAG_SNAPSHOT_TASK_PREFIX${variant.capitalizedName()}"

    return tasks.register(taskName, GetLastTagSnapshotTask::class.java) { task ->
        task.buildTagSnapshotFile.set(tagBuildSnapshotFile)
        task.buildVariantName.set(variant.name)
        task.buildTagPattern.set(params.buildTagPattern)
        task.useStubsForTagAsFallback.set(params.useStubsForTagAsFallback)
    }
}

@Suppress("MaxLineLength") // One parameter function
private fun Project.registerComputeVersionCodeTask(params: ComputeVersionCodeParams): TaskProvider<ComputeVersionCodeTask> {
    val variant = params.buildVariant
    val taskName = "$COMPUTE_VERSION_CODE_TASK_PREFIX${variant.capitalizedName()}"
    val versionCodeFile = project.versionCodeFileProvider(variant.name)

    val buildTagSnapshotFile = params.buildTagSnapshotProvider.flatMap { it.buildTagSnapshotFile }
    return tasks.register(taskName, ComputeVersionCodeTask::class.java) { task ->
        task.buildTagSnapshotFile.set(buildTagSnapshotFile)
        task.versionCodeFile.set(versionCodeFile)
        task.useVersionsFromTag.set(params.useVersionsFromTag)
        task.useDefaultsForFallback.set(params.useDefaultsForVersionsAsFallback)
        task.buildVariant.set(variant)
        task.versionCodeStrategy.set(params.versionCodeStrategy)

        task.dependsOn(params.buildTagSnapshotProvider)
    }
}

private fun Project.registerComputeApkOutputFileNameTask(
    params: ComputeApkOutputFileNameParams,
): TaskProvider<ComputeApkOutputFileNameTask> {
    val variant = params.buildVariant
    val taskName = "$COMPUTE_APK_OUTPUT_FILENAME_TASK_PREFIX${variant.capitalizedName()}"
    val apkOutputFileNameFile = project.apkOutputFileNameProvider(variant.name)

    val buildTagSnapshotFile = params.buildTagSnapshotProvider.flatMap { it.buildTagSnapshotFile }
    return tasks.register(taskName, ComputeApkOutputFileNameTask::class.java) { task ->
        task.apkOutputFileName.set(params.apkOutputFileName)
        task.useVersionsFromTag.set(params.useVersionsFromTag)
        task.baseFileName.set(params.baseFileName)
        task.buildTagSnapshotFile.set(buildTagSnapshotFile)
        task.buildVariant.set(variant)
        task.outputApkNameStrategy.set(params.outputApkNameStrategy)
        task.apkOutputFileNameFile.set(apkOutputFileNameFile)

        task.dependsOn(params.buildTagSnapshotProvider)
    }
}

@Suppress("MaxLineLength") // One parameter function
private fun Project.registerComputeVersionNameTask(params: ComputeVersionNameParams): TaskProvider<ComputeVersionNameTask> {
    val variant = params.buildVariant
    val taskName = "$COMPUTE_VERSION_NAME_TASK_PREFIX${variant.capitalizedName()}"
    val versionNameFile = project.versionNameProvider(variant.name)

    val buildTagSnapshotFile = params.buildTagSnapshotProvider.flatMap { it.buildTagSnapshotFile }
    return tasks.register(taskName, ComputeVersionNameTask::class.java) { task ->
        task.useVersionsFromTag.set(params.useVersionsFromTag)
        task.useDefaultsForVersionsAsFallback.set(params.useDefaultsForVersionsAsFallback)
        task.buildTagSnapshotFile.set(buildTagSnapshotFile)
        task.buildVariant.set(variant)
        task.versionNameStrategy.set(params.versionNameStrategy)
        task.versionNameFile.set(versionNameFile)

        task.dependsOn(params.buildTagSnapshotProvider)
    }
}

/**
 * Registers a [PrintLastIncreasedTag] task for the given [params].
 *
 * This task reads the last build tag from a JSON file, increments the build number,
 * and prints the new tag to standard output. It's typically used in CI/CD pipelines
 * to determine the next version tag for a build.
 *
 * @param params Configuration parameters for the task
 * @return A [TaskProvider] for the registered [PrintLastIncreasedTag] task
 */
@Suppress("MaxLineLength") // One parameter function
private fun Project.registerPrintLastIncreasedTagTask(params: PrintLastIncreasedTagTaskParams): TaskProvider<PrintLastIncreasedTag> {
    val taskName = "$PRINT_LAST_INCREASED_TAG_TASK_PREFIX${params.buildVariant.capitalizedName()}"
    return tasks.register(taskName, PrintLastIncreasedTag::class.java) { task ->
        task.buildTagSnapshotFile.set(params.buildTagSnapshotProvider.flatMap { it.buildTagSnapshotFile })

        task.dependsOn(params.buildTagSnapshotProvider)
    }
}

/**
 * Configuration parameters for the last tag task.
 */
internal data class LastTagSnapshotTaskParams(
    /**
     * The build variant to process
     */
    val buildVariant: BuildVariant,
    /**
     * Whether to use stubs when tag is not found
     */
    val useStubsForTagAsFallback: Provider<Boolean>,
    /**
     * The pattern to match build tags against
     */
    val buildTagPattern: Provider<String>,
)

internal data class ComputeVersionCodeParams(
    /**
     * The build variant to process
     */
    val buildVariant: BuildVariant,
    /**
     * Whether to use versions from the Git tag
     */
    val useVersionsFromTag: Provider<Boolean>,
    /**
     * Whether to use default versions as fallback
     */
    val useDefaultsForVersionsAsFallback: Provider<Boolean>,
    /**
     * Provider for the version code mapper.
     */
    val versionCodeStrategy: Provider<VersionCodeStrategy>,
    val buildTagSnapshotProvider: TaskProvider<GetLastTagSnapshotTask>,
)

internal data class ComputeVersionNameParams(
    /**
     * The build variant to process
     */
    val buildVariant: BuildVariant,
    /**
     * Whether to use versions from the Git tag
     */
    val useVersionsFromTag: Provider<Boolean>,
    /**
     * Whether to use default versions as fallback
     */
    val useDefaultsForVersionsAsFallback: Provider<Boolean>,
    /**
     * Provider for the version name mapper.
     */
    val versionNameStrategy: Provider<VersionNameStrategy>,
    val buildTagSnapshotProvider: TaskProvider<GetLastTagSnapshotTask>,
)

internal data class ComputeApkOutputFileNameParams(
    /**
     * The build variant to process
     */
    val buildVariant: BuildVariant,
    /**
     * The base name for the output APK file
     */
    val apkOutputFileName: Provider<String>,
    /**
     * Whether to use versions from the Git tag
     */
    val useVersionsFromTag: Provider<Boolean>,
    /**
     * The base file name to use for output files
     */
    val baseFileName: Provider<String>,
    /**
     * Provider for the output APK name mapper.
     */
    val outputApkNameStrategy: Provider<OutputApkNameStrategy>,
    val buildTagSnapshotProvider: TaskProvider<GetLastTagSnapshotTask>,
)

/**
 * Configuration parameters for the print last increased tag task.
 */
internal data class PrintLastIncreasedTagTaskParams(
    /**
     * The build variant to process
     */
    val buildVariant: BuildVariant,
    /**
     * Provider for the file containing the last build tag information
     */
    val buildTagSnapshotProvider: Provider<out GetLastTagSnapshotTaskOutput>,
)

/**
 * Configuration parameters for the task that renames an APK.
 */
internal data class RenameApkTaskParams(
    /**
     * Provider for the directory containing the input APK file
     */
    val inputDir: Provider<Directory>,
    /**
     * The build variant for which the APK is being renamed.
     */
    val buildVariant: BuildVariant,
    /**
     * Provider for the name of the output APK file.
     */
    val outputFileName: Provider<String>,
)
