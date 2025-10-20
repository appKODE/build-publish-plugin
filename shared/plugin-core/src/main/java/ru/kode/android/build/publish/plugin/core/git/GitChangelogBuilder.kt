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
     * Builds a changelog string for a given build tag by determining the tag range and collecting
     * commit messages between the tags.
     *
     * This function first attempts to find a [TagRange] between the specified [buildTag] and the
     * previous matching build tag using [buildTagPattern]. If no valid tag range is found, a warning
     * is logged and `null` is returned.
     *
     * If a valid tag range is found, it calls [buildChangelog] using the commit messages associated
     * with the provided [messageKey] within that range. If no changelog content is generated, it
     * optionally uses [defaultValueSupplier] to provide a fallback value.
     *
     * @param messageKey a key used to identify which commit messages to include in the changelog.
     * @param buildTag the build tag for which the changelog is being generated.
     * @param buildTagPattern the pattern used to find related build tags for determining the tag range.
     * @param defaultValueSupplier an optional function that supplies a default changelog string
     *        when no changelog can be built; receives the [TagRange] as input.
     *
     * @return the generated changelog string, the value from [defaultValueSupplier] if provided,
     *         or `null` if no tag range could be determined or no changelog could be built.
     */
    @Suppress("ReturnCount")
    fun buildForTag(
        messageKey: String,
        buildTag: Tag.Build,
        buildTagPattern: String,
        defaultValueSupplier: ((TagRange) -> String?)? = null,
    ): String? {
        val tagRange =
            gitRepository.findTagRange(buildTag, buildTagPattern)
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
