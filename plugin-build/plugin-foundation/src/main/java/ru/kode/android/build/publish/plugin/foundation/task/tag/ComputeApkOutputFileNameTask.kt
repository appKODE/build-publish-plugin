package ru.kode.android.build.publish.plugin.foundation.task.tag

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
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

abstract class ComputeApkOutputFileNameTask : DefaultTask() {
    init {
        group = BasePlugin.BUILD_GROUP
        outputs.upToDateWhen { false }
    }

    @get:ServiceReference
    abstract val loggerService: Property<LoggerService>

    @get:Internal
    abstract val buildVariant: Property<BuildVariant>

    @get:Internal
    abstract val outputApkNameStrategy: Property<OutputApkNameStrategy>

    @get:Input
    abstract val apkOutputFileName: Property<String>

    @get:Input
    abstract val useVersionsFromTag: Property<Boolean>

    @get:Input
    abstract val baseFileName: Property<String>

    @get:InputFile
    abstract val buildTagSnapshotFile: RegularFileProperty

    @get:OutputFile
    abstract val apkOutputFileNameFile: RegularFileProperty

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
