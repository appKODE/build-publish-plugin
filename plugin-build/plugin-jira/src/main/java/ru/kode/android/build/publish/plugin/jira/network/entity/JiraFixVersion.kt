package ru.kode.android.build.publish.plugin.jira.network.entity

import kotlinx.serialization.Serializable

/**
 * Jira API representation of a project version.
 */
@Serializable
internal data class JiraFixVersion(
    val id: String,
    val name: String,
    val self: String,
)
