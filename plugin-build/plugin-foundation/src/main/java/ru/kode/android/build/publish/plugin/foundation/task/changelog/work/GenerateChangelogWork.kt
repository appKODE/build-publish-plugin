package ru.kode.android.build.publish.plugin.foundation.task.changelog.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.core.git.mapper.fromJson
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.foundation.messages.changelogGeneratedMessage
import ru.kode.android.build.publish.plugin.foundation.messages.changelogNotGeneratedMessage
import ru.kode.android.build.publish.plugin.foundation.messages.noChangedDetectedSinceStartMessage
import ru.kode.android.build.publish.plugin.foundation.messages.noChangesChangelogMessage
import ru.kode.android.build.publish.plugin.foundation.messages.noChangesDetectedSinceBuildMessage
import ru.kode.android.build.publish.plugin.foundation.service.git.GitExecutorService
import javax.inject.Inject

/**
 * Defines the parameters required for the [GenerateChangelogWork] task.
 *
 * This interface extends Gradle's [WorkParameters] and provides all necessary
 * configuration for generating changelog information from Git commit history.
 */
internal interface GenerateChangelogParameters : WorkParameters {
    /**
     *  The key used to filter relevant commit messages
     */
    val commitMessageKey: Property<String>

    /**
     * Indicates whether the [commitMessageKey] should be removed from commit messages
     * in the generated changelog output.
     */
    val excludeMessageKey: Property<Boolean>

    /**
     * The pattern used to match Git tags for versioning
     */
    val buildTagPattern: Property<String>

    /**
     * JSON file containing information about the last build tag
     */
    val tagBuildFile: RegularFileProperty

    /**
     * The output file where the changelog will be written
     */
    val changelogFile: RegularFileProperty

    /**
     * Service for executing Git commands and retrieving commit history
     */
    val gitExecutorService: Property<GitExecutorService>

    /**
     * Service for logging messages during the changelog generation process
     */
    val loggerService: Property<LoggerService>
}

/**
 * A Gradle work action that generates a changelog by analyzing Git commit history.
 *
 * This work action is responsible for:
 * - Retrieving commit history between the last tag and HEAD
 * - Filtering commits based on a message key
 * - Formatting the changelog output
 * - Writing the results to a file
 *
 * The work is performed in a background thread to avoid blocking the main Gradle build thread.
 *
 * @see WorkAction
 * @see GenerateChangelogParameters
 */
internal abstract class GenerateChangelogWork
    @Inject
    constructor() : WorkAction<GenerateChangelogParameters> {
        override fun execute() {
            val messageKey = parameters.commitMessageKey.get()
            val excludeMessageKey = parameters.excludeMessageKey.get()
            val buildTagPattern = parameters.buildTagPattern.get()
            val currentBuildTag = fromJson(parameters.tagBuildFile.asFile.get())
            val logger = parameters.loggerService.get()

            val changelog =
                parameters.gitExecutorService.get()
                    .gitChangelogBuilder
                    .buildForTag(
                        messageKey,
                        excludeMessageKey,
                        currentBuildTag,
                        buildTagPattern,
                        defaultValueSupplier = { tagRange ->
                            val previousBuildName = tagRange.previousBuildTag?.name
                            if (previousBuildName != null) {
                                noChangesDetectedSinceBuildMessage(previousBuildName)
                            } else {
                                noChangedDetectedSinceStartMessage()
                            }
                        },
                    )
            val changelogOutput = parameters.changelogFile.asFile.get()

            if (changelog.isNullOrBlank()) {
                val noChangesMessage = noChangesChangelogMessage(currentBuildTag)
                logger.info(changelogNotGeneratedMessage(buildTagPattern, currentBuildTag))
                changelogOutput.writeText(noChangesMessage)
            } else {
                logger.info(changelogGeneratedMessage(buildTagPattern, currentBuildTag))
                changelogOutput.writeText(changelog)
            }
        }
    }
