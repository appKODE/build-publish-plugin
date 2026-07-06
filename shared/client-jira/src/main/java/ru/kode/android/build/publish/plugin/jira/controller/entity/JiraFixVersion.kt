package ru.kode.android.build.publish.plugin.jira.controller.entity

import kotlinx.serialization.Serializable

/**
 * Represents a Jira fix version.
 *
 * @property id The fix version's ID.
 * @property name The fix version's name.
 */
@Serializable
data class JiraFixVersion(
    val id: String,
    val name: String,
)
