package ru.kode.android.build.publish.plugin.core.issue

/**
 * Resolves an issue reference (extracted from a `CLOSES`/`FIXES` commit line) into tracker data used to
 * enrich the changelog.
 *
 * The seam is provider-agnostic: any integration plugin (Jira, ClickUp, …) contributes its own
 * implementation, and the changelog task tries all registered resolvers in order. An implementation must
 * return `null` — never throw — when it cannot handle the given reference (it belongs to another tracker)
 * or when the lookup fails; the changelog generation stays non-blocking and falls back gracefully.
 */
interface IssueResolver {
    /**
     * @param reference The raw issue token after the marker, e.g. `"TBI-3458"`, `"3458"` or `"CU-abc"`.
     * @return The resolved issue, or `null` if this provider cannot handle the reference or the lookup
     *   failed.
     */
    fun resolve(reference: String): ResolvedIssue?
}

/**
 * A resolved issue: its canonical [key] and human-readable [title].
 *
 * @property key The canonical issue key (e.g. `"TBI-3458"`), used both for rendering and de-duplication.
 * @property title The issue title/summary fetched from the tracker.
 */
data class ResolvedIssue(
    val key: String,
    val title: String,
)
