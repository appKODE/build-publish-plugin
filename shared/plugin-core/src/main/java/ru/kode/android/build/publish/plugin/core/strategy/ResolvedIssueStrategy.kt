package ru.kode.android.build.publish.plugin.core.strategy

import java.io.Serializable

/**
 * Strategy that decides how a **resolved** issue reference is rendered in the changelog.
 *
 * A reference is resolved when a registered [ru.kode.android.build.publish.plugin.core.issue.IssueResolver]
 * returned a title for its token. This strategy formats that `key`/`title` pair into the final changelog
 * line, mirroring [UnresolvedIssueStrategy] for the failure case.
 *
 * Implementations must be serializable so the strategy can be carried as a Gradle task property under the
 * configuration cache.
 */
interface ResolvedIssueStrategy : Serializable {
    /**
     * @param key The resolved issue key (e.g. `"TBI-3458"`).
     * @param title The issue title fetched from the provider.
     * @param commitChangelogLine The commit's own `CHANGELOG:` line, or `null` when the commit has none.
     * @return The changelog line to add for this reference, or `null` to omit it entirely.
     */
    fun build(
        key: String,
        title: String,
        commitChangelogLine: String?,
    ): String?
}

/**
 * Default strategy: renders `• [key] title` (key + fetched title).
 */
object KeyAndTitleResolvedStrategy : ResolvedIssueStrategy {
    override fun build(
        key: String,
        title: String,
        commitChangelogLine: String?,
    ) = "• [$key] $title".trim()
}

/**
 * Renders `• title` only (no key); downstream integrations no longer link the key.
 */
object TitleOnlyResolvedStrategy : ResolvedIssueStrategy {
    override fun build(
        key: String,
        title: String,
        commitChangelogLine: String?,
    ) = "• $title".trim()
}

/**
 * Renders `• [key]` only (drops the fetched title); downstream integrations link the key.
 */
object KeyOnlyResolvedStrategy : ResolvedIssueStrategy {
    override fun build(
        key: String,
        title: String,
        commitChangelogLine: String?,
    ) = "• [$key]"
}
