package ru.kode.android.build.publish.plugin.core.git

import ru.kode.android.build.publish.plugin.core.enity.BuildTagSnapshot
import ru.kode.android.build.publish.plugin.core.enity.Tag

/**
 * Provides high-level Git repository operations tailored for build and publish workflows.
 *
 * This class serves as a facade over [GitCommandExecutor], offering a more convenient API for
 * common Git operations needed during the build process. It simplifies tasks such as:
 * - Finding and parsing build tags
 * - Extracting and formatting commit messages for changelogs
 * - Managing version tags and their metadata
 *
 * The class is designed to be used within the build pipeline to interact with the Git repository
 * in a type-safe and consistent manner.
 *
 * @see GitCommandExecutor For lower-level Git command execution capabilities.
 * @see BuildTagSnapshot For information about build tag snapshots.
 * @see Tag For details about Git tag representations.
 */
class GitRepository(
    /**
     * The underlying executor used to perform low-level Git operations.
     */
    private val gitCommandExecutor: GitCommandExecutor,
) {
    /**
     * Finds the most recent build tag matching the specified pattern for a given build variant.
     *
     * This method searches through the Git history to find tags that match the provided
     * [buildTagPattern] and are associated with the specified [buildVariant]. The search is
     * performed in reverse chronological order, so the most recent matching tag is returned.
     *
     * @param buildVariant The build variant to search tags for (e.g., "debug", "release").
     * @param buildTagPattern Regex pattern used to match and parse build tags. The pattern
     *                       should include capture groups for version components.
     *
     * @return A [BuildTagSnapshot] containing the most recent matching tag and its metadata,
     *         or `null` if no matching tag is found.
     *
     * @throws IllegalArgumentException If the [buildTagPattern] is not a valid regular expression
     *                                 or if it doesn't contain the expected capture groups.
     *
     * @sample
     * ```kotlin
     * val snapshot = gitRepository.findTagSnapshot("debug", "app\\.(\\d+)\\.(\\d+)\\.(\\d+)-debug")
     * println("Latest version: ${snapshot?.current}")
     * ```
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
     * filters those that contain the specified [messageKey], and formats each matching
     * commit message as a bullet point for inclusion in a changelog.
     *
     * The method supports two modes of operation controlled by the [excludeKey] parameter:
     * - When `true`, the [messageKey] and any trailing colon are removed from the messages
     * - When `false`, the original message including the key is preserved
     *
     * Each line in the output is prefixed with a bullet point (•) for consistent formatting.
     *
     * @param messageKey The key used to identify relevant commit messages (e.g., `"CHANGELOG"`).
     *                   Only commits containing this key (case-sensitive) will be included.
     * @param excludeKey Whether to remove the [messageKey] from the output messages.
     * @param buildTagSnapshot The range of tags within which to analyze commits.
     *
     * @return A list of formatted commit messages, each prefixed with a bullet point.
     *
     * @throws IllegalArgumentException if the provided [buildTagSnapshot] is invalid or if
     *                                 the tag range cannot be determined.
     *
     * @sample
     * ```kotlin
     * val messages = gitRepository.markedCommitMessages(
     *     messageKey = "CHANGELOG",
     *     excludeKey = true,
     *     buildTagSnapshot = snapshot
     * )
     * ```
     */
    fun markedCommitMessages(
        messageKey: String,
        buildTagSnapshot: BuildTagSnapshot,
    ): List<String> {
        return gitCommandExecutor
            .extractMarkedCommitMessages(messageKey, buildTagSnapshot.asCommitRange())
    }
}
