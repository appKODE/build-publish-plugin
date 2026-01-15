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
import ru.kode.android.build.publish.plugin.core.strategy.BuildVersionCodeStrategy
import ru.kode.android.build.publish.plugin.core.strategy.DEFAULT_VERSION_CODE
import ru.kode.android.build.publish.plugin.core.strategy.VersionCodeStrategy
import ru.kode.android.build.publish.plugin.foundation.messages.computedVersionCodeMessage
import ru.kode.android.build.publish.plugin.foundation.messages.formDefaultVersionCodeMessage
import ru.kode.android.build.publish.plugin.foundation.messages.formNullVersionCodeMessage
import ru.kode.android.build.publish.plugin.foundation.messages.formRichVersionCodeMessage
import ru.kode.android.build.publish.plugin.foundation.messages.resolvedVersionCodeParamsMessage

abstract class ComputeVersionCodeTask : DefaultTask() {
    init {
        group = BasePlugin.BUILD_GROUP
        outputs.upToDateWhen { false }
    }

    @get:ServiceReference
    abstract val loggerService: Property<LoggerService>

    @get:Internal
    abstract val buildVariant: Property<BuildVariant>

    @get:Internal
    abstract val versionCodeStrategy: Property<VersionCodeStrategy>

    @get:InputFile
    abstract val buildTagSnapshotFile: RegularFileProperty

    @get:Input
    abstract val useVersionsFromTag: Property<Boolean>

    @get:Input
    abstract val useDefaultsForFallback: Property<Boolean>

    @get:OutputFile
    abstract val versionCodeFile: RegularFileProperty

    @TaskAction
    fun compute() {
        val buildVariant = buildVariant.get()
        val versionCodeStrategy = versionCodeStrategy.orNull ?: BuildVersionCodeStrategy
        val useVersionsFromTag = useVersionsFromTag.get()
        val useDefaultVersionsAsFallback = useDefaultsForFallback.get()
        val tagSnapshot = buildTagSnapshotFile.get()

        val loggerService = loggerService.get()

        loggerService.info(
            resolvedVersionCodeParamsMessage(
                useVersionsFromTag,
                useDefaultVersionsAsFallback,
                versionCodeStrategy,
                buildVariant.name,
            ),
        )

        val versionCode: Int =
            when {
                useVersionsFromTag -> {
                    val tag =
                        if (tagSnapshot.asFile.exists()) {
                            fromJson(tagSnapshot.asFile).current
                        } else {
                            null
                        }
                    loggerService.info(formRichVersionCodeMessage(buildVariant, tag))
                    versionCodeStrategy.build(buildVariant, tag)
                }
                useDefaultVersionsAsFallback -> {
                    loggerService.info(formDefaultVersionCodeMessage(buildVariant))
                    DEFAULT_VERSION_CODE
                }
                else -> {
                    loggerService.info(formNullVersionCodeMessage(buildVariant))
                    buildVariant.defaultVersionCode ?: 0
                }
            }

        val file = versionCodeFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(versionCode.toString())
        logger.info(computedVersionCodeMessage(buildVariant, versionCode))
    }
}
