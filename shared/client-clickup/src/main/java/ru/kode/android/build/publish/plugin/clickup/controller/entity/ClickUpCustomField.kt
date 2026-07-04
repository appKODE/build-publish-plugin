package ru.kode.android.build.publish.plugin.clickup.controller.entity

/**
 * ClickUp custom field descriptor used by the controller layer.
 *
 * @property id The ID of the custom field.
 * @property name The name of the custom field.
 * @property type The type of the custom field.
 * @property value The current value of the custom field, or `null` if unset.
 */
data class ClickUpCustomField(
    val id: String,
    val name: String,
    val type: String,
    val value: String?,
)
