package ru.kode.android.build.publish.plugin.jira.network.entity

import kotlinx.serialization.Serializable

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
