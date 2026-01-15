package ru.kode.android.build.publish.plugin.core.enity

import kotlinx.serialization.Serializable

/**
 * Represents a snapshot of Git tags, typically used to define a snapshot of commits for changelog generation.
 *
 * This class holds a pair of [Tag] instances representing the current and previous build tags.
 * It's primarily used to determine the set of commits to include in a changelog or release notes.
 *
 * @see Tag For information about the tag structure
 * @see GitChangelogBuilder For how this snapshot is used in changelog generation
 */
@Serializable
data class BuildTagSnapshot(
    /**
     * The most recent tag in the snapshot (inclusive)
     */
    val current: Tag.Build,
    /**
     * The previous tag in the snapshot (exclusive), or null if this is the first build
     */
    private val previousInOrder: Tag.Build?,
    private val previousOnDifferentCommit: Tag.Build?,
) {
    val previous: Tag.Build? get() = previousOnDifferentCommit

    val pointSameCommit: Boolean get() = current.commitSha == previousOnDifferentCommit?.commitSha

    /**
     * Converts this [BuildTagSnapshot] into a [CommitRange] that can be used with Git operations.
     *
     * The resulting [CommitRange] will include all commits that are reachable from [current]
     * but not from [previousOnDifferentCommit] (or all commits if [previousOnDifferentCommit] is null).
     *
     * @return A [CommitRange] representing the same snapshot of commits as this tag snapshot
     * @see CommitRange For information about the commit snapshot format
     */
    fun asCommitRange(): CommitRange {
        return CommitRange(previousOnDifferentCommit?.commitSha, current.commitSha)
    }
}
