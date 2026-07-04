package ru.kode.android.build.publish.plugin.jira.controller

import ru.kode.android.build.publish.plugin.jira.messages.addingFixVersionToIssuesMessage
import ru.kode.android.build.publish.plugin.jira.messages.addingLabelToIssuesMessage
import ru.kode.android.build.publish.plugin.jira.messages.transitionNotFoundMessage
import ru.kode.android.build.publish.plugin.jira.messages.transitioningIssuesMessage

/**
 * High-level, task-oriented Jira operations shared by the plugin's standalone tasks
 * and the aggregated sender plugin. Keeping the orchestration here guarantees both
 * entry points execute identical logic (no copy-paste drift).
 */
fun JiraController.addLabelToIssues(
    label: String,
    issues: Collection<String>,
    log: (String) -> Unit = {},
) {
    log(addingLabelToIssuesMessage(label, issues.size))
    issues.forEach { issue -> addIssueLabel(issue, label) }
}

fun JiraController.addFixVersionToIssues(
    projectKey: String,
    version: String,
    issues: Collection<String>,
    log: (String) -> Unit = {},
) {
    val normalizedProjectKey = projectKey.uppercase()
    log(addingFixVersionToIssuesMessage(version, issues.size))
    // Idempotent: only create the project version when it does not already exist, otherwise reuse it.
    // Blindly creating would fail with a 400 "version already exists" on any re-run against the same
    // project, leaving the fix version unattached.
    val versionExists = getProjectVersions(normalizedProjectKey).any { it.name == version }
    if (!versionExists) {
        val projectId = getProjectId(normalizedProjectKey)
        createProjectVersion(projectId, version)
    }
    issues.forEach { issue -> addIssueFixVersion(issue, version) }
}

fun JiraController.transitionIssues(
    projectKey: String,
    transitionName: String,
    issues: Collection<String>,
    log: (String) -> Unit = {},
) {
    val normalizedProjectKey = projectKey.uppercase()
    val issueList = issues.toList()
    log(transitioningIssuesMessage(issueList.size, transitionName))
    val transitionId =
        getStatusTransitionId(normalizedProjectKey, transitionName, issueList)
            ?: error(transitionNotFoundMessage(transitionName, normalizedProjectKey))
    issueList.forEach { issue -> setIssueStatus(issue, transitionId) }
}
