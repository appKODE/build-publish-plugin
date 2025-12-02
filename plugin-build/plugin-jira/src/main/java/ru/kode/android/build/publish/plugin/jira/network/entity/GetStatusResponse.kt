package ru.kode.android.build.publish.plugin.jira.network.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Suppress("ConstructorParameterNaming") // network model
internal data class GetStatusResponse(
    val fields: Fields,
) {
    @JsonClass(generateAdapter = true)
    @Suppress("ConstructorParameterNaming") // network model
    data class Fields(
        val status: Status,
    )

    @JsonClass(generateAdapter = true)
    @Suppress("ConstructorParameterNaming") // network model
    data class Status(
        val id: String,
        val name: String,
        val description: String?,
        val statusCategory: StatusCategory?
    )

    @JsonClass(generateAdapter = true)
    @Suppress("ConstructorParameterNaming") // network model
    data class StatusCategory(
        val id: Int?,
        val name: String?
    )
}