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

/**
 * Computes the `versionCode` value for a specific Android build variant.
 *
 * The task reads a tag snapshot file (produced by
 * [ru.kode.android.build.publish.plugin.foundation.task.tag.GetLastTagSnapshotTask]) and derives a
 * version code using a configured [VersionCodeStrategy]. Depending on the flags, it can:
 * - Use the tag-derived value
 * - Fall back to [DEFAULT_VERSION_CODE]
 * - Or fall back to the Android DSL defaults attached to [BuildVariant]
 *
 * The computed version code is written to [versionCodeFile] as plain text and later consumed by
 * the Android variant configuration.
 */
abstract class ComputeVersionCodeTask : DefaultTask() {
    init {
        group = BasePlugin.BUILD_GROUP
        outputs.upToDateWhen { false }
    }

    /**
     * Provides structured logging for the task execution.
     */
    @get:ServiceReference
    abstract val loggerService: Property<LoggerService>

    /**
     * The Android build variant for which the version code is calculated.
     */
    @get:Internal
    abstract val buildVariant: Property<BuildVariant>

    /**
     * Strategy used to build the final version code.
     *
     * If not set, [BuildVersionCodeStrategy] is used.
     */
    @get:Internal
    abstract val versionCodeStrategy: Property<VersionCodeStrategy>

    /**
     * JSON file with the last build tag snapshot.
     */
    @get:InputFile
    abstract val buildTagSnapshotFile: RegularFileProperty

    /**
     * Whether to compute the version code using the Git tag snapshot.
     */
    @get:Input
    abstract val useVersionsFromTag: Property<Boolean>

    /**
     * Whether to fall back to [DEFAULT_VERSION_CODE] when tag-based versioning is disabled.
     */
    @get:Input
    abstract val useDefaultsForVersionsAsFallback: Property<Boolean>

    /**
     * Output file containing the computed version code.
     */
    @get:OutputFile
    abstract val versionCodeFile: RegularFileProperty

    /**
     * Performs the computation and writes the result into [versionCodeFile].
     */
    @TaskAction
    fun compute() {
        val buildVariant = buildVariant.get()
        val versionCodeStrategy = versionCodeStrategy.orNull ?: BuildVersionCodeStrategy
        val useVersionsFromTag = useVersionsFromTag.get()
        val useDefaultsForVersionsAsFallback = useDefaultsForVersionsAsFallback.get()
        val tagSnapshot = buildTagSnapshotFile.get()

        val loggerService = loggerService.get()

        loggerService.info(
            resolvedVersionCodeParamsMessage(
                useVersionsFromTag,
                useDefaultsForVersionsAsFallback,
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
                useDefaultsForVersionsAsFallback -> {
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
