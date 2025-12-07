package ru.kode.android.build.publish.plugin.jira.network.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class GetStatusResponse(
    val fields: Fields,
) {
    @Serializable
    data class Fields(
        val status: Status,
    )

    @Serializable
    data class Status(
        val id: String,
        val name: String,
        val description: String? = null,
        val statusCategory: StatusCategory? = null
    )

    @Serializable
    data class StatusCategory(
        val id: Int? = null,
        val name: String? = null
    )
}