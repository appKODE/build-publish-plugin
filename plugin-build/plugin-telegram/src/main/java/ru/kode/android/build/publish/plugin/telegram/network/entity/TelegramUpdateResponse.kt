package ru.kode.android.build.publish.plugin.telegram.network.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class TelegramUpdateResponse(
    val ok: Boolean,
    val result: List<TelegramUpdate>,
)
