package ru.kode.android.build.publish.plugin.jira.controller.entity

/**
 * Represents a Jira issue transition.
 *
 * @property id The transition ID.
 * @property name The transition name.
 * @property statusId The ID of the target status.
 */
data class JiraIssueTransition(
    val id: String,
    val name: String,
    val statusId: String,
)
