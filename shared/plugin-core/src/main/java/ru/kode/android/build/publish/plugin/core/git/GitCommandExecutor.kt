package ru.kode.android.build.publish.plugin.core.git

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.gradle.api.GradleException
import ru.kode.android.build.publish.plugin.core.enity.BuildTagSnapshot
import ru.kode.android.build.publish.plugin.core.enity.CommitRange
import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.logger.PluginLogger
import ru.kode.android.build.publish.plugin.core.messages.cannotReturnTagMessage
import ru.kode.android.build.publish.plugin.core.messages.finTagsByRegexAfterSortingMessage
import ru.kode.android.build.publish.plugin.core.messages.findTagsByRegexAfterFilterMessage
import ru.kode.android.build.publish.plugin.core.messages.findTagsByRegexBeforeFilterMessage
import ru.kode.android.build.publish.plugin.core.util.getBuildNumber
import ru.kode.android.build.publish.plugin.core.util.getCommitsByRange
import ru.kode.android.build.publish.plugin.core.util.utcDateTime
import org.ajoberstar.grgit.Tag as GrgitTag

/**
 * Executes Git commands and provides high-level Git operations for the build system.
 *
 * This class serves as a wrapper around the Grgit library, offering a more convenient and type-safe
 * API for common Git operations needed during the build and publish process. It handles tasks such as:
 *
 * - Extracting and filtering commit messages for changelog generation
 * - Finding and parsing build tags
 * - Managing commit ranges and version information
 * - Validating tag formats and consistency
 *
 * The class is designed to work within the build pipeline and provides detailed logging for debugging
 * and traceability purposes.
 *
 * @see Grgit For more information about the underlying Git operations.
 * @see BuildTagSnapshot For information about the structure of build tag snapshots.
 * @see Tag For details about how tags are represented in the system.
 */
class GitCommandExecutor(
    /**
     * The Grgit instance used to execute Git commands. This provides access to the
     * underlying Git repository.
     */
    private val grgit: Grgit,
    /**
     * The logger instance used for recording debug and error information.
     */
    private val logger: PluginLogger,
) {
    /**
     * Extracts commit messages containing a specific key from a range of commits.
     *
     * This method retrieves all commits within the specified range and filters their commit messages
     * to find lines containing the provided key. It's primarily used to extract changelog entries
     * that follow a specific format (e.g., lines starting with "CHANGELOG:").
     *
     * The search is performed line-by-line within each commit message, and only lines containing
     * the exact [key] (case-sensitive) are included in the results.
     *
     * @param key The string to search for in commit messages. Only lines containing this exact
     *            string will be included in the results.
     * @param range The range of commits to search through. If `null`, all commits in the repository
     *              will be considered.
     *
     * @return A list of strings, where each string is a line from a commit message that contains
     *         the specified [key]. The order of results follows the commit history (newest first).
     *
     * @throws org.eclipse.jgit.api.errors.GitAPIException If there's an error accessing the Git repository
     *                                                     or processing the commit history.
     *
     * @sample
     * ```kotlin
     * // Find all changelog entries between two tags
     * val changes = gitCommandExecutor.extractMarkedCommitMessages(
     *     key = "CHANGELOG:",
     *     range = CommitRange(from = "v1.0.0", to = "HEAD")
     * )
     * ```
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
     * Finds and parses build tags matching the specified pattern for a given build variant.
     *
     * This method performs a comprehensive search for Git tags that match the provided regular expression
     * and are associated with the specified build variant. It handles:
     *
     * 1. Filtering tags using the provided regex pattern
     * 2. Validating the format of matching tags
     * 3. Sorting tags by commit date (newest first)
     * 4. Creating a [BuildTagSnapshot] with the most recent tag and its predecessor
     *
     * The method is designed to be fault-tolerant and will log detailed information about the search
     * process for debugging purposes.
     *
     * @param buildVariant The build variant to search for (e.g., "debug", "release").
     * @param buildTagRegex The regular expression used to match and parse build tag names.
     *                     The pattern should include capture groups for version components.
     *
     * @return A [BuildTagSnapshot] containing the most recent matching tag and its predecessor,
     *         or `null` if no matching tags are found.
     *
     * @throws IllegalArgumentException If the [buildTagRegex] is invalid or if matching tags
     *                                 don't conform to the expected format.
     *
     * @sample
     * ```kotlin
     * // Find the most recent build tag for the release variant
     * val snapshot = gitCommandExecutor.findSnapshot(
     *     buildVariant = "release",
     *     buildTagRegex = "app\\.(\\d+)\\.(\\d+)\\.(\\d+)-release".toRegex()
     * )
     * ```
     */
    fun findSnapshot(
        buildVariant: String,
        buildTagRegex: Regex,
    ): BuildTagSnapshot? {
        val allTags = findTagsByRegex(buildTagRegex)
        val lastTwoTags = allTags.take(2)
        if (lastTwoTags.isNotEmpty()) validateTags(lastTwoTags, buildTagRegex)

        val lastTag = allTags.firstOrNull()
        return lastTag?.let { lastGitTag ->
            val previousInOrder = lastTwoTags.getOrNull(1)
            val previousOnDifferentCommit = allTags.firstOrNull { it.commit.id != lastTag.commit.id }
            BuildTagSnapshot(
                current =
                    Tag.Build(
                        Tag.Generic(
                            name = lastGitTag.name,
                            commitSha = lastGitTag.commit.id,
                            message = lastGitTag.fullMessage,
                        ),
                        buildVariant,
                    ),
                previousInOrder =
                    previousInOrder?.let {
                        Tag.Build(
                            Tag.Generic(
                                name = it.name,
                                commitSha = it.commit.id,
                                message = it.fullMessage,
                            ),
                            buildVariant,
                        )
                    },
                previousOnDifferentCommit =
                    previousOnDifferentCommit?.let {
                        Tag.Build(
                            Tag.Generic(
                                name = it.name,
                                commitSha = it.commit.id,
                                message = it.fullMessage,
                            ),
                            buildVariant,
                        )
                    },
            )
        }
    }

    /**
     * Finds and processes Git tags that match the specified regular expression.
     *
     * This function performs the following operations:
     * 1. Retrieves all tags from the Git repository
     * 2. Filters them using the provided [buildTagRegex]
     * 3. Extracts commit information for each matching tag
     * 4. Sorts the tags by commit date and build number
     * 5. Returns the specified number of most recent tags
     *
     * The function includes detailed logging at each step for debugging and monitoring purposes.
     * It handles errors gracefully by skipping tags that can't be processed and logging the issues.
     *
     * @param buildTagRegex The regular expression used to filter tag names. Only tags matching
     *                     this pattern will be included in the results.
     *
     * @return A list of [GrgitTag] objects representing the matching tags, sorted by commit date
     *         (newest first) and then by build number. Returns an empty list if no matching tags
     *         are found or if an error occurs.
     *
     * @see GrgitTag For information about the structure of Git tags.
     * @see getBuildNumber For details on how build numbers are extracted from tag names.
     *
     * @sample
     * ```kotlin
     * // Find the 5 most recent version tags
     * val versionTags = findTagsByRegex(
     *     buildTagRegex = "v\\d+\\.\\d+\\.\\d+".toRegex()
     * )
     * ```
     */
    private fun findTagsByRegex(buildTagRegex: Regex): List<GrgitTag> {
        val commitsLog = grgit.log()
        val tagsList = grgit.tag.list()

        return tagsList
            .also { tags -> logger.info(findTagsByRegexBeforeFilterMessage(tags)) }
            .filter { tag -> tag.name.matches(buildTagRegex) }
            .also { tags -> logger.info(findTagsByRegexAfterFilterMessage(buildTagRegex, tags)) }
            .map { tag ->
                val commit =
                    try {
                        findCommit(tag.commit.id)
                    } catch (_: Exception) {
                        null
                    }
                val commitIndex =
                    commitsLog
                        .indexOfFirst { it.id == tag.commit.id }
                        .takeIf { it >= 0 }
                        ?: -1
                val commitTimeMs = commit?.utcDateTime()?.toInstant()?.toEpochMilli() ?: 0L
                val buildNumber = tag.getBuildNumber(buildTagRegex)
                TagDetails(tag, commitIndex, commitTimeMs, buildNumber)
            }
            .sortedWith { tagDetails1, tagDetails2 ->
                if (tagDetails1.commitIndex >= 0 && tagDetails2.commitIndex >= 0) {
                    if (tagDetails1.commitIndex != tagDetails2.commitIndex) {
                        return@sortedWith tagDetails1.commitIndex.compareTo(tagDetails2.commitIndex)
                    }
                    if (tagDetails1.commitTimeMs != tagDetails2.commitTimeMs) {
                        return@sortedWith tagDetails2.commitTimeMs.compareTo(tagDetails1.commitTimeMs)
                    }
                }
                tagDetails2.buildNumber.compareTo(tagDetails1.buildNumber)
            }
            .map { it.tag }
            .also { tags -> logger.info(finTagsByRegexAfterSortingMessage(tags)) }
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

                if (previousTagCommit.utcDateTime().isAfter(lastTagCommit.utcDateTime()) ||
                    previousTagBuildNumber >= lastTagBuildNumber
                ) {
                    throw GradleException(
                        cannotReturnTagMessage(
                            previousTagBuildNumber = previousTagBuildNumber,
                            previousTag = previousTag,
                            previousTagCommit = previousTagCommit,
                            lastTag = lastTag,
                            lastTagCommit = lastTagCommit,
                            lastTagBuildNumber = lastTagBuildNumber,
                        ),
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

/**
 * Represents the details of a Git tag, including its commit index, commit time in milliseconds,
 * and build number.
 *
 * This data class is used internally by [GitCommandExecutor] to store and sort tag information.
 * It's not meant to be used directly by clients of the library.
 *
 * @property tag The original Git tag.
 * @property commitIndex The index of the commit in the Git history.
 * @property commitTimeMs The commit time in milliseconds since epoch.
 * @property buildNumber The build number extracted from the tag name.
 */
private data class TagDetails(
    val tag: GrgitTag,
    val commitIndex: Int,
    val commitTimeMs: Long,
    val buildNumber: Int,
)
