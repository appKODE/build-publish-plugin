package ru.kode.android.build.publish.plugin.clickup.controller.entity

/**
 * ClickUp task tags snapshot returned by the controller layer.
 */
data class ClickUpTaskTags(
    val id: String,
    val tags: List<String>,
)
