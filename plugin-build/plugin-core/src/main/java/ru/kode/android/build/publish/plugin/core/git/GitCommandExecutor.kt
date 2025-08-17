package ru.kode.android.build.publish.plugin.core.git

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import ru.kode.android.build.publish.plugin.core.enity.CommitRange
import ru.kode.android.build.publish.plugin.core.enity.Tag

private const val UNKNOWN_COMMIT_INDEX = -1

/**
 * Executes Git commands and provides high-level Git operations.
 *
 * This class serves as a wrapper around the Grgit library to perform common Git operations
 * needed during the build and publish process, such as extracting commit messages and managing tags.
 *
 * @property grgit The Grgit instance used to execute Git commands
 * @see Grgit For more information about the underlying Git operations
 */
class GitCommandExecutor(
    private val grgit: Grgit,
) {
    /**
     * Extracts lines containing the specified key from commit messages within a given range.
     *
     * This method retrieves all commits in the specified range and filters their commit messages
     * to find lines containing the provided key. This is commonly used to extract changelog entries
     * that follow a specific format.
     *
     * @param key The string to search for in commit messages (case-sensitive)
     * @param range The range of commits to search through, or null to search all commits
     *
     * @return A list of matching lines from commit messages
     * @throws org.eclipse.jgit.api.errors.GitAPIException If there's an error accessing the Git repository
     * @see CommitRange For information about how commit ranges are specified
     */
    fun extractMarkedCommitMessages(
        key: String,
        range: CommitRange?,
    ): List<String> {
        return getCommitsByRange(range)
            .flatMap { commit ->
                commit.fullMessage
                    .split('\n')
                    .filter { message -> message.contains(key) }
            }
    }

    /**
     * Retrieves a list of Git tags matching the specified pattern, sorted by build number.
     *
     * This method finds all tags that match the provided regular expression, parses their build numbers,
     * and returns them in descending order (newest first). The result is limited to the specified count.
     *
     * @param buildTagRegex Regular expression used to match and parse version tags
     * @param limitResultCount Maximum number of tags to return
     *
     * @return A list of [Tag] objects, or null if no matching tags are found
     * @throws IllegalArgumentException If a matching tag's name doesn't contain a valid build number
     * @throws org.eclipse.jgit.api.errors.GitAPIException If there's an error accessing the Git repository
     */
    fun findBuildTags(
        buildTagRegex: Regex,
        limitResultCount: Int,
    ): List<Tag>? {
        return grgit.tag.list()
            .filter { tag -> tag.name.matches(buildTagRegex) }
            .sortedBy { tag ->
                buildTagRegex.find(tag.name)?.groupValues?.get(1)?.toIntOrNull()
                    ?: error("internal error: failed to parse build number for tag ${tag.name}")
            }
            .map { tag ->
                Tag.Generic(
                    name = tag.name,
                    commitSha = tag.commit.id,
                    message = tag.fullMessage,
                )
            }
            .reversed()
            .take(limitResultCount)
            .ifEmpty { null }
    }

    /**
     * Retrieves a list of Git commits within the specified range, or all commits if the range is null.
     *
     * If the range's sha1 is null, the method retrieves all commits starting from the commit with the
     * specified sha2. Otherwise, it retrieves commits within the specified range.
     *
     * @param range The range of commits to retrieve (null for all commits)
     *
     * @return A list of [Commit] objects within the specified range
     * @throws org.eclipse.jgit.api.errors.GitAPIException If there's an error accessing the Git repository
     */
    private fun getCommitsByRange(range: CommitRange?): List<Commit> {
        return when {
            range == null -> grgit.log()
            range.sha1 == null -> {
                val commits = grgit.log()
                val lastCommitIndex = commits.indexOfFirst { it.id == range.sha2 }
                return if (lastCommitIndex == UNKNOWN_COMMIT_INDEX) {
                    commits
                } else {
                    commits.subList(lastCommitIndex, commits.size)
                }
            }

            else -> grgit.log { options -> options.range(range.sha1, range.sha2) }
        }
    }
}
