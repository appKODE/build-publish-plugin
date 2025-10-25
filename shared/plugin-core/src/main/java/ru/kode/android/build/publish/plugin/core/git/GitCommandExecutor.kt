package ru.kode.android.build.publish.plugin.core.git

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.gradle.api.GradleException
import org.gradle.internal.cc.base.logger
import ru.kode.android.build.publish.plugin.core.enity.CommitRange
import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.util.getBuildNumber
import ru.kode.android.build.publish.plugin.core.util.getCommitsByRange
import org.ajoberstar.grgit.Tag as GrgitTag

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
        return grgit.getCommitsByRange(range)
            .flatMap { commit ->
                commit.fullMessage
                    .split('\n')
                    .filter { message -> message.contains(key) }
            }
    }

    /**
     * Finds build tags in the repository that match the given [buildTagRegex].
     *
     * This function retrieves all Git tags, filters them using [buildTagRegex], validates
     * them (if any exist), and sorts them by recency and build number. It then limits the
     * number of results according to [limitResultCount] and maps each tag to a [Tag.Generic].
     *
     * Logging is performed at each stage for debugging and traceability.
     *
     * @param buildTagRegex the regular expression used to match valid build tag names.
     * @param limitResultCount the maximum number of tags to return after sorting.
     *
     * @return a list of [Tag.Generic] objects representing the most recent matching build tags,
     *         or `null` if no matching tags are found.
     */
    fun findBuildTags(
        buildTagRegex: Regex,
        limitResultCount: Int,
    ): List<Tag>? {
        val tags = findTagsByRegex(buildTagRegex)
        if (tags.isNotEmpty()) validateTags(tags, buildTagRegex)

        return tags
            .take(limitResultCount)
            .map { tag ->
                Tag.Generic(
                    name = tag.name,
                    commitSha = tag.commit.id,
                    message = tag.fullMessage,
                )
            }
            .ifEmpty { null }
    }

    /**
     * Finds the range of tags starting from the given [buildTag] and including
     * the previous matching tag according to [buildTagRegex].
     *
     * This function retrieves and filters tags by the given regex, sorts them by commit order
     * and build number, validates them, and then determines the two most recent tags relevant
     * to the provided [buildTag].
     *
     * The result typically contains the current tag and its immediate predecessor in history,
     * represented as [Tag.Generic] objects.
     *
     * @param buildTag the starting build tag from which to find the range.
     * @param buildTagRegex the regular expression used to match valid build tag names.
     *
     * @return a list of up to two [Tag.Generic] objects (the current and previous tags),
     *         or `null` if no matching tags are found.
     */
    fun findTagRange(
        buildTag: Tag.Build,
        buildTagRegex: Regex,
    ): List<Tag>? {
        val tags = findTagsByRange(buildTag, buildTagRegex)
        if (tags.isNotEmpty()) validateTags(tags, buildTagRegex)

        return tags
            .map { tag ->
                Tag.Generic(
                    name = tag.name,
                    commitSha = tag.commit.id,
                    message = tag.fullMessage,
                )
            }
            .ifEmpty { null }
    }

    /**
     * Finds and sorts all Git tags that match the provided [buildTagRegex].
     *
     * This function:
     * - Retrieves all tags from the repository.
     * - Filters tags matching [buildTagRegex].
     * - Sorts them in descending order by commit position in history and build number.
     * - Returns the sorted list of matching [GrgitTag] objects.
     *
     * This method is internal and used by [findBuildTags].
     *
     * @param buildTagRegex the regex used to filter valid build tag names.
     *
     * @return a sorted list of [GrgitTag] objects matching the given regex.
     */
    private fun findTagsByRegex(buildTagRegex: Regex): List<GrgitTag> {
        val commitsLog = grgit.log()
        val tagsList = grgit.tag.list()

        return tagsList
            .also { tags ->
                logger.info(
                    """
                    [FIND TAGS BY REGEX] Tags original list:
                        ${tags.joinToString { tag -> "${tag.name} (datetime: ${tag.dateTime})" }}
                    """.trimIndent(),
                )
            }
            .filter { tag -> tag.name.matches(buildTagRegex) }
            .also { tags ->
                logger.info(
                    """
                    [FIND TAGS BY REGEX] Tags after filter by regex ($buildTagRegex): 
                        ${tags.joinToString { it.name }}
                    """.trimIndent(),
                )
            }
            .sortedWith(
                compareByDescending<GrgitTag> { tag ->
                    val index = commitsLog.indexOfFirst { it.id == tag.commit.id }
                    if (index >= 0) -index else Int.MIN_VALUE
                }.thenByDescending { tag ->
                    tag.getBuildNumber(buildTagRegex)
                },
            )
            .also { tags ->
                logger.info(
                    """
                    [FIND TAGS BY REGEX] Tags after descending sorting by date and build number: 
                        ${tags.joinToString { "${it.name} / ${it.commit.id}" }}
                    """.trimIndent(),
                )
            }
    }

    /**
     * Finds up to two build tags (the current and previous ones) in the Git history
     * based on the provided [startTag] and [buildTagRegex].
     *
     * The function filters all tags by regex, sorts them by commit order and build number,
     * and finds the index of [startTag] in that ordered list. It then extracts the sublist
     * containing [startTag] and its immediate predecessor (if available).
     *
     * Throws a [GradleException] if the [startTag] cannot be found in the Git history.
     *
     * @param startTag the current build tag to start from when determining the range.
     * @param buildTagRegex the regex used to identify valid build tags.
     *
     * @return a list of up to two [GrgitTag] objects (the current and previous tags).
     *
     * @throws GradleException if the provided [startTag] cannot be found in the repository.
     */
    private fun findTagsByRange(
        startTag: Tag.Build,
        buildTagRegex: Regex,
    ): List<GrgitTag> {
        val commitsLog = grgit.log()
        val tagsList = grgit.tag.list()

        logger.info("[FIND TAGS BY RANGE] Start search from tag ${startTag.name}")

        val filteredAndSortedTags =
            tagsList
                .filter { it.name.matches(buildTagRegex) }
                .sortedWith(
                    compareByDescending<GrgitTag> { tag ->
                        val index = commitsLog.indexOfFirst { it.id == tag.commit.id }
                        if (index >= 0) -index else Int.MIN_VALUE
                    }.thenByDescending { tag ->
                        tag.getBuildNumber(buildTagRegex)
                    },
                )
                .distinctBy { it.commit.id }

        val startIndex = filteredAndSortedTags.indexOfFirst { it.commit.id == startTag.commitSha }
        if (startIndex < 0) {
            val availableTagNames = filteredAndSortedTags.joinToString { it.name }
            throw GradleException(
                """
                ❌ Could not find the provided build tag '${startTag.name}' in the git history.
                
                Details:
                  - Provided tag commit SHA: ${startTag.commitSha}
                  - Regex filter used: $buildTagRegex
                  - Available tags after filtering: [$availableTagNames]
                """.trimIndent(),
            )
        }

        val lastTwoTags =
            filteredAndSortedTags.subList(
                startIndex,
                minOf(startIndex + 2, filteredAndSortedTags.size),
            )

        logger.info(
            "[FIND TAGS BY RANGE] Found tags: ${lastTwoTags.joinToString { it.name }}",
        )

        return lastTwoTags
    }

    /**
     * Validates that the provided list of Git tags are ordered correctly based on their
     * associated commit timestamps and build numbers.
     *
     * This function ensures that each subsequent tag (the "lastTag") has:
     *  - A commit date that is **not earlier** than the previous tag's commit date.
     *  - A build number that is **greater than** the previous tag's build number.
     *
     * If any tag pair violates these rules, a [GradleException] is thrown with detailed
     * diagnostic information about the conflicting tags.
     *
     * Typical use case:
     * - To ensure semantic version tags or build tags are sequentially increasing
     *   both by commit chronology and build number before performing release operations.
     *
     * @param tags the list of [Tag] objects to validate, expected to be sorted in ascending order.
     * @param buildTagRegex the [Regex] used to extract the build number from each tag’s name.
     *
     * @throws GradleException if:
     *  - The previous tag’s commit date is after the last tag’s commit date.
     *  - The previous tag’s build number is equal to or greater than the last tag’s build number.
     * ```
     */
    private fun validateTags(
        tags: List<GrgitTag>,
        buildTagRegex: Regex,
    ) {
        tags.zipWithNext()
            .forEach { (lastTag, previousTag) ->
                val lastTagCommit = findCommit(lastTag.commit.id)
                val previousTagCommit = findCommit(previousTag.commit.id)
                val lastTagBuildNumber = lastTag.getBuildNumber(buildTagRegex)
                val previousTagBuildNumber = previousTag.getBuildNumber(buildTagRegex)

                if (previousTagCommit.dateTime.isAfter(lastTagCommit.dateTime) ||
                    previousTagBuildNumber >= lastTagBuildNumber
                ) {
                    throw GradleException(
                        """
                        Cannot return tag, because incorrect tag order detected!
                        Potential reasons:
                         - Previous tag datetime is after last tag commit datetime
                         - Previous tag build number the same or greater than last tag build number
                        
                        Details.
                          Last tag = ${lastTag.name}:
                            - commit ${lastTag.commit.id};
                            - date ${lastTagCommit.dateTime};
                            - build number $lastTagBuildNumber.
                          Previous tag = ${previousTag.name}:
                            - commit ${previousTag.commit.id};
                            - date ${previousTagCommit.dateTime};
                            - build number $previousTagBuildNumber.
                        """.trimIndent(),
                    )
                }
            }
    }

    /**
     * Finds and returns a Git commit corresponding to the given commit SHA.
     *
     * This function resolves the specified commit hash using the underlying
     * [grgit] instance and returns it as a [Commit] object.
     *
     * It is typically used to retrieve commit metadata (e.g., author, date, message)
     * for validation, comparison, or logging purposes within Git operations.
     *
     * @param commitSha The full or abbreviated SHA-1 hash of the commit to look up.
     *
     * @return The [Commit] object that matches the provided commit SHA.
     *
     * @throws org.eclipse.jgit.api.errors.GitAPIException If there is an error while
     *         accessing the Git repository or resolving the commit.
     * @throws IllegalArgumentException If the provided SHA does not correspond to a valid commit.
     *
     * Example usage:
     * ```
     * val commit = findCommit("a1b2c3d4")
     * println("Commit message: ${commit.fullMessage}")
     * ```
     */
    private fun findCommit(commitSha: String): Commit {
        return grgit.resolve.toCommit(commitSha)
    }
}
