package ru.kode.android.build.publish.plugin.jira.network.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Suppress("ConstructorParameterNaming") // network model
internal data class GetFixVersionsResponse(
    val fields: Fields,
) {
    @JsonClass(generateAdapter = true)
    @Suppress("ConstructorParameterNaming") // network model
    data class Fields(
        val fixVersions: List<JiraFixVersion>,
    )
}