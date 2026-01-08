package ru.kode.android.build.publish.plugin.foundation.task.tag.work

import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.core.git.mapper.toJson
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.core.strategy.DEFAULT_VERSION_CODE
import ru.kode.android.build.publish.plugin.core.strategy.HardcodedTagGenerationStrategy
import ru.kode.android.build.publish.plugin.foundation.messages.invalidTagMessage
import ru.kode.android.build.publish.plugin.foundation.messages.tagNotCreatedMessage
import ru.kode.android.build.publish.plugin.foundation.messages.usingStabMessage
import ru.kode.android.build.publish.plugin.foundation.messages.validBuildTagFoundMessage
import ru.kode.android.build.publish.plugin.foundation.service.git.GitExecutorService
import javax.inject.Inject

/**
 * Defines the parameters required for the [GenerateTagWork] task.
 *
 * This interface extends Gradle's [WorkParameters] and provides all necessary
 * configuration for generating build tag information.
 */
internal interface GenerateTagParameters : WorkParameters {
    /**
     * The name of the build variant (e.g., 'debug', 'release')
     */
    val buildVariant: Property<String>

    /**
     * The pattern to match Git tags against
     */
    val buildTagPattern: Property<String>

    /**
     * The output file where tag information will be written
     */
    val tagBuildFile: RegularFileProperty

    /**
     * Service for executing Git commands
     */
    val gitExecutorService: Property<GitExecutorService>

    /**
     * The logger service for logging messages during the work.
     */
    val loggerService: Property<LoggerService>

    /**
     * Whether to use stub values when no matching tag is found
     */
    val useStubsForTagAsFallback: Property<Boolean>
}

/**
 * A Gradle work action that generates build tag information by querying Git.
 *
 * This work action is responsible for:
 * - Finding the most recent Git tag matching a specific pattern
 * - Handling fallback behavior when no tag is found
 * - Writing tag information to a JSON file for use by other tasks
 * - Supporting different build variants and tag patterns
 *
 * The work is performed in a background thread to avoid blocking the main Gradle build thread.
 *
 * @see WorkAction
 * @see GenerateTagParameters
 */
internal abstract class GenerateTagWork
    @Inject
    constructor() : WorkAction<GenerateTagParameters> {
        override fun execute() {
            val buildVariant = parameters.buildVariant.get()
            val buildTagPattern = parameters.buildTagPattern.get()
            val tagBuildOutput = parameters.tagBuildFile.asFile.get()
            val useStubsForTagAsFallback = parameters.useStubsForTagAsFallback.get()
            val logger = parameters.loggerService.get()

            val buildTag =
                parameters.gitExecutorService.get()
                    .repository
                    .findRecentBuildTag(buildVariant, buildTagPattern)

            // NOTE: 0 or negative values for build number are invalid — project will fail
            val isTagValid = buildTag != null && buildTag.buildNumber >= DEFAULT_VERSION_CODE

            when {
                buildTag != null && isTagValid -> {
                    logger.quiet(validBuildTagFoundMessage(buildTag, buildVariant))
                    tagBuildOutput.writeText(buildTag.toJson())
                }

                buildTag != null && !isTagValid -> {
                    throw GradleException(invalidTagMessage(buildTag, buildVariant))
                }

                useStubsForTagAsFallback -> {
                    val tag = HardcodedTagGenerationStrategy.build(buildVariant)
                    tagBuildOutput.writeText(tag.toJson())
                    logger.quiet(usingStabMessage(buildVariant, buildTagPattern))
                }

                else -> {
                    throw GradleException(tagNotCreatedMessage(buildVariant, buildTagPattern))
                }
            }
        }
    }
