package ru.kode.android.build.publish.plugin.nextcloud.network.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class OcsJsonResponse(
    val ocs: OcsJsonPayload,
)

@Serializable
internal data class OcsJsonPayload(
    val meta: OcsMeta,
    val data: JsonElement,
)

@Serializable
internal data class OcsMeta(
    val status: String? = null,
    @SerialName("statuscode")
    val statusCode: Int,
    val message: String? = null,
)
