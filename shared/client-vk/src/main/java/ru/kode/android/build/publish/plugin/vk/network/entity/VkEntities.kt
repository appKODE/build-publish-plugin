package ru.kode.android.build.publish.plugin.vk.network.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class VkResponse<T>(
    val response: T? = null,
    val error: VkError? = null,
)

@Serializable
internal data class VkError(
    @SerialName("error_code")
    val code: Int? = null,
    @SerialName("error_msg")
    val message: String? = null,
)

@Serializable
internal data class VkUploadServer(
    @SerialName("upload_url")
    val uploadUrl: String,
)

@Serializable
internal data class VkUploadResult(
    val file: String? = null,
    val error: String? = null,
    @SerialName("error_descr")
    val errorDescription: String? = null,
)

@Serializable
internal data class VkDocSaveResult(
    val type: String? = null,
    val doc: VkDoc,
)

@Serializable
internal data class VkDoc(
    val id: Long,
    @SerialName("owner_id")
    val ownerId: Long,
    val url: String? = null,
    @SerialName("access_key")
    val accessKey: String? = null,
)
