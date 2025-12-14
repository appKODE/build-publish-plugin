package ru.kode.android.build.publish.plugin.clickup.network.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class GetSpacesResponse(
    val spaces: List<ClickUpSpace>
) {

    @Serializable
    internal data class ClickUpSpace(
        val id: String,
        val name: String,
    )
}