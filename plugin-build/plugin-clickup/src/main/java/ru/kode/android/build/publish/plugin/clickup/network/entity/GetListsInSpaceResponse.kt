package ru.kode.android.build.publish.plugin.clickup.network.entity

import kotlinx.serialization.Serializable

@Serializable
data class GetListsInSpaceResponse(
    val lists: List<ListInSpaceMinimal>,
) {
    @Serializable
    data class ListInSpaceMinimal(
        val id: String,
    )
}
