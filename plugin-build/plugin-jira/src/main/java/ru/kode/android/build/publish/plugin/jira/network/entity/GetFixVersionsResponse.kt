package ru.kode.android.build.publish.plugin.jira.network.entity

import kotlinx.serialization.Serializable

/**
 * Jira API response for retrieving issue fix versions.
 */
@Serializable
internal data class GetFixVersionsResponse(
    val fields: Fields,
) {
    @Serializable
    data class Fields(
        val fixVersions: List<JiraFixVersion>,
    )
}
