package ru.kode.android.build.publish.plugin.core.git

import org.gradle.api.logging.Logger
import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.enity.TagRange

/**
 * Builds changelogs by extracting and formatting commit messages from Git history.
 *
 * This class provides functionality to generate human-readable changelogs by analyzing
 * Git commit messages between tags. It supports custom message keys and fallback values.
 *
 * @property gitRepository The Git repository to extract commit history from
 * @property logger Optional logger for warning and debug messages
 *
 * @see GitRepository For repository operations used to gather commit data
 */
class GitChangelogBuilder(
    private val gitRepository: GitRepository,
    private val logger: Logger?,
) {
    /**
     * Builds a changelog for a specific build tag.
     *
     * This method generates a changelog by extracting commit messages that contain the specified
     * [messageKey] from the Git history between the current build tag and the previous one.
     *
     * @param messageKey The key to search for in commit messages (e.g., "CHANGELOG")
     * @param buildTag The build tag to generate the changelog for
     * @param buildTagPattern Regex pattern used to match build tags (e.g., ".*\\.(\\d+)-%s")
     * @param defaultValueSupplier Optional supplier for a default changelog when no commits are found
     *
     * @return The formatted changelog, or null if no relevant commits or tags are found
     * @throws IllegalArgumentException If the build tag pattern is invalid
     *
     * @see Tag.Build For information about build tags
     */
    @Suppress("ReturnCount")
    fun buildForBuildTag(
        messageKey: String,
        buildTag: Tag.Build,
        buildTagPattern: String,
        defaultValueSupplier: ((TagRange) -> String?)? = null,
    ): String? {
        val buildVariant = buildTag.buildVariant
        return buildForBuildVariant(messageKey, buildVariant, buildTagPattern, defaultValueSupplier)
    }

    /**
     * Builds a changelog for a specific build variant.
     *
     * This internal method finds the appropriate tags for the build variant and generates
     * a changelog from the commit messages between them.
     *
     * @param messageKey The key to search for in commit messages
     * @param buildVariant The build variant to generate the changelog for (e.g., "debug", "release")
     * @param buildTagPattern Regex pattern used to match build tags
     * @param defaultValueSupplier Optional supplier for a default changelog when no commits are found
     *
     * @return The formatted changelog, or null if no relevant commits or tags are found
     */
    private fun buildForBuildVariant(
        messageKey: String,
        buildVariant: String,
        buildTagPattern: String,
        defaultValueSupplier: ((TagRange) -> String?)? = null,
    ): String? {
        val tagRange =
            gitRepository.findTagRange(buildVariant, buildTagPattern)
                .also { if (it == null) logger?.warn("failed to build a changelog: no build tags") }
                ?: return null
        return buildChangelog(
            tagRange,
            { gitRepository.markedCommitMessages(messageKey, tagRange) },
        ) ?: defaultValueSupplier?.invoke(tagRange)
    }

    /**
     * Constructs the final changelog text from the given tag range and commit messages.
     *
     * This method formats the changelog with the annotated tag message (if present) and
     * the list of commit messages, each prefixed with a bullet point.
     *
     * @param tagRange The range of tags to include in the changelog
     * @param markedCommitMessagesResolver Function that provides the list of commit messages
     *
     * @return The formatted changelog string, or null if no messages are found
     */
    private fun buildChangelog(
        tagRange: TagRange,
        markedCommitMessagesResolver: () -> List<String>,
    ): String? {
        val messageBuilder =
            StringBuilder().apply {
                val annotatedTagMessage = tagRange.currentBuildTag.message
                if (annotatedTagMessage?.isNotBlank() == true) {
                    appendLine("*$annotatedTagMessage*")
                }
            }

        // it can happen that 2 tags point to the same commit, so no extraction of changelog is necessary
        // (but remember, tags can be annotated - which is taken care of above)
        if (tagRange.currentBuildTag.commitSha != tagRange.previousBuildTag?.commitSha) {
            markedCommitMessagesResolver()
                .forEach { messageBuilder.appendLine(it) }
        }
        return messageBuilder.toString().takeIf { it.isNotBlank() }?.trim()
    }
}
