package ru.kode.android.build.publish.plugin.jira.controller.entity

import kotlinx.serialization.Serializable

/**
 * Represents the status of a Jira issue, including both the name and ID.
 *
 * @property id The ID of the issue status.
 * @property name The name of the issue status.
 */
@Serializable
data class JiraIssueStatus(
    val id: String,
    val name: String,
)
