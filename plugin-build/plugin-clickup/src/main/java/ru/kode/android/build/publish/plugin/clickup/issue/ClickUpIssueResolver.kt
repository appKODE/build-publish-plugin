package ru.kode.android.build.publish.plugin.clickup.issue

import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.clickup.service.network.ClickUpService
import ru.kode.android.build.publish.plugin.core.issue.IssueResolver
import ru.kode.android.build.publish.plugin.core.issue.ResolvedIssue

/**
 * The account + workspace a ClickUp custom-task-id prefix routes to.
 */
data class AccountWorkspace(
    val accountName: String,
    val workspaceName: String,
)

/**
 * [IssueResolver] backed by ClickUp: resolves a `CLOSES`/`FIXES` reference to its task name.
 *
 * Routing supports two modes:
 * - **Prefix routing** — a reference with a known custom-task-id prefix (`APP-123`) is routed by that
 *   prefix to its account + workspace via [accountWorkspaceByPrefix] and resolved as a custom task id
 *   (scoped to the workspace's team).
 * - **Native fallback** — a reference without a known prefix is attempted against each account in
 *   [fallbackAccounts] in order as a native ClickUp id; the first non-null result wins.
 *
 * Returns `null` (never throws) for references it does not handle or when the lookup fails, keeping
 * changelog generation non-blocking. Holds a Gradle [Provider] (not a resolved service) so it stays
 * configuration-cache safe.
 */
class ClickUpIssueResolver(
    private val service: Provider<ClickUpService>,
    private val accountWorkspaceByPrefix: Map<String, AccountWorkspace>,
    private val fallbackAccounts: List<String>,
) : IssueResolver {
    override fun resolve(reference: String): ResolvedIssue? {
        val prefix = reference.substringBefore('-', missingDelimiterValue = "").uppercase()
        val routed = accountWorkspaceByPrefix[prefix]
        return if (prefix.isNotEmpty() && routed != null) {
            resolveRouted(reference, routed)
        } else {
            resolveNative(reference)
        }
    }

    private fun resolveRouted(
        reference: String,
        routed: AccountWorkspace,
    ): ResolvedIssue? {
        val teamId = service.get().getTeamId(routed.accountName, routed.workspaceName)
        val name = service.get().getTaskName(routed.accountName, reference, teamId) ?: return null
        return ResolvedIssue(reference, name)
    }

    private fun resolveNative(reference: String): ResolvedIssue? {
        for (accountName in fallbackAccounts) {
            val name = service.get().getTaskName(accountName, reference)
            if (name != null) return ResolvedIssue(reference, name)
        }
        return null
    }
}
