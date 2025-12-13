package ru.kode.android.build.publish.plugin.jira.controller.entity


/**
 * Represents the status of a Jira issue, including both the name and ID.
 *
 * @property id The ID of the issue status.
 * @property name The name of the issue status.
 */
data class JiraIssueStatus(
    val id: String,
    val name: String,
)