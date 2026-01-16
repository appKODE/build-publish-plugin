package ru.kode.android.build.publish.plugin.clickup.network.entity

import kotlinx.serialization.Serializable

/**
 * ClickUp API response for listing custom fields attached to a list.
 */
@Serializable
internal data class GetCustomFieldsResponse(
    val fields: List<ClickUpCustomField>,
) {
    @Serializable
    internal data class ClickUpCustomField(
        val id: String,
        val name: String,
        /**
         * ClickUp field type (for example: "drop_down", "text", "number", "checkbox", "date").
         */
        val type: String,
    )
}
