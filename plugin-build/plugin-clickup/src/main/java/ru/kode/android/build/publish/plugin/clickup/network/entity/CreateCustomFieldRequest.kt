package ru.kode.android.build.publish.plugin.clickup.network.entity

import kotlinx.serialization.Serializable

/**
 * ClickUp API request body for creating a custom field in a list.
 */
@Serializable
internal data class CreateCustomFieldRequest(
    val name: String,
    /**
     * ClickUp field type (for example: "text", "number", "dropdown").
     */
    val type: String,
)
