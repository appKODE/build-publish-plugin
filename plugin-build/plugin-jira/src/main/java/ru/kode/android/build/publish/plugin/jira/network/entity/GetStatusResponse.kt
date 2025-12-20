package ru.kode.android.build.publish.plugin.jira.network.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class GetStatusResponse(
    val fields: Fields,
) {
    @Serializable
    internal data class Fields(
        val status: Status,
    )

    @Serializable
    internal data class Status(
        val id: String,
        val name: String,
        val description: String? = null,
        val statusCategory: StatusCategory? = null,
    )

    @Serializable
    internal data class StatusCategory(
        val id: Int? = null,
        val name: String? = null,
    )
}
