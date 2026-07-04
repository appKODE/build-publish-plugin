package ru.kode.android.build.publish.plugin.core.strategy

/**
 * Strategy that decides how an **unresolved** issue reference is rendered in the changelog.
 *
 * A reference is unresolved when no registered [ru.kode.android.build.publish.plugin.core.issue.IssueResolver]
 * could return a title for it (unknown tracker, missing issue, or a network failure). Resolution stays
 * non-blocking: instead of failing the build, the builder delegates the rendering of that entry to this
 * strategy.
 *
 * Implementations must be serializable so the strategy can be carried as a Gradle task property under the
 * configuration cache.
 */
interface UnresolvedIssueStrategy {
    /**
     * @param key The issue key/token that could not be resolved (e.g. `"TBI-3458"`).
     * @param commitChangelogLine The commit's own `CHANGELOG:` line, already emitted as a manual entry, or
     *   `null` when the commit has none.
     * @return The changelog line to add for this reference, or `null` to omit it entirely.
     */
    fun build(
        key: String,
        commitChangelogLine: String?,
    ): String?
}

/**
 * Default strategy: when the commit already carries a `CHANGELOG:` line that manual entry represents the
 * change, so the reference adds nothing; otherwise the bare `• [key]` is shown (downstream integrations
 * turn the key into a link).
 */
object ChangelogLineOrKeyUnresolvedStrategy : UnresolvedIssueStrategy {
    override fun build(
        key: String,
        commitChangelogLine: String?,
    ) = if (commitChangelogLine != null) null else "• [$key]"
}

/**
 * Always renders `• [key]` (empty description); downstream integrations link the key.
 */
object KeyOnlyUnresolvedStrategy : UnresolvedIssueStrategy {
    override fun build(
        key: String,
        commitChangelogLine: String?,
    ) = "• [$key]"
}

/**
 * Omits unresolved references from the changelog entirely.
 */
object SkipUnresolvedStrategy : UnresolvedIssueStrategy {
    override fun build(
        key: String,
        commitChangelogLine: String?,
    ): String? = null
}

/**
 * Renders `• [key] <text>` using a fixed [text] for every unresolved reference.
 */
class FallbackTextUnresolvedStrategy(private val text: String) : UnresolvedIssueStrategy {
    override fun build(
        key: String,
        commitChangelogLine: String?,
    ) = "• [$key] $text"
}
