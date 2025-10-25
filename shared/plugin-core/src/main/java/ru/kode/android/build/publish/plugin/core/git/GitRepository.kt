package ru.kode.android.build.publish.plugin.core.git

import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.enity.TagRange

/**
 * Provides high-level Git repository operations for build and publish workflows.
 *
 * This class serves as a facade over [GitCommandExecutor] to provide more complex Git operations
 * such as finding tag ranges and extracting formatted commit messages. It's primarily used
 * for generating changelogs and managing version tags during the build process.
 *
 * @property gitCommandExecutor The underlying executor used to perform Git operations
 * @see GitCommandExecutor For lower-level Git operations
 */
class GitRepository(
    private val gitCommandExecutor: GitCommandExecutor,
) {
    /**
     * Finds the range between the given [buildTag] and the previous matching build tag.
     *
     * This function uses the provided [buildTagPattern] to create a [Regex] used to
     * identify related build tags. It then delegates to [gitCommandExecutor.findTagRange]
     * to locate the current and previous build tags that match the pattern.
     *
     * If a previous build tag is found, it returns a [TagRange] containing both the
     * current and previous tags. If no previous tag exists, the [previousBuildTag] in
     * the returned [TagRange] will be `null`. If no matching tags are found at all,
     * this function returns `null`.
     *
     * @param buildTag the current build tag serving as the upper bound of the tag range.
     * @param buildTagPattern the regex pattern used to find related build tags in the repository.
     *
     * @return a [TagRange] representing the range between [buildTag] and its previous matching tag,
     *         or `null` if no matching tags could be found.
     */
    fun findTagRange(
        buildTag: Tag.Build,
        buildTagPattern: String,
    ): TagRange? {
        val buildTagRegex = Regex(buildTagPattern)
        return gitCommandExecutor.findTagRange(buildTag, buildTagRegex)
            ?.let { tags ->
                val previousBuildTag = tags.getOrNull(1)
                TagRange(
                    currentBuildTag = buildTag,
                    previousBuildTag = previousBuildTag,
                )
            }
    }

    /**
     * Finds the most recent build tag for a given build variant.
     *
     * @param buildVariant The build variant to find the tag for (e.g., "debug", "release")
     * @param buildTagPattern Regex pattern used to match build tags
     *
     * @return The most recent [Tag.Build] matching the pattern, or null if none found
     * @throws IllegalArgumentException If the build tag pattern is invalid
     */
    fun findRecentBuildTag(
        buildVariant: String,
        buildTagPattern: String,
    ): Tag.Build? {
        val buildTagRegex = Regex(buildTagPattern)
        val tags = gitCommandExecutor.findBuildTags(buildTagRegex, limitResultCount = 1)
        return tags?.first()?.let { Tag.Build(it, buildVariant) }
    }

    /**
     * Extracts and formats commit messages containing a specific key within a tag range.
     *
     * This method finds all commits between two tags, extracts lines containing the specified key,
     * and formats them with bullet points. This is typically used to generate changelog entries.
     *
     * @param messageKey The key to search for in commit messages (e.g., "CHANGELOG")
     * @param tagRange The range of tags to search between
     *
     * @return A list of formatted commit messages, each prefixed with a bullet point
     * @throws IllegalArgumentException If the tag range is invalid
     */
    fun markedCommitMessages(
        messageKey: String,
        tagRange: TagRange,
    ): List<String> {
        return gitCommandExecutor
            .extractMarkedCommitMessages(messageKey, tagRange.asCommitRange())
            .map { it.replace(Regex("\\s*$messageKey:?\\s*"), "• ").trim() }
    }
}
