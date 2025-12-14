package ru.kode.android.build.publish.plugin.clickup.network.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class CreateCustomFieldRequest(
    val name: String,
    val type: String, // "text", "number", "dropdown", etc.
)