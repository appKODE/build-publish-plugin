package ru.kode.android.build.publish.plugin.clickup.controller.entity

/**
 * ClickUp task tags snapshot returned by the controller layer.
 *
 * @property id The ID of the ClickUp task.
 * @property tags The names of the tags currently attached to the task.
 */
data class ClickUpTaskTags(
    val id: String,
    val tags: List<String>,
)
