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

/**
 * Computes the `versionName` value for a specific Android build variant.
 *
 * The task reads a tag snapshot file (produced by [ru.kode.android.build.publish.plugin.foundation.task.tag.GetLastTagSnapshotTask]) and derives a version
 * name using a configured [VersionNameStrategy]. Depending on the flags, it can:
 * - Use the tag-derived value
 * - Fall back to a default constant
 * - Or fall back to the Android DSL defaults attached to [BuildVariant]
 *
 * The computed version name is written to [versionNameFile] as plain text and later consumed by
 * the Android variant configuration.
 */
abstract class ComputeVersionNameTask : DefaultTask() {
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
     * The Android build variant for which the version name is calculated.
     */
    @get:Internal
    abstract val buildVariant: Property<BuildVariant>

    /**
     * Strategy used to build the final version name.
     *
     * If not set, [BuildVersionNameStrategy] is used.
     */
    @get:Internal
    abstract val versionNameStrategy: Property<VersionNameStrategy>

    /**
     * Whether to compute the version name using the Git tag snapshot.
     */
    @get:Input
    abstract val useVersionsFromTag: Property<Boolean>

    /**
     * Whether to fall back to [DEFAULT_VERSION_NAME] when tag-based versioning is disabled.
     */
    @get:Input
    abstract val useDefaultsForVersionsAsFallback: Property<Boolean>

    /**
     * JSON file with the last build tag snapshot.
     */
    @get:InputFile
    abstract val buildTagSnapshotFile: RegularFileProperty

    /**
     * Output file containing the computed version name.
     */
    @get:OutputFile
    abstract val versionNameFile: RegularFileProperty

    /**
     * Performs the computation and writes the result into [versionNameFile].
     */
    @TaskAction
    fun compute() {
        val logger = loggerService.get()

        val useVersionsFromTag = useVersionsFromTag.get()
        val useDefaultsForVersionsAsFallback = useDefaultsForVersionsAsFallback.get()
        val buildVariant = buildVariant.get()
        val strategy = versionNameStrategy.orNull ?: BuildVersionNameStrategy
        val tagSnapshot = buildTagSnapshotFile.get()

        logger.info(
            resolvedVersionNameMessage(
                useVersionsFromTag,
                useDefaultsForVersionsAsFallback,
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

                useDefaultsForVersionsAsFallback -> {
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
