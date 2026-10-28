package ru.kode.android.build.publish.plugin.firebase.network.entity

import kotlinx.serialization.Serializable
import ru.kode.android.build.publish.plugin.firebase.controller.entity.RemoteConfigVersion

@Serializable
internal data class ListVersionsResponse(
    val versions: List<RemoteConfigVersion> = emptyList(),
    val nextPageToken: String? = null,
)
