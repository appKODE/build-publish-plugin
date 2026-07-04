package ru.kode.android.build.publish.plugin.clickup.issue

import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.clickup.service.network.ClickUpService
import ru.kode.android.build.publish.plugin.core.issue.IssueResolver
import ru.kode.android.build.publish.plugin.core.issue.ResolvedIssue

/**
 * [IssueResolver] backed by ClickUp: resolves a `CLOSES`/`FIXES` reference to its task name.
 *
 * When [taskIdPattern] is set, only tokens fully matching it are attempted (so it ignores references
 * belonging to other trackers); otherwise every token is attempted. Returns `null` (never throws) for
 * references it does not handle or when the lookup fails, keeping changelog generation non-blocking.
 *
 * Holds a Gradle [Provider] (not a resolved service) so it stays configuration-cache safe.
 */
class ClickUpIssueResolver(
    private val service: Provider<ClickUpService>,
    private val taskIdPattern: String?,
) : IssueResolver {
    override fun resolve(reference: String): ResolvedIssue? {
        if (taskIdPattern != null && !Regex(taskIdPattern).matches(reference)) return null
        val name = service.get().getTaskName(reference) ?: return null
        return ResolvedIssue(reference, name)
    }
}
