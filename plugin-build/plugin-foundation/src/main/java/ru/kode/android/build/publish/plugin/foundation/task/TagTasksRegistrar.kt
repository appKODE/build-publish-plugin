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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal const val PRINT_LAST_INCREASED_TAG_TASK_PREFIX = "printLastIncreasedTag"
internal const val GET_LAST_TAG_TASK_PREFIX = "getLastTag"

internal const val DEFAULT_BASE_FILE_NAME = "dev-build"
internal const val DEFAULT_BUILD_VERSION = "v0.0.1"
internal const val DEFAULT_VERSION_NAME = "$DEFAULT_BUILD_VERSION-dev"
internal const val DEFAULT_VERSION_CODE = 1

internal object TagTasksRegistrar {
    internal fun registerLastTagTask(
        project: Project,
        params: LastTagTaskParams,
    ): LastTagTaskOutput {
        val lastBuildTag = project.registerGetLastTagTask(params)
        val useVersionsFromTag =
            params
                .useVersionsFromTag
                .get()
        val useDefaultVersionsAsFallback =
            params
                .useDefaultsForVersionsAsFallback
                .get()
        val versionCode =
            when {
                useVersionsFromTag -> lastBuildTag.map(::mapToVersionCode)
                useDefaultVersionsAsFallback -> project.provider { DEFAULT_VERSION_CODE }
                else -> null
            }
        val apkOutputFileName =
            if (useVersionsFromTag) {
                params.baseFileName.zip(lastBuildTag) { baseFileName, tagBuildFile ->
                    mapToOutputApkFileName(tagBuildFile, params.apkOutputFileName, baseFileName)
                }
            } else {
                params.baseFileName.map { baseFileName ->
                    createDefaultOutputFileName(baseFileName, params.apkOutputFileName)
                }
            }
        val versionName =
            when {
                useVersionsFromTag -> {
                    lastBuildTag.map { tagBuildFile ->
                        mapToVersionName(tagBuildFile, params.buildVariant)
                    }
                }

                useDefaultVersionsAsFallback -> project.provider { DEFAULT_VERSION_NAME }
                else -> null
            }

        return LastTagTaskOutput(
            versionName = versionName,
            versionCode = versionCode,
            apkOutputFileName = apkOutputFileName,
            lastBuildTagFile = lastBuildTag,
        )
    }

    internal fun registerPrintLastIncreasedTagTask(
        project: Project,
        params: PrintLastIncreasedTagTaskParams,
    ): TaskProvider<PrintLastIncreasedTag> {
        return project.tasks.registerPrintLastIncreasedTagTask(params)
    }
}

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
    }.flatMap { it.tagBuildFile }
}

@Suppress("MaxLineLength") // One parameter function
private fun TaskContainer.registerPrintLastIncreasedTagTask(params: PrintLastIncreasedTagTaskParams): TaskProvider<PrintLastIncreasedTag> {
    return register(
        "$PRINT_LAST_INCREASED_TAG_TASK_PREFIX${params.buildVariant.capitalizedName()}",
        PrintLastIncreasedTag::class.java,
    ) { task ->
        task.buildTagFile.set(params.lastBuildTagFile)
    }
}

private fun mapToVersionName(
    tagBuildFile: RegularFile,
    buildVariant: BuildVariant,
): String {
    val file = tagBuildFile.asFile
    return if (file.exists()) {
        fromJson(tagBuildFile.asFile).name
    } else {
        "$DEFAULT_BUILD_VERSION-${buildVariant.name}"
    }
}

private fun mapToVersionCode(tagBuildFile: RegularFile): Int {
    val file = tagBuildFile.asFile
    return if (file.exists()) {
        fromJson(file).buildNumber
    } else {
        DEFAULT_VERSION_CODE
    }
}

private fun mapToOutputApkFileName(
    tagBuildFile: RegularFile,
    outputFileName: String,
    baseFileName: String?,
): String {
    val file = tagBuildFile.asFile
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

private fun createDefaultOutputFileName(
    baseFileName: String?,
    outputFileName: String,
): String {
    return (baseFileName ?: DEFAULT_BASE_FILE_NAME)
        .let { "$it.${outputFileName.split(".").last()}" }
}

internal data class LastTagTaskOutput(
    val versionName: Provider<String>?,
    val versionCode: Provider<Int>?,
    val apkOutputFileName: Provider<String>,
    val lastBuildTagFile: Provider<RegularFile>,
)

internal data class LastTagTaskParams(
    val buildVariant: BuildVariant,
    val apkOutputFileName: String,
    val useVersionsFromTag: Provider<Boolean>,
    val baseFileName: Provider<String>,
    val useDefaultsForVersionsAsFallback: Provider<Boolean>,
    val useStubsForTagAsFallback: Provider<Boolean>,
    val buildTagPattern: Provider<String>,
)

internal data class PrintLastIncreasedTagTaskParams(
    val buildVariant: BuildVariant,
    val lastBuildTagFile: Provider<RegularFile>,
)
