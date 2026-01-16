package ru.kode.android.build.publish.plugin.telegram.network.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response payload for Telegram Bot API `sendDocument`.
 */
@Serializable
@Suppress("ConstructorParameterNaming") // network model
internal data class TelegramUploadResponse(
    val ok: Boolean,
    @SerialName("error_code")
    val errorCode: String? = null,
)
