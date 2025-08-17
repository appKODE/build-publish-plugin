package ru.kode.android.build.publish.plugin.core.enity

/**
 * Represents a range of Git tags, typically used to define a range of commits for changelog generation.
 *
 * This class holds a pair of [Tag] instances representing the current and previous build tags.
 * It's primarily used to determine the set of commits to include in a changelog or release notes.
 *
 * @see Tag For information about the tag structure
 * @see GitChangelogBuilder For how this range is used in changelog generation
 */
data class TagRange(
    /**
     * The most recent tag in the range (inclusive)
     */
    val currentBuildTag: Tag,
    /**
     * The previous tag in the range (exclusive), or null if this is the first build
     */
    val previousBuildTag: Tag?,
) {
    /**
     * Converts this [TagRange] into a [CommitRange] that can be used with Git operations.
     *
     * The resulting [CommitRange] will include all commits that are reachable from [currentBuildTag]
     * but not from [previousBuildTag] (or all commits if [previousBuildTag] is null).
     *
     * @return A [CommitRange] representing the same range of commits as this tag range
     * @see CommitRange For information about the commit range format
     */
    fun asCommitRange(): CommitRange {
        return CommitRange(previousBuildTag?.commitSha, currentBuildTag.commitSha)
    }
}
