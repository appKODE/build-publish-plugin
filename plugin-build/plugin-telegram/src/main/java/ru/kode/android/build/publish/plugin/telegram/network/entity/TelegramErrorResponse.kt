package ru.kode.android.build.publish.plugin.telegram.network.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class TelegramErrorResponse(
    val ok: Boolean,
    val error_code: Int,
    val description: String,
    val parameters: TelegramErrorParameters? = null
)