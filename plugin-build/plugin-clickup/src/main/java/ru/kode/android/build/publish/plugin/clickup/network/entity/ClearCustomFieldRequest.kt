package ru.kode.android.build.publish.plugin.clickup.network.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class ClearCustomFieldRequest(
    val value: String,
)
