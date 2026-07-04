package ru.kode.android.build.publish.plugin.clickup.controller.entity

/**
 * ClickUp task custom fields snapshot returned by the controller layer.
 *
 * @property id The ID of the ClickUp task.
 * @property fields The custom fields currently defined on the task.
 */
data class ClickUpTaskFields(
    val id: String,
    val fields: List<ClickUpCustomField>,
)
