package ru.kode.android.build.publish.plugin.foundation.task

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.git.mapper.fromJson
import ru.kode.android.build.publish.plugin.core.strategy.VersionedApkNamingStrategy
import ru.kode.android.build.publish.plugin.core.strategy.BuildVersionCodeStrategy
import ru.kode.android.build.publish.plugin.core.strategy.BuildVersionNameStrategy
import ru.kode.android.build.publish.plugin.core.strategy.DEFAULT_BUILD_VERSION
import ru.kode.android.build.publish.plugin.core.strategy.DEFAULT_VERSION_CODE
import ru.kode.android.build.publish.plugin.core.strategy.SimpleApkNamingStrategy
import ru.kode.android.build.publish.plugin.core.strategy.OutputApkNameStrategy
import ru.kode.android.build.publish.plugin.core.strategy.VersionCodeStrategy
import ru.kode.android.build.publish.plugin.core.strategy.VersionNameStrategy
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.foundation.task.rename.RenameApkTask
import ru.kode.android.build.publish.plugin.foundation.task.tag.GetLastTagTask
import ru.kode.android.build.publish.plugin.foundation.task.tag.PrintLastIncreasedTag

internal const val PRINT_LAST_INCREASED_TAG_TASK_PREFIX = "printLastIncreasedTag"
internal const val RENAME_APK_TASK_PREFIX = "renameApk"

internal const val GET_LAST_TAG_TASK_PREFIX = "getLastTag"

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
 * @see GetLastTagTask
 * @see PrintLastIncreasedTag
 */
internal object TagTasksRegistrar {
    /**
     * Registers a task to get the last Git tag and processes version information.
     *
     * This method registers a [GetLastTagTask] and processes its output to generate:
     * - Version name (based on the tag or fallback)
     * - Version code (based on the build number from the tag or fallback)
     * - Output APK file name (formatted with version information)
     *
     * @param project The Gradle project
     * @param params Configuration parameters for the task
     *
     * @return [LastTagTaskOutput] containing version information and the last build tag file
     */
    internal fun registerLastTagTask(
        project: Project,
        params: LastTagTaskParams,
    ): LastTagTaskOutput {
        val lastBuildTag = project.registerGetLastTagTask(params)
        val versionCode =
            params.useVersionsFromTag
                .zip(
                    params.useDefaultsForVersionsAsFallback,
                ) { useVersionsFromTag, useDefaultVersionsAsFallback ->
                    useVersionsFromTag to useDefaultVersionsAsFallback
                }
                .flatMap { (useVersionsFromTag, useDefaultVersionsAsFallback) ->
                    when {
                        useVersionsFromTag -> lastBuildTag.flatMap { tagBuildFile ->
                            params.versionCodeStrategy.orElse(
                                project.provider { BuildVersionCodeStrategy }
                            ).flatMap { versionCodeStrategy ->
                                project.provider {
                                    val tag = if (tagBuildFile.asFile.exists()) {
                                        fromJson(tagBuildFile.asFile)
                                    } else {
                                        null
                                    }
                                    versionCodeStrategy.build(
                                        params.buildVariant,
                                        tag
                                    )
                                }
                            }
                        }

                        useDefaultVersionsAsFallback -> project.provider { DEFAULT_VERSION_CODE }
                        else -> project.provider { null }
                    }
                }

        val apkOutputFileName: Provider<String> = params.apkOutputFileName.flatMap { outputFileName ->
            val outputApkNameStrategyOrDefault = params.outputApkNameStrategy
                .orElse(project.provider { VersionedApkNamingStrategy })
            params.useVersionsFromTag
                .zip(outputApkNameStrategyOrDefault) { useVersionsFromTag, outputApkNameStrategy ->
                    useVersionsFromTag to outputApkNameStrategy
                }.flatMap { (useVersionsFromTag, outputApkNameStrategy) ->
                    if (useVersionsFromTag) {
                        params.baseFileName
                            .zip(lastBuildTag) { baseFileName, tagBuildFile -> baseFileName to tagBuildFile }
                            .map { (baseFileName, tagBuildFile) ->
                                val tag = if (tagBuildFile.asFile.exists()) {
                                    fromJson(tagBuildFile.asFile)
                                } else {
                                    null
                                }
                                outputApkNameStrategy.build(outputFileName, tag, baseFileName)
                            }
                    } else {
                        params.baseFileName.map { baseFileName ->
                            SimpleApkNamingStrategy.build(outputFileName, null, baseFileName)
                        }
                    }
                }
        }

        val versionName =
            params.useVersionsFromTag
                .zip(
                    params.useDefaultsForVersionsAsFallback,
                ) { useVersionsFromTag, useDefaultVersionsAsFallback ->
                    useVersionsFromTag to useDefaultVersionsAsFallback
                }
                .flatMap { (useVersionsFromTag, useDefaultVersionsAsFallback) ->
                    when {
                        useVersionsFromTag -> {
                            lastBuildTag.flatMap { tagBuildFile ->
                                params.versionNameStrategy.orElse(
                                    project.provider { BuildVersionNameStrategy }
                                ).flatMap { versionNameStrategy ->
                                    project.provider {
                                        val tag = if (tagBuildFile.asFile.exists()) {
                                            fromJson(tagBuildFile.asFile)
                                        } else {
                                            null
                                        }
                                        versionNameStrategy.build(
                                            params.buildVariant,
                                            tag
                                        )
                                    }
                                }
                            }
                        }

                        useDefaultVersionsAsFallback -> project.provider { DEFAULT_VERSION_NAME }
                        else -> project.provider { null }
                    }
                }

        return LastTagTaskOutput(
            versionName = versionName,
            versionCode = versionCode,
            apkOutputFileName = apkOutputFileName,
            lastBuildTagFile = lastBuildTag,
        )
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
        return project.tasks.registerPrintLastIncreasedTagTask(params)
    }

    internal fun registerRenameApkTask(
        project: Project,
        params: RenameApkTaskParams,
    ): TaskProvider<RenameApkTask> {
        return project.registerRenameApkTask(params)
    }
}

@Suppress("MaxLineLength")
private fun Project.registerRenameApkTask(
    params: RenameApkTaskParams,
): TaskProvider<RenameApkTask> {
    val variant = params.buildVariant
    val taskName = "$RENAME_APK_TASK_PREFIX${variant.capitalizedName()}"
    return tasks.register(taskName, RenameApkTask::class.java) { task ->
        task.outputFileName.set(params.outputFileName)
        task.inputDir.set(params.inputDir)
    }
}

/**
 * Registers a [GetLastTagTask] task for the given [params].
 *
 * This task retrieves the last Git tag for a build variant and generates a JSON file containing the
 * tag information. The generated file is located in the build directory and has the name
 * "tag-build-${params.buildVariant.name}.json".
 *
 * @param params Configuration parameters for the task
 * @return A [Provider] for the generated JSON file containing the last tag information
 */
private fun Project.registerGetLastTagTask(params: LastTagTaskParams): Provider<RegularFile> {
    val variant = params.buildVariant
    val tagBuildFile =
        project.layout.buildDirectory
            .file("tag-build-${variant.name}.json")

    val taskName = "$GET_LAST_TAG_TASK_PREFIX${variant.capitalizedName()}"
    return tasks.register(taskName, GetLastTagTask::class.java) { task ->
        task.tagBuildFile.set(tagBuildFile)
        task.buildVariantName.set(variant.name)
        task.buildTagPattern.set(params.buildTagPattern)
        task.useStubsForTagAsFallback.set(params.useStubsForTagAsFallback)
    }.map {
        project.layout.projectDirectory.file(it.outputs.files.singleFile.path)
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
private fun TaskContainer.registerPrintLastIncreasedTagTask(params: PrintLastIncreasedTagTaskParams): TaskProvider<PrintLastIncreasedTag> {
    val taskName = "$PRINT_LAST_INCREASED_TAG_TASK_PREFIX${params.buildVariant.capitalizedName()}"
    return register(taskName, PrintLastIncreasedTag::class.java) { task ->
        task.buildTagFile.set(params.lastBuildTagFile)
    }
}

/**
 * Data class containing the output of the last tag task.
 */
internal data class LastTagTaskOutput(
    /**
     * Provider for the version name, or null if not applicable
     */
    val versionName: Provider<String?>,
    /**
     * Provider for the version code, or null if not applicable
     */
    val versionCode: Provider<Int?>,
    /**
     * Provider for the formatted APK output file name
     */
    val apkOutputFileName: Provider<String>,
    /**
     * Provider for the file containing the last build tag information
     */
    val lastBuildTagFile: Provider<RegularFile>,
)

/**
 * Configuration parameters for the last tag task.
 */
internal data class LastTagTaskParams(
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
     * Whether to use default versions as fallback
     */
    val useDefaultsForVersionsAsFallback: Provider<Boolean>,
    /**
     * Whether to use stubs when tag is not found
     */
    val useStubsForTagAsFallback: Provider<Boolean>,
    /**
     * The pattern to match build tags against
     */
    val buildTagPattern: Provider<String>,
    /**
     * Provider for the version name mapper.
     */
    val versionNameStrategy: Provider<VersionNameStrategy>,
    /**
     * Provider for the version code mapper.
     */
    val versionCodeStrategy: Provider<VersionCodeStrategy>,
    /**
     * Provider for the output APK name mapper.
     */
    val outputApkNameStrategy: Provider<OutputApkNameStrategy>,
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
    val lastBuildTagFile: Provider<RegularFile>,
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
