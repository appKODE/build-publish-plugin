package ru.kode.android.build.publish.plugin.telegram.network.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Suppress("ConstructorParameterNaming") // network model
internal data class TelegramUploadResponse(
    val ok: Boolean,
    @SerialName("error_code")
    val errorCode: String? = null,
)
