package ru.kode.android.build.publish.plugin.foundation.task

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.git.mapper.fromJson
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.foundation.task.tag.GetLastTagTask
import ru.kode.android.build.publish.plugin.foundation.task.tag.PrintLastIncreasedTag
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal const val PRINT_LAST_INCREASED_TAG_TASK_PREFIX = "printLastIncreasedTag"

internal const val GET_LAST_TAG_TASK_PREFIX = "getLastTag"

const val DEFAULT_BASE_FILE_NAME = "dev-build"

const val DEFAULT_BUILD_VERSION = "v0.0.1"

const val DEFAULT_VERSION_NAME = "$DEFAULT_BUILD_VERSION-dev"

const val DEFAULT_VERSION_CODE = 1

const val DEFAULT_TAG_PATTERN = ".+\\.(\\d+)-%s"

const val DEFAULT_TAG_NAME = "$DEFAULT_BUILD_VERSION-%s"

const val DEFAULT_TAG_COMMIT_SHA = "hardcoded_default_stub_commit_sha"

const val DEFAULT_TAG_COMMIT_MESSAGE = "hardcoded_default_stub_commit_message"

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
        val versionCode = params.useVersionsFromTag
            .zip(params.useDefaultsForVersionsAsFallback) { useVersionsFromTag, useDefaultVersionsAsFallback ->
                useVersionsFromTag to useDefaultVersionsAsFallback
            }.flatMap { (useVersionsFromTag, useDefaultVersionsAsFallback) ->
                when {
                    useVersionsFromTag -> lastBuildTag.map { mapToVersionCode(it.asFile) }
                    useDefaultVersionsAsFallback -> project.provider { DEFAULT_VERSION_CODE }
                    else -> project.provider { null }
                }
            }

        val apkOutputFileName = params.useVersionsFromTag.flatMap { useVersionsFromTag ->
            if (useVersionsFromTag) {
                params.baseFileName.zip(lastBuildTag) { baseFileName, tagBuildFile ->
                    mapToOutputApkFileName(tagBuildFile.asFile, params.apkOutputFileName, baseFileName)
                }
            } else {
                params.baseFileName.map { baseFileName ->
                    createDefaultOutputFileName(baseFileName, params.apkOutputFileName)
                }
            }
        }
        val versionName = params.useVersionsFromTag
            .zip(params.useDefaultsForVersionsAsFallback) { useVersionsFromTag, useDefaultVersionsAsFallback ->
                useVersionsFromTag to useDefaultVersionsAsFallback
            }.flatMap { (useVersionsFromTag, useDefaultVersionsAsFallback) ->
                when {
                    useVersionsFromTag -> {
                        lastBuildTag.map { tagBuildFile ->
                            mapToVersionName(tagBuildFile.asFile, params.buildVariant)
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
    val tagBuildFile =
        project.layout.buildDirectory
            .file("tag-build-${params.buildVariant.name}.json")

    return tasks.register(
        "$GET_LAST_TAG_TASK_PREFIX${params.buildVariant.capitalizedName()}",
        GetLastTagTask::class.java,
    ) { task ->
        task.tagBuildFile.set(tagBuildFile)
        task.buildVariantName.set(params.buildVariant.name)
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
    return register(
        "$PRINT_LAST_INCREASED_TAG_TASK_PREFIX${params.buildVariant.capitalizedName()}",
        PrintLastIncreasedTag::class.java,
    ) { task ->
        task.buildTagFile.set(params.lastBuildTagFile)
    }
}

/**
 * Maps a [tagBuildFile] to a version name.
 *
 * This function reads the [tagBuildFile] and extracts the version name from it. If the file exists,
 * it uses the version name from the JSON file. If the file does not exist, it uses the default build
 * version with the name of the build variant appended.
 *
 * @param tagBuildFile The file containing the tag build information.
 * @param buildVariant The build variant for which the version name is being mapped.
 *
 * @return The version name extracted from the [tagBuildFile] or the default build version with the
 * name of the build variant appended.
 */
private fun mapToVersionName(
    file: File,
    buildVariant: BuildVariant,
): String {
    return if (file.exists()) {
        fromJson(file).name
    } else {
        "$DEFAULT_BUILD_VERSION-${buildVariant.name}"
    }
}

/**
 * Maps a [tagBuildFile] to a version code.
 *
 * This function reads the [tagBuildFile] and extracts the version code from it. If the file exists,
 * it uses the version code from the JSON file. If the file does not exist, it uses the default version
 * code defined in [DEFAULT_VERSION_CODE].
 *
 * @param tagBuildFile The file containing the tag build information.
 *
 * @return The version code extracted from the [tagBuildFile] or the default version code defined in
 * [DEFAULT_VERSION_CODE].
 */
private fun mapToVersionCode(file: File): Int {
    return if (file.exists()) {
        fromJson(file).buildNumber
    } else {
        DEFAULT_VERSION_CODE
    }
}

/**
 * Maps a [tagBuildFile] to the output APK file name.
 *
 * This function reads the [tagBuildFile] and extracts the version name and version code from it.
 * If the file exists and the [outputFileName] ends with .apk, it constructs the APK file name
 * in the format `<baseFileName>-<versionName>-vc<versionCode>-<formattedDate>.apk`.
 * If the file does not exist and the [outputFileName] ends with .apk, it constructs the APK
 * file name in the format `<baseFileName>-<formattedDate>.apk`.
 * If the file does not exist and the [outputFileName] does not end with .apk, it passes the
 * [outputFileName] through [createDefaultOutputFileName].
 *
 * @param tagBuildFile The file containing the tag build information.
 * @param outputFileName The name of the output APK file.
 * @param baseFileName The base name of the output APK file.
 *
 * @return The APK file name extracted from the [tagBuildFile] or the default APK file name based on
 * the [outputFileName] and [baseFileName].
 */
private fun mapToOutputApkFileName(
    file: File,
    outputFileName: String,
    baseFileName: String?,
): String {
    val formattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy"))
    return if (file.exists() && outputFileName.endsWith(".apk")) {
        val tagBuild = fromJson(file)
        val versionName = tagBuild.buildVariant
        val versionCode = tagBuild.buildNumber
        "$baseFileName-$versionName-vc$versionCode-$formattedDate.apk"
    } else if (!file.exists() && outputFileName.endsWith(".apk")) {
        "$baseFileName-$formattedDate.apk"
    } else {
        createDefaultOutputFileName(baseFileName, outputFileName)
    }
}

/**
 * Generates the default output file name if the [tagBuildFile] is not available.
 *
 * If the [baseFileName] is not null, it will be used as the base of the output file name.
 * Otherwise, [DEFAULT_BASE_FILE_NAME] will be used.
 * The function extracts the file extension from the [outputFileName] and appends it to the
 * base file name.
 *
 * @param baseFileName The base file name of the output APK, or null if not available.
 * @param outputFileName The name of the output APK file.
 *
 * @return The default output file name if the [tagBuildFile] is not available.
 */
private fun createDefaultOutputFileName(
    baseFileName: String?,
    outputFileName: String,
): String {
    return (baseFileName ?: DEFAULT_BASE_FILE_NAME)
        .let { "$it.${outputFileName.split(".").last()}" }
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
    val apkOutputFileName: String,
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
