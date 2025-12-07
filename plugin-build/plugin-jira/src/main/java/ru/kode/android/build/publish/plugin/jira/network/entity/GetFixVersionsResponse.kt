package ru.kode.android.build.publish.plugin.jira.network.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class GetFixVersionsResponse(
    val fields: Fields,
) {
    @Serializable
    data class Fields(
        val fixVersions: List<JiraFixVersion>,
    )
}