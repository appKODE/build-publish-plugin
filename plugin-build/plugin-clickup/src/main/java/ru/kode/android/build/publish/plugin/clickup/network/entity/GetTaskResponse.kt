package ru.kode.android.build.publish.plugin.clickup.network.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class GetTaskResponse(
    val id: String,
    val name: String?,
    val tags: List<ClickUpTag>,
    val custom_fields: List<ClickUpCustomFieldValue>,
) {
    @Serializable
    internal data class ClickUpTag(
        val name: String,
    )

    @Serializable
    internal data class ClickUpCustomFieldValue(
        val id: String,
        val name: String,
        val type: String,
        val value: String? = null,
    )
}
