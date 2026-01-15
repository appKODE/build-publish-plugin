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
import ru.kode.android.build.publish.plugin.core.strategy.BuildVersionNameStrategy
import ru.kode.android.build.publish.plugin.core.strategy.VersionNameStrategy
import ru.kode.android.build.publish.plugin.foundation.messages.computedVersionNameMessage
import ru.kode.android.build.publish.plugin.foundation.messages.formDefaultVersionNameMessage
import ru.kode.android.build.publish.plugin.foundation.messages.formNullVersionNameMessage
import ru.kode.android.build.publish.plugin.foundation.messages.formRichVersionNameMessage
import ru.kode.android.build.publish.plugin.foundation.messages.resolvedVersionNameMessage
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_VERSION_NAME

abstract class ComputeVersionNameTask : DefaultTask() {
    init {
        group = BasePlugin.BUILD_GROUP
        outputs.upToDateWhen { false }
    }

    @get:ServiceReference
    abstract val loggerService: Property<LoggerService>

    @get:Internal
    abstract val buildVariant: Property<BuildVariant>

    @get:Internal
    abstract val versionNameStrategy: Property<VersionNameStrategy>

    @get:Input
    abstract val useVersionsFromTag: Property<Boolean>

    @get:Input
    abstract val useDefaultsForVersionsAsFallback: Property<Boolean>

    @get:InputFile
    abstract val buildTagSnapshotFile: RegularFileProperty

    @get:OutputFile
    abstract val versionNameFile: RegularFileProperty

    @TaskAction
    fun compute() {
        val logger = loggerService.get()

        val useVersionsFromTag = useVersionsFromTag.get()
        val useFallback = useDefaultsForVersionsAsFallback.get()
        val buildVariant = buildVariant.get()
        val strategy = versionNameStrategy.orNull ?: BuildVersionNameStrategy
        val tagSnapshot = buildTagSnapshotFile.get()

        logger.info(
            resolvedVersionNameMessage(
                useVersionsFromTag,
                useFallback,
                strategy,
                buildVariant.name,
            ),
        )

        val versionName: String? =
            when {
                useVersionsFromTag -> {
                    val tag =
                        if (tagSnapshot.asFile.exists()) {
                            fromJson(tagSnapshot.asFile).current
                        } else {
                            null
                        }
                    logger.info(formRichVersionNameMessage(buildVariant, tag))
                    strategy.build(buildVariant, tag)
                }

                useFallback -> {
                    logger.info(formDefaultVersionNameMessage(buildVariant))
                    DEFAULT_VERSION_NAME
                }

                else -> {
                    logger.info(formNullVersionNameMessage(buildVariant))
                    buildVariant.defaultVersionName
                }
            }

        val file = versionNameFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(versionName.orEmpty())
        logger.info(computedVersionNameMessage(buildVariant, versionName))
    }
}
