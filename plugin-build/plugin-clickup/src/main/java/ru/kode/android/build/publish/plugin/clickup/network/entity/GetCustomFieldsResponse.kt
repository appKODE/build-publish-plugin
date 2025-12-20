package ru.kode.android.build.publish.plugin.clickup.network.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class GetCustomFieldsResponse(
    val fields: List<ClickUpCustomField>,
) {
    @Serializable
    internal data class ClickUpCustomField(
        val id: String,
        val name: String,
        // drop_down, text, number, checkbox, date
        val type: String,
    )
}
