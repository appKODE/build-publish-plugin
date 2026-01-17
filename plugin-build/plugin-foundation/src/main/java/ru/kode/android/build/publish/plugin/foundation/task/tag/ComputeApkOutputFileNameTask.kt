package ru.kode.android.build.publish.plugin.foundation.task.tag

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.git.mapper.fromJson
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.core.strategy.OutputApkNameStrategy
import ru.kode.android.build.publish.plugin.core.strategy.SimpleApkNamingStrategy
import ru.kode.android.build.publish.plugin.core.strategy.VersionedApkNamingStrategy
import ru.kode.android.build.publish.plugin.foundation.messages.computedApkOutputFileNameMessage
import ru.kode.android.build.publish.plugin.foundation.messages.formRichApkFileNameMessage
import ru.kode.android.build.publish.plugin.foundation.messages.formSimpleApkFileNameMessage
import ru.kode.android.build.publish.plugin.foundation.messages.resolvedApkOutputFileNameParamsMessage

/**
 * Computes the final output APK file name for a specific Android build variant.
 *
 * The task can optionally incorporate version/tag metadata (from
 * [ru.kode.android.build.publish.plugin.foundation.task.tag.GetLastTagSnapshotTask]) using an
 * [OutputApkNameStrategy]. The computed name is written to [apkOutputFileNameFile] as plain text
 * and later used by tasks that rename/move the produced APK.
 */
abstract class ComputeApkOutputFileNameTask : DefaultTask() {
    init {
        group = BasePlugin.BUILD_GROUP
        outputs.upToDateWhen { false }
    }

    /**
     * Provides structured logging for the task execution.
     */
    @get:Internal
    abstract val loggerService: Property<LoggerService>

    /**
     * The Android build variant for which the output file name is calculated.
     */
    @get:Internal
    abstract val buildVariant: Property<BuildVariant>

    /**
     * Strategy used to build the final APK file name.
     *
     * If not set, [VersionedApkNamingStrategy] is used.
     */
    @get:Internal
    abstract val outputApkNameStrategy: Property<OutputApkNameStrategy>

    /**
     * The original output file name produced by the Android build.
     */
    @get:Input
    abstract val apkOutputFileName: Property<String>

    /**
     * Whether to include tag/version data when building the final file name.
     */
    @get:Input
    abstract val useVersionsFromTag: Property<Boolean>

    /**
     * User-defined base name (prefix) for the produced APK file.
     */
    @get:Input
    abstract val baseFileName: Property<String>

    /**
     * JSON file with the last build tag snapshot.
     */
    @get:InputFile
    abstract val buildTagSnapshotFile: RegularFileProperty

    /**
     * Output file containing the computed APK file name.
     */
    @get:OutputFile
    abstract val apkOutputFileNameFile: RegularFileProperty

    /**
     * Performs the computation and writes the result into [apkOutputFileNameFile].
     */
    @TaskAction
    fun compute() {
        val logger = loggerService.get()

        val outputFileName = apkOutputFileName.get()
        val useVersionsFromTag = useVersionsFromTag.get()
        val baseFileName = baseFileName.get()
        val buildVariant = buildVariant.get()
        val strategy = outputApkNameStrategy.orNull ?: VersionedApkNamingStrategy
        val tagSnapshot = buildTagSnapshotFile.get()

        logger.info(
            resolvedApkOutputFileNameParamsMessage(
                outputFileName,
                useVersionsFromTag,
                strategy,
                buildVariant.name,
            ),
        )

        val apkOutputFileName =
            if (useVersionsFromTag) {
                val tag =
                    if (useVersionsFromTag && tagSnapshot.asFile.exists()) {
                        fromJson(tagSnapshot.asFile).current
                    } else {
                        null
                    }
                logger.info(
                    formRichApkFileNameMessage(
                        buildVariant,
                        outputFileName,
                        tag,
                        baseFileName,
                    ),
                )
                strategy.build(outputFileName, tag, baseFileName)
            } else {
                logger.info(formSimpleApkFileNameMessage(buildVariant, baseFileName))
                SimpleApkNamingStrategy.build(outputFileName, null, baseFileName)
            }

        val file = apkOutputFileNameFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(apkOutputFileName)

        logger.info(computedApkOutputFileNameMessage(buildVariant, apkOutputFileName))
    }
}
