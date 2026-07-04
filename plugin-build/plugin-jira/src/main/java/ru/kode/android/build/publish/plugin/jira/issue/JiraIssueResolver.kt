package ru.kode.android.build.publish.plugin.jira.issue

import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.core.issue.IssueResolver
import ru.kode.android.build.publish.plugin.core.issue.ResolvedIssue
import ru.kode.android.build.publish.plugin.jira.service.network.JiraService

/**
 * [IssueResolver] backed by Jira: resolves a `CLOSES`/`FIXES` reference to its issue title.
 *
 * References are routed to a Jira instance by their project-key prefix ([instanceByPrefix]). A prefixed
 * key (e.g. `TBI-3458`) is used as-is; a bare number (e.g. `3458`) is qualified with the single selected
 * project key ([soleProjectKey]) — when several projects are selected it is ambiguous and left
 * unresolved. Returns `null` (never throws) for references it cannot handle or when the lookup fails, so
 * changelog generation stays non-blocking.
 *
 * Holds a single Gradle [Provider] for the variant's Jira service (not a resolved service) so it stays
 * configuration-cache safe; the service is resolved lazily at execution time and picks the instance by
 * name.
 */
class JiraIssueResolver(
    private val service: Provider<JiraService>,
    private val instanceByPrefix: Map<String, String>,
    private val soleProjectKey: String?,
) : IssueResolver {
    override fun resolve(reference: String): ResolvedIssue? {
        val key =
            if (reference.contains('-')) {
                reference
            } else {
                val projectKey = soleProjectKey ?: return null
                "$projectKey-$reference"
            }
        val prefix = key.substringBefore('-').uppercase()
        val instanceName = instanceByPrefix[prefix] ?: return null
        return service.get().getIssueSummary(instanceName, key)?.let { title -> ResolvedIssue(key, title) }
    }
}
