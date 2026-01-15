package ru.kode.android.build.publish.plugin.core.git

import ru.kode.android.build.publish.plugin.core.enity.BuildTagSnapshot
import ru.kode.android.build.publish.plugin.core.enity.Tag

private const val DOT_PREFIX = "•"

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
     * Finds the most recent build tag for a given build variant.
     *
     * @param buildTagPattern Regex pattern used to match build tags
     *
     * @return The most recent [Tag.Build] matching the pattern, or null if none found
     * @throws IllegalArgumentException If the build tag pattern is invalid
     */
    fun findTagSnapshot(
        buildVariant: String,
        buildTagPattern: String,
    ): BuildTagSnapshot? {
        val buildTagRegex = Regex(buildTagPattern)
        return gitCommandExecutor.findSnapshot(buildVariant, buildTagRegex)
    }

    /**
     * Extracts and formats commit messages containing a specific key within a tag range.
     *
     * This method retrieves all commits between two tags defined by [buildTagSnapshot],
     * filters those that contain the specified [messageKey], and formats each
     * matching commit message as a bullet point for inclusion in a changelog.
     *
     * If [excludeKey] is `true`, occurrences of the [messageKey] (for example `[CHANGELOG]`)
     * are removed from the commit messages before formatting. When `false`, the key remains
     * visible in the resulting messages.
     *
     * This function is typically used to prepare human-readable changelog entries
     * from the commit history.
     *
     * @param messageKey the key used to identify relevant commit messages (e.g., `"CHANGELOG"`).
     * @param excludeKey whether to remove the [messageKey] from the commit messages in the output.
     * @param buildTagSnapshot the range of tags within which commits are analyzed.
     *
     * @return a list of formatted commit messages, each prefixed with a bullet point.
     * @throws IllegalArgumentException if the provided [buildTagSnapshot] is invalid.
     */
    fun markedCommitMessages(
        messageKey: String,
        excludeKey: Boolean,
        buildTagSnapshot: BuildTagSnapshot,
    ): List<String> {
        return gitCommandExecutor
            .extractMarkedCommitMessages(messageKey, buildTagSnapshot.asCommitRange())
            .map { message ->
                if (excludeKey) {
                    val cleanMessage = message.replace(Regex("\\s*$messageKey:?\\s*"), "").trim()
                    "$DOT_PREFIX $cleanMessage".trim()
                } else {
                    "$DOT_PREFIX $message".trim()
                }
            }
    }
}
