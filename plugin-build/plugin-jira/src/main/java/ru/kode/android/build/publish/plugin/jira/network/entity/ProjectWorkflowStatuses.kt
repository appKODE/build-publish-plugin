package ru.kode.android.build.publish.plugin.jira.network.entity

import kotlinx.serialization.Serializable

/**
 * Jira API response item for workflow statuses available for a project.
 */
@Serializable
internal data class ProjectWorkflowStatuses(
    val id: String,
    val name: String,
    val statuses: List<JiraStatus>,
) {
    @Serializable
    internal data class JiraStatus(
        val id: String,
        val name: String,
    )
}
