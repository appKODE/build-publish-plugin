package ru.kode.android.build.publish.plugin.foundation.task.tag.work

import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.git.mapper.toJson
import ru.kode.android.build.publish.plugin.foundation.service.git.GitExecutorService
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_BUILD_VERSION
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_VERSION_CODE
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_TAG_COMMIT_SHA
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_TAG_COMMIT_MESSAGE
import ru.kode.android.build.publish.plugin.foundation.task.DEFAULT_TAG_NAME
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
        private val logger = Logging.getLogger(this::class.java)

        override fun execute() {
            val buildVariant = parameters.buildVariant.get()
            val buildTagPattern = parameters.buildTagPattern.get()
            val tagBuildOutput = parameters.tagBuildFile.asFile.get()
            val useStubsForTagAsFallback = parameters.useStubsForTagAsFallback.get()

            val buildTag =
                parameters.gitExecutorService.get()
                    .repository
                    .findRecentBuildTag(buildVariant, buildTagPattern)

            // NOTE: 0 or negative values for build number are invalid — project will fail
            val isTagValid = buildTag != null && buildTag.buildNumber >= DEFAULT_VERSION_CODE

            when {
                buildTag != null && isTagValid -> {
                    logger.info(
                        "Valid build tag '${buildTag.name}' found for '$buildVariant' " +
                            "(build number: ${buildTag.buildNumber}). Generating tag build file..."
                    )
                    tagBuildOutput.writeText(buildTag.toJson())
                }

                buildTag != null && !isTagValid -> {
                    val errorMessage =
                        """
                        |Invalid build tag '${buildTag.name}' for '$buildVariant' variant.
                        |Detected build number: ${buildTag.buildNumber}, expected >= $DEFAULT_VERSION_CODE.
                        |
                        |According to Google Play requirements, every Android build must have a positive 
                        |(greater than 0) and incrementing version code. A tag producing a non-positive or 
                        |reset build number cannot be used for release builds.
                        |
                        |Fix:
                        |1. Ensure the tag encodes a valid build number (>= $DEFAULT_VERSION_CODE)
                        |2. Delete and recreate the incorrect tag if necessary:
                        |   git tag -d ${buildTag.name} && git push origin :refs/tags/${buildTag.name}
                        |   git tag <correct_tag> && git push origin <correct_tag>
                        |3. Re-run the build after correcting the tag.
                        """.trimMargin()

                    logger.error(
                        "Invalid build tag '${buildTag.name}' for '$buildVariant' — " +
                            "build number (${buildTag.buildNumber}) < expected minimum ($DEFAULT_VERSION_CODE)."
                    )
                    throw GradleException(errorMessage)
                }

                useStubsForTagAsFallback -> {
                    val stubTag = Tag.Build(
                        name = DEFAULT_TAG_NAME.format(buildVariant),
                        commitSha = DEFAULT_TAG_COMMIT_SHA,
                        message = DEFAULT_TAG_COMMIT_MESSAGE,
                        buildVersion = DEFAULT_BUILD_VERSION,
                        buildVariant = buildVariant,
                        buildNumber = DEFAULT_VERSION_CODE,
                    )
                    tagBuildOutput.writeText(stubTag.toJson())
                    logger.warn(
                        "Using stub tag for build variant '$buildVariant' " +
                            "because no valid tag was found using pattern '$buildTagPattern'."
                    )
                }

                else -> {
                    val errorMessage =
                        """
                        |Build tag file not created for '$buildVariant' build variant 
                        |and no stub tag was used because 'useStubsForTagAsFallback' is false.
                        |
                        |Possible reasons:
                        |- The pattern '$buildTagPattern' is incorrect
                        |- No matching tag exists in the repository
                        |- The tag exists but wasn’t fetched
                        |
                        |This is a critical error as other tasks depend on this file.
                        |
                        |Troubleshooting steps:
                        |1. Verify that a tag matching the pattern exists:
                        |   git tag -l '$buildTagPattern'
                        |2. If the tag exists but isn't being found, try fetching all tags:
                        |   git fetch --all --tags
                        |3. Check the pattern in your build configuration
                        |4. For more details, run with --info flag
                        """.trimMargin()

                    logger.error(
                        "No build tag found for '$buildVariant' using pattern '$buildTagPattern'. " +
                            "Stub tag generation disabled (`useStubsForTagAsFallback` = false)."
                    )
                    throw GradleException(errorMessage)
                }
            }
        }
    }
