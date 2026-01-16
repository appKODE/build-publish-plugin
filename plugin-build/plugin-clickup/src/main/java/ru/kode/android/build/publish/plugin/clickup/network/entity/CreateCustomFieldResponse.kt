package ru.kode.android.build.publish.plugin.clickup.network.entity

import kotlinx.serialization.Serializable

/**
 * ClickUp API response for creating a custom field in a list.
 */
@Serializable
internal data class CreateCustomFieldResponse(
    val field: CustomField,
) {
    @Serializable
    data class CustomField(
        val id: String,
    )
}
