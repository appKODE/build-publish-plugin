package ru.kode.android.build.publish.plugin.clickup.network.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class CreateCustomFieldResponse(
    val field: CustomField,
) {
    @Serializable
    data class CustomField(
        val id: String,
    )
}
