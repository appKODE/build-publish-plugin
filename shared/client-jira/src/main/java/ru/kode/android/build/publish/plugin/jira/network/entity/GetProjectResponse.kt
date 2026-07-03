package ru.kode.android.build.publish.plugin.jira.network.entity

import kotlinx.serialization.Serializable

/**
 * Jira API response for retrieving a project.
 */
@Serializable
internal data class GetProjectResponse(
    val id: Long,
    val key: String,
    val name: String,
)
