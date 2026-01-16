package ru.kode.android.build.publish.plugin.clickup.controller.entity

/**
 * ClickUp task custom fields snapshot returned by the controller layer.
 */
data class ClickUpTaskFields(
    val id: String,
    val fields: List<ClickUpCustomField>,
)
