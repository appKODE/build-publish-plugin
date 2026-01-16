package ru.kode.android.build.publish.plugin.jira.network.entity

import kotlinx.serialization.Serializable

/**
 * Jira API request body for creating a version in a project.
 */
@Serializable
internal data class CreateVersionRequest(
    val name: String,
    val projectId: Long,
)
