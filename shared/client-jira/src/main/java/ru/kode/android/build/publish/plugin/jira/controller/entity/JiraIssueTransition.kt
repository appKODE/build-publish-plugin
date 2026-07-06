package ru.kode.android.build.publish.plugin.jira.controller.entity

import kotlinx.serialization.Serializable

/**
 * Represents a Jira issue transition.
 *
 * @property id The transition ID.
 * @property name The transition name.
 * @property statusId The ID of the target status.
 */
@Serializable
data class JiraIssueTransition(
    val id: String,
    val name: String,
    val statusId: String,
)
