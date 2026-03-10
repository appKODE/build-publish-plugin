package ru.kode.android.build.publish.plugin.foundation.task.tag

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.git.mapper.fromJson
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.core.strategy.OutputApkNameStrategy
import ru.kode.android.build.publish.plugin.core.strategy.OutputBundleNameStrategy
import ru.kode.android.build.publish.plugin.core.strategy.SimpleApkNamingStrategy
import ru.kode.android.build.publish.plugin.core.strategy.VersionedApkNamingStrategy
import ru.kode.android.build.publish.plugin.core.strategy.VersionedBundleNamingStrategy
import ru.kode.android.build.publish.plugin.foundation.messages.computedBundleOutputFileNameMessage
import ru.kode.android.build.publish.plugin.foundation.messages.formRichBundleFileNameMessage
import ru.kode.android.build.publish.plugin.foundation.messages.formSimpleBundleFileNameMessage
import ru.kode.android.build.publish.plugin.foundation.messages.resolvedBundleOutputFileNameParamsMessage

/**
 * Computes the final output Bundle (AAB) file name for a specific Android build variant.
 *
 * The task can optionally incorporate version/tag metadata (from
 * [ru.kode.android.build.publish.plugin.foundation.task.tag.GetLastTagSnapshotTask]) using an
 * [OutputApkNameStrategy]. The computed name is written to [bundleOutputFileNameFile] as plain text
 * and later used by tasks that rename/move the produced Bundle.
 */
@CacheableTask
abstract class ComputeBundleOutputFileNameTask : DefaultTask() {
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
     * Strategy used to build the final Bundle file name.
     *
     * If not set, [VersionedApkNamingStrategy] is used.
     */
    @get:Internal
    abstract val outputBundleNameStrategy: Property<OutputBundleNameStrategy>

    /**
     * The original output file name produced by the Android build.
     */
    @get:Input
    abstract val bundleOutputFileName: Property<String>

    /**
     * Whether to include tag/version data when building the final file name.
     */
    @get:Input
    abstract val useVersionsFromTag: Property<Boolean>

    /**
     * User-defined base name (prefix) for the produced Bundle file.
     */
    @get:Input
    abstract val baseFileName: Property<String>

    /**
     * JSON file with the last build tag snapshot.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val buildTagSnapshotFile: RegularFileProperty

    /**
     * Output file containing the computed Bundle file name.
     */
    @get:OutputFile
    abstract val bundleOutputFileNameFile: RegularFileProperty

    /**
     * Performs the computation and writes the result into [bundleOutputFileNameFile].
     */
    @TaskAction
    fun compute() {
        val logger = loggerService.get()

        val outputFileName = bundleOutputFileName.get()
        val useVersionsFromTag = useVersionsFromTag.get()
        val baseFileName = baseFileName.get()
        val buildVariant = buildVariant.get()
        val strategy = outputBundleNameStrategy.orNull ?: VersionedBundleNamingStrategy
        val tagSnapshot = buildTagSnapshotFile.get()

        logger.info(
            resolvedBundleOutputFileNameParamsMessage(
                outputFileName,
                useVersionsFromTag,
                strategy,
                buildVariant.name,
            ),
        )

        val bundleOutputFileName =
            if (useVersionsFromTag) {
                val tag =
                    if (tagSnapshot.asFile.exists()) {
                        fromJson(tagSnapshot.asFile).current
                    } else {
                        null
                    }
                logger.info(
                    formRichBundleFileNameMessage(
                        buildVariant,
                        outputFileName,
                        tag,
                        baseFileName,
                    ),
                )
                strategy.build(outputFileName, tag, baseFileName)
            } else {
                logger.info(formSimpleBundleFileNameMessage(buildVariant, baseFileName))
                SimpleApkNamingStrategy.build(outputFileName, null, baseFileName)
            }

        val file = bundleOutputFileNameFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(bundleOutputFileName)

        logger.info(computedBundleOutputFileNameMessage(buildVariant, bundleOutputFileName))
    }
}
