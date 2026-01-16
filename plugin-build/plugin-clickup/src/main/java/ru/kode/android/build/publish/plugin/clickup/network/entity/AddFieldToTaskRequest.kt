package ru.kode.android.build.publish.plugin.clickup.network.entity

import kotlinx.serialization.Serializable

/**
 * ClickUp API request body for setting a custom field value on a task.
 */
@Serializable
internal data class AddFieldToTaskRequest(
    val value: String,
)
