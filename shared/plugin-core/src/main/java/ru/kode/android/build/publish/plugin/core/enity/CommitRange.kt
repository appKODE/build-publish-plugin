package ru.kode.android.build.publish.plugin.core.enity

/**
 * Represents a range of Git commits, defined by two commit SHAs.
 *
 * This class is used to specify a range of commits in a Git repository, typically for operations
 * like generating changelogs or determining which commits to include in a release. The range is
 * exclusive of the starting commit (sha1) and inclusive of the ending commit (sha2).
 *
 * @see BuildTagSnapshot
 * @see GitCommandExecutor For usage in Git operations
 */
data class CommitRange(
    /**
     * The SHA of the starting commit (exclusive). When null, represents the initial commit.
     */
    val sha1: String?,
    /**
     * For a higher-level representation using tags instead of direct SHAs
     */
    val sha2: String,
)
