package ru.kode.android.build.publish.plugin.clickup.controller.entity

/**
 * ClickUp custom field descriptor used by the controller layer.
 */
data class ClickUpCustomField(
    val id: String,
    val name: String,
    val type: String,
    val value: String?,
)
