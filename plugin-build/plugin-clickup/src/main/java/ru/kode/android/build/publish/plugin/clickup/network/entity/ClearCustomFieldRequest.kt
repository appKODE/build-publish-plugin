package ru.kode.android.build.publish.plugin.clickup.network.entity

import kotlinx.serialization.Serializable

/**
 * ClickUp API request body for clearing a custom field value on a task.
 */
@Serializable
internal data class ClearCustomFieldRequest(
    val value: String,
)
